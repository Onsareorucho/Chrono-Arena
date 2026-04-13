package server;

import client.gui.GameOverScreen;
import client.gui.GameScreen;
import shared.GameResult;
import shared.GameStateUpdate;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * ServerDisplay — spectator window on the server machine.
 * Reuses GameScreen and GameOverScreen directly — identical to the client view.
 */
public class ServerDisplay {

    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel container;

    private GameScreen     gameScreen;
    private GameOverScreen gameOverScreen;
    private JPanel         waitingPanel;

    private volatile GameStateUpdate lastState;

    public ServerDisplay() {
        SwingUtilities.invokeLater(() -> {
            gameScreen     = new GameScreen();
            gameOverScreen = new GameOverScreen();
            waitingPanel   = buildWaitingPanel();

            // server has no local player — pass -1 so nobody gets highlighted
            gameScreen.setLocalPlayerId(-1);

            // game over on server just stays on screen (no auto-return)
            gameOverScreen.setOnReturnToMenu(() -> {});

            cardLayout = new CardLayout();
            container  = new JPanel(cardLayout);
            container.add(waitingPanel, "WAITING");
            container.add(gameScreen,   "GAME");
            container.add(gameOverScreen, "GAMEOVER");

            frame = new JFrame("ChronoArena — Server View");
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setResizable(false);
            frame.add(container);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            cardLayout.show(container, "WAITING");
        });
    }

    /** Called every tick via ServerNetworkManager.onStateRendered */
    public void onGameStateUpdate(GameStateUpdate state) {
        lastState = state;
        gameScreen.updateState(state);
    }

    /** Called by GameServer when minimum players joined and game loop starts */
    public void onGameStarted() {
        SwingUtilities.invokeLater(() -> {
            cardLayout.show(container, "GAME");
            gameScreen.startGame();
        });
    }

    /** Called by GameServer when game over is broadcast */
    public void onGameOver(GameResult result) {
        SwingUtilities.invokeLater(() -> {
            gameScreen.stopGame();
            int winnerScore = result.getLeaderboard().isEmpty() ? 0
                            : result.getLeaderboard().get(0).totalScore;
            List<GameStateUpdate.PlayerSnapshot> standings =
                    lastState != null ? lastState.getPlayers() : List.of();
            gameOverScreen.showGameOver(result.getWinnerName(), winnerScore, standings);
            cardLayout.show(container, "GAMEOVER");
        });
    }

    /** Called by GameServer when state resets for a new game */
    public void onReset() {
        SwingUtilities.invokeLater(() -> {
            gameOverScreen.stopScreen();
            cardLayout.show(container, "WAITING");
        });
    }

    private JPanel buildWaitingPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(20, 20, 30));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setPreferredSize(new Dimension(800, 600));
        panel.setOpaque(false);

        JLabel label = new JLabel("Waiting for players...");
        label.setForeground(new Color(201, 180, 117));
        label.setFont(new Font("Arial", Font.BOLD, 28));
        panel.add(label);
        return panel;
    }
}
