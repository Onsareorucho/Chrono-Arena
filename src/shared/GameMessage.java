package shared;


import java.io.Serializable;


public class GameMessage implements Serializable {
    
    // Needed for Java serialization to work across versions
    private static final long serialVersionUID = 1L;
    
    // What kind of message is this
    private final MessageType type;
    
    // When the message was created (server time)
    private final long timestamp;
    
    // Sequence number for ordering UDP packets
    // If packet 5 arrives before packet 4, we know something's off
    private final int sequenceNumber;
    
    // Who sent this message (player id, or -1 for server)
    private final int senderId;
    
    // The actual data being sent - can be anything serializable
    // Different message types will have different payload objects
    private final Serializable payload;
    
    
    public GameMessage(MessageType type, int senderId, int sequenceNumber, Serializable payload) {
        this.type = type;
        this.senderId = senderId;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Convenience constructor when you don't need a sequence number (TCP messages)
    public GameMessage(MessageType type, int senderId, Serializable payload) {
        this(type, senderId, 0, payload);
    }
    
    // for server messages that don't need sender id
    public GameMessage(MessageType type, Serializable payload) {
        this(type, -1, 0, payload);
    }
    
    
    // Getters
    
    public MessageType getType() {
        return type;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    public int getSenderId() {
        return senderId;
    }
    
    public Serializable getPayload() {
        return payload;
    }
    
    // Cast the payload to whatever type we expect
    // make sure you know what type the payload actually is
    @SuppressWarnings("unchecked")
    public <T> T getPayloadAs(Class<T> clazz) {
        return (T) payload;
    }
    
    
    @Override
    public String toString() {
        return "GameMessage{" +
                "type=" + type +
                ", sender=" + senderId +
                ", seq=" + sequenceNumber +
                ", time=" + timestamp +
                '}';
    }
}
