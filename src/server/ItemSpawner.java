package server;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ItemSpawner {

    private static final int SPAWN_INTERVAL_TICKS = 200; // every 10 seconds
    private static final int MAX_ITEMS_ON_MAP     = 5;

    private final GameState gameState;
    private final int mapWidth;
    private final int mapHeight;
    private final Random random;
    private int ticksSinceLastSpawn;

    public ItemSpawner(GameState gameState, int mapWidth, int mapHeight) {
        this.gameState = gameState;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.random = new Random();
        this.ticksSinceLastSpawn = 0;
    }

    // called every tick by GameLoop
    public void update() {
        ticksSinceLastSpawn++;

        if (ticksSinceLastSpawn >= SPAWN_INTERVAL_TICKS) {
            ticksSinceLastSpawn = 0;

            // don't spawn if map is full
            if (gameState.getItems().size() >= MAX_ITEMS_ON_MAP) {
                System.out.println("Max items on map — skipping spawn");
                return;
            }

            spawnItem();
        }
    }

    private void spawnItem() {
        int[] position = getSafeSpawnPosition();
        if (position == null) {
            System.out.println("No safe spawn position found");
            return;
        }

        // pick a random item type
        ItemType[] types = ItemType.values();
        ItemType type = types[random.nextInt(types.length)];

        // unique ID for each item
        String itemId = "item-" + UUID.randomUUID().toString().substring(0, 8);

        Item item = new Item(itemId, type, position[0], position[1]);
        gameState.addItem(item);

        System.out.println("Spawned " + type + " at (" + position[0] + ", " + position[1] + ")");
    }

    private int[] getSafeSpawnPosition() {
        // snapshot once — avoids repeated lock cycles and list copies inside the loop
        List<Item>   items   = gameState.getItems();
        List<Zone>   zones   = gameState.getZones();
        var          players = gameState.getPlayers().values();

        for (int i = 0; i < 20; i++) {
            int x = random.nextInt(mapWidth);
            int y = random.nextInt(mapHeight);

            boolean itemThere   = items.stream()
                .anyMatch(item -> item.getItemPositionX() == x && item.getItemPositionY() == y);
            boolean playerThere = players.stream()
                .anyMatch(p -> p.getPlayerPositionX() == x && p.getPlayerPositionY() == y);
            boolean inZone      = zones.stream()
                .anyMatch(z -> x >= z.getZonePositionX() && x < z.getZonePositionX() + z.getZoneWidth()
                            && y >= z.getZonePositionY() && y < z.getZonePositionY() + z.getZoneHeight());

            if (!itemThere && !playerThere && !inZone) {
                return new int[]{x, y};
            }
        }
        return null;
    }
}