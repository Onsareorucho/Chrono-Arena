package shared.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Centralized JSON serialization/deserialization for all ChronoArena messages.
 *
 * Wire format (TCP):  length-prefixed UTF-8 JSON  →  [4-byte int][json bytes]
 * Wire format (UDP):  raw UTF-8 JSON datagram      →  [json bytes]
 *
 * Usage:
 *   // Serialize
 *   String json = MessageSerializer.toJson(new JoinRequest("Alice"));
 *
 *   // Deserialize
 *   Message msg = MessageSerializer.fromJson(json);
 *   if (msg.type == MessageType.JOIN_REQUEST) {
 *       JoinRequest jr = (JoinRequest) msg;
 *   }
 *
 *   // Deserialize GameState inside a GameStateUpdate
 *   GameState gs = MessageSerializer.gson().fromJson(update.gameState, GameState.class);
 */
public final class MessageSerializer {

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    private MessageSerializer() {}

    // ── Serialization ──────────────────────────────────────────

    /** Serialize any Message subclass to a JSON string. */
    public static String toJson(Message message) {
        return GSON.toJson(message);
    }

    // ── Deserialization ────────────────────────────────────────

    /**
     * Deserialize a JSON string to the correct concrete Message subclass.
     *
     * Reads the mandatory "type" field first, then delegates to Gson
     * with the correct target class. Throws IllegalArgumentException on
     * malformed input or unknown type values.
     */
    public static Message fromJson(String json) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON: " + json, e);
        }

        if (!root.has("type")) {
            throw new IllegalArgumentException("Message JSON missing 'type' field: " + json);
        }

        Message.MessageType type;
        try {
            type = Message.MessageType.valueOf(root.get("type").getAsString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown message type: " + root.get("type"), e);
        }

        return switch (type) {
            case JOIN_REQUEST      -> GSON.fromJson(json, JoinRequest.class);
            case JOIN_RESPONSE     -> GSON.fromJson(json, JoinResponse.class);
            case ACTION_MESSAGE    -> GSON.fromJson(json, ActionMessage.class);
            case GAME_STATE_UPDATE -> GSON.fromJson(json, GameStateUpdate.class);
            case SCORE_UPDATE      -> GSON.fromJson(json, ScoreUpdate.class);
            case PLAYER_EVENT      -> GSON.fromJson(json, PlayerEvent.class);
            case HEARTBEAT         -> GSON.fromJson(json, Heartbeat.class);
            case HEARTBEAT_ACK     -> GSON.fromJson(json, HeartbeatAck.class);
            case KILL_CLIENT       -> GSON.fromJson(json, KillClient.class);
            case GAME_OVER         -> GSON.fromJson(json, GameOver.class);
        };
    }

    /**
     * Expose the Gson instance for callers that need to serialize/deserialize
     * domain objects (e.g., GameState inside GameStateUpdate).
     *
     * Example:
     *   GameState gs = MessageSerializer.gson()
     *                      .fromJson(update.gameState, GameState.class);
     */
    public static Gson gson() {
        return GSON;
    }
}
