import client.GameClient;
import server.GameServer;
import shared.GameConfig;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        try {
            switch (args[0].toLowerCase()) {
                case "server" -> {
                    System.out.println("Starting ChronoArena Server...");
                    GameConfig config = new GameConfig();
                    GameServer server = new GameServer();
                    server.start();
                }
                case "client" -> {
                    String[] clientArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, clientArgs, 0, args.length - 1);
                    System.out.println("Starting ChronoArena Client...");
                    GameClient.main(clientArgs);
                }
                case "test" -> {
                    System.out.println("Starting ChronoArena Test Mode...");
                    GameClient.main(new String[]{"--test"});
                }
                default -> {
                    System.out.println("Unknown mode: " + args[0]);
                    printUsage();
                }
            }
        } catch (Exception e) {
            System.err.println("Error starting: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void printUsage() {
        System.out.println("========================================");
        System.out.println("         ChronoArena Launcher           ");
        System.out.println("========================================");
        System.out.println(" Usage:                                 ");
        System.out.println("   java Main server      - Start server ");
        System.out.println("   java Main client      - Start client ");
        System.out.println("   java Main client Bob  - Client as Bob");
        System.out.println("========================================");
    }
}