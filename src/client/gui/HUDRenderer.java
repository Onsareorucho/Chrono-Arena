package client.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import shared.GameStateUpdate;
import shared.GameStateUpdate.PlayerSnapshot;

public class HUDRenderer {

    private SpriteManager spriteManager;

    private int localPlayerId = 1;

    public HUDRenderer(SpriteManager spriteManager) {
        this.spriteManager = spriteManager;
    }

    public void setLocalPlayerId(int playerId) {
        this.localPlayerId = playerId;
    }

    public void render(Graphics2D g2d, GameStateUpdate gameState, int screenWidth, int screenHeight) {
        
        if (gameState == null) {
            drawWaitingScreen(g2d, screenWidth, screenHeight);
            return;
        }

        drawTimer(g2d, gameState.getTimeRemainingMs(), screenWidth);
        drawScoreboard(g2d, gameState, screenWidth);
        drawLocalPlayerHUD(g2d, gameState, screenWidth);
        drawControlHints(g2d, screenHeight);
        drawCooldownIndicator(g2d, gameState, screenWidth, screenHeight);
    }

    private void drawWaitingScreen(Graphics2D g2d, int screenWidth, int screenHeight) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(screenWidth/2 - 150, screenHeight/2 - 30, 300, 60, 10, 10);
    
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        String msg = "Waiting for game state...";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (screenWidth - fm.stringWidth(msg)) / 2;
        g2d.drawString(msg, x, screenHeight/2 + 8);
    }

    private void drawTimer(Graphics2D g2d, long timeMs, int screenWidth) {
        int seconds = (int) (timeMs / 1000);
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

    private void drawScoreboard(Graphics2D g2d, GameStateUpdate state, int screenWidth) { 
        List<PlayerSnapshot> players = state.getPlayers();
        
        g2d.setColor(new Color( 0, 0, 0, 180));
        g2d.fillRoundRect(screenWidth - 145, 10, 135, 30 + (players.size() *25), 10, 10);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.WHITE);
        g2d.drawString("SCORES", screenWidth - 130, 32);

        List<PlayerSnapshot> sorted = new ArrayList<>(players);
        sorted.sort((a,b) -> b.score - a.score);

        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        int yPos = 55;

        for(PlayerSnapshot player : sorted) {
            Color playerColor = spriteManager.getPlayerColor(player.playerId - 1);
            g2d.setColor(playerColor);
            g2d.fillOval(screenWidth - 138, yPos - 10, 12, 12);

            g2d.setColor(Color.WHITE);
            g2d.drawString(player.name + ": " + player.score, screenWidth - 120, yPos);
            yPos += 25;
        }
    }

    private void drawLocalPlayerHUD(Graphics2D g2d, GameStateUpdate gameState, int screenWidth) {
        PlayerSnapshot localPlayer = null;
        for (PlayerSnapshot p : gameState.getPlayers()) {
            if (p.playerId == localPlayerId) {
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

        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Score: " + localPlayer.score, 20, 52);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        int statusY = 72;

        if (localPlayer.isFrozen) {
            g2d.setColor(Color.CYAN);
            g2d.drawString("FROZEN!", 20, statusY);
            statusY += 16;
        }
 
        if (localPlayer.hasSpeedBoost) {
            g2d.setColor(Color.MAGENTA);
            g2d.drawString("SPEED BOOST", 20, statusY);
            statusY += 16;
        }

        if (localPlayer.hasFreezeRay) {
            g2d.setColor(Color.BLUE);
            g2d.drawString("FREEZE RAY READY", 20, statusY);
        } else {
            g2d.setColor(Color.GRAY);
            g2d.drawString("No weapon", 20, statusY);
        }
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

    private void drawCooldownIndicator(Graphics2D g2d, GameStateUpdate gameState, int screenWidth, int screenHeight) {
        int size = 50;
        int x = screenWidth - 70;
        int y = screenHeight - 70; 

        PlayerSnapshot localPlayer = null;
        for (PlayerSnapshot p : gameState.getPlayers()) {
            if(p.playerId == localPlayerId){
                localPlayer = p;
                break;
            }
        }

        boolean isReady = localPlayer != null && localPlayer.hasFreezeRay;

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