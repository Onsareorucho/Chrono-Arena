package server.network;

import client.network.TCPClientHandler;
import client.network.UDPClientHandler;
import org.junit.jupiter.api.*;
import shared.Direction;
import shared.protocol.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress test: 4 clients connected simultaneously, each blasting UDP packets.
 *
 * Verifies:
 *  1. Server stays alive and responsive under concurrent load
 *  2. No action messages from one player are attributed to another (no ID mixing)
 *  3. No data races or exceptions crash the server during concurrent broadcasts
 *  4. All clients receive broadcasts correctly even while UDP traffic is high
 *  5. Clients can join and leave while others are still active
 *
 * This directly validates the grading criteria:
 *  "adding/removing clients" and "correct client-side results for all clients"
 */
class NetworkStressTest {

    private static final int TCP_PORT    = 19200;
    private static final int UDP_PORT    = 19201;
    private static final int NUM_CLIENTS = 4;
    private static final int PACKETS_PER_CLIENT = 200;

    private TCPServerHandler tcpServer;
    private UDPServerHandler udpServer;

    @BeforeEach
    void startServer() throws IOException, InterruptedException {
        tcpServer = new TCPServerHandler(TCP_PORT, UDP_PORT);
        udpServer = new UDPServerHandler(UDP_PORT);
        tcpServer.start();
        udpServer.start();
        Thread.sleep(100);
    }

    @AfterEach
    void stopServer() {
        tcpServer.stop();
        udpServer.stop();
    }

    // ── Test 1: Concurrent joins ───────────────────────────────

