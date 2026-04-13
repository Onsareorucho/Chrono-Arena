# ChronoArena

A multiplayer arena game where players compete to capture zones while freezing opponents with freeze rays.

## Requirements

- Java 17 or higher
- Maven 3.9+ (for building)

## Building

```bash
mvn clean package
```

This creates two JAR files in `target/`:
- `ChronoArena-Server.jar` - The game server
- `ChronoArena-Client.jar` - The game client

## Running

**Start the server first:**
```bash
java -jar target/ChronoArena-Server.jar
```

**Then start clients (one per player):**
```bash
java -jar target/ChronoArena-Client.jar
```

## Controls

| Key | Action |
|-----|--------|
| W / Up Arrow | Move up |
| S / Down Arrow | Move down |
| A / Left Arrow | Move left |
| D / Right Arrow | Move right |
| Space | Fire freeze ray |
| E | Use powerup |

## Configuration

Edit `config/game.properties` to change settings:

```properties
server.ip=localhost
server.tcp.port=9000
server.udp.port=9001
game.duration.seconds=60
game.tick.rate.ms=50
map.width=40
map.height=40
min.players=2
```

## Project Structure

```
src/
├── Main.java           # Entry point
├── client/             # Client-side code
│   ├── GameClient.java # Main client class
│   ├── InputHandler.java
│   ├── gui/            # Rendering and UI
│   └── network/        # Client networking
├── server/             # Server-side code
│   ├── GameServer.java # Main server class
│   ├── GameLoop.java   # Game tick logic
│   ├── GameState.java  # World state
│   ├── logic/          # Game rules (combat, zones)
│   └── network/        # Server networking
└── shared/             # Shared protocol
    ├── GameMessage.java
    ├── MessageType.java
    ├── MessageSerializer.java
    └── ...
```

## Network Protocol

The game uses a dual-channel networking approach:
- **TCP** (port 9000): Reliable delivery for game state updates, join/leave, and events
- **UDP** (port 9001): Fast delivery for player movement and actions

All messages are wrapped in `GameMessage` objects and serialized using Java's built-in serialization.
