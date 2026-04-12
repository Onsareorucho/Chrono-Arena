package client.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import shared.GameStateUpdate;
import shared.GameStateUpdate.PlayerSnapshot;

/**
 * LobbyScreen - Waiting room before the game starts.
 * 
 * Shows:
 * - "X/4 players - Waiting for others..." status
 * - Players can move around in a practice area
 * - List of connected players
 * - Ready status
 * 
 * Transitions to GameScreen when game starts.
 */
public class LobbyScreen extends JPanel implements Runnable {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int TARGET_FPS = 60;

    // Colors
    private static final Color ACCENT = new Color(201, 180, 117);
    private static final Color TEXT_PRIMARY = new Color(255, 210, 87);
    private static final Color TEXT_SECONDARY = new Color(255, 242, 201);
    private static final Color PANEL_BG = new Color(201, 180, 117, 200);
    private static final Color ARENA_BG = new Color(169, 145, 71);
    private static final Color ARENA_BORDER = new Color(201, 180, 117);

    private Thread lobbyThread;
    private volatile boolean running = false;

    private SpriteManager spriteManager;

    // Lobby state
    private int currentPlayerCount = 1;
    private int maxPlayers = 2;
    private int localPlayerId = -1;
    private String localPlayerName = "Player";
    private List<PlayerSnapshot> players = new ArrayList<>();

    private float dotAnimation = 0;
    private long startTime;

    private Runnable onGameStart;

    private static final int ARENA_X = 50;
    private static final int ARENA_Y = 120;
    private static final int ARENA_WIDTH = 500;
    private static final int ARENA_HEIGHT = 400;

    private static final int PLAYER_SIZE = 60;

    public LobbyScreen() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        startTime = System.currentTimeMillis();

