package server.logic;

import server.*;
import shared.GameConstants;
import shared.GameEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;


public class CollisionHandler {

    private final GameState gameState;
    private final ZoneCaptureHandler zoneCaptureHandler;
    private Consumer<GameEvent> eventBroadcaster = e -> {};

    public CollisionHandler(GameState gameState, ZoneCaptureHandler zoneCaptureHandler) {
        this.gameState = gameState;
        this.zoneCaptureHandler = zoneCaptureHandler;
    }

    public void setEventBroadcaster(Consumer<GameEvent> broadcaster) {
        this.eventBroadcaster = broadcaster;
    }

    // called every tick by GameLoop
    public void update() {
        List<Zone> zones = gameState.getZones();
        List<Item> items = gameState.getItems();

        for (Player player : gameState.getPlayers().values()) {

            // skip frozen players — they can't interact with anything
            if (player.isFrozen())
                continue;

            // ── Zone detection ───────────────────────────────────
            for (Zone zone : zones) {
                if (isInsideZone(player, zone)) {
                    zoneCaptureHandler.handlePlayerInZone(player, zone);
                } else {
                    zoneCaptureHandler.handlePlayerLeftZone(player, zone);
                }
            }
        }

        // ── Item pickup detection ────────────────────────────
        for (Item item : items) {
            if (!item.isAvailable())
                continue;

            List<Player> contenders = new ArrayList<>();
            for (Player p : gameState.getPlayers().values()) {
                if (!p.isFrozen() && isOnItem(p, item)) {
                    contenders.add(p);
                }
            }

            if (contenders.isEmpty())
                continue;

            // fairness rule — highest sequence number wins
            Player winner = contenders.stream()
                    .max(Comparator.comparingInt(Player::getLastSeq))
                    .orElse(null);

            if (winner != null) {
                handleItemPickup(winner, item);
            }
        }
    }

    // check if player is within the zone bounds
    private boolean isInsideZone(Player player, Zone zone) {
        boolean insideX = player.getPlayerPositionX() >= zone.getZonePositionX()
                && player.getPlayerPositionX() < zone.getZonePositionX() + zone.getZoneWidth();
        boolean insideY = player.getPlayerPositionY() >= zone.getZonePositionY()
                && player.getPlayerPositionY() < zone.getZonePositionY() + zone.getZoneHeight();
        return insideX && insideY;
    }

    // check if player is on the same tile as an item
    private boolean isOnItem(Player player, Item item) {
        return player.getPlayerPositionX() == item.getItemPositionX()
                && player.getPlayerPositionY() == item.getItemPositionY();
    }

    // handle item collection
    private void handleItemPickup(Player player, Item item) {
        String itemTypeName = item.getItemType().name();
        switch (item.getItemType()) {
            case ENERGY -> {
                player.setPlayerScore(player.getPlayerScore() + GameConstants.ENERGY_POINTS);
                System.out.println(player.getPlayerName() + " collected ENERGY +" + GameConstants.ENERGY_POINTS + " points");
            }
            case FREEZE_RAY -> {
                player.setArmed(true);
                System.out.println(player.getPlayerName() + " picked up FREEZE RAY");
            }
            case SPEED_BOOST -> {
                player.setHasSpeedBoost(true);
                player.setSpeedBoostTicksLeft((int) (GameConstants.SPEED_BOOST_DURATION_MS / 50));
                System.out.println(player.getPlayerName() + " picked up SPEED BOOST — capture time halved");
            }
        }

        eventBroadcaster.accept(GameEvent.itemCollected(
                player.getPlayerId(), item.getItemId().hashCode(), itemTypeName,
                (float) item.getItemPositionX(), (float) item.getItemPositionY()));

        // mark unavailable and remove from game state
        item.setAvailable(false);
        gameState.removeItem(item.getItemId());
    }

    // check if two players are within attack range
    public boolean isWithinAttackRange(Player attacker, Player target, int range) {
        int dx = Math.abs(attacker.getPlayerPositionX() - target.getPlayerPositionX());
        int dy = Math.abs(attacker.getPlayerPositionY() - target.getPlayerPositionY());
        return dx <= range && dy <= range;
    }
}