import java.io.*;
import java.util.Properties;

/**
 * Loads settings from the game.properties config file.
 * 
 * The project requirements say no hardcoded IPs or ports, so everything
 * comes from this config file. Server and client both read it.
 */
public class GameConfig {
    
    private static final String DEFAULT_CONFIG_FILE = "config/game.properties";
    
    private final Properties props;
    
    
    public GameConfig() throws IOException {
        this(DEFAULT_CONFIG_FILE);
    }
    
    public GameConfig(String configPath) throws IOException {
        props = new Properties();
        
        // Try to load from file
        File configFile = new File(configPath);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }
        } else {
            System.out.println("Config file not found at " + configPath + ", using defaults");
        }
    }
    
    
    // Network settings
    
    public String getServerIp() {
        return props.getProperty("server.ip", "localhost");
    }
    
    public int getServerTcpPort() {
        return Integer.parseInt(props.getProperty("server.tcp.port", "9000"));
    }
    
    public int getServerUdpPort() {
        return Integer.parseInt(props.getProperty("server.udp.port", "9001"));
    }
    
    
    // Game settings
    
    public int getGameDurationSeconds() {
        return Integer.parseInt(props.getProperty("game.duration.seconds", 
                String.valueOf(GameConstants.DEFAULT_GAME_DURATION_SECONDS)));
    }
    
    public int getTickRateMs() {
        return Integer.parseInt(props.getProperty("game.tick.rate.ms", "50"));
    }
    
    
    // Map settings
    
    public int getMapWidth() {
        return Integer.parseInt(props.getProperty("map.width", 
                String.valueOf(GameConstants.DEFAULT_MAP_WIDTH)));
    }
    
    public int getMapHeight() {
        return Integer.parseInt(props.getProperty("map.height", 
                String.valueOf(GameConstants.DEFAULT_MAP_HEIGHT)));
    }
    
    
    // Generic getter for any property
    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
    
    public int getInt(String key, int defaultValue) {
        return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
    }
}
