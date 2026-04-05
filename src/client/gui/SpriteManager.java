package client.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class SpriteManager {

    private Map<String, BufferedImage> sprites;
    private static final String ASSETS_PATH = "assets/";

    public SpriteManager() {
        sprites = new HashMap<>();
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

    public void loadAllSprites() {
        // ==== PLAYER SPRITES ====
        loadSprite("player_1", " ");
        loadSprite("player_2", " ");
        loadSprite("player_3", " ");
        loadSprite("player_4", " ");

        // ==== ZONE SPRITES ====

        // ==== ITEM SPRITES ====

        // ==== UI SPRTIES ====

        // ==== BACKGROUND ====

        System.out.println("Sprite loading complete.");
    }
}