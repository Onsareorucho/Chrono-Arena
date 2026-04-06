

import java.io.Serializable;

/**
 * Sent from server to client over TCP after a successful join request.
 * Tells the client their assigned player ID and the current game state
 * so they can start rendering.
 */
public class JoinResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // The unique ID assigned to this player (used in all future messages)
    private final int playerId;
    
    // Where the player starts on the map
    private final float startX;
    private final float startY;
    
    // Current game info so the client can sync up
    private final int mapWidth;
    private final int mapHeight;
    private final long gameTimeRemainingMs;
    
    // The UDP port the server is listening on
    private final int serverUdpPort;
    
    
    public JoinResponse(int playerId, float startX, float startY, 
                        int mapWidth, int mapHeight, 
                        long gameTimeRemainingMs, int serverUdpPort) {
        this.playerId = playerId;
        this.startX = startX;
        this.startY = startY;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.gameTimeRemainingMs = gameTimeRemainingMs;
        this.serverUdpPort = serverUdpPort;
    }
    
    
    public int getPlayerId() {
        return playerId;
    }
    
    public float getStartX() {
        return startX;
    }
    
    public float getStartY() {
        return startY;
    }
    
    public int getMapWidth() {
        return mapWidth;
    }
    
    public int getMapHeight() {
        return mapHeight;
    }
    
    public long getGameTimeRemainingMs() {
        return gameTimeRemainingMs;
    }
    
    public int getServerUdpPort() {
        return serverUdpPort;
    }
    
    
    @Override
    public String toString() {
        return "JoinResponse{playerId=" + playerId + ", start=(" + startX + "," + startY + ")}";
    }
}
