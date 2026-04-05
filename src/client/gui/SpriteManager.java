package client.gui;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class SpriteManager {

    private Map<String, BufferedImage> sprites;
    private Map<String, BufferedImage[]> animations;
    private static final String ASSETS_PATH = "assets/";

    private static final Color[] PLAYER_COLORS = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};

    public SpriteManager() {
        sprites = new HashMap<>();
        animations = new HashMap<>();
    }

    public void loadSprite(String name, String filename) {
        try {
            BufferedImage img = ImageIO.read(new File(ASSETS_PATH + filename));
            sprites.put(name, img);
            System.out.println("Loaded sprite: " + name);
        } catch (Exception e) {
            System.err.println("Failed to load sprite: " + filename);
        }
    }

    public BufferedImage getSprite(String name) {
        return sprites.get(name);
    }

    public boolean hasSprite(String name){
        return sprites.containsKey(name);
    }

    public Color getPlayerColor(int playerIndex) {
        return PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];
    }

    public void loadSpriteStates(String baseName, String filename, int frameWidth, int frameHeight, String[] stateNames) {
        try {
            BufferedImage sheet = ImageIO.read(new File(ASSETS_PATH + filename));

            for (int i = 0; i < stateNames.length; i++) {
                BufferedImage frame = sheet.getSubimage(i*frameWidth, 0, frameWidth, frameHeight);

                sprites.put(baseName + "_" + stateNames[i], frame);
            }

        } catch (Exception e) {
            System.err.println("Sprite sheet not found: " + filename);
        }
    }

    public void loadAllSprites() {
        String[] playerStates = {"idle", "walk_right", "walk_left", "frozen"};

        // ==== PLAYER SPRITES ====
        loadSpriteStates("player_1", "player_1.png", 64, 64, playerStates);
        loadSpriteStates("player_2", "player_2.png", 64, 64, playerStates);
        loadSpriteStates("player_3", "player_3.png", 64, 64, playerStates);
        loadSpriteStates("player_4", "player_4.png", 64, 64, playerStates);
        loadSpriteStates("player_5", "player_5.png", 64, 64, playerStates);
        loadSpriteStates("player_6", "player_6.png", 64, 64, playerStates);
        
        // ==== ZONE SPRITES ====
        loadSprite("zone_unclaimed", "zone_unclaimed.png");
        loadSprite("zone_owned", "zone_owned.png");
        loadSprite("zone_contested", "zone_contested.png");

        // ==== ITEM SPRITES ====
        loadSprite("item_energy", "item_energy.png");
        loadSprite("item_freeze_ray", "item_freeze_ray.png");
        loadSprite("item_speed", "item_speed.png");

        // ==== UI SPRTIES ====
        loadSprite("health_bar_border", "health_bar_border.png");
        loadSprite("cooldown_ready", "cooldown_ready.png");

        // ==== BACKGROUND ====
        loadSprite("arena_background", "arena_background.png");

        System.out.println("Sprite loading complete.");
    }
}