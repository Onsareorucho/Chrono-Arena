package shared.protocol;

/**
 * Server → specific client: you are being forcibly disconnected.
 *
 * Sent by TCPServerHandler.kill() as part of the KILL_SWITCH feature.
 * The client should display the reason to the player before closing.
 *
 * After sending this, the server closes the socket on its end.
 * The client should call TCPClientHandler.stop() upon receiving this.
 */
public class KillClient extends Message {

    public int    targetPlayerId;
    public String reason;

    public KillClient() { super(MessageType.KILL_CLIENT); }

    public KillClient(int targetPlayerId, String reason) {
        super(MessageType.KILL_CLIENT);
        this.targetPlayerId = targetPlayerId;
        this.reason         = reason;
    }
}
