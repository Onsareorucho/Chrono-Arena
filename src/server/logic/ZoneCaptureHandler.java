package server.logic;

import server.*;

public class ZoneCaptureHandler {

    private static final int CAPTURE_TICKS = 60; // 3 seconds at 20 ticks/sec
    private static final int GRACE_TICKS = 100; // 5 seconds

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
                if (!player.getPlayerId().equals(zone.getContestingPlayerId())) {
                    zone.setZoneState(ZoneState.CONTESTED);
                    System.out.println("Zone " + zone.getZoneId() + " is now CONTESTED");
                }
                // if same player — do nothing, GameLoop counts down the timer
            }

            case CONTROLLED -> {
                // someone else enters a controlled zone — contest it
                if (!player.getPlayerId().equals(zone.getControllingPlayerId())) {
                    zone.setZoneState(ZoneState.CONTESTED);
                    System.out.println("Zone " + zone.getZoneId() + " is now CONTESTED");
                }
            }

            case GRACE -> {
                // owner came back during grace period — restore control
                if (player.getPlayerId().equals(zone.getControllingPlayerId())) {
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
                if (player.getPlayerId().equals(zone.getContestingPlayerId())) {
                    zone.setZoneState(ZoneState.UNCLAIMED);
                    zone.setContestingPlayerId(null);
                    zone.setCaptureTicksLeft(CAPTURE_TICKS);
                    System.out.println(player.getPlayerName() + " left zone " + zone.getZoneId() + " — capture reset");
                }
            }

            case CONTROLLED -> {
                // controlling player left — start grace timer
                if (player.getPlayerId().equals(zone.getControllingPlayerId())) {
                    zone.setZoneState(ZoneState.GRACE);
                    zone.setGraceTicksLeft(GRACE_TICKS);
                    System.out.println(
                            player.getPlayerName() + " left zone " + zone.getZoneId() + " — grace period started");
                }
            }

            case CONTESTED -> {
                // one player left contested zone — other player starts capturing
                if (player.getPlayerId().equals(zone.getContestingPlayerId())) {
                    zone.setZoneState(ZoneState.CAPTURING);
                    zone.setContestingPlayerId(zone.getControllingPlayerId());
                    zone.setControllingPlayerId(null);
                    zone.setCaptureTicksLeft(CAPTURE_TICKS);
                    System.out.println("Zone " + zone.getZoneId() + " — contesting player left, capture resumed");
                } else if (player.getPlayerId().equals(zone.getControllingPlayerId())) {
                    zone.setZoneState(ZoneState.CAPTURING);
                    zone.setControllingPlayerId(null);
                    zone.setCaptureTicksLeft(CAPTURE_TICKS);
                    System.out.println(
                            "Zone " + zone.getZoneId() + " — controlling player left, contester resumes capture");
                }
            }

            case UNCLAIMED, GRACE -> {
                // nothing to do
            }
        }
    }
}