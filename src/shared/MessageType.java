package shared;

public enum MessageType {
    
    // Client -> Server (TCP): player wants to join the game
    JOIN_REQUEST,
    
    // Server -> Client (TCP): welcome aboard, here's your player id
    JOIN_RESPONSE,
    
    // Server -> Client (TCP): sorry, couldn't join (game full, etc)
    JOIN_DENIED,
    
    // Client -> Server (TCP): player is leaving gracefully
    LEAVE_REQUEST,
    
    // Server -> Client (TCP): acknowledged, you're out
    LEAVE_RESPONSE,
    
    // Client -> Server (UDP): player moved or changed direction
    // This is the high-frequency one, needs to be fast
    PLAYER_INPUT,
    
    // Client -> Server (UDP): player used freeze ray
    PLAYER_ACTION,
    
    // Server -> Client (TCP): here's the full game state
    // Sent periodically so clients stay in sync
    GAME_STATE_UPDATE,
    
    // Server -> Client (TCP): something specific happened (zone captured, item picked up, etc)
    GAME_EVENT,
    
    // Server -> Client (TCP): current scores for scoreboard
    SCORE_UPDATE,
    
    // Server -> Client (TCP): game is starting, get ready
    GAME_START,
    
    // Server -> Client (TCP): round is over, here are the final results
    GAME_END,
    
    // Server -> Client (TCP): you've been frozen by another player
    PLAYER_FROZEN,
    
    // Server -> Client (TCP): you're unfrozen, you can move again
    PLAYER_UNFROZEN,
    
    // Server -> Client (TCP): a new player joined the game
    PLAYER_JOINED,
    
    // Server -> Client (TCP): a player left the game
    PLAYER_LEFT,
    
    // Server -> Client (TCP): an item spawned on the map
    ITEM_SPAWNED,
    
    // Server -> Client (TCP): an item was picked up (by you or someone else)
    ITEM_COLLECTED,
    
    // Server -> Client (TCP): zone ownership changed
    ZONE_UPDATE,
    
    // Both directions (TCP): just checking if the connection is alive
    HEARTBEAT,
    
    // Server -> Client (TCP): you're being kicked (kill switch activated)
    KICK,
    
    // Server -> Client (TCP): something went wrong, here's an error message
    ERROR;
    
    
    // Quick way to check if this message type should go over UDP
    // Only the fast player input uses UDP
    public boolean isUdp() {
        return this == PLAYER_INPUT || this == PLAYER_ACTION;
    }
    
    // Check if this is a message the client sends
    public boolean isClientMessage() {
        return this == JOIN_REQUEST 
            || this == LEAVE_REQUEST 
            || this == PLAYER_INPUT 
            || this == PLAYER_ACTION
            || this == HEARTBEAT;
    }
}
