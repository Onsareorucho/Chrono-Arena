package client;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import client.gui.GameScreen;
import client.network.ReconnectHandler;
import shared.GameConfig;
import shared.GameEvent;
import shared.GameStateUpdate;
import shared.PlayerAction;
import shared.PlayerInput;

public class GameClient {
    
    private GameScreen gameScreen;
    private InputHandler inputHandler;
    private LocalPlayer localPlayer;

    private ReconnectHandler networkManager;

    private String serverIP;
    private int tcpPort;
    private int udpPort;
    private String playerName;

    public GameClient(String serverIP, int tcpPort, int udpPort, String playerName) {
        this.serverIP = serverIP;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.playerName = playerName;
    }

    public void start() {

        localPlayer = new LocalPlayer();
        localPlayer.setName(playerName);

        gameScreen= new GameScreen();
        inputHandler = new InputHandler(this);
        gameScreen.addKeyListener(inputHandler);

        JFrame frame = new JFrame("ChronoArena");
        frame.add(gameScreen);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        gameScreen.startGame();

        connectToServer();
    }

    private void connectToServer() {
        try {
            GameConfig config = new GameConfig();

            networkManager = new ReconnectHandler(config, playerName);

            networkManager.onGameStateUpdate = this::onGameStateReceived;
            networkManager.onGameEvent = this::onGameEvent;
            networkManager.onGameOver = result -> {
                System.out.println("Game Over! Winner: " + result.getWinnerName());
                gameScreen.stopGame();
                gameScreen.showNotification("GAME OVER - " + result.getWinnerName() + "WINS!", java.awt.Color.YELLOW);
                gameScreen.stopGame();
            };
            networkManager.onKickReceived = kick -> { 
                System.out.println("Kicked: " + kick.getReason());
                onKicked(kick.getReason());
            };
            networkManager.onPlayerJoined = playerId -> {
                System.out.println("Player " + playerId + " joined");
            };
            networkManager.onPlayerLeft = playerId -> {
                System.out.println("Player " + playerId + " left");
            };
            networkManager.onReconnected = () -> {
                System.out.println("Reconnected to server!");

                localPlayer.setID(networkManager.getPlayerId());
                gameScreen.setLocalPlayerId(networkManager.getPlayerId());
            };
            networkManager.onGiveUp = () -> {
                System.out.println("Could not reconnect to server");
                gameScreen.showNotification("CONNECTION LOST", java.awt.Color.RED);
            };

            networkManager.connectAndStart();

            localPlayer.setID(networkManager.getPlayerId());
            gameScreen.setLocalPlayerId(networkManager.getPlayerId());

            System.out.println("Connected as player: " + networkManager.getPlayerId());

        } catch (Exception e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
        }
    }

    public void sendMovement(int dx, int dy) {

        if (networkManager != null) {
            PlayerInput input = PlayerInput.move(dx, dy);
            networkManager.sendInput(input);
        }
    }

    public void sendAction(String actionType, String direction) {
        if(networkManager == null) return;

        if ("FREEZE_RAY".equals(actionType)) {
            int dirX = 0, dirY = 0;
            switch(direction) {
                case "UP": dirY = -1; break;
                case "DOWN": dirY = 1; break;
                case "LEFT": dirX = -1; break;
                case "RIGHT": dirX = 1; break;
            }
            PlayerAction action = PlayerAction.freezeRay(dirX, dirY);
            networkManager.sendAction(action);
        } else if ("USE_POWERUP".equals(actionType)) { 
            PlayerAction action = PlayerAction.usePowerup();
            networkManager.sendAction(action);
        }
    }

    public void onGameStateReceived(GameStateUpdate gameState) {
        gameScreen.updateState(gameState);

        for (GameStateUpdate.PlayerSnapshot player : gameState.getPlayers()) {
            if (player.playerId == localPlayer.getID()) {
                localPlayer.setPosition((int) player.x, (int) player.y);
                localPlayer.setScore(player.score);
                localPlayer.setFrozen(player.isFrozen);
                localPlayer.setHasFreezeRay(player.hasFreezeRay);
                localPlayer.setHasSpeedBoost(player.hasSpeedBoost);
                break;
            }
            
        }
    }

    public void onGameEvent(GameEvent event) {
        switch (event.getEventType()) {
            case PLAYER_FROZEN:
                if (event.getTargetPlayerId() == localPlayer.getID()) {
                    System.out.println("You were frozen!");
                    gameScreen.showNotification("FROZEN", java.awt.Color.CYAN);
                } else if (event.getSourcePlayerId() == localPlayer.getID()) {
                    System.out.println("You froze someone!");
                    gameScreen.showNotification("TARGET FROZEN!", java.awt.Color.GREEN);
                }
                break;
            case PLAYER_UNFROZEN:
                if (event.getSourcePlayerId() == localPlayer.getID()) {
                    gameScreen.showNotification("UNFROZEN!", java.awt.Color.WHITE);
                }
                break;
            case ZONE_CAPTURED:
                if (event.getSourcePlayerId() == localPlayer.getID()) {
                    gameScreen.showNotification("ZONE CAPTURED!", java.awt.Color.GREEN);
                }
                break;
            case ZONE_LOST:
                if (event.getSourcePlayerId() == localPlayer.getID()) {
                    gameScreen.showNotification("ZONE LOST!", java.awt.Color.RED);
                }
                break;
            case ITEM_COLLECTED:
                if (event.getSourcePlayerId() == localPlayer.getID()) {
                    String itemType = event.getExtraData();
                    gameScreen.showNotification("+" + itemType, java.awt.Color.YELLOW);
                }
                break;
            default:
                System.out.println("Event: " + event);
        }
    }

    public void onKicked(String reason) {
        System.out.println("Kicked from server: " + reason);
        gameScreen.stopGame();
    }

    public void disconnect() {
        if (networkManager != null) {
            networkManager.stop();
        }
    }

    public LocalPlayer getLocalPlayer() {
        return localPlayer;
    }
    public static void main(String[] args) {
        String ip = "localhost";
        int tcp = 9000;
        int udp = 9001;
        String name = "Player";

        try {
            GameConfig config = new GameConfig();
            ip = config.getServerIp();
            tcp = config.getServerTcpPort();
            udp = config.getServerUdpPort();
            System.out.println("Loaded config: " + ip + ":" + tcp + "/" + udp);
        } catch (Exception e) {
            System.err.println("Using default connection settings: " + ip + ":" + tcp + "/" + udp);
        }

        final String finalIp = ip;
        final int finalTcp= tcp;
        final int finalUdp = udp;
        final String finalName = name;
        
        SwingUtilities.invokeLater(() -> {
            GameClient client = new GameClient(finalIp, finalTcp, finalUdp, finalName);
            client.start();
        });
    }
}