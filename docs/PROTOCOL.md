# ChronoArena Network Protocol

**Author:** Person 2 (Networking Layer)  
**Version:** 1.0

---

## Overview

ChronoArena uses two transport protocols:

| Direction         | Transport | Purpose                              |
|-------------------|-----------|--------------------------------------|
| Client → Server   | UDP       | Movement & action inputs (low latency) |
| Client → Server   | TCP       | Join request, heartbeats             |
| Server → Client   | TCP       | Game state, scores, player events    |

All messages are serialized as **JSON** using Gson.  
TCP messages use **length-prefixed framing**: `[4-byte big-endian int][JSON bytes]`.  
UDP messages are raw JSON (no length prefix — they're self-contained datagrams).

---

## TCP Wire Format

```
[INT: payload length in bytes][UTF-8 JSON bytes]
```

Both `ClientConnection.send()` and `TCPClientHandler.send()` implement this.  
Both sides' read methods call `DataInputStream.readInt()` then `readFully()`.

---

## Message Types

All messages have a `"type"` field. `MessageSerializer.fromJson()` uses it to pick the right class.

### JOIN_REQUEST  _(Client → Server, TCP)_
```json
{ "type": "JOIN_REQUEST", "playerName": "Alice" }
```

### JOIN_RESPONSE  _(Server → Client, TCP)_
```json
{ "type": "JOIN_RESPONSE", "accepted": true, "assignedPlayerId": 3, "udpPort": 9001 }
{ "type": "JOIN_RESPONSE", "accepted": false, "rejectReason": "Game already in progress" }
```

### ACTION_MESSAGE  _(Client → Server, UDP)_
```json
{
  "type": "ACTION_MESSAGE",
  "playerId": 3,
  "sequenceNumber": 42,
  "actionType": "MOVE",
  "direction": "UP",
  "timestamp": 1712345678900
}
```
`actionType` values: `MOVE`, `ATTACK`, `COLLECT`, `USE_ABILITY`  
`direction` values: `UP`, `DOWN`, `LEFT`, `RIGHT`, `UP_LEFT`, `UP_RIGHT`, `DOWN_LEFT`, `DOWN_RIGHT`, `NONE`

### GAME_STATE_UPDATE  _(Server → Client, TCP)_
```json
{
  "type": "GAME_STATE_UPDATE",
  "tickNumber": 1234,
  "gameState": { /* full GameState object — defined by Person 1 */ }
}
```
`tickNumber` is monotonically increasing. Client should **discard** updates with `tickNumber ≤ lastApplied`.

### SCORE_UPDATE  _(Server → Client, TCP)_
```json
{ "type": "SCORE_UPDATE", "scores": { "1": 150, "2": 90, "3": 210 } }
```

### PLAYER_EVENT  _(Server → Client, TCP)_
```json
{
  "type": "PLAYER_EVENT",
  "eventType": "FROZEN",
  "playerId": 2,
  "details": "Hit by Player 1's freeze-ray"
}
```
`eventType` values: `JOINED`, `LEFT`, `FROZEN`, `UNFROZEN`, `KILLED`

### KILL_CLIENT  _(Server → Client, TCP)_
```json
{ "type": "KILL_CLIENT", "targetPlayerId": 2, "reason": "Erratic behavior detected" }
```

### GAME_OVER  _(Server → Client, TCP)_
```json
{ "type": "GAME_OVER", "finalScores": { "1": 500, "2": 300 }, "winnerPlayerId": 1 }
```

### HEARTBEAT / HEARTBEAT_ACK  _(bidirectional, TCP)_
```json
{ "type": "HEARTBEAT",     "playerId": 3, "sentAt": 1712345678900 }
{ "type": "HEARTBEAT_ACK", "originalSentAt": 1712345678900 }
```
Client sends `HEARTBEAT` every 2 seconds. Server replies immediately with `HEARTBEAT_ACK`.  
Server evicts a client after 5 missed heartbeats (~10 seconds of silence).

---

## UDP Sequence Numbers

- Each `ACTION_MESSAGE` carries a `sequenceNumber` starting at 0, incrementing by 1 per packet.
- The server (`PacketSequencer`) uses a **sliding window of size 32**.
- Packets more than 32 behind the latest seen sequence are **dropped as duplicates/stale**.
- A sudden jump forward (> 64) is treated as a client restart/reconnect — window resets.

---

## Integration Wiring (how to connect the layers)

### Server initialization (Person 1 / GameServer.java)
```java
ConfigLoader config = new ConfigLoader("game.properties");

TCPServerHandler tcp = new TCPServerHandler(config.getTcpPort(), config.getUdpPort());
UDPServerHandler udp = new UDPServerHandler(config.getUdpPort());

// Wire UDP into the server's action queue
udp.onActionReceived = actionQueue::add;

// Wire TCP events into server logic
tcp.onPlayerJoined = gameState::addPlayer;
tcp.onPlayerLeft   = gameState::removePlayer;

tcp.start();
udp.start();

// In game loop — broadcast every tick:
tcp.broadcast(new GameStateUpdate(tickNumber, gameState));

// KILL_SWITCH usage:
tcp.kill(playerId, "Too many invalid packets");
```

### Client initialization (Person 3 / GameClient.java)
```java
ConfigLoader config = new ConfigLoader("game.properties");

TCPClientHandler tcp = new TCPClientHandler(config.getServerIp(), config.getTcpPort(), playerName);
JoinResponse jr = tcp.connect(); // blocks until server responds

UDPClientHandler udp = new UDPClientHandler(config.getServerIp(), jr.udpPort, jr.assignedPlayerId);

// Wire TCP updates to GUI
tcp.onGameStateUpdate = gameScreen::updateState;
tcp.onScoreUpdate     = gameScreen::updateScores;
tcp.onPlayerEvent     = gameScreen::handlePlayerEvent;
tcp.onGameOver        = gameScreen::showGameOver;

tcp.start();
udp.start();

// From InputHandler on key press:
udp.sendMove(Direction.UP);
udp.sendAttack();
```

---

## Conflict Resolution Rules

These must be implemented in **ActionProcessor.java** (Person 1), but the networking layer  
preserves fairness by ensuring the server's action queue receives packets in arrival order.

| Conflict                          | Resolution Rule                                              |
|-----------------------------------|--------------------------------------------------------------|
| Two players grab same item        | Lower `sequenceNumber` arrival timestamp wins. Loser gets nothing. |
| Two players capture same zone     | Both must be present → zone is CONTESTED. First arrival started the timer. |
| Player disconnects while in zone  | Grace timer of 5s starts. If not back → zone resets to UNCLAIMED. |
| Duplicate UDP packet              | `PacketSequencer` drops it — never reaches the queue.        |
| Stale out-of-order UDP packet     | `PacketSequencer` drops if outside 32-packet window.         |
| Client silent for >10s            | Heartbeat monitor evicts and broadcasts `PLAYER_EVENT: LEFT`. |

---

## Properties File Format (game.properties)

```properties
server.ip=localhost
server.tcp.port=9000
server.udp.port=9001
game.duration.seconds=180
game.tick.rate.ms=50
map.width=20
map.height=20
```
