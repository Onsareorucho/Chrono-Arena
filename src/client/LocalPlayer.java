package client;

public class LocalPlayer {

    private int id = -1;
    private String name = "Player";
    private int x, y;
    private int score = 0;
    private int hp = 100;
    private boolean hasFreezeRay = false;
    private boolean isFrozen = false;
    private boolean hasSpeedBoost = false;

    public LocalPlayer() {

    }

    public void setID(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void setPosition(int x, int y) {
        this.x = x;
        this. y = y;
    }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public boolean hasFreezeRay() { return hasFreezeRay; }
    public void setHasFreezeRay(boolean has) { this.hasFreezeRay = has; }

    public boolean isFrozen() { return isFrozen; }
    public void setFrozen(boolean frozen) { this.isFrozen = frozen; }

    public boolean hasSpeedBoost() { return hasSpeedBoost; }
    public void setHasSpeedBoost(boolean has) { this.hasSpeedBoost = has; }
}