import server.*;
import server.logic.*;
import shared.ItemType;

public class TestMain {
    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== ChronoArena Game Logic Test ===\n");

        // ── Setup ────────────────────────────────────────────
        final long tickRateMs = 50;
        GameState gameState = new GameState(10000, tickRateMs); // 10 second test game
        gameState.setPhase(GameState.GamePhase.IN_PROGRESS);
        GameLoop gameLoop = new GameLoop(gameState);

        // add zones
        gameState.addZone(new Zone("A", 2, 2, 4, 4));
        gameState.addZone(new Zone("B", 14, 2, 4, 4));
        gameState.addZone(new Zone("C", 8, 10, 4, 4));

        // add players at spawn points
        Player p1 = new Player(1, "Alice", 0, 0, 100, false, 0, true, -1, false);
        Player p2 = new Player(2, "Bob", 19, 0, 100, false, 0, true, -1, false);
        Player p3 = new Player(3, "Chad", 0, 19, 100, false, 0, true, -1, false);
        gameState.addPlayer(p1);
        gameState.addPlayer(p2);
        gameState.addPlayer(p3);

        // wire handlers
        ZoneCaptureHandler zoneCaptureHandler = new ZoneCaptureHandler(tickRateMs);
        CollisionHandler collisionHandler = new CollisionHandler(gameState, zoneCaptureHandler);
        CombatHandler combatHandler = new CombatHandler(collisionHandler, tickRateMs);
        ItemSpawner itemSpawner = new ItemSpawner(gameState, 20, 20, tickRateMs);

        // ── Test 1: Zone Capture ─────────────────────────────
        // p1 stands in zone A for 65 ticks (capture takes 60)
        // expect: CONTROLLED, owner=1, score > 0 (1 pt per tick while CONTROLLED)
        System.out.println("--- Test 1: Zone Capture ---");
        p1.setPlayerPositionX(3);
        p1.setPlayerPositionY(3);

        for (int i = 0; i < 65; i++) {
            collisionHandler.update();
            gameLoop.updateZones();
            gameState.tick(tickRateMs);
        }

        Zone zoneA = gameState.getZones().get(0);
        System.out.println("Zone A state (expect CONTROLLED): " + zoneA.getZoneState());
        System.out.println("Zone A owner (expect 1): " + zoneA.getControllingPlayerId());
        System.out.println("P1 score (expect > 0): " + p1.getPlayerScore());
        System.out.println();

        // ── Test 2: Zone Contestation ────────────────────────
        // p2 enters zone A while p1 controls it
        // expect: CONTESTED
        System.out.println("--- Test 2: Zone Contestation ---");
        p2.setPlayerPositionX(3);
        p2.setPlayerPositionY(3);

        collisionHandler.update();
        System.out.println("Zone A state (expect CONTESTED): " + zoneA.getZoneState());
        System.out.println();

        // ── Test 3: Grace Timer ──────────────────────────────
        // both players leave a CONTESTED zone — p1 (controlling) leaves first so p2 resumes capture,
        // then p2 (now capturing) also leaves so zone fully resets
        // expect: UNCLAIMED
        System.out.println("--- Test 3: Grace Timer ---");
        p1.setPlayerPositionX(0);
        p2.setPlayerPositionX(0);
        collisionHandler.update();
        System.out.println("Zone A state after both leave (expect UNCLAIMED): " + zoneA.getZoneState());
        System.out.println();

        // ── Test 4: Item Pickup ──────────────────────────────
        // p1 and p2 both on item — p1 wins (higher seq number)
        // expect: p1 score +10, item no longer available
        System.out.println("--- Test 4: Item Pickup ---");
        Item energyItem = new Item("i1", ItemType.ENERGY, 5, 5);
        gameState.addItem(energyItem);
        p1.setPlayerPositionX(5);
        p1.setPlayerPositionY(5);
        p1.setLastSeq(10); // p1 wins tiebreak
        p2.setPlayerPositionX(5);
        p2.setPlayerPositionY(5);
        p2.setLastSeq(5);

        collisionHandler.update();
        System.out.println("P1 score (expect previous + 50): " + p1.getPlayerScore());
        System.out.println("Item available (expect false): " + energyItem.isAvailable());
        System.out.println();

        // ── Test 5: Freeze Attack ────────────────────────────
        // p1 (armed) freezes p2 within range
        // p2 score was 0 so deduction floors at 0 — score change = 0, not -25
        // expect: p2 frozen=true, p2 score change=0 (floored), p1 score +25, p1 disarmed
        System.out.println("--- Test 5: Freeze Attack ---");
        p1.setArmed(true);
        p1.setPlayerPositionX(0);
        p1.setPlayerPositionY(0);
        p2.setPlayerPositionX(1);
        p2.setPlayerPositionY(1);
        int p2ScoreBefore = p2.getPlayerScore();
        int p1ScoreBefore = p1.getPlayerScore();

        combatHandler.handleFreezeAttack(p1, p2);
        System.out.println("P2 frozen (expect true): " + p2.isFrozen());
        System.out.println("P2 score change (expect 0 — already at floor): " + (p2.getPlayerScore() - p2ScoreBefore));
        System.out.println("P1 score change (expect +25): " + (p1.getPlayerScore() - p1ScoreBefore));
        System.out.println("P1 armed after attack (expect false): " + p1.isArmed());
        System.out.println();

        // ── Test 6: Speed Boost ──────────────────────────────
        // p3 picks up speed boost, then enters zone B
        // expect: capture ticks halved (30 instead of 60)
        System.out.println("--- Test 6: Speed Boost ---");
        Item speedBoost = new Item("i2", ItemType.SPEED_BOOST, 7, 7);
        gameState.addItem(speedBoost);
        p3.setPlayerPositionX(7);
        p3.setPlayerPositionY(7);
        p3.setLastSeq(99);

        collisionHandler.update();
        System.out.println("P3 has speed boost (expect true): " + p3.isHasSpeedBoost());

        p3.setPlayerPositionX(15);
        p3.setPlayerPositionY(3);
        collisionHandler.update();
        Zone zoneB = gameState.getZones().get(1);
        System.out.println("Zone B capture ticks (expect 30): " + zoneB.getCaptureTicksLeft());
        System.out.println();

        // ── Test 7: Item Spawner ─────────────────────────────
        // spawner fires every 200 ticks — after 201 updates one item should have spawned
        System.out.println("--- Test 7: Item Spawner ---");
        int itemsBefore = gameState.getItems().size();
        for (int i = 0; i < 201; i++) {
            itemSpawner.update();
        }
        System.out.println("Items before: " + itemsBefore);
        System.out.println("Items after 201 ticks (expect " + (itemsBefore + 1) + "): " + gameState.getItems().size());
        System.out.println();

        // ── Test 8: Player Disconnect ────────────────────────
        // disconnecting p1 should remove them and clean up any zones they own
        System.out.println("--- Test 8: Player Disconnect ---");
        gameState.handlePlayerDisconnect(1);
        System.out.println("P1 still in game (expect false): " + (gameState.getPlayer(1) != null));
        System.out.println();

        // ── Test 9: Winner ───────────────────────────────────
        // p1 removed — winner should be whoever has the highest score among remaining players
        System.out.println("--- Test 9: Winner ---");
        Player winner = gameState.getWinner();
        System.out.println("Winner: "
                + (winner != null ? winner.getPlayerName() + " with " + winner.getPlayerScore() + " points" : "none"));

        System.out.println("\n=== Test Complete ===");
    }
}
