package server.logic;

import server.*;

public class ZoneCaptureHandler {

    public static final int CAPTURE_TICKS = 60; // 3 seconds at 20 ticks/sec
    public static final int GRACE_TICKS   = 100; // 5 seconds

    // called when a player is detected inside a zone
    public void handlePlayerInZone(Player player, Zone zone) {
        switch (zone.getZoneState()) {

            case UNCLAIMED -> {
                zone.setZoneState(ZoneState.CAPTURING);
                zone.setContestingPlayerId(player.getPlayerId());

                // speed boost halves capture time
                int captureTicks = player.isHasSpeedBoost() ? CAPTURE_TICKS / 2 : CAPTURE_TICKS;
                zone.setCaptureTicksLeft(captureTicks);

                // consume the boost
                player.setHasSpeedBoost(false);

                System.out.println(player.getPlayerName() + " started capturing zone " + zone.getZoneId());
            }

            case CAPTURING -> {
                // someone else is already capturing — contest it
                if (player.getPlayerId() != zone.getContestingPlayerId()) {
                    zone.setZoneState(ZoneState.CONTESTED);
                    // store challenger so we know who resumes if the capturer leaves
                    zone.setControllingPlayerId(player.getPlayerId());
                    System.out.println("Zone " + zone.getZoneId() + " is now CONTESTED");
                }
                // if same player — do nothing, GameLoop counts down the timer
            }

            case CONTROLLED -> {
                // someone else enters a controlled zone — contest it
                if (player.getPlayerId() != zone.getControllingPlayerId()) {
                    zone.setZoneState(ZoneState.CONTESTED);
                    System.out.println("Zone " + zone.getZoneId() + " is now CONTESTED");
                }
            }

            case GRACE -> {
                // owner came back during grace period — restore control
                if (player.getPlayerId() == zone.getControllingPlayerId()) {
                    zone.setZoneState(ZoneState.CONTROLLED);
                    zone.setGraceTicksLeft(0);
                    System.out.println(player.getPlayerName() + " returned — zone " + zone.getZoneId() + " restored");
                } else {
                    // different player enters during grace — contest it
                    zone.setZoneState(ZoneState.CONTESTED);
                    System.out.println("Zone " + zone.getZoneId() + " contested during grace period");
                }
            }

            case CONTESTED -> {
                // already contested — nothing changes until one player leaves
            }
        }
    }

    // called when a player is NOT inside a zone
    public void handlePlayerLeftZone(Player player, Zone zone) {
        switch (zone.getZoneState()) {

            case CAPTURING -> {
                // capturing player left — reset zone
                if (player.getPlayerId() == zone.getContestingPlayerId()) {
                    zone.setZoneState(ZoneState.UNCLAIMED);
                    zone.setContestingPlayerId(-1);
                    zone.setCaptureTicksLeft(CAPTURE_TICKS);
                    System.out.println(player.getPlayerName() + " left zone " + zone.getZoneId() + " — capture reset");
                }
            }

            case CONTROLLED -> {
                // controlling player left — start grace timer
                if (player.getPlayerId() == zone.getControllingPlayerId()) {
                    zone.setZoneState(ZoneState.GRACE);
                    zone.setGraceTicksLeft(GRACE_TICKS);
                    System.out.println(
                            player.getPlayerName() + " left zone " + zone.getZoneId() + " — grace period started");
                }
            }

            case CONTESTED -> {
                // one player left — the other resumes capturing
                if (player.getPlayerId() == zone.getContestingPlayerId()) {
                    int remaining = zone.getControllingPlayerId();
                    zone.setControllingPlayerId(-1);
                    startCapture(zone, remaining);
                    System.out.println("Zone " + zone.getZoneId() + " — contesting player left, capture resumed");
                } else if (player.getPlayerId() == zone.getControllingPlayerId()) {
                    startCapture(zone, zone.getContestingPlayerId());
                    System.out.println("Zone " + zone.getZoneId() + " — controlling player left, contester resumes capture");
                }
            }

            case UNCLAIMED, GRACE -> {
                // nothing to do
            }
        }
    }

    private void startCapture(Zone zone, int playerId) {
        zone.setZoneState(ZoneState.CAPTURING);
        zone.setContestingPlayerId(playerId);
        zone.setCaptureTicksLeft(CAPTURE_TICKS);
    }
}
