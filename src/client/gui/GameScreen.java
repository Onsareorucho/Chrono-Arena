package client.gui;

import java.awt.Color; // TODO: replace with actual shared
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JFrame;
import javax.swing.JPanel;

import client.mock.MockGameState;

public class GameScreen extends JPanel implements Runnable {
    
    private static final int BASE_WIDTH = 800;
    private static final int BASE_HEIGHT = 600;
    private static final int TARGET_FPS = 60;

    private Thread gameThread;
    private boolean running = false;

    private SpriteManager spriteManager;
    private ArenaRenderer arenaRenderer;
    private HUDRenderer hudRenderer;

    // temporary: replace when game state is developed
    private MockGameState gameState;

    public GameScreen() {
        setPreferredSize(new Dimension(BASE_WIDTH, BASE_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        spriteManager = new SpriteManager();
        spriteManager.loadAllSprites();

        arenaRenderer = new ArenaRenderer(spriteManager);
        hudRenderer = new HUDRenderer(spriteManager);

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

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        double scaleX = getWidth() / (double) BASE_WIDTH;
        double scaleY = getHeight() / (double) BASE_HEIGHT;
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) (BASE_WIDTH * scale);
        int scaledHeight = (int) (BASE_HEIGHT * scale);
        int offsetX = (getWidth() - scaledWidth)/2;
        int offsetY = (getHeight() - scaledHeight)/2;

        g2d.translate(offsetX, offsetY);
        g2d.scale(scale, scale);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    
        arenaRenderer.render(g2d, gameState);
        hudRenderer.render(g2d, gameState, BASE_WIDTH, BASE_HEIGHT);
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