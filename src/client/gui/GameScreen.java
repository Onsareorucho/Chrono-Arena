package client.gui;

import javax.swing.*;
import java.awt.*;

public class GameScreen extends JPanel implements Runnable {
    
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private Thread gameThread;
    private boolean running = false;

    // temporary: replace when game state is developed
    private MockGameState gameState;

    public GameScreen() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(40, 40, 40));
        setFocusable(true);

        // temporary: replace when game state is developed
        gameState = new MockGameState();
    }

    public void startGame() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override 
    public void run() {
        while (running) {
            update();
            repaint();
            try { Thread.sleep(16); } catch (InterruptedException e) {}
        }
    }

    private void update() {
        // temporary: fake countdown for modeling game
        gameState.timeRemainingSeconds--;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // temporary: drawing for modeling

        // zones
        for (Mockzone zone : gameState.zones) {
            if (zone.isContested) {
                g2d.setColor(new Color(255, 165, 0, 100));
            } else if (zone.ownerID == -1) {
                g2d.setColor(new Color(128, 128, 128, 100));
            } else {
                g2d.setColor(new Color(0, 255, 0, 100));
            }
            g2d.fillRect(zone.x, zone.y, zone.width, zone.height);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(zone.x, zone.y, zone.width, zone.height);
        }

        // items
        for (MockItem item : gameState.items) {
            if (item.type.equals("ENERGY")) {
                g2d.setColor(Color.YELLOW);
            } else {
                g2d.setColor(Color.CYAN);
            }
            g2d.fillOval(item.x - 10, item.y - 10, 10, 20, 20);
        }
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