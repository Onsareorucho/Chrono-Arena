package client.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JFrame;
import javax.swing.JPanel;

import shared.GameConfig;
import shared.GameStateUpdate;

public class GameScreen extends JPanel implements Runnable {
    
    private static final int BASE_WIDTH = 800;
    private static final int BASE_HEIGHT = 600;
    private static final int TARGET_FPS = 60;

    private Thread gameThread;
    private boolean running = false;

    private SpriteManager spriteManager;
    private ArenaRenderer arenaRenderer;
    private HUDRenderer hudRenderer;

    private GameStateUpdate gameState;

    private long gameStartTime;
    private long gameDurationMs = 180000;

    private String notificationText = null;
    private Color notificationColor = Color.WHITE;
    private long notificationEndTime = 0;
    private static final long NOTIFICATION_DURATION_MS = 2000;

    public GameScreen() {
        setPreferredSize(new Dimension(BASE_WIDTH, BASE_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        spriteManager = new SpriteManager();
        spriteManager.loadAllSprites();

        arenaRenderer = new ArenaRenderer(spriteManager);
        hudRenderer = new HUDRenderer(spriteManager);

        gameState = null;
        gameStartTime = System.currentTimeMillis();

        try {
            GameConfig config = new GameConfig();
            gameDurationMs = config.getGameDurationSeconds() * 1000L;
        } catch (Exception e) {

        }
    }

    public void startGame() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void stopGame() {
        running = false;
    }

    @Override 
    public void run() {
        long frameTime = 1000 / TARGET_FPS;

        while (running) {
            long startTime = System.currentTimeMillis();

            update();
            repaint();

            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = frameTime - elapsed;
            if (sleepTime > 0) {
                try { 
                    Thread.sleep(sleepTime); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void update() {
        if (notificationText != null && System.currentTimeMillis() > notificationEndTime) {
            notificationText = null;
        }
    }

    public void updateState(GameStateUpdate newState) {
        this.gameState = newState;
    }

    public void setLocalPlayerId(int playerId) {
        hudRenderer.setLocalPlayerId(playerId);
    }

    public void showNotification(String text, Color color) {
        this.notificationText = text;
        this.notificationColor = color;
        this.notificationEndTime = System.currentTimeMillis() + NOTIFICATION_DURATION_MS;
    }

    public long getLocalTimeRemainingMs() {
        long elapsed = System.currentTimeMillis() - gameStartTime;
        return Math.max(0, gameDurationMs - elapsed);
    }

    public void setGameDuration(long durationMs) {
        this.gameDurationMs = durationMs;
    }

    public void resetTimer() {
        this.gameStartTime = System.currentTimeMillis();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        double scaleX = getWidth() / (double) BASE_WIDTH;
        double scaleY = getHeight() / (double) BASE_HEIGHT;
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) (BASE_WIDTH * scale);
        int scaledHeight = (int) (BASE_HEIGHT * scale);
        int offsetX = (getWidth() - scaledWidth)/2;
        int offsetY = (getHeight() - scaledHeight)/2;

        g2d.translate(offsetX, offsetY);
        g2d.scale(scale, scale);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    
        arenaRenderer.render(g2d, gameState);
        hudRenderer.render(g2d, gameState, BASE_WIDTH, BASE_HEIGHT);

        if (notificationText != null) {
            drawNotification(g2d);
        }
    }

    public void drawNotification(Graphics2D g2d) {
        long timeLeft = notificationEndTime - System.currentTimeMillis();
        float alpha = Math.min(1.0f, timeLeft / 500.0f);

        Font notifyFont = new Font("Arial", Font.BOLD, 48);
        g2d.setFont(notifyFont);
        FontMetrics fm = g2d.getFontMetrics();

        int textWidth = fm.stringWidth(notificationText);
        int x = (BASE_WIDTH - textWidth) / 2;
        int y = BASE_HEIGHT / 3;

        g2d.setColor(new Color(0, 0, 0, (int)(200 * alpha)));
        g2d.drawString(notificationText, x + 3, y + 3);

        g2d.setColor(new Color(notificationColor.getRed(), notificationColor.getGreen(), notificationColor.getBlue(), (int)(255 * alpha)));
        g2d.drawString(notificationText, x, y);
    }
    public static void main(String[] args) {
        JFrame frame = new JFrame("ChronoArena");
        GameScreen game = new GameScreen();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        game.startGame();
    }
}