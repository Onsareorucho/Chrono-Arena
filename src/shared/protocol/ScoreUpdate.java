package shared.protocol;

import java.util.Map;

// ─────────────────────────────────────────────────────────────────
// ScoreUpdate
// ─────────────────────────────────────────────────────────────────

/**
 * Lightweight server → client score broadcast.
 * Sent after every tick that changes any score so the HUD stays current
 * without waiting for the next full GameStateUpdate.
 */
public class ScoreUpdate extends Message {

    /** Maps playerId (Integer) → current score (Integer). */
    public Map<Integer, Integer> scores;

    public ScoreUpdate() { super(MessageType.SCORE_UPDATE); }

    public ScoreUpdate(Map<Integer, Integer> scores) {
        super(MessageType.SCORE_UPDATE);
        this.scores = scores;
    }
}
