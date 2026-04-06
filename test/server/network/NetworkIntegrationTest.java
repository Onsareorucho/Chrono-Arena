package server.network;

import client.network.TCPClientHandler;
import client.network.UDPClientHandler;
import org.junit.jupiter.api.*;
import shared.Direction;
import shared.protocol.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests using real loopback sockets.
 *
 * These tests actually bind ports, connect real clients, and exchange
 * messages — proving that the TCP framing, JSON serialization, UDP
 * delivery, and sequence tracking all work together correctly.
 *
 * Ports 19100/19101 are used (unlikely to conflict with anything).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NetworkIntegrationTest {

    private static final int TCP_PORT = 19100;
    private static final int UDP_PORT = 19101;

    private TCPServerHandler tcpServer;
    private UDPServerHandler udpServer;

    @BeforeEach
    void startServer() throws IOException, InterruptedException {
        tcpServer = new TCPServerHandler(TCP_PORT, UDP_PORT);
        udpServer = new UDPServerHandler(UDP_PORT);
        tcpServer.start();
        udpServer.start();
        Thread.sleep(100); // give sockets time to bind
    }

    @AfterEach
    void stopServer() {
        tcpServer.stop();
        udpServer.stop();
    }

    // ── Join flow ──────────────────────────────────────────────

    @Test
    @Order(1)
    void client_join_receivesPlayerId() throws Exception {
        TCPClientHandler client = new TCPClientHandler("localhost", TCP_PORT, "Bob");
        JoinResponse jr = client.connect();

        assertTrue(jr.accepted, "Join should be accepted");
        assertTrue(jr.assignedPlayerId >= 1, "Should receive a valid player ID");
        assertEquals(UDP_PORT, jr.udpPort, "Should receive the correct UDP port");

        client.stop();
    }

    @Test
    @Order(2)
    void multipleClients_receiveUniquePlayerIds() throws Exception {
        TCPClientHandler c1 = new TCPClientHandler("localhost", TCP_PORT, "Alice");
        TCPClientHandler c2 = new TCPClientHandler("localhost", TCP_PORT, "Bob");
        TCPClientHandler c3 = new TCPClientHandler("localhost", TCP_PORT, "Carol");

        JoinResponse r1 = c1.connect();
        JoinResponse r2 = c2.connect();
        JoinResponse r3 = c3.connect();

        assertTrue(r1.accepted);
        assertTrue(r2.accepted);
        assertTrue(r3.accepted);

        // All IDs must be distinct
        assertNotEquals(r1.assignedPlayerId, r2.assignedPlayerId);
        assertNotEquals(r2.assignedPlayerId, r3.assignedPlayerId);
        assertNotEquals(r1.assignedPlayerId, r3.assignedPlayerId);

        c1.stop(); c2.stop(); c3.stop();
    }

    // ── Server → Client broadcast ──────────────────────────────

    @Test
    @Order(3)
    void server_broadcastScoreUpdate_clientReceivesIt() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ScoreUpdate> received = new AtomicReference<>();

        TCPClientHandler client = new TCPClientHandler("localhost", TCP_PORT, "Dave");
        JoinResponse jr = client.connect();
        client.onScoreUpdate = update -> {
            received.set(update);
            latch.countDown();
        };
        client.start();

        // Give reader thread a moment to start
        Thread.sleep(50);

        // Server broadcasts a score update
        tcpServer.broadcast(new ScoreUpdate(Map.of(jr.assignedPlayerId, 250)));

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Client should receive ScoreUpdate within 2s");
        assertNotNull(received.get());
        assertEquals(250, received.get().scores.get(jr.assignedPlayerId));

        client.stop();
    }

    @Test
    @Order(4)
    void server_broadcastPlayerEvent_allClientsReceiveIt() throws Exception {
        int clientCount = 3;
        CountDownLatch latch = new CountDownLatch(clientCount);
        List<PlayerEvent> events = new ArrayList<>();

        List<TCPClientHandler> clients = new ArrayList<>();
        for (int i = 0; i < clientCount; i++) {
            TCPClientHandler c = new TCPClientHandler("localhost", TCP_PORT, "Player" + i);
            c.connect();
            c.onPlayerEvent = evt -> {
                if (evt.eventType == PlayerEvent.EventType.FROZEN) {
                    synchronized (events) { events.add(evt); }
                    latch.countDown();
                }
            };
            c.start();
            clients.add(c);
        }

        Thread.sleep(100); // let all reader threads start

        tcpServer.broadcast(new PlayerEvent(PlayerEvent.EventType.FROZEN, 99, "Test freeze"));

        assertTrue(latch.await(3, TimeUnit.SECONDS), "All clients should receive FROZEN event");
        assertEquals(clientCount, events.size());
        assertTrue(events.stream().allMatch(e -> e.playerId == 99));

        clients.forEach(TCPClientHandler::stop);
    }

    // ── KILL_SWITCH ────────────────────────────────────────────

    @Test
    @Order(5)
    void killSwitch_clientReceivesKillMessage() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<KillClient> killMsg = new AtomicReference<>();

        TCPClientHandler client = new TCPClientHandler("localhost", TCP_PORT, "Erratic");
        JoinResponse jr = client.connect();
        client.onKillReceived = msg -> {
            killMsg.set(msg);
            latch.countDown();
        };
        client.start();

        Thread.sleep(50);

        tcpServer.kill(jr.assignedPlayerId, "Too many bad packets");

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Client should receive KillClient message");
        assertEquals("Too many bad packets", killMsg.get().reason);

        client.stop();
    }

    // ── UDP action delivery ────────────────────────────────────

    @Test
    @Order(6)
    void udpClient_sendsMove_serverActionQueueReceivesIt() throws Exception {
        CountDownLatch latch = new CountDownLatch(3);
        List<ActionMessage> received = new ArrayList<>();

        udpServer.onActionReceived = action -> {
            synchronized (received) { received.add(action); }
            latch.countDown();
        };

        TCPClientHandler tcp = new TCPClientHandler("localhost", TCP_PORT, "Mover");
        JoinResponse jr = tcp.connect();

        UDPClientHandler udp = new UDPClientHandler("localhost", jr.udpPort, jr.assignedPlayerId);
        udp.start();

        // Send 3 move packets
        udp.sendMove(Direction.UP);
        udp.sendMove(Direction.RIGHT);
        udp.sendMove(Direction.DOWN);

        assertTrue(latch.await(3, TimeUnit.SECONDS), "Server should receive 3 UDP move packets");
        assertEquals(3, received.size());
        // UDP doesn't guarantee ordering — just verify all 3 arrived with correct type
        assertTrue(received.stream().allMatch(a -> a.actionType == ActionMessage.ActionType.MOVE));
        assertTrue(received.stream().anyMatch(a -> a.direction == Direction.UP));
        assertTrue(received.stream().anyMatch(a -> a.direction == Direction.RIGHT));
        assertTrue(received.stream().anyMatch(a -> a.direction == Direction.DOWN));

        udp.stop();
        tcp.stop();
    }

    @Test
    @Order(7)
    void udpClient_sendsAttack_serverReceivesIt() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ActionMessage> received = new AtomicReference<>();

        udpServer.onActionReceived = action -> {
            received.set(action);
            latch.countDown();
        };

        TCPClientHandler tcp = new TCPClientHandler("localhost", TCP_PORT, "Attacker");
        JoinResponse jr = tcp.connect();

        UDPClientHandler udp = new UDPClientHandler("localhost", jr.udpPort, jr.assignedPlayerId);
        udp.start();
        udp.sendAttack();

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(ActionMessage.ActionType.ATTACK, received.get().actionType);
        assertEquals(Direction.NONE, received.get().direction);

        udp.stop();
        tcp.stop();
    }

    // ── UDP duplicate rejection ────────────────────────────────

    @Test
    @Order(8)
    void udp_duplicatePackets_onlyFirstDeliveredToQueue() throws Exception {
        AtomicInteger deliveredCount = new AtomicInteger(0);
        // Only count packets from player 999 so other test traffic doesn't interfere
        udpServer.onActionReceived = action -> {
            if (action.playerId == 999) deliveredCount.incrementAndGet();
        };

        TCPClientHandler tcp = new TCPClientHandler("localhost", TCP_PORT, "DupTest");
        JoinResponse jr = tcp.connect();

        UDPClientHandler udp = new UDPClientHandler("localhost", jr.udpPort, jr.assignedPlayerId);
        udp.start();

        final int pid = jr.assignedPlayerId;
        udpServer.onActionReceived = action -> {
            if (action.playerId == pid) deliveredCount.incrementAndGet();
        };

        // Send the same sequence number twice by building ActionMessages manually
        ActionMessage first  = ActionMessage.move(jr.assignedPlayerId, 77L, Direction.UP);
        ActionMessage second = ActionMessage.move(jr.assignedPlayerId, 77L, Direction.DOWN); // same seqNum!

        udp.send(first);
        Thread.sleep(50);
        udp.send(second); // should be dropped

        Thread.sleep(300); // wait for any delayed delivery

        // Only 1 should have been delivered
        assertEquals(1, deliveredCount.get(),
                "Duplicate UDP packet should be dropped by PacketSequencer");

        udp.stop();
        tcp.stop();
    }

    // ── Player join/leave callbacks ────────────────────────────

    @Test
    @Order(9)
    void playerJoin_triggerServerCallback() throws Exception {
        CountDownLatch joinLatch = new CountDownLatch(1);
        AtomicInteger joinedId = new AtomicInteger(-1);

        tcpServer.onPlayerJoined = id -> {
            joinedId.set(id);
            joinLatch.countDown();
        };

        TCPClientHandler client = new TCPClientHandler("localhost", TCP_PORT, "JoinTest");
        JoinResponse jr = client.connect();

        assertTrue(joinLatch.await(2, TimeUnit.SECONDS), "onPlayerJoined should fire");
        assertEquals(jr.assignedPlayerId, joinedId.get());

        client.stop();
    }

    @Test
    @Order(10)
    void playerDisconnect_triggerServerCallback() throws Exception {
        CountDownLatch leaveLatch = new CountDownLatch(1);
        AtomicInteger leftId = new AtomicInteger(-1);

        tcpServer.onPlayerLeft = id -> {
            leftId.set(id);
            leaveLatch.countDown();
        };

        TCPClientHandler client = new TCPClientHandler("localhost", TCP_PORT, "LeaveTest");
        JoinResponse jr = client.connect();
        client.start();
        Thread.sleep(50);

        client.stop(); // disconnect

        assertTrue(leaveLatch.await(3, TimeUnit.SECONDS), "onPlayerLeft should fire after disconnect");
        assertEquals(jr.assignedPlayerId, leftId.get());
    }

    // ── GameOver broadcast ─────────────────────────────────────

    @Test
    @Order(11)
    void gameOver_clientReceivesWinner() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GameOver> goMsg = new AtomicReference<>();

        TCPClientHandler client = new TCPClientHandler("localhost", TCP_PORT, "Finalist");
        client.connect();
        client.onGameOver = go -> {
            goMsg.set(go);
            latch.countDown();
        };
        client.start();

        Thread.sleep(50);
        tcpServer.broadcast(new GameOver(Map.of(1, 500, 2, 200), 1));

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(1, goMsg.get().winnerPlayerId);
        assertEquals(500, goMsg.get().finalScores.get(1));

        client.stop();
    }
}
