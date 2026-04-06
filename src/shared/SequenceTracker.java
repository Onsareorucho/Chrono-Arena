package shared;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks UDP sequence numbers to handle out of order and duplicate packets.
 * 
 * UDP doesn't guarantee order or delivery
 *  Packets might arrive out of order (packet 5 before packet 4)
 *  Packets might arrive twice (network retry)
 *  Packets might not arrive at all
 * 
 * This class helps detect old/duplicate packets so we can ignore them.
 * The server creates one of these to track each client's sequence numbers.
 */
public class SequenceTracker {
    
    // Last sequence number we processed for each sender
    // Key = player ID, Value = last sequence number
    private final Map<Integer, Integer> lastSeenSequence = new ConcurrentHashMap<>();
    
    // How many sequence numbers back we still accept (for slight out-of-order)
    // If a packet is more than this many behind, we reject it
    private final int windowSize;
    
    
    public SequenceTracker() {
        this(10);  // default window of 10
    }
    
    public SequenceTracker(int windowSize) {
        this.windowSize = windowSize;
    }
    
    
    // Check if we should process this message or ignore it
    // Returns true if the message is new enough to process
    public boolean shouldProcess(int senderId, int sequenceNumber) {
        Integer lastSeen = lastSeenSequence.get(senderId);
        
        if (lastSeen == null) {
            // First message from this sender, accept it
            lastSeenSequence.put(senderId, sequenceNumber);
            return true;
        }
        
        // Check if this is newer than what we've seen
        // We use a window to allow slightly out-of-order packets
        if (sequenceNumber > lastSeen) {
            // Newer packet, update and accept
            lastSeenSequence.put(senderId, sequenceNumber);
            return true;
        }
        
        // Allow packets within the window (slightly out of order is ok)
        if (sequenceNumber >= lastSeen - windowSize) {
            // Within window, accept but don't update lastSeen
            return true;
        }
        
        // Too old, reject
        return false;
    }
    
    // Check if this looks like a duplicate (exact same sequence)
    public boolean isDuplicate(int senderId, int sequenceNumber) {
        Integer lastSeen = lastSeenSequence.get(senderId);
        return lastSeen != null && lastSeen.equals(sequenceNumber);
    }
    
    // Remove tracking for a player (when they disconnect)
    public void removePlayer(int playerId) {
        lastSeenSequence.remove(playerId);
    }
    
    // Reset all tracking
    public void clear() {
        lastSeenSequence.clear();
    }
    
    // Get the last sequence number from a player
    public int getLastSequence(int senderId) {
        return lastSeenSequence.getOrDefault(senderId, -1);
    }
}