        spriteManager = new SpriteManager();
        spriteManager.loadAllSprites();
    }

    public void startLobby() {
        running = true;
        lobbyThread = new Thread(this, "lobby-thread");
        lobbyThread.start();
    }

    public void stopLobby() {
        running = false;
    }

    @Override
    public void run() {
        long frameTime = 1000 / TARGET_FPS;

        while (running) {
            long frameStart = System.currentTimeMillis();

            update();
            repaint();

            long elapsed = System.currentTimeMillis() - frameStart;
            long sleepTime = frameTime - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void update() {
        // Animate the dots
        dotAnimation += 0.05f;
        if (dotAnimation > 3) dotAnimation = 0;
    }

    public void updateState(GameStateUpdate state) {
        if (state != null) {
            this.players = state.getPlayers();
            this.currentPlayerCount = players.size();
        }
    }

    public void setPlayerCount(int count) {
        this.currentPlayerCount = count;
    }

    public void setMaxPlayers(int max) {
        this.maxPlayers = max;
    }

    public void setLocalPlayerId(int id) {
        this.localPlayerId = id;
    }

    public void setLocalPlayerName(String name) {
        this.localPlayerName = name;
    }

    public void setOnGameStart(Runnable callback) {
        this.onGameStart = callback;
    }

    public void triggerGameStart() {
        if (onGameStart != null) {
            onGameStart.run();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (spriteManager.hasSprite("lobby_background")) {
            BufferedImage bg = spriteManager.getSprite("lobby_background");
            g2d.drawImage(bg, 0, 0, WIDTH, HEIGHT, null);
        } else {
            g2d.setColor(new Color(30, 30, 50));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }

        drawHeader(g2d);
        drawPracticeArena(g2d);
        drawPlayers(g2d);
        drawPlayerList(g2d);
        drawInstructions(g2d);
    }

    private void drawHeader(Graphics2D g2d) {
        String countText = currentPlayerCount + "/" + maxPlayers + " players";
        String statusText;
        
        if (currentPlayerCount >= maxPlayers) {
            statusText = "Starting soon!";
        } else {
            int dots = (int) dotAnimation;
            statusText = "Waiting for others" + ".".repeat(dots + 1);
        }

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fm = g2d.getFontMetrics();
        
        String fullStatus = countText + " — " + statusText;
        int statusX = (WIDTH - fm.stringWidth(fullStatus)) / 2;
        
        g2d.setColor(ACCENT);
        g2d.drawString(countText, statusX, 85);
        
        g2d.setColor(TEXT_SECONDARY);
        int dashX = statusX + fm.stringWidth(countText);
        g2d.drawString(" — " + statusText, dashX, 85);
    }

    private void drawPracticeArena(Graphics2D g2d) {
        g2d.setColor(ARENA_BG);
        g2d.fill(new RoundRectangle2D.Float(ARENA_X, ARENA_Y, ARENA_WIDTH, ARENA_HEIGHT, 15, 15));

        g2d.setColor(ARENA_BORDER);
        g2d.setStroke(new BasicStroke(3));
        g2d.draw(new RoundRectangle2D.Float(ARENA_X, ARENA_Y, ARENA_WIDTH, ARENA_HEIGHT, 15, 15));

        g2d.setColor(new Color(60, 60, 90, 50));
        g2d.setStroke(new BasicStroke(1));
        int gridSize = 50;
        for (int x = ARENA_X + gridSize; x < ARENA_X + ARENA_WIDTH; x += gridSize) {
            g2d.drawLine(x, ARENA_Y, x, ARENA_Y + ARENA_HEIGHT);
        }
        for (int y = ARENA_Y + gridSize; y < ARENA_Y + ARENA_HEIGHT; y += gridSize) {
            g2d.drawLine(ARENA_X, y, ARENA_X + ARENA_WIDTH, y);
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(TEXT_SECONDARY);
        g2d.drawString("Practice Area - Move around with WASD", ARENA_X + 10, ARENA_Y + ARENA_HEIGHT - 10);
    }

    private void drawPlayers(Graphics2D g2d) {
        long elapsed = System.currentTimeMillis() - startTime;

        if (players.isEmpty()) {
            float bobOffset = (float) Math.sin(elapsed * 0.003) * 3;
            int x = ARENA_X + ARENA_WIDTH / 2;
            int y = ARENA_Y + ARENA_HEIGHT / 2 + (int) bobOffset;

            drawSinglePlayer(g2d, x, y, 1, localPlayerName, false, true, elapsed);
        } else {
            for (PlayerSnapshot player : players) {
                int x = ARENA_X + (int) (player.x * 30);
                int y = ARENA_Y + (int) (player.y * 30);

                x = Math.max(ARENA_X + PLAYER_SIZE/2, Math.min(ARENA_X + ARENA_WIDTH - PLAYER_SIZE/2, x));
                y = Math.max(ARENA_Y + PLAYER_SIZE/2, Math.min(ARENA_Y + ARENA_HEIGHT - PLAYER_SIZE/2, y));

                boolean isLocal = player.playerId == localPlayerId;
                float bobOffset = isLocal ? (float) Math.sin(elapsed * 0.003) * 3 : 0;
                
                drawSinglePlayer(g2d, x, y + (int) bobOffset, player.playerId, 
                                player.name, player.isFrozen, isLocal, elapsed);
            }
        }
    }

    private void drawSinglePlayer(Graphics2D g2d, int x, int y, int playerId, String name, boolean isFrozen, boolean isLocal, long elapsed) {
        String animName;
        int frameSpeed;

        if (isFrozen) {
            animName = "player_" + playerId + "_frozen";
            frameSpeed = 300;
        } else {
            animName = "player_" + playerId + "_idle";
            frameSpeed = 400;
        }

        BufferedImage frame = null;

        // Try to get animation frame
        if (spriteManager.hasAnimation(animName)) {
            frame = spriteManager.getAnimationFrames(animName, elapsed, frameSpeed);
        } else if (spriteManager.hasSprite(animName)) {
            frame = spriteManager.getSprite(animName);
        }

        // Draw player sprite or fallback circle
        if (frame != null) {
            g2d.drawImage(frame, x - PLAYER_SIZE/2, y - PLAYER_SIZE/2, PLAYER_SIZE, PLAYER_SIZE, null);
        } else {
            // Fallback: draw colored circle
            Color playerColor = spriteManager.getPlayerColor(playerId - 1);

            // Shadow
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.fillOval(x - PLAYER_SIZE/2 + 3, y - PLAYER_SIZE/2 + 3, PLAYER_SIZE, PLAYER_SIZE);

            // Player body
            g2d.setColor(playerColor);
            g2d.fillOval(x - PLAYER_SIZE/2, y - PLAYER_SIZE/2, PLAYER_SIZE, PLAYER_SIZE);

            // Highlight
            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.fillOval(x - PLAYER_SIZE/4, y - PLAYER_SIZE/4, PLAYER_SIZE/3, PLAYER_SIZE/3);

            // Border
            g2d.setColor(new Color(255, 255, 255, 150));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x - PLAYER_SIZE/2, y - PLAYER_SIZE/2, PLAYER_SIZE, PLAYER_SIZE);

            // Frozen overlay
            if (isFrozen) {
                g2d.setColor(new Color(0, 200, 255, 100));
                g2d.fillOval(x - PLAYER_SIZE/2, y - PLAYER_SIZE/2, PLAYER_SIZE, PLAYER_SIZE);
            }
        }

        // Name above player
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int nameWidth = fm.stringWidth(name);
        
        // Name background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(x - nameWidth/2 - 4, y - PLAYER_SIZE/2 - 20, nameWidth + 8, 16, 5, 5);
        
        // Name text
        g2d.setColor(isLocal ? ACCENT : TEXT_PRIMARY);
        g2d.drawString(name, x - nameWidth/2, y - PLAYER_SIZE/2 - 8);

        // "YOU" indicator for local player
        if (isLocal) {
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.setColor(ACCENT);
            String youText = "▼ YOU";
            int youWidth = g2d.getFontMetrics().stringWidth(youText);
            g2d.drawString(youText, x - youWidth/2, y - PLAYER_SIZE/2 - 32);
        }
    }

    private void drawPlayerList(Graphics2D g2d) {
        int panelX = 580;
        int panelY = 120;
        int panelWidth = 180;
        int panelHeight = 200;

        // Panel background
        g2d.setColor(PANEL_BG);
        g2d.fill(new RoundRectangle2D.Float(panelX, panelY, panelWidth, panelHeight, 10, 10));

        // Panel border
        g2d.setColor(ARENA_BORDER);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(new RoundRectangle2D.Float(panelX, panelY, panelWidth, panelHeight, 10, 10));

        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(TEXT_PRIMARY);
        g2d.drawString("PLAYERS", panelX + 15, panelY + 25);

        // Divider
        g2d.setColor(ARENA_BORDER);
        g2d.drawLine(panelX + 10, panelY + 35, panelX + panelWidth - 10, panelY + 35);

        // Player list
        g2d.setFont(new Font("Arial", Font.PLAIN, 13));
        int yOffset = panelY + 55;

        if (players.isEmpty()) {
            // Show local player if no state yet
            drawPlayerListEntry(g2d, panelX + 15, yOffset, localPlayerName, 1, true);
            yOffset += 30;

            // Empty slots
            for (int i = 1; i < maxPlayers; i++) {
                g2d.setColor(new Color(100, 100, 120));
                g2d.drawString("• Waiting...", panelX + 15, yOffset);
                yOffset += 25;
            }
        } else {
            for (PlayerSnapshot player : players) {
                boolean isLocal = player.playerId == localPlayerId;
                drawPlayerListEntry(g2d, panelX + 15, yOffset, player.name, 
                                   player.playerId, isLocal);
                yOffset += 30;
            }

            // Empty slots
            for (int i = players.size(); i < maxPlayers; i++) {
                g2d.setColor(new Color(100, 100, 120));
                g2d.drawString("• Waiting...", panelX + 15, yOffset);
                yOffset += 25;
            }
        }
    }

    private void drawPlayerListEntry(Graphics2D g2d, int x, int y, String name, 
                                      int playerId, boolean isLocal) {
        Color playerColor = spriteManager.getPlayerColor(playerId - 1);

        // Color dot
        g2d.setColor(playerColor);
        g2d.fillOval(x, y - 10, 12, 12);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawOval(x, y - 10, 12, 12);

        // Name
        g2d.setColor(isLocal ? ACCENT : TEXT_PRIMARY);
        g2d.drawString(name + (isLocal ? " (You)" : ""), x + 20, y);
    }

    private void drawInstructions(Graphics2D g2d) {
        int panelX = 580;
        int panelY = 350;
        int panelWidth = 180;
        int panelHeight = 180;

        // Panel background
        g2d.setColor(PANEL_BG);
        g2d.fill(new RoundRectangle2D.Float(panelX, panelY, panelWidth, panelHeight, 10, 10));

        // Panel border
        g2d.setColor(ARENA_BORDER);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(new RoundRectangle2D.Float(panelX, panelY, panelWidth, panelHeight, 10, 10));

        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(TEXT_PRIMARY);
        g2d.drawString("CONTROLS", panelX + 15, panelY + 25);

        // Divider
        g2d.setColor(ARENA_BORDER);
        g2d.drawLine(panelX + 10, panelY + 35, panelX + panelWidth - 10, panelY + 35);

        // Instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(TEXT_SECONDARY);
        
        String[] instructions = {
            "WASD - Move",
            "SPACE - Freeze Ray",
            "E - Use Powerup",
            "",
            "Capture zones to",
            "earn points!",
            "",
            "Freeze enemies to",
            "slow them down!"
        };

        int yOffset = panelY + 45;
        for (String line : instructions) {
            g2d.drawString(line, panelX + 15, yOffset);
            yOffset += 16;
        }
    }
}