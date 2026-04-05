package client.gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class HUDRenderer {

    private SpriteManager spriteMangaer;

    public HUDRenderer(SpriteManager spriteManager) {
        this.spriteMangaer = spriteManager;
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

    private void drawScoreboard() { 
        
    }

    private void drawLocalPlayerHUD() {

    }

    private void drawControlHints() {

    }

    private void drawCooldownIndicator() {

    }

}