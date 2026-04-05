package server.util;

import server.GameState;
import server.Player;

public class KillSwitch {

    private final GameState gameState;

    public KillSwitch(GameState gameState) {
        this.gameState = gameState;
    }

    // called by admin or erratic behavior detector
    public void killPlayer(String playerId) {
        Player player = gameState.getPlayer(playerId);

        if (player == null) {
            System.out.println("KillSwitch: player " + playerId + " not found");
            return;
        }

        System.out.println("KillSwitch: killing player " + playerId);

        // mark disconnected
        player.setConnected(false);

        // clean up their zones
        gameState.handlePlayerDisconnect(playerId);

        // TODO: P2 hooks in here to close the TCP socket
        // tcpServerHandler.closeConnection(playerId);

        System.out.println("KillSwitch: player " + playerId + " removed from game");
    }
}