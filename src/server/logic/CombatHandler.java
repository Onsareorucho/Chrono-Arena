package server.logic;

import server.Player;

public class CombatHandler {

    private static final int FREEZE_TICKS = 60; // 3 seconds frozen
    private static final int POINTS_DEDUCTED = 15; // points lost when frozen
    private static final int POINTS_AWARDED = 15; // points awarded to attacker
    private static final int ATTACK_RANGE = 2; // tiles
    private static final int WEAPON_COOLDOWN_TICKS = 200; // 10 seconds

    private final CollisionHandler collisionHandler;

    public CombatHandler(CollisionHandler collisionHandler) {
        this.collisionHandler = collisionHandler;
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
        target.setFrozenTicksLeft(FREEZE_TICKS);

        // deduct points from target
        int newScore = Math.max(0, target.getPlayerScore() - POINTS_DEDUCTED);
        target.setPlayerScore(newScore);

        // reward attacker
        attacker.setPlayerScore(attacker.getPlayerScore() + POINTS_AWARDED);

        // disarm attacker and start cooldown
        attacker.setArmed(false);

        System.out.println(attacker.getPlayerName() + " froze " + target.getPlayerName()
                + " — +" + POINTS_AWARDED + " to attacker, -" + POINTS_DEDUCTED + " from target");
    }
}