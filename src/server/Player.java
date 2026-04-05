package server;

public class Player {
    private String playerId;
    private String playerName;
    private int playerPositionX;
    private int playerPositionY;
    private int playerHealth;
    private int frozenTicksLeft;

    public boolean isFrozen() {
        return frozenTicksLeft > 0;
    }

    private boolean isArmed;
    private int playerScore;
    private boolean isConnected;
    private int lastSeq;

    public Player(String playerID, String playerName, int playerPositionX, int playerPositionY, int playerHealth,
            boolean isArmed, int playerScore, boolean isConnected, int lastSeq) {
        this.playerId = playerID;
        this.playerName = playerName;
        this.playerPositionX = playerPositionX;
        this.playerPositionY = playerPositionY;
        this.playerHealth = playerHealth;
        this.frozenTicksLeft = 0;
        this.isArmed = isArmed;
        this.playerScore = playerScore;
        this.isConnected = isConnected;
        this.lastSeq = lastSeq;
    }

    public Player() {
    }

    public int getPlayerPositionX() {
        return playerPositionX;
    }

    public int getPlayerPositionY() {
        return playerPositionY;
    }

    public int getPlayerHealth() {
        return playerHealth;
    }

    public int getLastSeq() {
        return lastSeq;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getFrozenTicksLeft() {
        return frozenTicksLeft;
    }

    public boolean isArmed() {
        return isArmed;
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setPlayerPositionX(int playerPositionX) {
        this.playerPositionX = playerPositionX;
    }

    public void setPlayerPositionY(int playerPositionY) {
        this.playerPositionY = playerPositionY;
    }

    public void setPlayerHealth(int playerHealth) {
        this.playerHealth = playerHealth;
    }

    public void setArmed(boolean armed) {
        isArmed = armed;
    }

    public void setPlayerScore(int playerScore) {
        this.playerScore = playerScore;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public void setLastSeq(int lastSeq) {
        this.lastSeq = lastSeq;
    }

    public void setFrozenTicksLeft(int frozenTicksLeft) {
        this.frozenTicksLeft = frozenTicksLeft;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
