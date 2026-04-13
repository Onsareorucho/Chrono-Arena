package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import shared.GameConstants;

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
    private final long tickRateMs;

    // Map so we can look up any player instantly by ID
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final List<Zone> zones = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();

    public GameState(long gameDurationMs, long tickRateMs) {
        this.timeRemainingMs = gameDurationMs;
        this.tickRateMs = tickRateMs;
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

    public void removePlayer(int playerId) {
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

    public Player getPlayer(int playerId) {
        lock.readLock().lock();
        try {
            return players.get(playerId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<Integer, Player> getPlayers() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableMap(new HashMap<>(players));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Zone> getZones() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(zones);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Item> getItems() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(items);
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

    public int[] getSpawnPosition() {
        lock.readLock().lock();
        try {
            int[][] spawnPoints = {
                    { 0, 0 }, // top left
                    { 19, 0 }, // top right
                    { 0, 19 }, // bottom left
                    { 19, 19 }, // bottom right
                    { 9, 0 }, // top center
                    { 9, 19 }, // bottom center
                    { 0, 9 }, // left center
                    { 19, 9 } // right center
            };

            // find first unoccupied spawn point
            for (int[] spawn : spawnPoints) {
                boolean occupied = players.values().stream()
                        .anyMatch(p -> p.getPlayerPositionX() == spawn[0]
                                && p.getPlayerPositionY() == spawn[1]);
                if (!occupied)
                    return spawn;
            }

            // all 8 points taken — fallback to center
            System.out.println("All spawn points occupied — using center fallback");
            return new int[] { 10, 10 };

        } finally {
            lock.readLock().unlock();
        }
    }

    public void handlePlayerDisconnect(int playerId) {
        lock.writeLock().lock();
        try {
            for (Zone zone : zones) {
                if (playerId == zone.getControllingPlayerId()) {
                    zone.setZoneState(ZoneState.GRACE);
                    zone.setGraceTicksLeft((int)(GameConstants.ZONE_GRACE_PERIOD_MS / tickRateMs));
                    System.out.println("Zone " + zone.getZoneId() + " entering grace — owner disconnected");
                }
                if (playerId == zone.getContestingPlayerId()) {
                    zone.setZoneState(ZoneState.UNCLAIMED);
                    zone.setContestingPlayerId(-1);
                    zone.setCaptureTicksLeft((int)(GameConstants.ZONE_CAPTURE_TIME_MS / tickRateMs));
                }
            }
            players.remove(playerId);
            System.out.println("Player " + playerId + " removed from game");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void reset(long gameDurationMs) {
        lock.writeLock().lock();
        try {
            players.clear();
            items.clear();
            tickNumber = 0;
            timeRemainingMs = gameDurationMs;
            phase = GamePhase.WAITING;
            for (Zone zone : zones) {
                zone.setZoneState(ZoneState.UNCLAIMED);
                zone.setControllingPlayerId(-1);
                zone.setContestingPlayerId(-1);
                zone.setCaptureTicksLeft(0);
                zone.setGraceTicksLeft(0);
            }
            System.out.println("Game state reset — ready for new game");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Player getWinner() {
        lock.readLock().lock();
        try {
            return players.values().stream()
                    .max(Comparator.comparingInt(Player::getPlayerScore)
                            .thenComparing(Comparator.comparingInt(Player::getPlayerId).reversed()))
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

}