package client.gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class HUDRenderer {

    private SpriteManager spriteManager;

    public HUDRenderer(SpriteManager spriteManager) {
        this.spriteManager = spriteManager;
    }

    public void render(Graphics2D g2d, GameState gameState, int screenWidth, int screenHeight) {
        drawTimer(g2d, gameState.timeRemaningSeconds, screenWidth);
        drawScoreboard(g2d, gameState, screenWidth);
        drawLocalPlayerHUD(g2d, gameState, screenWidth);
        drawControlHints(g2d, screenHeight);
        drawCooldownIndicator(g2d, screenWidth, screenHeight);
    }

    private void drawTimer(Graphics2D g2d, int seconds, int screenWidth) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        String timeStr = String.format("TIME LEFT: %02d:%02d", minutes, secs);

        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(screenWidth/2 - 100, 10, 200, 40, 10, 10); 

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(seconds <= 30 ? Color.RED : Color.WHITE);

        FontMetrics fm = g2d.getFontMetrics();
        int x = (screenWidth - fm.stringWidth(timeStr))/2;
        g2d.drawString(timeStr, x, 38);
    }

    private void drawScoreboard(Graphics2D g2d, GameState state, int screenWidth) { 
        g2d.setColor(new Color( 0, 0, 0, 180));
        g2d.fillRoundRect(screenWidth - 145, 10, 135, 30 + (state.players.size() *25), 10, 10);
        g2d.drawString("SCORES", screenWidth - 132, 32);

        List<Player> sorted = new ArrayList<>(state.players);
        sorted.sort((a,b) -> b.score - a.score);

        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        int yPos = 55;

        for(Player player : sorted) {
            Color playerColor = spriteManager.getPlayerColor(player.id - 1);
            g2d.setColor(playerColor);
            g2d.fillOval(screenWidth - 138, yPos - 10, 12, 12);

            g2d.setColor(Color.WHITE);
            g2d.drawString(player.name + ": " + player.score, screenWidth - 120, yPos);
            yPos += 25;
        }
    }

    private void drawLocalPlayerHUD(Graphics2D g2d, Gamestate gameState, int screenWidth) {
        Player localPlayer = null;
        for (Player p : gameState.players) {
            if (p.id == 1) {
                localPlayer = p;
                break;
            }
        }
        if (localPlayer == null) return;

        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(10, 10, 150, 80, 10, 10);

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.WHITE);
        g2d.drawString(localPlayer.name, 20, 32);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("HP:", 20, 52);

        int barX = 50;
        int barY = 42;
        int barWidth = 100;
        int barHeight = 14;

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(barX, barY, barWidth, barHeight);

        int hp = localPlayer.hp;
        g2d.setColor(hp > 50 ? Color.GREEN : (hp > 25 ? Color.YELLOW : Color.RED));
        int fillWidth = (int) ((hp/100.0) * barWidth);
        g2d.fillRect(barX, barY, fillWidth, barHeight);

        g2d.setColor(Color.WHITE);
        g2d.drawRect(barX, barY, barWidth, barHeight);

        g2d.drawString("Score: " + localPlayer.score, 20, 78);
    }

    private void drawControlHints(Graphics2D g2d, int screenHeight) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, screenHeight - 100, 160, 90, 10, 10);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(200, 200, 200));

        String[] hints = {
            "WASD / Arrows - Move",
            "SPACE - Freeze Ray",
            "E - Collect Item",
            "ESC - Menu"
        };

        int yPos = screenHeight - 80;
        for (String hint : hints) {
            g2d.drawString(hint, 20, yPos);
            yPos += 18;
        }
    }

    private void drawCooldownIndicator(Graphics2D g2d, int screenWidth, int screenHeight) {
        int size = 50;
        int x = screenWidth - 70;
        int y = screenHeight - 70; 

        boolean isReady = true; // TODO: get from game state

        String spriteName = isReady ? "cooldown_ready" : "cooldown_active";

        if (spriteManager.hasSprite(spriteName)) {
            BufferedImage sprite = spriteManager.getSprite(spriteName);
            g2d.drawImage(sprite, x, y, size, size, null);
        } else {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillOval(x, y, size, size);
            
            g2d.setColor(isReady ? Color.GREEN : Color.GRAY);
            g2d.fillOval(x + 5, y+ 5, size - 10, size - 10);

            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x, y, size, size);

            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("FREEZE", x + 5, y+ size + 15);
        }
    }

}