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

    private static final int TILE = 30;

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

    private static final int MAP_TILES   = 20;
    private static final int ARENA_SIZE  = MAP_TILES * TILE;  // 600px

    private void drawBackground(Graphics2D g2d, int width, int height) {
        // Fill entire panel with dark outer background
        g2d.setColor(new Color(20, 20, 30));
        g2d.fillRect(0, 0, width, height);

        if (spriteManager.hasSprite("arena_background")) {
            BufferedImage bg = spriteManager.getSprite("arena_background");
            g2d.drawImage(bg, 0, 0, ARENA_SIZE, ARENA_SIZE, null);
        } else {
            // Checkerboard tile grid
            for (int row = 0; row < MAP_TILES; row++) {
                for (int col = 0; col < MAP_TILES; col++) {
                    boolean dark = (row + col) % 2 == 0;
                    g2d.setColor(dark ? new Color(40, 44, 52) : new Color(50, 54, 64));
                    g2d.fillRect(col * TILE, row * TILE, TILE, TILE);
                }
            }
        }

        // Arena border — clear white outline so players know the boundary
        g2d.setColor(new Color(180, 180, 200));
        g2d.setStroke(new java.awt.BasicStroke(3));
        g2d.drawRect(0, 0, ARENA_SIZE, ARENA_SIZE);

        // Subtle inner grid lines
        g2d.setColor(new Color(80, 80, 100, 80));
        g2d.setStroke(new java.awt.BasicStroke(1));
        for (int i = 1; i < MAP_TILES; i++) {
            g2d.drawLine(i * TILE, 0, i * TILE, ARENA_SIZE);
            g2d.drawLine(0, i * TILE, ARENA_SIZE, i * TILE);
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

            int zoneX = (int) (zone.x * TILE);
            int zoneY = (int) (zone.y * TILE);
            int zoneWidth = (int) (zone.radius * TILE);
            int zoneHeight = (int) (zone.radius * TILE);

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
        g2d.fillRect(x, y, fillWidth, barHeight);

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

            int px = (int)(item.x * TILE) + TILE / 2;
            int py = (int)(item.y * TILE) + TILE / 2;
            if (spriteName != null && spriteManager.hasSprite(spriteName)) {
                BufferedImage sprite = spriteManager.getSprite(spriteName);
                g2d.drawImage(sprite, px - itemSize/2, py - itemSize/2, itemSize, itemSize, null);
            } else {
                g2d.setColor(fallbackColor);
                g2d.fillOval(px - itemSize/2, py - itemSize/2, itemSize, itemSize);
                g2d.setColor(Color.WHITE);
                g2d.drawOval(px - itemSize/2, py - itemSize/2, itemSize, itemSize);
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

            int playerX = (int)(player.x * TILE) + TILE / 2;
            int playerY = (int)(player.y * TILE) + TILE / 2;

            if (frame != null) {
                g2d.drawImage(frame, playerX - playerSize/2, playerY - playerSize/2, playerSize, playerSize, null);
                // Frozen overlay on top of sprite
                if (player.isFrozen) {
                    g2d.setColor(new Color(0, 200, 255, 100));
                    g2d.fillOval(playerX - playerSize/2, playerY - playerSize/2, playerSize, playerSize);
                }
            } else {
                Color baseColor = player.isFrozen
                        ? new Color(0, 200, 255)
                        : spriteManager.getPlayerColor(player.playerId - 1);
                g2d.setColor(baseColor);
                g2d.fillOval(playerX - playerSize/2, playerY - playerSize/2, playerSize, playerSize);
                if (player.isFrozen) {
                    g2d.setColor(new Color(255, 255, 255, 180));
                    g2d.setStroke(new java.awt.BasicStroke(3));
                    g2d.drawOval(playerX - playerSize/2, playerY - playerSize/2, playerSize, playerSize);
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 10));
                    g2d.drawString("ICE", playerX - 10, playerY + 4);
                }
            }

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            int nameWidth = fm.stringWidth(player.name);
            g2d.drawString(player.name, playerX - nameWidth/2, playerY - playerSize/2 - 8);

            drawPlayerStatus(g2d, playerX, playerY + playerSize/2 + 5, player);
        }
    }

    public void drawPlayerStatus(Graphics2D g2d, int x, int y, PlayerSnapshot player) {

        int indicatorSize = 12;
        int spacing = 14;
        int currentX = x - spacing;

        if (player.hasSpeedBoost) {
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