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
                BufferedImage frame = sheet.getSubimage(0, i*frameHeight, frameWidth, frameHeight);

                sprites.put(baseName + "_" + stateNames[i], frame);
            }
            System.out.println("Sprite sheet found: " + filename);
        } catch (Exception e) {
            System.err.println("Sprite sheet not found: " + filename);
        }
    }

    public void loadAnimationSheet(String baseName, String filename, int frameCount, int frameWidth, int frameHeight, String[] stateNames) {
        try {
            BufferedImage sheet = ImageIO.read(new File(ASSETS_PATH + filename));

            for(int row = 0; row < stateNames.length; row++){
                BufferedImage[] frames = new BufferedImage[frameCount];

                for(int col = 0; col < frameCount; col++) {
                    frames[col] = sheet.getSubimage(col*frameWidth, row*frameHeight, frameWidth, frameHeight);
                }

                animations.put(baseName + "_" + stateNames[row], frames);
            }

            System.out.println("Loaded animation: " + baseName);

        } catch (Exception e) {
            System.err.println("Animation sheet not found: " + filename);
        }
    }

    public BufferedImage getAnimationFrames(String name, long elapsedMillis, int frameDurationMillis) {
        BufferedImage[] frames = animations.get(name);
        if(frames == null || frames.length == 0) {
            return null;
        }
        int frameIndex = (int) (elapsedMillis / frameDurationMillis) % frames.length;
        return frames[frameIndex];
    }

    public boolean hasAnimation(String name) {
        return animations.containsKey(name) && animations.get(name) != null;
    }

    public void loadAllSprites() {
        String[] playerStates = {"idle", "walk_right", "walk_left", "frozen"};

        // ==== PLAYER SPRITES ====
        loadAnimationSheet("player_1", "player_1.png", 2, 128, 128, playerStates); 
        loadAnimationSheet("player_2", "player_2.png", 2, 128, 128, playerStates); 
        loadAnimationSheet("player_3", "player_3.png", 2, 128, 128, playerStates); 
        loadAnimationSheet("player_4", "player_4.png", 2, 128, 128, playerStates); 
        loadAnimationSheet("player_5", "player_5.png", 2, 128, 128, playerStates); 
        loadAnimationSheet("player_6", "player_6.png", 2, 128, 128, playerStates); 
        
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
        loadSprite("cooldown_active", "cooldown_active.png");

        // ==== BACKGROUND ====
        loadSprite("arena_background", "arena_background.png");

        System.out.println("Sprite loading complete.");
    }
}