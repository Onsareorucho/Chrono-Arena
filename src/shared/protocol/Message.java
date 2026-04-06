package shared.protocol;

import java.io.Serializable;

/**
 * Base class for every network message in ChronoArena.
 *
 * Serialization strategy (matches Communication Contracts doc):
 *   - TCP messages: JSON via Gson (human-readable, easy to debug)
 *   - UDP messages: compact JSON (ActionMessage only)
 *
 * Every subclass must be registered in MessageType so deserializers
 * can instantiate the right class from the "type" field.
 */
public abstract class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Identifies the concrete message class during deserialization. */
    public final MessageType type;

    protected Message(MessageType type) {
        this.type = type;
    }

    public enum MessageType {
        JOIN_REQUEST,
        JOIN_RESPONSE,
        ACTION_MESSAGE,
        GAME_STATE_UPDATE,
        SCORE_UPDATE,
        PLAYER_EVENT,
        HEARTBEAT,
        HEARTBEAT_ACK,
        KILL_CLIENT,
        GAME_OVER
    }
}
