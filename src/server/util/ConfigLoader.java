package server.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads server/game configuration from game.properties file.
 * No IPs or ports are ever hardcoded — all values come from this file.
 *
 * Expected properties:
 *   server.ip
 *   server.tcp.port
 *   server.udp.port
 *   game.duration.seconds
 *   game.tick.rate.ms
 *   map.width
 *   map.height
 */
public class ConfigLoader {

    private final Properties props = new Properties();

    /**
     * Loads properties from a file path (e.g., "game.properties" in the working directory).
     */
    public ConfigLoader(String propertiesFilePath) throws IOException {
        try (InputStream in = new FileInputStream(propertiesFilePath)) {
            props.load(in);
        }
    }

    /**
     * Also supports loading from classpath for testing.
     */
    public static ConfigLoader fromClasspath(String resourceName) throws IOException {
        ConfigLoader cl = new ConfigLoader("/dev/null"); // dummy
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) throw new IOException("Resource not found: " + resourceName);
            cl.props.load(in);
        }
        return cl;
    }

    public String getServerIp() {
        return require("server.ip");
    }

    public int getTcpPort() {
        return Integer.parseInt(require("server.tcp.port"));
    }

    public int getUdpPort() {
        return Integer.parseInt(require("server.udp.port"));
    }

    public int getGameDurationSeconds() {
        return Integer.parseInt(props.getProperty("game.duration.seconds", "180"));
    }

    public int getTickRateMs() {
        return Integer.parseInt(props.getProperty("game.tick.rate.ms", "50"));
    }

    public int getMapWidth() {
        return Integer.parseInt(props.getProperty("map.width", "20"));
    }

    public int getMapHeight() {
        return Integer.parseInt(props.getProperty("map.height", "20"));
    }

    private String require(String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config property: " + key);
        }
        return value.trim();
    }
}
