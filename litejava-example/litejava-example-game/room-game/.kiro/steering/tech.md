# Tech Stack

## Backend
- Java 21
- LiteJava (custom lightweight framework)
- MyBatis + MySQL (lobby-server only)
- WebSocket + JSON for real-time communication
- Custom service registry for discovery

## Frontend
- Vue 3 + Vite 5
- Pinia (state management)
- Vue Router 4
- Plain CSS (no framework)

## Build System
Maven multi-module project with parent POM inheritance.

## Common Commands

### Build
```bash
mvn clean package
mvn clean package -DskipTests
```

### Run Individual Services
```bash
# Account Server (start first)
mvn exec:java -pl account-server -Dexec.mainClass=game.account.AccountServer

# Hall Server
mvn exec:java -pl hall-server -Dexec.mainClass=game.hall.HallServer

# Game Servers (examples)
mvn exec:java -pl game-doudizhu -Dexec.mainClass=game.doudizhu.DoudizhuServer
mvn exec:java -pl game-gobang -Dexec.mainClass=game.gobang.GobangServer
mvn exec:java -pl game-mahjong -Dexec.mainClass=game.mahjong.MahjongServer
```

### Run with Custom Ports (for scaling)
```bash
mvn exec:java -pl game-doudizhu -Dserver.id=ddz-2 -Dserver.wsPort=9102 -Dserver.httpPort=9103
```

### Frontend
```bash
cd game-client
npm install
npm run dev      # Development server on port 3000
npm run build    # Production build
```

### One-Click Start (Windows)
```bash
start-all.cmd    # CMD
.\start-all.ps1  # PowerShell
```

## Key Dependencies
- `litejava-plugins`: Core framework with WebSocket support
- `room-game-common`: Shared code (Cmd, GameServer base class, VOs)
- `snakeyaml`: YAML configuration
- `jackson-databind`: JSON serialization
- `HikariCP` + `mybatis` + `mysql-connector-j`: Database (lobby only)
