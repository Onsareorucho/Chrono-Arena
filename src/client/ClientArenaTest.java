package client;

import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import client.gui.GameScreen;
import shared.GameStateUpdate;
import shared.GameStateUpdate.ItemSnapshot;
import shared.GameStateUpdate.PlayerSnapshot;
import shared.GameStateUpdate.ZoneSnapshot;
import shared.ItemType;

/**
 * Test harness for client-side arena rendering.
 * Simulates game state updates without needing the server.
 * 
 * Run this to verify:
 * 1. Arena renders correctly
 * 2. Players display and move
 * 3. Zones render with different states
 * 4. Items appear correctly
 * 5. Animations work
 * 
 * Controls:
 * - WASD: Move player 1
 * - Arrow keys: Move player 2
 * - SPACE: Toggle player 1 frozen
 * - F: Toggle player 1 freeze ray
 * - B: Toggle player 1 speed boost
 * - 1/2/3: Spawn Energy/FreezeRay/SpeedBoost item
 * - Z: Cycle zone 1 state (unclaimed -> owned -> contested)
 */
public class ClientArenaTest {

    private static final int ARENA_WIDTH = 800;
    private static final int ARENA_HEIGHT = 600;
    
    private GameScreen gameScreen;
    private Timer updateTimer;
    
    // Simulated game state
    private float player1X = 200, player1Y = 300;
    private float player2X = 600, player2Y = 300;
    private boolean player1Frozen = false;
    private boolean player1HasFreezeRay = false;
    private boolean player1HasSpeedBoost = false;
    private int player1Score = 0;
    private int player2Score = 0;
    
    private List<ItemData> items = new ArrayList<>();
    private int nextItemId = 1;
    
    private int zone1State = 0; // 0=unclaimed, 1=owned by p1, 2=contested
    
    private long timeRemainingMs = 180000;
    private int tickNumber = 0;
    
