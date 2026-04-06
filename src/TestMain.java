import server.*;
import server.logic.*;

public class TestMain {
    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== ChronoArena Game Logic Test ===\n");

        // ── Setup ────────────────────────────────────────────
        GameState gameState = new GameState(10000); // 10 second test game
        gameState.setPhase(GameState.GamePhase.IN_PROGRESS);

        // add zones
        gameState.addZone(new Zone("A", 2, 2, 4, 4));
        gameState.addZone(new Zone("B", 14, 2, 4, 4));
        gameState.addZone(new Zone("C", 8, 10, 4, 4));

        // add players at spawn points
        Player p1 = new Player("p1", "Alice", 0, 0, 100, false, 0, true, -1, false);
        Player p2 = new Player("p2", "Bob", 19, 0, 100, false, 0, true, -1, false);
        Player p3 = new Player("p3", "Chad", 0, 19, 100, false, 0, true, -1, false);
        gameState.addPlayer(p1);
        gameState.addPlayer(p2);
        gameState.addPlayer(p3);

        // wire handlers
        ZoneCaptureHandler zoneCaptureHandler = new ZoneCaptureHandler();
        CollisionHandler collisionHandler = new CollisionHandler(gameState, zoneCaptureHandler);
        CombatHandler combatHandler = new CombatHandler(collisionHandler);
        ItemSpawner itemSpawner = new ItemSpawner(gameState, 20, 20);

        // ── Test 1: Zone Capture ─────────────────────────────
        System.out.println("--- Test 1: Zone Capture ---");
        p1.setPlayerPositionX(3);
        p1.setPlayerPositionY(3); // move p1 into zone A

        for (int i = 0; i < 65; i++) {
            collisionHandler.update();
            gameState.tick(50);
        }

        Zone zoneA = gameState.getZones().get(0);
        System.out.println("Zone A state: " + zoneA.getZoneState()); // should be CONTROLLED
        System.out.println("Zone A owner: " + zoneA.getControllingPlayerId()); // should be p1
        System.out.println("P1 score: " + p1.getPlayerScore()); // should be > 0
        System.out.println();

        // ── Test 2: Zone Contestation ────────────────────────
        System.out.println("--- Test 2: Zone Contestation ---");
        p2.setPlayerPositionX(3);
        p2.setPlayerPositionY(3); // move p2 into zone A

        collisionHandler.update();
        System.out.println("Zone A state: " + zoneA.getZoneState()); // should be CONTESTED
        System.out.println();

        // ── Test 3: Grace Timer ──────────────────────────────
        System.out.println("--- Test 3: Grace Timer ---");
        p1.setPlayerPositionX(0); // p1 leaves zone
        p2.setPlayerPositionX(0); // p2 leaves zone
        collisionHandler.update();
        System.out.println("Zone A state after both leave: " + zoneA.getZoneState());
        System.out.println();

        // ── Test 4: Item Pickup ──────────────────────────────
        System.out.println("--- Test 4: Item Pickup ---");
        Item energyItem = new Item("i1", ItemType.ENERGY, 5, 5);
        gameState.addItem(energyItem);
        p1.setPlayerPositionX(5);
        p1.setPlayerPositionY(5); // move p1 onto item
        p1.setLastSeq(10); // p1 has higher seq number
        p2.setPlayerPositionX(5);
        p2.setPlayerPositionY(5); // p2 also on item
        p2.setLastSeq(5); // p2 has lower seq number

        collisionHandler.update();
        System.out.println("P1 score (should have +10): " + p1.getPlayerScore());
        System.out.println("Item available (should be false): " + energyItem.isAvailable());
        System.out.println();

        // ── Test 5: Freeze Attack ────────────────────────────
        System.out.println("--- Test 5: Freeze Attack ---");
        p1.setArmed(true);
        p1.setPlayerPositionX(0);
        p1.setPlayerPositionY(0);
        p2.setPlayerPositionX(1); // p2 within range
        p2.setPlayerPositionY(1);
        int p2ScoreBefore = p2.getPlayerScore();
        int p1ScoreBefore = p1.getPlayerScore();

        combatHandler.handleFreezeAttack(p1, p2);
        System.out.println("P2 frozen: " + p2.isFrozen()); // should be true
        System.out.println("P2 score change: " + (p2.getPlayerScore() - p2ScoreBefore)); // should be -15
        System.out.println("P1 score change: " + (p1.getPlayerScore() - p1ScoreBefore)); // should be +15
        System.out.println("P1 armed after attack: " + p1.isArmed()); // should be false
        System.out.println();

        // ── Test 6: Speed Boost ──────────────────────────────
        System.out.println("--- Test 6: Speed Boost ---");
        Item speedBoost = new Item("i2", ItemType.SPEED_BOOST, 7, 7);
        gameState.addItem(speedBoost);
        p3.setPlayerPositionX(7);
        p3.setPlayerPositionY(7);
        p3.setLastSeq(99); // highest seq

        collisionHandler.update();
        System.out.println("P3 has speed boost: " + p3.isHasSpeedBoost()); // should be true

        // now move p3 into zone B
        p3.setPlayerPositionX(15);
        p3.setPlayerPositionY(3);
        collisionHandler.update();
        Zone zoneB = gameState.getZones().get(1);
        System.out.println("Zone B capture ticks (should be 30): " + zoneB.getCaptureTicksLeft());
        System.out.println();

        // ── Test 7: Item Spawner ─────────────────────────────
        System.out.println("--- Test 7: Item Spawner ---");
        int itemsBefore = gameState.getItems().size();
        for (int i = 0; i < 201; i++) {
            itemSpawner.update();
        }
        System.out.println("Items before: " + itemsBefore);
        System.out.println("Items after 201 ticks: " + gameState.getItems().size()); // should have spawned one
        System.out.println();

        // ── Test 8: Player Disconnect ────────────────────────
        System.out.println("--- Test 8: Player Disconnect ---");
        gameState.handlePlayerDisconnect("p1");
        System.out.println("P1 still in game: " + (gameState.getPlayer("p1") != null)); // should be false
        System.out.println();

        // ── Test 9: Winner ───────────────────────────────────
        System.out.println("--- Test 9: Winner ---");
        Player winner = gameState.getWinner();
        System.out.println("Winner: "
                + (winner != null ? winner.getPlayerName() + " with " + winner.getPlayerScore() + " points" : "none"));

        System.out.println("\n=== Test Complete ===");
    }
}