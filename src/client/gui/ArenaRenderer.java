package client.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import shared.GameStateUpdate;
import shared.GameStateUpdate.ItemSnapshot;
import shared.GameStateUpdate.PlayerSnapshot;
import shared.GameStateUpdate.ZoneSnapshot;

public class ArenaRenderer {

    private static final Color ZONE_UNCLAIMED = new Color(128, 128, 128, 100);
    private static final Color ZONE_OWNED = new Color(0, 200, 0, 100);
    private static final Color ZONE_CONTESTED = new Color(255, 165, 0, 100);
    private static final Color ITEM_ENERGY = Color.YELLOW;
    private static final Color ITEM_FREEZE = Color.CYAN;
    private static final Color ITEM_SPEED = Color.MAGENTA;

    private SpriteManager spriteManager;

    private long startTime = System.currentTimeMillis();

    public ArenaRenderer(SpriteManager spriteManager) { 
        this.spriteManager = spriteManager;
    }

    // TODO: replace gamestate with official gamestate
    public void render(Graphics2D g2d, GameStateUpdate gameState) {
        drawBackground(g2d, 800, 600);
        if (gameState != null) {
            drawZones(g2d, gameState);
            drawItems(g2d, gameState);
            drawPlayers(g2d, gameState);
        }
    }

    private void drawBackground(Graphics2D g2d, int width, int height) {
        if (spriteManager.hasSprite("arena_background")) {
            BufferedImage bg = spriteManager.getSprite("arena_background");
            g2d.drawImage(bg, 0, 0, width, height, null);
        }
    }

    private void drawZones(Graphics2D g2d, GameStateUpdate state) {
        for(ZoneSnapshot zone : state.getZones()) {
            
            boolean isContested = zone.contestedById != -1;
            
            if (isContested) {
                g2d.setColor(ZONE_CONTESTED);
            } else if (zone.ownerId == -1) { 
                g2d.setColor(ZONE_UNCLAIMED);
            } else {
                g2d.setColor(ZONE_OWNED);
            }

            int zoneX = (int) (zone.x - zone.radius);
            int zoneY = (int) (zone.y - zone.radius);
            int zoneWidth = (int) (zone.radius*2);
            int zoneHeight = (int) (zone.radius*2);

            g2d.fillRect(zoneX, zoneY, zoneWidth, zoneHeight);

            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(zoneX, zoneY, zoneWidth, zoneHeight);

            String label = isContested ? "CONTESTED" : (zone.ownerId == -1 ? "UNCLAIMED" : "CONTROLLED");
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            int labelX = zoneX + (zoneWidth/2)-30;
            int labelY = zoneY - 5;
            g2d.drawString(label, labelX, labelY);

            if (zone.captureProgress > 0 && zone.captureProgress < 1.0f) {
                drawCaptureProgress(g2d, zoneX, zoneY + zoneHeight + 5, zoneWidth, zone.captureProgress);
            }
        }
    }

    private void drawCaptureProgress(Graphics2D g2d, int x, int y, int width, float progress) {
        int barHeight = 6;

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x, y, width, barHeight);

        g2d.setColor(Color.ORANGE);
        int fillWidth = (int) (progress * width);
        g2d.fillRect(x, y, width, barHeight);

        g2d.setColor(Color.WHITE);
        g2d.drawRect(x, y, width, barHeight);
    }

    private void drawItems(Graphics2D g2d, GameStateUpdate state) {
        int itemSize = 48;
        
        for(ItemSnapshot item : state.getItems()) {
            String spriteName = null;
            Color fallbackColor;

            switch (item.type) {
                case ENERGY:
                    spriteName = "item_energy";
                    fallbackColor = Color.YELLOW;
                    break;
                case FREEZE_RAY:
                    spriteName = "item_freeze_ray";
                    fallbackColor = Color.CYAN;
                    break;
                case SPEED_BOOST:
                    spriteName = "item_speed";
                    fallbackColor = Color.MAGENTA;
                    break;
                default:
                    fallbackColor = Color.WHITE;
            }

            if (spriteName != null && spriteManager.hasSprite(spriteName)) {
                BufferedImage sprite = spriteManager.getSprite(spriteName);
                g2d.drawImage(sprite, (int)item.x - itemSize/2, (int)item.y - itemSize/2, itemSize, itemSize, null);
            } else {
                g2d.setColor(fallbackColor);
                g2d.fillOval((int) item.x - itemSize/2, (int) item.y - itemSize/2, itemSize, itemSize);
                g2d.setColor(Color.WHITE);
                g2d.drawOval((int) item.x - itemSize/2, (int) item.y -itemSize/2, itemSize, itemSize);
            }
        }
    }

    private void drawPlayers(Graphics2D g2d, GameStateUpdate state) {
        int playerSize = 80;
        long elasped = System.currentTimeMillis() - startTime;

        for(PlayerSnapshot player : state.getPlayers()) {
            String animName;
            int frameSpeed;

            if (player.isFrozen) {
                animName = "player_" + player.playerId + "_frozen";
                frameSpeed = 300;
            } else {
                animName = "player_" + player.playerId + "_idle";
                frameSpeed = 400;
            }

            BufferedImage frame = null;

            if (spriteManager.hasAnimation(animName)) {
                frame = spriteManager.getAnimationFrames(animName, elasped, frameSpeed);
            } else if (spriteManager.hasSprite(animName)) {
                frame = spriteManager.getSprite(animName);
            }

            int playerX = (int) player.x;
            int playerY = (int) player.y;

            if (frame != null) {
                g2d.drawImage(frame, (int) player.x - playerSize/2, (int) player.y - playerSize/2, playerSize, playerSize, null);
            } else {
                g2d.setColor(spriteManager.getPlayerColor(player.playerId - 1));
                g2d.fillOval((int) player.x - playerSize/2, (int) player.y - playerSize/2, playerSize, playerSize);
            }

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            int nameWidth = fm.stringWidth(player.name);
            g2d.drawString(player.name, player.x - nameWidth/2, player.y - playerSize/2 - 8);
        
            drawPlayerStatus(g2d, playerX, playerY + playerSize/2 + 5, player);
        }
    }

    public void drawPlayerStatus(Graphics2D g2d, int x, int y, PlayerSnapshot player) {

        int indicatorSize = 12;
        int spacing = 14;
        int currentX = x - spacing;

        if (player.hasSpeedBoost) {
            g2d.setColor(Color.MAGENTA);
            g2d.setColor(Color.MAGENTA);
            g2d.fillRect(currentX, y, indicatorSize, indicatorSize);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(currentX, y, indicatorSize, indicatorSize);
            g2d.setFont(new Font("Arial", Font.BOLD, 8));
            g2d.drawString("S", currentX + 3, y + 10);
            currentX += spacing;
        }

        if (player.hasFreezeRay) {
            g2d.setColor(Color.BLUE);
            g2d.fillRect(currentX, y, indicatorSize, indicatorSize);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(currentX, y, indicatorSize, indicatorSize);
            g2d.setFont(new Font("Arial", Font.BOLD, 8));
            g2d.drawString("R", currentX + 3, y + 10);
        }
    }

}