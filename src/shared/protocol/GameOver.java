package shared.protocol;

import java.util.Map;

/**
 * Broadcast when the round timer reaches zero.
 * Sent server → all clients over TCP.
 *
 * winnerPlayerId is -1 if the game ended with no winner (e.g., all players left).
 */
public class GameOver extends Message {

    /** Final scores: playerId → total score. */
    public Map<Integer, Integer> finalScores;

    /** The player ID with the highest score, or -1 if no winner. */
    public int winnerPlayerId;

    public GameOver() { super(MessageType.GAME_OVER); }

    public GameOver(Map<Integer, Integer> finalScores, int winnerPlayerId) {
        super(MessageType.GAME_OVER);
        this.finalScores    = finalScores;
        this.winnerPlayerId = winnerPlayerId;
    }
}
