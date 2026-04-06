package server.network;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player UDP sequence numbers to detect:
 *  1. Duplicate packets  — same seqNum seen twice from same player
 *  2. Out-of-order delivery — seqNum is older than the last accepted one
 *
 * Design decisions:
 *  - We use a "sliding window" rather than strict ordering.
 *    Strict ordering would cause packet loss to block all later inputs;
 *    a window lets slightly-late packets still be accepted.
 *  - Window size is configurable (default 32). Anything older than
 *    (lastAccepted - WINDOW) is silently dropped as a duplicate.
 *  - This class is thread-safe: one ConcurrentHashMap per player ID.
 *
 * Usage:
 *   PacketSequencer seq = new PacketSequencer();
 *   if (seq.accept(playerId, seqNum)) {
 *       actionQueue.add(action);   // only process fresh packets
 *   }
 */
public class PacketSequencer {

    /** How many sequence numbers behind "current" we still accept. */
    private static final int WINDOW_SIZE = 32;

    /**
     * Per-player state.
     * lastAccepted: the highest sequence number we've seen from this player.
     * recentlySeenBitset: bit i is set if (lastAccepted - i) has been seen.
     */
    private static class PlayerSeqState {
        long lastAccepted = -1;
        // 64-bit sliding window bitmask: bit 0 = lastAccepted, bit 1 = lastAccepted-1, etc.
        long windowBits = 0L;
    }

    private final ConcurrentHashMap<Integer, PlayerSeqState> playerStates = new ConcurrentHashMap<>();

    /**
     * Decide whether to accept this packet.
     *
     * @param playerId   sender's player ID
     * @param seqNum     the sequence number on the incoming packet
     * @return true if the packet is fresh and should be processed;
     *         false if it is a duplicate or too old
     */
    public boolean accept(int playerId, long seqNum) {
        PlayerSeqState state = playerStates.computeIfAbsent(playerId, id -> new PlayerSeqState());

        synchronized (state) {
            // First ever packet from this player
            if (state.lastAccepted == -1) {
                state.lastAccepted = seqNum;
                state.windowBits = 1L; // bit 0 set
                return true;
            }

            long diff = seqNum - state.lastAccepted;

            if (diff > 0) {
                // New highest packet — advance the window
                if (diff >= 64) {
                    // Very large jump: reset the window (handles client restart)
                    state.windowBits = 1L;
                } else {
                    state.windowBits = (state.windowBits << diff) | 1L;
                }
                state.lastAccepted = seqNum;
                return true;
            } else {
                // Packet is at or behind our current position
                long behind = -diff;
                if (behind >= WINDOW_SIZE) {
                    // Too old — drop silently
                    return false;
                }
                long mask = 1L << behind;
                if ((state.windowBits & mask) != 0) {
                    // Already seen — duplicate
                    return false;
                }
                // Within window and not seen — accept (out-of-order but still valid)
                state.windowBits |= mask;
                return true;
            }
        }
    }

    /** Remove tracking state for a player when they disconnect. */
    public void removePlayer(int playerId) {
        playerStates.remove(playerId);
    }

    /** Reset all state (e.g., between rounds). */
    public void reset() {
        playerStates.clear();
    }
}
