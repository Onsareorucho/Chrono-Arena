package client;

import java.awt.CardLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import client.gui.GameOverScreen;
import client.gui.GameScreen;
import client.gui.LobbyScreen;
import client.gui.MainMenuScreen;
import client.gui.SoundManager;
import client.network.ReconnectHandler;
import shared.GameConfig;
import shared.GameEvent;
import shared.GameStateUpdate;
import shared.PlayerAction;
import shared.PlayerInput;

public class GameClient {
    
    private JFrame frame;
    private JPanel screenContainer;
    private CardLayout cardLayout;

    private MainMenuScreen mainMenuScreen;
    private LobbyScreen lobbyScreen;
    private GameScreen gameScreen;
    private GameOverScreen gameOverScreen;
    private InputHandler inputHandler;
    private LocalPlayer localPlayer;

    private SoundManager soundManager;

    private ReconnectHandler networkManager;

    private String serverIP = "localhost";
    private int tcpPort = 9000;
    private int udpPort = 9001;
    private String playerName = "Player";

    private boolean gameStarted = false;
    private int lobbyPlayerCount = 1; // starts at 1 (self), incremented by PLAYER_JOINED events

    private GameStateUpdate lastGameState;

    public GameClient() {
        try {
            GameConfig config = new GameConfig();
            serverIP = config.getServerIp();
            tcpPort = config.getServerTcpPort();
            udpPort = config.getServerUdpPort();
        } catch (Exception e) {
            System.err.println("Using default connection settings");
        }
    }

    public void start() {

        frame = new JFrame("ChronoArena");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        cardLayout = new CardLayout();
        screenContainer = new JPanel(cardLayout);

        mainMenuScreen = new MainMenuScreen();
        lobbyScreen = new LobbyScreen();
        gameScreen = new GameScreen();
        gameOverScreen = new GameOverScreen();
        inputHandler = new InputHandler(this);

        soundManager = new SoundManager();
        soundManager.loadAllSounds();

        mainMenuScreen.setOnPlayClicked(this::onPlayedClicked);
        lobbyScreen.setOnGameStart(this::onGameStarted);
        gameOverScreen.setOnReturnToMenu(this::returnToMenu);

        screenContainer.add(mainMenuScreen, "MENU");
        screenContainer.add(lobbyScreen, "LOBBY");
        screenContainer.add(gameScreen, "GAME");
        screenContainer.add(gameOverScreen, "GAMEOVER");

        frame.add(screenContainer);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        showScreen("MENU");
    }

    private void showScreen(String screenName) {
        cardLayout.show(screenContainer, screenName);

        switch (screenName) {
            case "GAME" -> {
                gameScreen.requestFocusInWindow();
                soundManager.playMusic("game");
            }
            case "MENU" -> {
                mainMenuScreen.requestFocusInWindow();
                soundManager.playMusic("menu");
            }
            case "LOBBY" -> {
                lobbyScreen.requestFocusInWindow();
                soundManager.playMusic("lobby");
            }
            case "GAMEOVER" -> {
                gameOverScreen.requestFocusInWindow();
                soundManager.playMusic("gameover");
            }
        }
    }

    private void onPlayedClicked(String name, String serverIp) {
        this.playerName = name;
        this.serverIP = serverIp;

        localPlayer = new LocalPlayer();
        localPlayer.setName(playerName);

        // Add input handler to lobby screen for practice movement
        lobbyScreen.addKeyListener(inputHandler);
        lobbyScreen.setLocalPlayerName(playerName);

        showScreen("LOBBY");
        lobbyScreen.startLobby();

        new Thread(() -> {
            connectToServer();
        }).start();
    }

    private void onGameStarted() {
        if (gameStarted) return;
        gameStarted = true;

        SwingUtilities.invokeLater(() -> {
            lobbyScreen.stopLobby();
            
            // Transfer input handler to game screen
            lobbyScreen.removeKeyListener(inputHandler);
            gameScreen.addKeyListener(inputHandler);
            gameScreen.setLocalPlayerId(localPlayer.getID());

            showScreen("GAME");
            gameScreen.startGame();
        });
    }

