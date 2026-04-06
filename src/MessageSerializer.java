import java.io.*;

/**
 * Converts GameMessage objects to bytes and back.
 * 
 * We use Java's built-in serialization - it's not the fastest but it's simple
 * and works with any Serializable object. Both TCP and UDP use this.
 * 
 */
public class MessageSerializer {
    
    // Turn a GameMessage into bytes that can be sent over the network
    public static byte[] serialize(GameMessage message) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        
        objectStream.writeObject(message);
        objectStream.flush();
        
        return byteStream.toByteArray();
    }
    
    // Turn bytes back into a GameMessage
    public static GameMessage deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream);
        
        return (GameMessage) objectStream.readObject();
    }
    
    // Same as deserialize but takes a length (for when you have a buffer that's bigger than the actual data)
    public static GameMessage deserialize(byte[] data, int length) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data, 0, length);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream);
        
        return (GameMessage) objectStream.readObject();
    }
    
    
    // For TCP we need to handle the stream differently since messages come one after another
    // These methods work with the socket's streams directly
    
    // Write a message to a TCP stream (prepends length so receiver knows how much to read)
    public static void writeToStream(GameMessage message, OutputStream out) throws IOException {
        byte[] data = serialize(message);
        
        // Write the length first as 4 bytes (big-endian)
        // This way the receiver knows exactly how many bytes to read
        DataOutputStream dataOut = new DataOutputStream(out);
        dataOut.writeInt(data.length);
        dataOut.write(data);
        dataOut.flush();
    }
    
    // Read a message from a TCP stream
    public static GameMessage readFromStream(InputStream in) throws IOException, ClassNotFoundException {
        DataInputStream dataIn = new DataInputStream(in);
        
        // Read the length first
        int length = dataIn.readInt();
        
        // Sanity check to avoid reading crazy amounts of data
        if (length <= 0 || length > GameConstants.MAX_TCP_MESSAGE_SIZE) {
            throw new IOException("Invalid message length: " + length);
        }
        
        // Read exactly that many bytes
        byte[] data = new byte[length];
        dataIn.readFully(data);
        
        return deserialize(data);
    }
}
