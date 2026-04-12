package server.network;

import client.network.TCPClientHandler;
import client.network.UDPClientHandler;
import server.logic.ActionQueue;
import shared.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Network integration tests for ChronoArena.
 *
 * Runs as a plain Java program (no JUnit needed) so it works regardless
 * of whether Maven can download JUnit from the internet.
 *
 * Run with:
 *   java -cp target/classes server.network.NetworkIntegrationTest
 *
 * Each test prints PASS or FAIL with a description.
 */
public class NetworkIntegrationTest {

    private static final int TCP_PORT = 19100;
    private static final int UDP_PORT = 19101;

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== ChronoArena Network Integration Tests ===\n");

        test_join_receivesPlayerId();
        test_multipleClients_uniqueIds();
        test_broadcast_clientReceivesIt();
        test_killSwitch_clientReceivesKick();
        test_udp_inputReachesActionQueue();
        test_playerJoin_triggersCallback();
        test_playerDisconnect_triggersCallback();

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
    }

    // ── Test 1 ─────────────────────────────────────────────────

    static void test_join_receivesPlayerId() throws Exception {
        ActionQueue queue = new ActionQueue();
        TCPServerHandler server = new TCPServerHandler(TCP_PORT, UDP_PORT);
        UDPServerHandler udpServer = new UDPServerHandler(UDP_PORT, queue);
        server.start(); udpServer.start();
        Thread.sleep(100);

        try {
            TCPClientHandler client = new TCPClientHandler("localhost", TCP_PORT);
            JoinResponse jr = client.connect("Bob");

            check("join: playerId >= 1", jr.getPlayerId() >= 1);
            check("join: udpPort correct", jr.getServerUdpPort() == UDP_PORT);
            client.stop();
        } finally {
            server.stop(); udpServer.stop(); Thread.sleep(100);
        }
    }

    // ── Test 2 ─────────────────────────────────────────────────

    static void test_multipleClients_uniqueIds() throws Exception {
        ActionQueue queue = new ActionQueue();
        TCPServerHandler server = new TCPServerHandler(TCP_PORT + 2, UDP_PORT + 2);
        UDPServerHandler udpServer = new UDPServerHandler(UDP_PORT + 2, queue);
        server.start(); udpServer.start();
        Thread.sleep(100);

        try {
            TCPClientHandler c1 = new TCPClientHandler("localhost", TCP_PORT + 2);
            TCPClientHandler c2 = new TCPClientHandler("localhost", TCP_PORT + 2);
            TCPClientHandler c3 = new TCPClientHandler("localhost", TCP_PORT + 2);

            JoinResponse r1 = c1.connect("Alice");
            JoinResponse r2 = c2.connect("Bob");
            JoinResponse r3 = c3.connect("Carol");

            check("unique ids: 1 != 2", r1.getPlayerId() != r2.getPlayerId());
            check("unique ids: 2 != 3", r2.getPlayerId() != r3.getPlayerId());
            check("unique ids: 1 != 3", r1.getPlayerId() != r3.getPlayerId());

            c1.stop(); c2.stop(); c3.stop();
        } finally {
            server.stop(); udpServer.stop(); Thread.sleep(100);
        }
    }

    // ── Test 3 ─────────────────────────────────────────────────

    static void test_broadcast_clientReceivesIt() throws Exception {
        ActionQueue queue = new ActionQueue();
        TCPServerHandler server = new TCPServerHandler(TCP_PORT + 4, UDP_PORT + 4);
        UDPServerHandler udpServer = new UDPServerHandler(UDP_PORT + 4, queue);
        server.start(); udpServer.start();
        Thread.sleep(100);

        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<GameEvent> received = new AtomicReference<>();

            TCPClientHandler client = new TCPClientHandler("localhost", TCP_PORT + 4);
            client.connect("Dave");
            client.onGameEvent = evt -> { received.set(evt); latch.countDown(); };
            client.start();
            Thread.sleep(50);

            GameEvent event = GameEvent.zoneCaptured(1, 0, 5.0f, 5.0f);
            server.broadcast(new GameMessage(MessageType.GAME_EVENT, event));

            boolean ok = latch.await(2, TimeUnit.SECONDS);
            check("broadcast: client received GameEvent", ok && received.get() != null);
            client.stop();
        } finally {
            server.stop(); udpServer.stop(); Thread.sleep(100);
        }
    }

    // ── Test 4 ─────────────────────────────────────────────────

    static void test_killSwitch_clientReceivesKick() throws Exception {
        ActionQueue queue = new ActionQueue();
        TCPServerHandler server = new TCPServerHandler(TCP_PORT + 6, UDP_PORT + 6);
        UDPServerHandler udpServer = new UDPServerHandler(UDP_PORT + 6, queue);
        server.start(); udpServer.start();
        Thread.sleep(100);

        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<KickNotification> kick = new AtomicReference<>();

            TCPClientHandler client = new TCPClientHandler("localhost", TCP_PORT + 6);
            JoinResponse jr = client.connect("Erratic");
            client.onKickReceived = k -> { kick.set(k); latch.countDown(); };
            client.start();
            Thread.sleep(50);

            server.kill(jr.getPlayerId(), "Test kick");

            boolean ok = latch.await(2, TimeUnit.SECONDS);
            check("killswitch: client received KickNotification", ok && kick.get() != null);
            client.stop();
        } finally {
            server.stop(); udpServer.stop(); Thread.sleep(100);
        }
    }

    // ── Test 5 ─────────────────────────────────────────────────

    static void test_udp_inputReachesActionQueue() throws Exception {
        ActionQueue queue = new ActionQueue();
        TCPServerHandler server = new TCPServerHandler(TCP_PORT + 8, UDP_PORT + 8);
        UDPServerHandler udpServer = new UDPServerHandler(UDP_PORT + 8, queue);
        server.start(); udpServer.start();
        Thread.sleep(100);

        try {
            TCPClientHandler tcp = new TCPClientHandler("localhost", TCP_PORT + 8);
            JoinResponse jr = tcp.connect("Mover");

            UDPClientHandler udp = new UDPClientHandler("localhost", jr.getServerUdpPort(), jr.getPlayerId());
            udp.start();

            udp.sendInput(PlayerInput.move(0, -1));
            udp.sendInput(PlayerInput.move(1,  0));
            udp.sendInput(PlayerInput.move(0,  1));
            Thread.sleep(500);

            List<GameMessage> actions = new ArrayList<>();
            queue.drainTo(actions);
            check("udp: at least 3 inputs reached ActionQueue", actions.size() >= 3);
            check("udp: all are PLAYER_INPUT type",
                    actions.stream().allMatch(a -> a.getType() == MessageType.PLAYER_INPUT));

            udp.stop(); tcp.stop();
        } finally {
            server.stop(); udpServer.stop(); Thread.sleep(100);
        }
    }

    // ── Test 6 ─────────────────────────────────────────────────

    static void test_playerJoin_triggersCallback() throws Exception {
        ActionQueue queue = new ActionQueue();
        TCPServerHandler server = new TCPServerHandler(TCP_PORT + 10, UDP_PORT + 10);
        UDPServerHandler udpServer = new UDPServerHandler(UDP_PORT + 10, queue);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger joinedId = new AtomicInteger(-1);
        server.onPlayerJoined = (id, name) -> { joinedId.set(id); latch.countDown(); };

        server.start(); udpServer.start();
        Thread.sleep(100);

        try {
            TCPClientHandler client = new TCPClientHandler("localhost", TCP_PORT + 10);
            JoinResponse jr = client.connect("JoinTest");

            boolean ok = latch.await(2, TimeUnit.SECONDS);
            check("callback: onPlayerJoined fired", ok);
            check("callback: correct playerId", joinedId.get() == jr.getPlayerId());
            client.stop();
        } finally {
            server.stop(); udpServer.stop(); Thread.sleep(100);
        }
    }

    // ── Test 7 ─────────────────────────────────────────────────

    static void test_playerDisconnect_triggersCallback() throws Exception {
        ActionQueue queue = new ActionQueue();
        TCPServerHandler server = new TCPServerHandler(TCP_PORT + 12, UDP_PORT + 12);
        UDPServerHandler udpServer = new UDPServerHandler(UDP_PORT + 12, queue);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger leftId = new AtomicInteger(-1);
        server.onPlayerLeft = id -> { leftId.set(id); latch.countDown(); };

        server.start(); udpServer.start();
        Thread.sleep(100);

        try {
            TCPClientHandler client = new TCPClientHandler("localhost", TCP_PORT + 12);
            JoinResponse jr = client.connect("LeaveTest");
            client.start();
            Thread.sleep(50);
            client.stop();

            boolean ok = latch.await(3, TimeUnit.SECONDS);
            check("callback: onPlayerLeft fired after disconnect", ok);
            check("callback: correct playerId", leftId.get() == jr.getPlayerId());
        } finally {
            server.stop(); udpServer.stop(); Thread.sleep(100);
        }
    }

    // ── Helpers ────────────────────────────────────────────────

    static void check(String description, boolean condition) {
        if (condition) {
            System.out.println("  PASS: " + description);
            passed++;
        } else {
            System.out.println("  FAIL: " + description);
            failed++;
        }
    }
}
