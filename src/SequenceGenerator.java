import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates sequence numbers for outgoing UDP messages.
 * 
 * Each client uses this to number their messages so the server can
 * detect out of order packets and duplicates.
 * 
 * multiple threads can send messages without conflicts.
 */
public class SequenceGenerator {
    
    // AtomicInteger handles thread safety for us
    private final AtomicInteger sequence = new AtomicInteger(0);
    
    
    // Get the next sequence number
    public int next() {
        return sequence.incrementAndGet();
    }
    
    // Get current value without incrementing (for debugging)
    public int current() {
        return sequence.get();
    }
    
    // Reset to zero (for when starting a new game)
    public void reset() {
        sequence.set(0);
    }
}
