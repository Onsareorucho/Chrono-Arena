package client.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import shared.GameStateUpdate.PlayerSnapshot;

public class GameOverScreen extends JPanel implements Runnable {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int TARGET_FPS = 60;

    private static final Color BG_TOP = new Color(15, 10, 30);
    private static final Color BG_BOTTOM = new Color(40, 20, 50);
    private static final Color GOLD = new Color(255, 215, 0);
    private static final Color SILVER = new Color(192, 192, 192);
    private static final Color BRONZE = new Color(205, 127, 50);
    private static final Color ACCENT = new Color(0, 200, 255);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_SECONDARY = new Color(180, 180, 200);
    private static final Color PANEL_BG = new Color(40, 40, 70, 200);

    private Thread screenThread;
    private volatile boolean running = false;

    private String winnerName = "Unknown";
    private int winnerScore = 0;
    private List<PlayerSnapshot> finalStandings = new ArrayList<>();

    private static final int RETURN_DELAY_SECONDS = 10;
    private long gameOverStartTime;
    private int secondsRemaining = RETURN_DELAY_SECONDS;

    private float glowAnimation = 0;
    private float confettiOffset = 0;
    private long startTime;

    private Runnable onReturnToMenu;

    public GameOverScreen() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        startTime = System.currentTimeMillis();
    }

    public void showGameOver(String winnerName, int winnnerScore, List<PlayerSnapshot> standings) {
        this.winnerName = winnerName;
        this.winnerScore = winnnerScore;
        this.finalStandings = new ArrayList<>(standings);

        this.finalStandings.sort((a,b) -> Integer.compare(b.score, a.score));

        this.gameOverStartTime = System.currentTimeMillis();
        this.secondsRemaining = RETURN_DELAY_SECONDS;

        startScreen();
    }

    public void startScreen() {
        running = true;
        screenThread = new Thread(this, "gameover-thread");
        screenThread.start();
    }

    public void stopScreen() {
        running = false;
    }

    public void setOnReturnToMenu(Runnable callback) {
        this.onReturnToMenu = callback;
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

    public void update() {
        glowAnimation += 0.05f;
        if (glowAnimation > Math.PI * 2) glowAnimation = 0;

        confettiOffset += 2;
        if (confettiOffset > HEIGHT) confettiOffset = 0;
 
        long elapsedMs = System.currentTimeMillis() - gameOverStartTime;
        secondsRemaining = RETURN_DELAY_SECONDS - (int)(elapsedMs / 1000);
 
        if (secondsRemaining <= 0) {
            running = false;
            if (onReturnToMenu != null) {
                onReturnToMenu.run();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
 
        GradientPaint gradient = new GradientPaint(0, 0, BG_TOP, 0, HEIGHT, BG_BOTTOM);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
 
        drawConfetti(g2d);
 
        drawTitle(g2d);
        drawWinner(g2d);
        drawScoreboard(g2d);
        drawCountdown(g2d);
    }

    private void drawConfetti(Graphics2D g2d) {
        Color[] confettiColors = {GOLD, ACCENT, Color.MAGENTA, Color.GREEN, Color.ORANGE, SILVER};
        
        for (int i = 0; i < 50; i++) {
            float x = (i * 73) % WIDTH;
            float y = ((i * 47) + confettiOffset) % (HEIGHT + 50) - 25;
            
            int colorIndex = i % confettiColors.length;
            int alpha = 100 + (i % 100);
            Color c = confettiColors[colorIndex];
            g2d.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            
            if (i % 3 == 0) {
                g2d.fillRect((int)x, (int)y, 8, 8);
            } else if (i % 3 == 1) {
                g2d.fillOval((int)x, (int)y, 6, 6);
            } else {
                int[] xPoints = {(int)x, (int)x + 4, (int)x + 8};
                int[] yPoints = {(int)y + 8, (int)y, (int)y + 8};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }
        }
    }

    private void drawTitle(Graphics2D g2d) {
        String title = "GAME OVER";
        
        g2d.setFont(new Font("Arial", Font.BOLD, 64));
        FontMetrics fm = g2d.getFontMetrics();
        int titleX = (WIDTH - fm.stringWidth(title)) / 2;
        int titleY = 100;
 
        // Glow effect
        float glowIntensity = (float) (Math.sin(glowAnimation) + 1) / 2 * 0.5f + 0.5f;
        Color glowColor = new Color(
            (int) (GOLD.getRed() * glowIntensity),
            (int) (GOLD.getGreen() * glowIntensity),
            (int) (GOLD.getBlue() * glowIntensity),
            100
        );
 
        // Draw glow layers
        g2d.setColor(glowColor);
        for (int i = 5; i > 0; i--) {
            g2d.drawString(title, titleX - i, titleY);
            g2d.drawString(title, titleX + i, titleY);
            g2d.drawString(title, titleX, titleY - i);
            g2d.drawString(title, titleX, titleY + i);
        }
 
        // Draw main title
        g2d.setColor(TEXT_PRIMARY);
        g2d.drawString(title, titleX, titleY);
    }

    private void drawWinner(Graphics2D g2d) {
        int centerX = WIDTH / 2;
        int y = 180;
 
        // Trophy/Crown emoji (using text)
        g2d.setFont(new Font("Arial", Font.PLAIN, 48));
        String trophy = "👑";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(trophy, centerX - fm.stringWidth(trophy)/2, y);
 
        // Winner text
        y += 60;
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        fm = g2d.getFontMetrics();
        
        String winnerText = "WINNER";
        g2d.setColor(GOLD);
        g2d.drawString(winnerText, centerX - fm.stringWidth(winnerText)/2, y);
 
        // Winner name
        y += 50;
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        fm = g2d.getFontMetrics();
        
        // Pulsing effect for winner name
        float pulse = (float) (Math.sin(glowAnimation * 2) + 1) / 2 * 0.3f + 0.7f;
        g2d.setColor(new Color(
            (int)(255 * pulse), 
            (int)(215 * pulse), 
            (int)(0 * pulse + 50)
        ));
        g2d.drawString(winnerName, centerX - fm.stringWidth(winnerName)/2, y);
 
        // Winner score
        y += 40;
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        fm = g2d.getFontMetrics();
        String scoreText = winnerScore + " points";
        g2d.setColor(TEXT_SECONDARY);
        g2d.drawString(scoreText, centerX - fm.stringWidth(scoreText)/2, y);
    }

    private void drawScoreboard(Graphics2D g2d) {
        int panelX = WIDTH / 2 - 150;
        int panelY = 360;
        int panelWidth = 300;
        int panelHeight = 40 + (finalStandings.size() * 35);
 
        // Panel background
        g2d.setColor(PANEL_BG);
        g2d.fill(new RoundRectangle2D.Float(panelX, panelY, panelWidth, panelHeight, 10, 10));
 
        // Panel border
        g2d.setColor(new Color(80, 80, 120));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(new RoundRectangle2D.Float(panelX, panelY, panelWidth, panelHeight, 10, 10));
 
        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(TEXT_PRIMARY);
        g2d.drawString("FINAL STANDINGS", panelX + 15, panelY + 25);
 
        // Divider
        g2d.setColor(new Color(80, 80, 120));
        g2d.drawLine(panelX + 10, panelY + 35, panelX + panelWidth - 10, panelY + 35);
 
        // Player entries
        int yOffset = panelY + 60;
        int rank = 1;
 
        for (PlayerSnapshot player : finalStandings) {
            drawScoreEntry(g2d, panelX + 15, yOffset, rank, player.name, player.score);
            yOffset += 35;
            rank++;
        }
    }

    private void drawScoreEntry(Graphics2D g2d, int x, int y, int rank, String name, int score) {
        // Rank color
        Color rankColor;
        switch (rank) {
            case 1 -> rankColor = GOLD;
            case 2 -> rankColor = SILVER;
            case 3 -> rankColor = BRONZE;
            default -> rankColor = TEXT_SECONDARY;
        }
 
        // Rank badge
        g2d.setColor(rankColor);
        g2d.fillOval(x, y - 12, 20, 20);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        String rankStr = String.valueOf(rank);
        g2d.drawString(rankStr, x + 10 - fm.stringWidth(rankStr)/2, y + 3);
 
        // Player name
        g2d.setColor(rank == 1 ? GOLD : TEXT_PRIMARY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString(name, x + 30, y + 5);
 
        // Score (right-aligned)
        String scoreStr = score + " pts";
        fm = g2d.getFontMetrics();
        g2d.setColor(TEXT_SECONDARY);
        g2d.drawString(scoreStr, x + 230, y + 5);
    }

    private void drawCountdown(Graphics2D g2d) {
        String countdownText = "Returning to menu in " + Math.max(0, secondsRemaining) + "s...";
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(countdownText)) / 2;
        int y = HEIGHT - 40;
 
        g2d.setColor(TEXT_SECONDARY);
        g2d.drawString(countdownText, x, y);
 
        // Progress bar
        int barWidth = 200;
        int barHeight = 6;
        int barX = (WIDTH - barWidth) / 2;
        int barY = y + 10;
 
        // Background
        g2d.setColor(new Color(60, 60, 80));
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 3, 3);
 
        // Progress
        float progress = (float) secondsRemaining / RETURN_DELAY_SECONDS;
        int fillWidth = (int) (barWidth * progress);
        g2d.setColor(ACCENT);
        g2d.fillRoundRect(barX, barY, fillWidth, barHeight, 3, 3);
    }
}
