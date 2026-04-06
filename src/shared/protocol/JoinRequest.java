package shared.protocol;

/**
 * Sent by a new client over TCP immediately after connecting.
 * The server will respond with a JoinResponse.
 */
public class JoinRequest extends Message {

    /** Display name chosen by the player (shown in the HUD). */
    public String playerName;

    /** Required for Gson deserialization. */
    public JoinRequest() {
        super(MessageType.JOIN_REQUEST);
    }

    public JoinRequest(String playerName) {
        super(MessageType.JOIN_REQUEST);
        this.playerName = playerName;
    }
}
