package client;

import javax.swing.JFrame;

import client.gui.GameScreen;
import shared.GameConfig;
import shared.GameStateUpdate;
import shared.PlayerAction;
import shared.PlayerInput;
import shared.SequenceGenerator;

public class GameClient {
    
    private GameScreen gameScreen;
    private InputHandler inputHandler;
    private LocalPlayer localPlayer;

    // TODO:
    // private TCPClientHandler tcpHandler;
    // private UDPClientHandler udpHandler;
    private SequenceGenerator sequenceGen;

    private String serverIP;
    private int tcpPort;
    private int udpPort;
    private int localUdpPort = 9002;

    public GameClient(String serverIP, int tcpPort, int udpPort) {
        this.serverIP = serverIP;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.sequenceGen = new SequenceGenerator();
    }

    public void start() {

        localPlayer = new LocalPlayer();

        gameScreen= new GameScreen();
        inputHandler = new InputHandler(this);
        gameScreen.addKeyListener(inputHandler);

        // TODO: 
        // tcpHandler = new TCPClientHandler(serverIP, tcpPort, this);
        // udpHandler = new UDPClientHandler(serverIP, udpPort);
        
        // tcpHandler.connect();
        // tcpHandler.sendJoinRequest(new JoinRequest(localPlayer.getName(), localUdpPort));

        JFrame frame = new JFrame("ChronoArena");
        frame.add(gameScreen);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        gameScreen.startGame();
    }

    // public void onJoinResponse(JoinResponse response) {
    //      localPlayer.setID(response.getPlayerId());
    //      localPlayer.setPosition((int) response.getStartX(), (int) response.getStartY());
    //      
    //      gameScreen.setLocalPlayerId(response.getPlayerId());
    //      System.out.println("Joined game as player " + response.getPlayerId());
    // }

    public void sendMovement(int dx, int dy) {
        PlayerInput input = PlayerInput.move(dx, dy);

        // TODO; send via UDP
        // GameMessage msg = new GameMessage(MessageType.PLAYER_INPUT, localPlayer.getID(), sequenceGen.next(), input);
        // udpHandle.send(msg);

        System.out.println("MOVE: " + input); // debugging
    }

    public void sendAction(String actionType, String direction) {
        
        if("FREEZE_RAY".equals(actionType)) {
            int dirX = 0, dirY = 0;
            switch (direction) {
                case "UP":
                    dirY = -1;
                    break;
                case "DOWN":
                    dirY = 1;
                    break;
                case "LEFT":
                    dirX = -1;
                    break;
                case"RIGHT":
                    dirX = 1;
            }
            PlayerAction action = PlayerAction.freezeRay(dirX, dirY);
        }
        
        // TODO: Send via UDP
        // GameMessage msg = new GameMessage(MessageType.PLAYER_ACTION, localPlayer.getID(), sequenceGen.next(), action);
        // udpHandler.send(msg);

        System.out.println("ACTION: " + actionType + " " + direction); // debugging
    }

    public void onGameStateReceived(GameStateUpdate gameState) {
        gameScreen.updateState(gameState);

        for (GameStateUpdate.PlayerSnapshot player : gameState.getPlayers()) {
            if (player.playerId == localPlayer.getID()) {
                localPlayer.setPosition((int) player.x, (int) player.y);
                localPlayer.setScore(player.score);
                localPlayer.setFrozen(player.isFrozen);
                localPlayer.setHasFreezeRay(player.hasFreezeRay);
                break;
            }
            
        }
    }

    public void onKicked(String reason) {
        System.out.println("Kicked from server: " + reason);
        gameScreen.stopGame();
    }

    public void disconnect() {
        // if (tcpHandler != null) tcpHandler.disconnect();
        // if (udpHandler !- null) udpHandler.close();
    }

    public LocalPlayer getLocalPlayer() {
        return localPlayer;
    }
    public static void main(String[] args) {
        String ip = "localhost";
        int tcp = 9000;
        int udp = 9001;

        try {
            GameConfig config = new GameConfig();
            ip = config.getServerIp();
            tcp = config.getServerTcpPort();
            udp = config.getServerUdpPort();
            System.out.println("Loaded config: " + ip + ":" + tcp + "/" + udp);
        } catch (Exception e) {
            System.err.println("Using default connection settings: " + ip + ":" + tcp + "/" + udp);
        }

        GameClient client = new GameClient(ip, tcp, udp);
        client.start();
    }
}