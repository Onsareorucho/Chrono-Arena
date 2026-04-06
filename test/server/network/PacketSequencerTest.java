package server.network;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PacketSequencer — the component that detects duplicate and
 * out-of-order UDP packets.
 *
 * These tests cover the correctness requirements from the spec:
 *  "Remember to have a logic for out of order and/or duplicate packets."
 */
class PacketSequencerTest {

    private PacketSequencer sequencer;

    @BeforeEach
    void setUp() {
        sequencer = new PacketSequencer();
    }

    // ── Basic accept ───────────────────────────────────────────

    @Test
    void firstPacket_alwaysAccepted() {
        assertTrue(sequencer.accept(1, 0L));
    }

    @Test
    void sequentialPackets_allAccepted() {
        for (long seq = 0; seq < 100; seq++) {
            assertTrue(sequencer.accept(1, seq),
                    "Packet " + seq + " should be accepted");
        }
    }

    // ── Duplicate detection ────────────────────────────────────

    @Test
    void duplicate_sameSequenceNumber_rejected() {
        assertTrue(sequencer.accept(1, 5L));
        assertFalse(sequencer.accept(1, 5L), "Exact duplicate should be rejected");
    }

    @Test
    void duplicate_withinWindow_rejected() {
        // Accept 0..20
        for (long seq = 0; seq <= 20; seq++) sequencer.accept(1, seq);
        // Resend seq=10 — within window, already seen
        assertFalse(sequencer.accept(1, 10L));
    }

    // ── Out-of-order acceptance ────────────────────────────────

    @Test
    void slightlyOutOfOrder_withinWindow_accepted() {
        sequencer.accept(1, 10L);
        // seq=8 arrives late but is within the 32-packet window
        assertTrue(sequencer.accept(1, 8L), "Late packet within window should be accepted");
    }

    @Test
    void outOfOrder_alreadySeen_rejected() {
        sequencer.accept(1, 10L);
        sequencer.accept(1, 8L);   // accepted first time
        assertFalse(sequencer.accept(1, 8L), "Duplicate of late packet should be rejected");
    }

    // ── Stale packet dropping ──────────────────────────────────

    @Test
    void veryStalePacket_outsideWindow_dropped() {
        // Advance to seq=50
        for (long seq = 0; seq <= 50; seq++) sequencer.accept(1, seq);
        // seq=5 is 45 behind current — outside the 32-packet window
        assertFalse(sequencer.accept(1, 5L), "Packet outside window should be dropped");
    }

    @Test
    void packetExactlyAtWindowEdge_accepted() {
        // Advance to seq=32
        for (long seq = 0; seq <= 32; seq++) sequencer.accept(1, seq);
        // seq=1 is exactly 31 behind (within window of 32)
        assertTrue(sequencer.accept(1, 1L), "Packet at window edge should be accepted");
    }

    @Test
    void packetJustOutsideWindowEdge_dropped() {
        // Advance to seq=33
        for (long seq = 0; seq <= 33; seq++) sequencer.accept(1, seq);
        // seq=0 is 33 behind — just outside the 32-packet window
        assertFalse(sequencer.accept(1, 0L), "Packet just outside window should be dropped");
    }

    // ── Large jump (client restart) ────────────────────────────

    @Test
    void largeSequenceJump_treatedAsClientRestart_accepted() {
        sequencer.accept(1, 5L);
        // Client restarted — sequence reset to 0, then jumps to a new session
        assertTrue(sequencer.accept(1, 1000L), "Large forward jump should be accepted");
    }

    @Test
    void afterLargeJump_previousSessionPackets_dropped() {
        sequencer.accept(1, 5L);
        sequencer.accept(1, 1000L); // client restart
        // Old session packet — should be dropped (1000 - 5 = 995, way outside window)
        assertFalse(sequencer.accept(1, 5L), "Old session packet should be dropped after restart");
    }

    // ── Multi-player isolation ─────────────────────────────────

    @Test
    void differentPlayers_independentSequenceWindows() {
        // Player 1 at seq=50
        for (long seq = 0; seq <= 50; seq++) sequencer.accept(1, seq);

        // Player 2 has their own counter starting at 0
        assertTrue(sequencer.accept(2, 0L), "Player 2 should have own sequence window");
        assertTrue(sequencer.accept(2, 1L));

        // Player 1 duplicate still rejected
        assertFalse(sequencer.accept(1, 10L));
    }

    @Test
    void removePlayer_clearsState() {
        sequencer.accept(1, 0L);
        sequencer.accept(1, 1L);
        sequencer.removePlayer(1);

        // After removal, sequence resets — seq=0 should be accepted again
        assertTrue(sequencer.accept(1, 0L), "After removePlayer, sequence should reset");
    }

    // ── Reset ──────────────────────────────────────────────────

    @Test
    void reset_clearsAllPlayers() {
        sequencer.accept(1, 100L);
        sequencer.accept(2, 200L);
        sequencer.reset();

        assertTrue(sequencer.accept(1, 0L), "After reset, player 1 sequence should restart");
        assertTrue(sequencer.accept(2, 0L), "After reset, player 2 sequence should restart");
    }

    // ── Edge: seq=0 first packet ───────────────────────────────

    @Test
    void sequenceZero_firstPacket_accepted() {
        assertTrue(sequencer.accept(1, 0L));
    }

    @Test
    void sequenceZero_duplicate_rejected() {
        sequencer.accept(1, 0L);
        assertFalse(sequencer.accept(1, 0L));
    }
}
