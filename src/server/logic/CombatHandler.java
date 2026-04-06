package server.logic;

import server.Player;
import shared.GameConstants;

public class CombatHandler {

    private static final int POINTS_DEDUCTED = GameConstants.FREEZE_RAY_POINT_PENALTY;
    private static final int POINTS_AWARDED  = GameConstants.FREEZE_RAY_POINT_PENALTY;
    public  static final int ATTACK_RANGE    = GameConstants.FREEZE_RAY_RANGE_TILES;

    private final int freezeTicks;
    private final CollisionHandler collisionHandler;

    public CombatHandler(CollisionHandler collisionHandler, long tickRateMs) {
        this.collisionHandler = collisionHandler;
        this.freezeTicks = (int) (GameConstants.FREEZE_DURATION_MS / tickRateMs);
    }

    // called when a player fires their freeze weapon at a target
    public void handleFreezeAttack(Player attacker, Player target) {

        // attacker must have a weapon
        if (!attacker.isArmed()) {
            System.out.println(attacker.getPlayerName() + " has no weapon");
            return;
        }

        // target must be within range
        if (!collisionHandler.isWithinAttackRange(attacker, target, ATTACK_RANGE)) {
            System.out.println(target.getPlayerName() + " is out of range");
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

        System.out.println(attacker.getPlayerName() + " froze " + target.getPlayerName()
                + " — +" + POINTS_AWARDED + " to attacker, -" + POINTS_DEDUCTED + " from target");
    }
}
