package client.gui;

import javax.swing.*;
import java.awt.*;

public class GameScreen extends JPanel implements Runnable {
    
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int TARGET_FPS = 60;

    private Thread gameThread;
    private boolean running = false;

    private ArenaRenderer arenaRenderer;
    private HUDRenderer hudRenderer;

    // temporary: replace when game state is developed
    private MockGameState gameState;

    public GameScreen() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(40, 40, 40));
        setFocusable(true);

        arenaRenderer = new ArenaRenderer();
        hudRenderer = new HUDRenderer();

        // temporary: replace when game state is developed
        gameState = new MockGameState();
    }

    public void startGame() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void stopGame() {
        running = false;
    }

    @Override 
    public void run() {
        long frameTime = 1000 / TARGET_FPS;

        while (running) {
            long startTime = System.currentTimeMillis();

            update();
            repaint();

            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = frameTime - elapsed;
            try { Thread.sleep(sleepTime); } catch (InterruptedException e) {}
        }
    }

    private void update() {
        // TODO: game state comes from network, no local update needed
    }

    // TODO: called by gc when new state arrives from server
    // public void updateState(GameState newState) {
    //     this.gameState = new newState;
    // }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
        arenaRenderer.render(g2d, gameState);

        hudRenderer.render(g2d, gameState, getWidth(), getHeight());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("ChronoArena");
        GameScreen game = new GameScreen();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        game.startGame();
    }
}