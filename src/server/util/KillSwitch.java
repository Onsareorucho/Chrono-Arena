package server.util;

import server.GameState;
import server.Player;
import server.network.ServerNetworkManager;

public class KillSwitch {

    private final GameState gameState;
    private ServerNetworkManager networkManager; // P2 — set after networking starts

    public KillSwitch(GameState gameState) {
        this.gameState = gameState;
    }

    /** Called by GameServer after networking starts. */
    public void setNetworkManager(ServerNetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    // called by admin or erratic behavior detector
    public void killPlayer(int playerId) {
        Player player = gameState.getPlayer(playerId);

        if (player == null) {
            System.out.println("KillSwitch: player " + playerId + " not found");
            return;
        }

        System.out.println("KillSwitch: killing player " + playerId);

        // mark disconnected in game state
        player.setConnected(false);

        // clean up their zones
        gameState.handlePlayerDisconnect(playerId);

        // close their TCP connection and notify all clients (P2)
        if (networkManager != null) {
            networkManager.killPlayer(playerId, "Removed by admin");
        }

        System.out.println("KillSwitch: player " + playerId + " removed from game");
    }

    /** Kill with a custom reason shown to the player. */
    public void killPlayer(int playerId, String reason) {
        Player player = gameState.getPlayer(playerId);
        if (player == null) return;

        player.setConnected(false);
        gameState.handlePlayerDisconnect(playerId);

        if (networkManager != null) {
            networkManager.killPlayer(playerId, reason);
        }

        System.out.println("KillSwitch: player " + playerId + " killed — " + reason);
    }
}
