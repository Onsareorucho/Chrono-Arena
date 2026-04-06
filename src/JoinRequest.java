

import java.io.Serializable;

/**
 * Sent from client to server over TCP when a player wants to join the game.
 * This is the first message a client sends after connecting.
 */
public class JoinRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // What the player wants to be called
    private final String playerName;
    
    // Client's UDP port so server knows where to expect UDP packets from
    private final int udpPort;
    
    
    public JoinRequest(String playerName, int udpPort) {
        this.playerName = playerName;
        this.udpPort = udpPort;
    }
    
    
    public String getPlayerName() {
        return playerName;
    }
    
    public int getUdpPort() {
        return udpPort;
    }
    
    
    @Override
    public String toString() {
        return "JoinRequest{name='" + playerName + "', udpPort=" + udpPort + "}";
    }
}
