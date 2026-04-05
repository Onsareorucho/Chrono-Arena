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
    private static final Color PLAYER_DEFAULT = Color.RED;
    private static final Color PLAYER_FROZEN = Color.BLUE;

    public ArenaRenderer() { 

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

}