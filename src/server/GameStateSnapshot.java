package server;

import java.util.List;

// Immutable copy of GameState — safe to serialize and send over the network
public class GameStateSnapshot {
    public final GameState.GamePhase phase;
    public final long tickNumber;
    public final long timeRemainingMs;
    public final List<Player> players;
    public final List<Zone> zones;
    public final List<Item> items;

    public GameStateSnapshot(GameState.GamePhase phase, long tickNumber, long timeRemainingMs,
            List<Player> players, List<Zone> zones, List<Item> items) {
        this.phase = phase;
        this.tickNumber = tickNumber;
        this.timeRemainingMs = timeRemainingMs;
        this.players = players;
        this.zones = zones;
        this.items = items;
    }
}