    private void onGameOver(String winnerName, int winnerScore) {
        SwingUtilities.invokeLater(() -> {
            gameScreen.stopGame();
            gameScreen.removeKeyListener(inputHandler);
 
            // Get final standings from last game state
            List<GameStateUpdate.PlayerSnapshot> standings = 
                (lastGameState != null) ? lastGameState.getPlayers() : List.of();
 
            gameOverScreen.showGameOver(winnerName, winnerScore, standings);
            showScreen("GAMEOVER");
        });
    }

    private void connectToServer() {
        try {
            GameConfig config = new GameConfig();
            config.setServerIp(serverIP);

            networkManager = new ReconnectHandler(config, playerName);

            networkManager.onGameStateUpdate = this::onGameStateReceived;
            networkManager.onGameEvent = this::onGameEvent;
            networkManager.onGameOver = result -> {
                System.out.println("Game Over! Winner: " + result.getWinnerName());
                int winnerScore = result.getLeaderboard().isEmpty() ? 0 : result.getLeaderboard().get(0).totalScore;
                onGameOver(result.getWinnerName(), winnerScore);
            };
            networkManager.onKickReceived = kick -> { 
                System.out.println("Kicked: " + kick.getReason());
                onKicked(kick.getReason());
            };
            networkManager.onPlayerJoined = playerId -> {
                System.out.println("Player " + playerId + " joined");
                if (playerId != networkManager.getPlayerId()) {
                    lobbyPlayerCount++;
                    lobbyScreen.setPlayerCount(lobbyPlayerCount);
                }
            };
            networkManager.onPlayerLeft = playerId -> {
                System.out.println("Player " + playerId + " left");
                if (playerId != networkManager.getPlayerId()) {
                    lobbyPlayerCount = Math.max(1, lobbyPlayerCount - 1);
                    lobbyScreen.setPlayerCount(lobbyPlayerCount);
                }
            };
            networkManager.onReconnected = () -> {
                System.out.println("Reconnected to server!");

                localPlayer.setID(networkManager.getPlayerId());
                lobbyScreen.setLocalPlayerId(networkManager.getPlayerId());
                gameScreen.setLocalPlayerId(networkManager.getPlayerId());
            };
            networkManager.onGiveUp = () -> {
                System.out.println("Could not reconnect to server");
                if (gameStarted) {
                    gameScreen.showNotification("CONNECTION LOST", java.awt.Color.RED);
                }
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    SwingUtilities.invokeLater(() -> returnToMenu());
                }).start();
            };

            networkManager.connectAndStart();

            localPlayer.setID(networkManager.getPlayerId());
            lobbyScreen.setLocalPlayerId(networkManager.getPlayerId());
            lobbyScreen.setMaxPlayers(networkManager.getMinPlayers());
            lobbyPlayerCount = networkManager.getCurrentPlayerCount();
            lobbyScreen.setPlayerCount(lobbyPlayerCount);
            gameScreen.setLocalPlayerId(networkManager.getPlayerId());

            System.out.println("Connected as player: " + networkManager.getPlayerId());

        } catch (Exception e) {
            System.err.println("Failed to connect to server: " + e.getMessage());

            SwingUtilities.invokeLater(() -> {
                // Show error and return to menu
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                    SwingUtilities.invokeLater(() -> returnToMenu());
                }).start();
            });
        }
    }

    private void returnToMenu() {
        gameStarted = false;
        lobbyPlayerCount = 1;
        lobbyScreen.stopLobby();
        gameScreen.stopGame();
        gameOverScreen.stopScreen();
        if (networkManager != null) {
            networkManager.stop();
            networkManager = null;
        }
        showScreen("MENU");
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
        lastGameState = gameState;

        // Update the appropriate screen based on game state
        if (!gameStarted) {
            // Still in lobby - update lobby screen
            lobbyScreen.updateState(gameState);
        } else {
            // Game is running - update game screen
            gameScreen.updateState(gameState);
        }

        // Always update local player state
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
        
        if (event.getEventType() == GameEvent.EventType.GAME_STARTING) {
            System.out.println("Server started the game!");
            soundManager.playSound("game_start");
            onGameStarted();
            return;
        }
        
        if (!gameStarted) return;

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
        lobbyScreen.stopLobby();
    }

    public void disconnect() {
        if (networkManager != null) {
            networkManager.stop();
        }
    }

    public LocalPlayer getLocalPlayer() {
        return localPlayer;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public static void main(String[] args) {      
        SwingUtilities.invokeLater(() -> {
            GameClient client = new GameClient();
            client.start();
        });
    }
}