    private Random random = new Random();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClientArenaTest().start();
        });
    }

    public void start() {
        JFrame frame = new JFrame("Client Arena Test - No Server Needed");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        gameScreen = new GameScreen();
        frame.add(gameScreen);
        frame.pack();
        frame.setLocationRelativeTo(null);
        
        // Add keyboard controls
        gameScreen.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e.getKeyCode());
            }
        });
        
        frame.setVisible(true);
        gameScreen.requestFocusInWindow();
        gameScreen.startGame();
        
        // Spawn some initial items
        spawnItem(ItemType.ENERGY, 400, 150);
        spawnItem(ItemType.FREEZE_RAY, 150, 450);
        spawnItem(ItemType.SPEED_BOOST, 650, 450);
        
        // Start simulated update loop (like server would send)
        updateTimer = new Timer(50, e -> {
            tickNumber++;
            timeRemainingMs -= 50;
            if (timeRemainingMs < 0) timeRemainingMs = 180000; // Reset timer
            
            sendSimulatedGameState();
        });
        updateTimer.start();
        
        printControls();
    }

    private void handleKeyPress(int keyCode) {
        float moveSpeed = 15;
        
        switch (keyCode) {
            // Player 1 movement (WASD)
            case KeyEvent.VK_W -> player1Y = Math.max(40, player1Y - moveSpeed);
            case KeyEvent.VK_S -> player1Y = Math.min(ARENA_HEIGHT - 40, player1Y + moveSpeed);
            case KeyEvent.VK_A -> player1X = Math.max(40, player1X - moveSpeed);
            case KeyEvent.VK_D -> player1X = Math.min(ARENA_WIDTH - 40, player1X + moveSpeed);
            
            // Player 2 movement (Arrow keys)
            case KeyEvent.VK_UP -> player2Y = Math.max(40, player2Y - moveSpeed);
            case KeyEvent.VK_DOWN -> player2Y = Math.min(ARENA_HEIGHT - 40, player2Y + moveSpeed);
            case KeyEvent.VK_LEFT -> player2X = Math.max(40, player2X - moveSpeed);
            case KeyEvent.VK_RIGHT -> player2X = Math.min(ARENA_WIDTH - 40, player2X + moveSpeed);
            
            // Toggle states
            case KeyEvent.VK_SPACE -> {
                player1Frozen = !player1Frozen;
                System.out.println("Player 1 frozen: " + player1Frozen);
                if (player1Frozen) {
                    gameScreen.showNotification("FROZEN!", Color.CYAN);
                }
            }
            case KeyEvent.VK_F -> {
                player1HasFreezeRay = !player1HasFreezeRay;
                System.out.println("Player 1 has freeze ray: " + player1HasFreezeRay);
            }
            case KeyEvent.VK_B -> {
                player1HasSpeedBoost = !player1HasSpeedBoost;
                System.out.println("Player 1 has speed boost: " + player1HasSpeedBoost);
            }
            
            // Spawn items
            case KeyEvent.VK_1 -> {
                spawnItem(ItemType.ENERGY, random.nextInt(700) + 50, random.nextInt(500) + 50);
                System.out.println("Spawned ENERGY item");
            }
            case KeyEvent.VK_2 -> {
                spawnItem(ItemType.FREEZE_RAY, random.nextInt(700) + 50, random.nextInt(500) + 50);
                System.out.println("Spawned FREEZE_RAY item");
            }
            case KeyEvent.VK_3 -> {
                spawnItem(ItemType.SPEED_BOOST, random.nextInt(700) + 50, random.nextInt(500) + 50);
                System.out.println("Spawned SPEED_BOOST item");
            }
            
            // Cycle zone state
            case KeyEvent.VK_Z -> {
                zone1State = (zone1State + 1) % 3;
                String[] states = {"UNCLAIMED", "OWNED BY P1", "CONTESTED"};
                System.out.println("Zone 1 state: " + states[zone1State]);
                gameScreen.showNotification("Zone: " + states[zone1State], Color.ORANGE);
            }
            
            // Add score
            case KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS -> {
                player1Score += 10;
                System.out.println("Player 1 score: " + player1Score);
            }
            
            // Clear items
            case KeyEvent.VK_C -> {
                items.clear();
                System.out.println("Cleared all items");
            }
        }
    }

    private void spawnItem(ItemType type, float x, float y) {
        items.add(new ItemData(nextItemId++, type, x, y));
    }

    private void sendSimulatedGameState() {
        // Build player snapshots
        List<PlayerSnapshot> players = new ArrayList<>();
        players.add(new PlayerSnapshot(
            1, "TestPlayer1", player1X, player1Y, player1Score,
            player1Frozen, player1HasSpeedBoost, player1HasFreezeRay
        ));
        players.add(new PlayerSnapshot(
            2, "TestPlayer2", player2X, player2Y, player2Score,
            false, false, false
        ));
        
        // Build zone snapshots
        List<ZoneSnapshot> zones = new ArrayList<>();
        int zone1Owner = (zone1State == 1) ? 1 : -1;
        int zone1Contested = (zone1State == 2) ? 2 : -1;
        float captureProgress = (zone1State == 2) ? 0.6f : 0f;
        
        zones.add(new ZoneSnapshot(1, 400, 300, 80, zone1Owner, zone1Contested, captureProgress));
        zones.add(new ZoneSnapshot(2, 150, 150, 60, -1, -1, 0));
        zones.add(new ZoneSnapshot(3, 650, 150, 60, 2, -1, 0));
        
        // Build item snapshots
        List<ItemSnapshot> itemSnapshots = new ArrayList<>();
        for (ItemData item : items) {
            itemSnapshots.add(new ItemSnapshot(item.id, item.type, item.x, item.y));
        }
        
        // Create and send game state
        GameStateUpdate state = new GameStateUpdate(
            players, zones, itemSnapshots, timeRemainingMs, tickNumber
        );
        
        gameScreen.updateState(state);
    }

    private void printControls() {
        System.out.println("===========================================");
        System.out.println("       CLIENT ARENA TEST - CONTROLS");
        System.out.println("===========================================");
        System.out.println("WASD        - Move Player 1 (blue)");
        System.out.println("Arrow Keys  - Move Player 2 (red)");
        System.out.println("SPACE       - Toggle Player 1 frozen");
        System.out.println("F           - Toggle freeze ray powerup");
        System.out.println("B           - Toggle speed boost powerup");
        System.out.println("1           - Spawn ENERGY item");
        System.out.println("2           - Spawn FREEZE_RAY item");
        System.out.println("3           - Spawn SPEED_BOOST item");
        System.out.println("Z           - Cycle zone state");
        System.out.println("+           - Add 10 points to Player 1");
        System.out.println("C           - Clear all items");
        System.out.println("===========================================");
        System.out.println("Positions are in PIXELS (0-800 x, 0-600 y)");
        System.out.println("===========================================");
    }

    // Helper class to store item data
    private static class ItemData {
        int id;
        ItemType type;
        float x, y;
        
        ItemData(int id, ItemType type, float x, float y) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }
}