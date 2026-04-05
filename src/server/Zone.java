package server;

public class Zone {
    private String zoneId;
    private int zonePositionX;
    private int zonePositionY;
    private int zoneWidth;
    private int zoneHeight;
    private int captureTicksLeft;
    private int graceTicksLeft;
    private String controllingPlayerId;
    private String contestingPlayerId;
    private ZoneState zoneState;

    public Zone(String zoneId, int zonePositionX, int zonePositionY, int zoneWidth, int zoneHeight) {
        this.zoneId = zoneId;
        this.zonePositionX = zonePositionX;
        this.zonePositionY = zonePositionY;
        this.zoneWidth = zoneWidth;
        this.zoneHeight = zoneHeight;
        this.zoneState = ZoneState.UNCLAIMED;
        this.captureTicksLeft = 60;
        this.graceTicksLeft = 0;
        this.controllingPlayerId = null;
        this.contestingPlayerId = null;
    }

    public Zone() {
    }

    public String getZoneId() {
        return zoneId;
    }

    public int getZonePositionX() {
        return zonePositionX;
    }

    public int getZonePositionY() {
        return zonePositionY;
    }

    public int getZoneWidth() {
        return zoneWidth;
    }

    public int getZoneHeight() {
        return zoneHeight;
    }

    public int getCaptureTicksLeft() {
        return captureTicksLeft;
    }

    public int getGraceTicksLeft() {
        return graceTicksLeft;
    }

    public String getControllingPlayerId() {
        return controllingPlayerId;
    }

    public String getContestingPlayerId() {
        return contestingPlayerId;
    }

    public ZoneState getZoneState() {
        return zoneState;
    }

    public void setZoneState(ZoneState zoneState) {
        this.zoneState = zoneState;
    }

    public void setControllingPlayerId(String controllingPlayerId) {
        this.controllingPlayerId = controllingPlayerId;
    }

    public void setContestingPlayerId(String contestingPlayerId) {
        this.contestingPlayerId = contestingPlayerId;
    }

    public void setCaptureTicksLeft(int captureTicksLeft) {
        this.captureTicksLeft = captureTicksLeft;
    }

    public void setGraceTicksLeft(int graceTicksLeft) {
        this.graceTicksLeft = graceTicksLeft;
    }

    public void setZonePositionX(int zonePositionX) {
        this.zonePositionX = zonePositionX;
    }

    public void setZonePositionY(int zonePositionY) {
        this.zonePositionY = zonePositionY;
    }

    public void setZoneWidth(int zoneWidth) {
        this.zoneWidth = zoneWidth;
    }

    public void setZoneHeight(int zoneHeight) {
        this.zoneHeight = zoneHeight;
    }

}
