package server.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    private static final Properties props = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream("config/game.properties")) {
            props.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Could not load game.properties", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(props.getProperty(key));
    }
}