import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/**
 * Sent when the game ends to show final scores and rankings.
 * Includes a sorted leaderboard so clients can display who won.
 */
public class GameResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Final scores for all players, sorted by score descending
    private final List<PlayerScore> leaderboard;
    
    // The player who won (could be a tie but we just pick whoever had the score first)
    private final int winnerId;
    private final String winnerName;
    
    // How long the game lasted
    private final long gameDurationMs;
    
    
    public GameResult(List<PlayerScore> leaderboard, int winnerId, 
                      String winnerName, long gameDurationMs) {
        this.leaderboard = new ArrayList<>(leaderboard);
        this.winnerId = winnerId;
        this.winnerName = winnerName;
        this.gameDurationMs = gameDurationMs;
    }
    
    
    public List<PlayerScore> getLeaderboard() {
        return leaderboard;
    }
    
    public int getWinnerId() {
        return winnerId;
    }
    
    public String getWinnerName() {
        return winnerName;
    }
    
    public long getGameDurationMs() {
        return gameDurationMs;
    }
    
    
    // A player's final score and stats
    public static class PlayerScore implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public final int playerId;
        public final String name;
        public final int totalScore;
        public final int zonesControlled;    // total times they captured a zone
        public final int itemsCollected;
        public final int freezesLanded;      // times they froze someone
        public final int timesFrozen;        // times they got frozen
        
        public PlayerScore(int playerId, String name, int totalScore,
                           int zonesControlled, int itemsCollected,
                           int freezesLanded, int timesFrozen) {
            this.playerId = playerId;
            this.name = name;
            this.totalScore = totalScore;
            this.zonesControlled = zonesControlled;
            this.itemsCollected = itemsCollected;
            this.freezesLanded = freezesLanded;
            this.timesFrozen = timesFrozen;
        }
        
        @Override
        public String toString() {
            return name + ": " + totalScore + " pts";
        }
    }
}
