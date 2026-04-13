package server.logic;

import server.Player;
import shared.GameConstants;
import shared.GameEvent;

import java.util.function.Consumer;

public class CombatHandler {

    private static final int POINTS_DEDUCTED = GameConstants.FREEZE_RAY_POINT_PENALTY;
    private static final int POINTS_AWARDED  = GameConstants.FREEZE_RAY_POINT_PENALTY;

    private final int freezeTicks;
    private Consumer<GameEvent> eventBroadcaster = e -> {};

    public CombatHandler(CollisionHandler collisionHandler, long tickRateMs) {
        this.freezeTicks = (int) (GameConstants.FREEZE_DURATION_MS / tickRateMs);
    }

    public void setEventBroadcaster(Consumer<GameEvent> broadcaster) {
        this.eventBroadcaster = broadcaster;
    }

    // called when a player fires their freeze weapon at a target
    public void handleFreezeAttack(Player attacker, Player target) {

        // attacker must have a weapon
        if (!attacker.isArmed()) {
            System.out.println(attacker.getPlayerName() + " has no weapon");
            return;
        }

        // target must not already be frozen
        if (target.isFrozen()) {
            System.out.println(target.getPlayerName() + " is already frozen");
            return;
        }

        // apply freeze to target
        target.setFrozenTicksLeft(freezeTicks);

        // deduct points from target
        int newScore = Math.max(0, target.getPlayerScore() - POINTS_DEDUCTED);
        target.setPlayerScore(newScore);

        // reward attacker
        attacker.setPlayerScore(attacker.getPlayerScore() + POINTS_AWARDED);

        // disarm attacker
        attacker.setArmed(false);

        eventBroadcaster.accept(GameEvent.playerFroze(
                attacker.getPlayerId(), target.getPlayerId(),
                target.getPlayerPositionX(), target.getPlayerPositionY()));

        System.out.println(attacker.getPlayerName() + " froze " + target.getPlayerName()
                + " — +" + POINTS_AWARDED + " to attacker, -" + POINTS_DEDUCTED + " from target");
    }
}
