package shared.protocol;

import com.google.gson.JsonObject;

/**
 * Full authoritative game state snapshot sent server → all clients over TCP.
 *
 * Why send the full state and not just deltas?
 *  - Simpler to implement correctly during the project timeline
 *  - At 20x20 grid with ~4 players the payload stays small (~2–5 KB)
 *  - Clients that miss a delta are automatically corrected on the next tick
 *
 * tickNumber ordering:
 *  The client must track the last applied tick and DISCARD any update with
 *  tickNumber <= lastAppliedTick. TCP guarantees ordering within a session,
 *  but if a client reconnects it may receive an update from a previous session.
 *
 * gameState is stored as a Gson JsonObject so the shared/ package does not
 * need to depend on server/ classes. TCPClientHandler delivers it raw;
 * the GUI (Person 3) deserializes it using:
 *
 *   GameState gs = MessageSerializer.gson().fromJson(update.gameState, GameState.class);
 */
public class GameStateUpdate extends Message {

    /** Monotonically increasing. Client discards stale updates. */
    public long       tickNumber;

    /**
     * Full serialized GameState. Stored as JsonObject so shared/ stays
     * independent of server/ data model. GUI deserializes with Gson.
     */
    public JsonObject gameState;

    /** Required for Gson deserialization. */
    public GameStateUpdate() {
        super(MessageType.GAME_STATE_UPDATE);
    }

    public GameStateUpdate(long tickNumber, JsonObject gameState) {
        super(MessageType.GAME_STATE_UPDATE);
        this.tickNumber = tickNumber;
        this.gameState  = gameState;
    }
}
