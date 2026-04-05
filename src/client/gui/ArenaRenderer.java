package client.gui;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ArenaRenderer {

    private static final Color ZONE_UNCLAIMED = new Color(128, 128, 128, 100);
    private static final Color ZONE_OWNED = new Color(0, 200, 0, 100);
    private static final Color ZONE_CONTESTED = new Color(255, 165, 0, 100);
    private static final Color ITEM_ENERGY = Color.YELLOW;
    private static final Color ITEM_FREEZE = Color.CYAN;
    private static final Color ITEM_SPEED = Color.MAGENTA;

    private SpriteManager spriteManager;

    public ArenaRenderer(SpriteManager spriteManager) { 
        this.spriteManager = spriteManager;
    }

    // TODO: replace gamestate with official gamestate
    public void render(Graphics2D g2d, GameState gameState) {
        drawZones(g2d, gameState);
        drawItems(g2d, gameState);
        drawPlayers(g2d, gameState);
    }

    private void drawZones(Graphics2D g2d, GameState state) {
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

    private void drawItems(Graphics2D g2d, GameState state) {
        for(Item item : state.items) {
            switch (item.type) {
                case "ENERGY":
                    g2d.setColor(ITEM_FREEZE);
            }
        }
    }

    private void drawPlayers(Graphics2D g2d, GameState state) {
        int playerSize = 32;

        for(Player player : state.players) {
            String spriteName;

            if (player.isFrozen) {
                spriteName = "player_" + player.id + "_frozen";
            } else if (player.isMovingRight) {
                spriteName = "player_" + player.id + "_walk_right";
            } else if (player.isMovingLeft) {
                spriteName = "player_" + player.id + "_walk_left";
            } else {
                spriteName = "player_" + player.id + "_idle";
            }

            BufferedImage sprite = spriteManager.getSprite(spriteName);

            if (sprite != null) {
                g2d.drawImage(sprite, player.x - playerSize/2, player.y - playerSize/2, playerSize, playerSize, null);
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