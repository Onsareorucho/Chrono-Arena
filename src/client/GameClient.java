package client;

import client.gui.GameScreen;
// TODO: import client.network.TCPClientHandler
// TODO: import client.network.UDPClientHandler

import javax.swing.*;

public class GameClient {
    
    private GameScreen gameScreen;
    private InputHandler inputHandler;
    private LocalPlayer localplayer;

    // TODO:
    // private TCPClientHandler tcpHandler;
    // private UDPClientHandler udpHandler;

    private String serverIP;
    private int tcpPort;
    private int udpPort;

    public GameClient(String serverIP, int tcpPort, int udpPort) {
        this.serverIP = serverIP;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
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

        JFrame frame = new JFrame("ChronoArena");
        frame.add(gameScreen);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        gameScreen.startGame();
    }

    public void sendAction(String actionType, String direction) {
        // TODO:
        // ActionMessage msg = new ActionMessage(localPlayer.getID(), actionType, direction);
        //udpHandler.send(msg);

        System.out.println("ACTION: " + actionType + " " + direction); // debugging
    }

    public void onGameStateReceived(Object gameState) {
        // TODO:
        // gameScreen.updateState(gameState);
    }

    public static void main(String[] args) {
        // TODO: read properties file
        String ip = "localhost";
        int tcp = 5000;
        int udp = 5001;

        GameClient client = new GameClient(ip, tcp, udp);
        client.start();
    }
}