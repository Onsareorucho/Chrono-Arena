package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GameState {

    // ── Game phase ───────────────────────────────────────────
    public enum GamePhase {
        WAITING, IN_PROGRESS, FINISHED
    }

    // ── Fields ───────────────────────────────────────────────
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private GamePhase phase = GamePhase.WAITING;
    private long tickNumber = 0;
    private long timeRemainingMs;

    // Map so we can look up any player instantly by ID
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final List<Zone> zones = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();

    public GameState(long gameDurationMs) {
        this.timeRemainingMs = gameDurationMs;
    }

    // ── Write operations (game loop only) ────────────────────

    public void addPlayer(Player player) {
        lock.writeLock().lock();
        try {
            players.put(player.getPlayerId(), player);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removePlayer(String playerId) {
        lock.writeLock().lock();
        try {
            players.remove(playerId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addZone(Zone zone) {
        lock.writeLock().lock();
        try {
            zones.add(zone);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addItem(Item item) {
        lock.writeLock().lock();
        try {
            items.add(item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeItem(String itemId) {
        lock.writeLock().lock();
        try {
            items.removeIf(i -> i.getItemId().equals(itemId));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void tick(long tickDurationMs) {
        lock.writeLock().lock();
        try {
            tickNumber++;
            timeRemainingMs -= tickDurationMs;
            if (timeRemainingMs <= 0) {
                timeRemainingMs = 0;
                phase = GamePhase.FINISHED;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void setPhase(GamePhase phase) {
        lock.writeLock().lock();
        try {
            this.phase = phase;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ── Read operations (network layer calls these) ───────────

    public Player getPlayer(String playerId) {
        lock.readLock().lock();
        try {
            return players.get(playerId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, Player> getPlayers() {
        lock.readLock().lock();
        try {
            return players;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Zone> getZones() {
        lock.readLock().lock();
        try {
            return zones;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Item> getItems() {
        lock.readLock().lock();
        try {
            return items;
        } finally {
            lock.readLock().unlock();
        }
    }

    public GamePhase getPhase() {
        lock.readLock().lock();
        try {
            return phase;
        } finally {
            lock.readLock().unlock();
        }
    }

    public long getTickNumber() {
        lock.readLock().lock();
        try {
            return tickNumber;
        } finally {
            lock.readLock().unlock();
        }
    }

    public long getTimeRemainingMs() {
        lock.readLock().lock();
        try {
            return timeRemainingMs;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ── Snapshot (network layer serializes this) ──────────────
    // Returns a simple copy — safe to serialize without holding the lock
    public GameStateSnapshot snapshot() {
        lock.readLock().lock();
        try {
            return new GameStateSnapshot(
                    phase,
                    tickNumber,
                    timeRemainingMs,
                    new ArrayList<>(players.values()),
                    new ArrayList<>(zones),
                    new ArrayList<>(items));
        } finally {
            lock.readLock().unlock();
        }
    }
}