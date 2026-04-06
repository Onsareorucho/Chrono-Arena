package shared.protocol;

import org.junit.jupiter.api.Test;
import shared.Direction;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that every message type survives a JSON roundtrip:
 *   object → toJson() → fromJson() → fields match original
 *
 * This is the most important networking test because if serialization
 * is broken, nothing else works at all.
 */
class MessageSerializerTest {

    // ── JoinRequest ────────────────────────────────────────────

    @Test
    void joinRequest_roundtrip() {
        JoinRequest original = new JoinRequest("Alice");
        String json = MessageSerializer.toJson(original);
        JoinRequest result = (JoinRequest) MessageSerializer.fromJson(json);

        assertEquals(Message.MessageType.JOIN_REQUEST, result.type);
        assertEquals("Alice", result.playerName);
    }

    // ── JoinResponse ───────────────────────────────────────────

    @Test
    void joinResponse_accept_roundtrip() {
        JoinResponse original = JoinResponse.accept(3, 9001);
        String json = MessageSerializer.toJson(original);
        JoinResponse result = (JoinResponse) MessageSerializer.fromJson(json);

        assertEquals(Message.MessageType.JOIN_RESPONSE, result.type);
        assertTrue(result.accepted);
        assertEquals(3, result.assignedPlayerId);
        assertEquals(9001, result.udpPort);
        assertNull(result.rejectReason);
    }

    @Test
    void joinResponse_reject_roundtrip() {
        JoinResponse original = JoinResponse.reject("Game in progress");
        String json = MessageSerializer.toJson(original);
        JoinResponse result = (JoinResponse) MessageSerializer.fromJson(json);

        assertFalse(result.accepted);
        assertEquals("Game in progress", result.rejectReason);
    }

    // ── ActionMessage ──────────────────────────────────────────

    @Test
    void actionMessage_move_roundtrip() {
        ActionMessage original = ActionMessage.move(2, 99L, Direction.UP_LEFT);
        String json = MessageSerializer.toJson(original);
        ActionMessage result = (ActionMessage) MessageSerializer.fromJson(json);

        assertEquals(Message.MessageType.ACTION_MESSAGE, result.type);
        assertEquals(2, result.playerId);
        assertEquals(99L, result.sequenceNumber);
        assertEquals(ActionMessage.ActionType.MOVE, result.actionType);
        assertEquals(Direction.UP_LEFT, result.direction);
    }

    @Test
    void actionMessage_attack_roundtrip() {
        ActionMessage original = ActionMessage.attack(1, 42L);
        String json = MessageSerializer.toJson(original);
        ActionMessage result = (ActionMessage) MessageSerializer.fromJson(json);

        assertEquals(ActionMessage.ActionType.ATTACK, result.actionType);
        assertEquals(Direction.NONE, result.direction);
        assertEquals(1, result.playerId);
    }

    // ── GameStateUpdate ────────────────────────────────────────

    @Test
    void gameStateUpdate_roundtrip() {
        // Use a simple JsonObject as stand-in for the full GameState
        com.google.gson.JsonObject fakeState = new com.google.gson.JsonObject();
        fakeState.addProperty("timeRemainingSeconds", 120);
        fakeState.addProperty("tickNumber", 500L);

        GameStateUpdate original = new GameStateUpdate(500L, fakeState);
        String json = MessageSerializer.toJson(original);
        GameStateUpdate result = (GameStateUpdate) MessageSerializer.fromJson(json);

        assertEquals(Message.MessageType.GAME_STATE_UPDATE, result.type);
        assertEquals(500L, result.tickNumber);
        assertEquals(120, result.gameState.get("timeRemainingSeconds").getAsInt());
    }

    // ── ScoreUpdate ────────────────────────────────────────────

    @Test
    void scoreUpdate_roundtrip() {
        ScoreUpdate original = new ScoreUpdate(Map.of(1, 150, 2, 90, 3, 210));
        String json = MessageSerializer.toJson(original);
        ScoreUpdate result = (ScoreUpdate) MessageSerializer.fromJson(json);

        assertEquals(Message.MessageType.SCORE_UPDATE, result.type);
        assertEquals(150, result.scores.get(1));
        assertEquals(90,  result.scores.get(2));
        assertEquals(210, result.scores.get(3));
    }

    // ── PlayerEvent ────────────────────────────────────────────

    @Test
    void playerEvent_frozen_roundtrip() {
        PlayerEvent original = new PlayerEvent(PlayerEvent.EventType.FROZEN, 4, "Hit by Player 2");
        String json = MessageSerializer.toJson(original);
        PlayerEvent result = (PlayerEvent) MessageSerializer.fromJson(json);

        assertEquals(Message.MessageType.PLAYER_EVENT, result.type);
        assertEquals(PlayerEvent.EventType.FROZEN, result.eventType);
        assertEquals(4, result.playerId);
        assertEquals("Hit by Player 2", result.details);
    }

    // ── KillClient ─────────────────────────────────────────────

    @Test
    void killClient_roundtrip() {
        KillClient original = new KillClient(7, "Sending malformed packets");
        String json = MessageSerializer.toJson(original);
        KillClient result = (KillClient) MessageSerializer.fromJson(json);

        assertEquals(Message.MessageType.KILL_CLIENT, result.type);
        assertEquals(7, result.targetPlayerId);
        assertEquals("Sending malformed packets", result.reason);
    }

    // ── GameOver ───────────────────────────────────────────────

    @Test
    void gameOver_roundtrip() {
        GameOver original = new GameOver(Map.of(1, 500, 2, 300), 1);
        String json = MessageSerializer.toJson(original);
        GameOver result = (GameOver) MessageSerializer.fromJson(json);

        assertEquals(Message.MessageType.GAME_OVER, result.type);
        assertEquals(1, result.winnerPlayerId);
        assertEquals(500, result.finalScores.get(1));
        assertEquals(300, result.finalScores.get(2));
    }

    // ── Heartbeat / Ack ────────────────────────────────────────

    @Test
    void heartbeat_roundtrip() {
        Heartbeat original = new Heartbeat(3);
        String json = MessageSerializer.toJson(original);
        Heartbeat result = (Heartbeat) MessageSerializer.fromJson(json);

        assertEquals(Message.MessageType.HEARTBEAT, result.type);
        assertEquals(3, result.playerId);
        assertTrue(result.sentAt > 0);
    }

    @Test
    void heartbeatAck_roundtrip() {
        HeartbeatAck original = new HeartbeatAck(1712345678900L);
        String json = MessageSerializer.toJson(original);
        HeartbeatAck result = (HeartbeatAck) MessageSerializer.fromJson(json);

        assertEquals(Message.MessageType.HEARTBEAT_ACK, result.type);
        assertEquals(1712345678900L, result.originalSentAt);
    }

    // ── Error cases ────────────────────────────────────────────

    @Test
    void fromJson_missingTypeField_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> MessageSerializer.fromJson("{\"playerName\":\"Alice\"}"));
    }

    @Test
    void fromJson_unknownType_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> MessageSerializer.fromJson("{\"type\":\"TOTALLY_FAKE_TYPE\"}"));
    }

    @Test
    void fromJson_malformedJson_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> MessageSerializer.fromJson("not json at all!!!"));
    }
}
