package client.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import client.mock.Item;
import client.mock.MockGameState;
import client.mock.Player;
import client.mock.Zone;

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
    public void render(Graphics2D g2d, MockGameState gameState) {
        drawBackground(g2d, 800, 600);
        drawZones(g2d, gameState);
        drawItems(g2d, gameState);
        drawPlayers(g2d, gameState);
    }

    private void drawBackground(Graphics2D g2d, int width, int height) {
        if (spriteManager.hasSprite("arena_background")) {
            BufferedImage bg = spriteManager.getSprite("arena_background");
            g2d.drawImage(bg, 0, 0, width, height, null);
        }
    }

    private void drawZones(Graphics2D g2d, MockGameState state) {
        for(Zone zone : state.zones) {
            if (zone.isContested) {
                g2d.setColor(ZONE_CONTESTED);
            } else if (zone.ownerID == -1) { 
                g2d.setColor(ZONE_UNCLAIMED);
            } else {
                g2d.setColor(ZONE_OWNED);
            }

            g2d.fillRect(zone.x, zone.y, zone.width, zone.height);

            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(zone.x, zone.y, zone.width, zone.height);

            String label = zone.isContested ? "CONTESTED" : (zone.ownerID == -1 ? "UNCLAIMED" : "CONTROLLED");
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            int labelX = zone.x + (zone.width/2)-30;
            int labelY = zone.y - 5;
            g2d.drawString(label, labelX, labelY);
        }
    }

    private void drawItems(Graphics2D g2d, MockGameState state) {
        int itemSize = 48;
        
        for(Item item : state.items) {
            String spriteName = null;
            Color fallbackColor;

            switch (item.type) {
                case "ENERGY":
                    spriteName = "item_energy";
                    fallbackColor = Color.YELLOW;
                    break;
                case "FREEZE_RAY":
                    spriteName = "item_freeze_ray";
                    fallbackColor = Color.CYAN;
                    break;
                case "SPEED":
                    spriteName = "item_speed";
                    fallbackColor = Color.MAGENTA;
                    break;
                default:
                    fallbackColor = Color.WHITE;
            }

            if (spriteName != null && spriteManager.hasSprite(spriteName)) {
                BufferedImage sprite = spriteManager.getSprite(spriteName);
                g2d.drawImage(sprite, item.x - itemSize/2, item.y - itemSize/2, itemSize, itemSize, null);
            } else {
                g2d.setColor(fallbackColor);
                g2d.fillOval(item.x - itemSize/2, item.y - itemSize/2, itemSize, itemSize);
                g2d.setColor(Color.WHITE);
                g2d.drawOval(item.x - itemSize/2, item.y -itemSize/2, itemSize, itemSize);
            }
        }
    }

    private void drawPlayers(Graphics2D g2d, MockGameState state) {
        int playerSize = 80;
        long elasped = System.currentTimeMillis() - startTime;

        for(Player player : state.players) {
            String animName;
            int frameSpeed;

            if (player.isFrozen) {
                animName = "player_" + player.id + "_frozen";
                frameSpeed = 300;
            } else if (player.isMovingRight) {
                animName = "player_" + player.id + "_walk_right";
                frameSpeed = 150;
            } else if (player.isMovingLeft) {
                animName = "player_" + player.id + "_walk_left";
                frameSpeed = 150;
            } else {
                animName = "player_" + player.id + "_idle";
                frameSpeed = 400;
            }

            BufferedImage frame = null;

            if (spriteManager.hasAnimation(animName)) {
                frame = spriteManager.getAnimationFrames(animName, elasped, frameSpeed);
            } else if (spriteManager.hasSprite(animName)) {
                frame = spriteManager.getSprite(animName);
            }

            if (frame != null) {
                g2d.drawImage(frame, player.x - playerSize/2, player.y - playerSize/2, playerSize, playerSize, null);
            } else {
                g2d.setColor(spriteManager.getPlayerColor(player.id - 1));
                g2d.fillOval(player.x - playerSize/2, player.y - playerSize/2, playerSize, playerSize);
            }

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            int nameWidth = fm.stringWidth(player.name);
            g2d.drawString(player.name, player.x - nameWidth/2, player.y - playerSize/2 - 8);
        
            drawHealthBar(g2d, player.x, player.y + playerSize/2 + 5, player.hp);
        }
    }

    public void drawHealthBar(Graphics2D g2d, int x, int y, int hp) {
        int barWidth = 50;
        int barHeight = 8;

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x - barWidth/2, y, barWidth, barHeight);

        Color hpColor = hp > 50 ? Color.GREEN : (hp > 25 ? Color.YELLOW : Color.RED);
        int fillWidth = (int)((hp/100.0)*barWidth);
        g2d.setColor(hpColor);
        g2d.fillRect(x - barWidth/2, y, fillWidth, barHeight);

        g2d.setColor(Color.WHITE);
        g2d.drawRect(x - barWidth/2, y, barWidth, barHeight);
    }

}