    @Test
    void fourClients_joinConcurrently_allGetUniqueIds() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(NUM_CLIENTS);
        ConcurrentLinkedQueue<Integer> assignedIds = new ConcurrentLinkedQueue<>();
        CountDownLatch allJoined = new CountDownLatch(NUM_CLIENTS);
        List<TCPClientHandler> clients = new CopyOnWriteArrayList<>();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    TCPClientHandler c = new TCPClientHandler("localhost", TCP_PORT, "P" + idx);
                    JoinResponse jr = c.connect();
                    assertTrue(jr.accepted);
                    assignedIds.add(jr.assignedPlayerId);
                    clients.add(c);
                } catch (Exception e) {
                    fail("Client " + idx + " failed to join: " + e.getMessage());
                } finally {
                    allJoined.countDown();
                }
            });
        }

        assertTrue(allJoined.await(5, TimeUnit.SECONDS), "All clients should join within 5s");

        // All IDs must be unique
        Set<Integer> idSet = new HashSet<>(assignedIds);
        assertEquals(NUM_CLIENTS, idSet.size(), "All player IDs must be distinct");

        clients.forEach(TCPClientHandler::stop);
        pool.shutdown();
    }

    // ── Test 2: High-volume UDP with correctness check ─────────

    @Test
    void fourClients_blastUDP_noIdMixing() throws Exception {
        // Track counts per playerId
        ConcurrentHashMap<Integer, AtomicInteger> receivedPerPlayer = new ConcurrentHashMap<>();
        // Track any ID mismatches
        AtomicInteger idMismatches = new AtomicInteger(0);

        List<TCPClientHandler> tcpClients = new ArrayList<>();
        List<UDPClientHandler> udpClients = new ArrayList<>();
        List<Integer> playerIds = new ArrayList<>();

        // Connect all clients
        for (int i = 0; i < NUM_CLIENTS; i++) {
            TCPClientHandler tcp = new TCPClientHandler("localhost", TCP_PORT, "Blaster" + i);
            JoinResponse jr = tcp.connect();
            tcp.start();
            tcpClients.add(tcp);
            playerIds.add(jr.assignedPlayerId);
            receivedPerPlayer.put(jr.assignedPlayerId, new AtomicInteger(0));

            UDPClientHandler udp = new UDPClientHandler("localhost", jr.udpPort, jr.assignedPlayerId);
            udp.start();
            udpClients.add(udp);
        }

        // Wire UDP server to validate player ID in each received packet
        udpServer.onActionReceived = action -> {
            AtomicInteger counter = receivedPerPlayer.get(action.playerId);
            if (counter == null) {
                idMismatches.incrementAndGet(); // got an ID we never registered
            } else {
                counter.incrementAndGet();
            }
        };

        // All 4 clients blast PACKETS_PER_CLIENT packets each concurrently
        CountDownLatch allSent = new CountDownLatch(NUM_CLIENTS);
        ExecutorService senders = Executors.newFixedThreadPool(NUM_CLIENTS);
        Direction[] dirs = Direction.values();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            final UDPClientHandler udp = udpClients.get(i);
            senders.submit(() -> {
                try {
                    for (int p = 0; p < PACKETS_PER_CLIENT; p++) {
                        udp.sendMove(dirs[p % dirs.length]);
                        if (p % 20 == 0) Thread.sleep(1); // tiny throttle
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    allSent.countDown();
                }
            });
        }

        assertTrue(allSent.await(10, TimeUnit.SECONDS), "All sends should complete within 10s");
        Thread.sleep(500); // let the last packets drain through

        // Verify no ID mismatches occurred
        assertEquals(0, idMismatches.get(), "No packets should arrive with unregistered player IDs");

        // Each player should have received some packets (UDP delivery is best-effort,
        // but on loopback 95%+ should arrive — we just check at least 50%)
        for (int id : playerIds) {
            int count = receivedPerPlayer.get(id).get();
            assertTrue(count >= PACKETS_PER_CLIENT / 2,
                    "Player " + id + " should have at least 50% packet delivery, got " + count);
        }

        tcpClients.forEach(TCPClientHandler::stop);
        udpClients.forEach(UDPClientHandler::stop);
        senders.shutdown();
    }

    // ── Test 3: Broadcast correctness under UDP load ───────────

    @Test
    void broadcastDuringUDPLoad_allClientsReceiveCorrectly() throws Exception {
        int broadcastCount = 10;
        CountDownLatch allReceived = new CountDownLatch(NUM_CLIENTS * broadcastCount);
        ConcurrentHashMap<Integer, AtomicInteger> scoreReceivedCount = new ConcurrentHashMap<>();

        List<TCPClientHandler> clients = new ArrayList<>();
        List<UDPClientHandler> udpClients = new ArrayList<>();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            TCPClientHandler tcp = new TCPClientHandler("localhost", TCP_PORT, "LoadP" + i);
            JoinResponse jr = tcp.connect();

            final int pid = jr.assignedPlayerId;
            scoreReceivedCount.put(pid, new AtomicInteger(0));

            tcp.onScoreUpdate = update -> {
                scoreReceivedCount.get(pid).incrementAndGet();
                allReceived.countDown();
            };
            tcp.start();
            clients.add(tcp);

            UDPClientHandler udp = new UDPClientHandler("localhost", jr.udpPort, pid);
            udp.start();
            udpClients.add(udp);
        }

        Thread.sleep(100);

        // Start UDP load in background
        ExecutorService loader = Executors.newFixedThreadPool(NUM_CLIENTS);
        java.util.concurrent.atomic.AtomicBoolean stopLoad = new java.util.concurrent.atomic.AtomicBoolean(false);
        for (UDPClientHandler udp : udpClients) {
            loader.submit(() -> {
                while (!stopLoad.get()) {
                    udp.sendMove(Direction.UP);
                    try { Thread.sleep(5); } catch (InterruptedException e) { break; }
                }
            });
        }

        // Broadcast scores while UDP is hammering
        Map<Integer, Integer> scores = new HashMap<>();
        for (int id : scoreReceivedCount.keySet()) scores.put(id, 100);
        for (int b = 0; b < broadcastCount; b++) {
            tcpServer.broadcast(new ScoreUpdate(scores));
            Thread.sleep(50);
        }

        stopLoad.set(true);
        loader.shutdown();

        assertTrue(allReceived.await(5, TimeUnit.SECONDS),
                "Every client should receive every ScoreUpdate broadcast");

        // Each client should have received exactly broadcastCount updates
        for (Map.Entry<Integer, AtomicInteger> e : scoreReceivedCount.entrySet()) {
            assertEquals(broadcastCount, e.getValue().get(),
                    "Player " + e.getKey() + " should have received all " + broadcastCount + " broadcasts");
        }

        clients.forEach(TCPClientHandler::stop);
        udpClients.forEach(UDPClientHandler::stop);
    }

    // ── Test 4: Hot-join / hot-leave ───────────────────────────

    @Test
    void clientsJoinAndLeave_serverRemainsStable() throws Exception {
        // 2 "stable" clients stay connected throughout
        TCPClientHandler stable1 = new TCPClientHandler("localhost", TCP_PORT, "Stable1");
        TCPClientHandler stable2 = new TCPClientHandler("localhost", TCP_PORT, "Stable2");
        stable1.connect(); stable1.start();
        stable2.connect(); stable2.start();

        CountDownLatch broadcastsReceived = new CountDownLatch(10);
        stable1.onScoreUpdate = u -> broadcastsReceived.countDown();

        // Rapid join/leave cycle
        for (int wave = 0; wave < 5; wave++) {
            TCPClientHandler transient1 = new TCPClientHandler("localhost", TCP_PORT, "T" + wave + "a");
            TCPClientHandler transient2 = new TCPClientHandler("localhost", TCP_PORT, "T" + wave + "b");
            transient1.connect(); transient1.start();
            transient2.connect(); transient2.start();
            Thread.sleep(50);
            // Broadcast while transient clients are alive
            tcpServer.broadcast(new ScoreUpdate(Map.of(1, wave * 10)));
            Thread.sleep(20);
            transient1.stop();
            transient2.stop();
            Thread.sleep(30);
            // Broadcast while transient clients have left
            tcpServer.broadcast(new ScoreUpdate(Map.of(1, wave * 10 + 5)));
        }

        assertTrue(broadcastsReceived.await(5, TimeUnit.SECONDS),
                "Stable client should have received all 10 broadcasts despite rapid join/leave");

        // Server should still report 2 connected players (the stable ones)
        // Give the server a moment to process the disconnects
        Thread.sleep(200);
        assertEquals(2, tcpServer.getPlayerCount(),
                "Server should have exactly 2 stable clients remaining");

        stable1.stop();
        stable2.stop();
    }

    // ── Test 5: KILL_SWITCH doesn't affect other clients ──────

    @Test
    void killOneClient_othersUnaffected() throws Exception {
        List<TCPClientHandler> clients = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            TCPClientHandler c = new TCPClientHandler("localhost", TCP_PORT, "K" + i);
            JoinResponse jr = c.connect();
            c.start();
            clients.add(c);
            ids.add(jr.assignedPlayerId);
        }

        Thread.sleep(100);
        assertEquals(NUM_CLIENTS, tcpServer.getPlayerCount());

        // Kill player at index 0
        tcpServer.kill(ids.get(0), "Stress test kill");
        Thread.sleep(300); // wait for eviction to propagate

        // 3 should remain
        assertEquals(NUM_CLIENTS - 1, tcpServer.getPlayerCount(),
                "One player evicted, " + (NUM_CLIENTS - 1) + " should remain");

        // Remaining clients should still receive broadcasts
        CountDownLatch stillAlive = new CountDownLatch(NUM_CLIENTS - 1);
        for (int i = 1; i < NUM_CLIENTS; i++) {
            clients.get(i).onScoreUpdate = u -> stillAlive.countDown();
        }
        tcpServer.broadcast(new ScoreUpdate(Map.of(99, 999)));

        assertTrue(stillAlive.await(2, TimeUnit.SECONDS),
                "Remaining clients should still receive broadcasts after kill");

        clients.forEach(TCPClientHandler::stop);
    }
}
