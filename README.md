# SoPra FS26 – Server

## Introduction

This is the RESTful backend for a multiplayer, vision-based tile game built as part of the Software Engineering Lab (SoPra) at the University of Zurich. Players form teams or individually, race to photograph real-world objects that match words on a 4×4 game board. Submitted images are analysed in real time using the Google Cloud Vision API; if a detected object matches a board word, the tile is captured and points are awarded.

## Technologies

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.0 |
| Persistence | Spring Data JPA · H2 (in-memory) |
| Image Analysis | Google Cloud Vision API 3.40.0 |
| Real-time Messaging | Pusher HTTP Java 1.0.0 |
| Object Mapping | MapStruct 1.5.5 |
| Build | Gradle (Wrapper) |
| Deployment | Docker (Eclipse Temurin JDK 17) |

## High-Level Components

The backend follows a classic layered architecture. The five main components are:

### 1. Controllers
[`controller/`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/)

REST entry points that map HTTP requests to service calls. Key controllers:
- [`GameController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/GameController.java) – image submission, leaderboard, game state
- [`LobbyController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/LobbyController.java) – lobby creation, joining, starting
- [`UserController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/UserController.java) – registration, login, profile
- [`ChatController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/ChatController.java) – real-time chat messages

### 2. Services
[`service/`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/)

Business logic layer. The orchestrating class is [`GameOrchestrationService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/GameOrchestrationService.java), which coordinates the full game lifecycle (lobby → game → scoring → end). Supporting services handle scoring ([`ScoreService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/ScoreService.java)), timers ([`GameTimerService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/GameTimerService.java)), and real-time updates ([`PusherService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/PusherService.java)).

### 3. Entities & Repositories
[`entity/`](src/main/java/ch/uzh/ifi/hase/soprafs26/entity/) · [`repository/`](src/main/java/ch/uzh/ifi/hase/soprafs26/repository/)

JPA-managed domain objects persisted via Spring Data repositories. Core entities: `User`, `Game`, `Lobby`, `LobbyPlayer`, `Tile`, `Leaderboard`.

### 4. Vision Integration
[`VisionQuickstartObjectLocalization`](src/main/java/ch/uzh/ifi/hase/soprafs26/VisionQuickstartObjectLocalization.java)

Sends player images to the Google Cloud Vision API (label detection, object localisation, web detection) and returns matched game words. Results are filtered with a 50 % confidence threshold and normalised through [`SynonymMap`](src/main/java/ch/uzh/ifi/hase/soprafs26/SynonymMap.java).

### 5. REST DTOs & Mapper
[`rest/`](src/main/java/ch/uzh/ifi/hase/soprafs26/rest/)

MapStruct-generated [`DTOMapper`](src/main/java/ch/uzh/ifi/hase/soprafs26/rest/mapper/DTOMapper.java) converts between JPA entities and the DTOs exposed to clients, keeping the API contract decoupled from the persistence model.

## Launch & Deployment

### Prerequisites

- **Java 17** – set `JAVA_HOME` to JDK 17 on Windows
- **Google Cloud Vision credentials** – place your service account JSON at the path expected by `VisionQuickstartObjectLocalization` and set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable
- **Pusher account** – set the following environment variables:
  ```
  PUSHER_APP_ID
  PUSHER_KEY
  PUSHER_SECRET
  PUSHER_CLUSTER
  ```

### Run locally

```bash
# Build
./gradlew build        # macOS / Linux
./gradlew.bat build    # Windows

# Start the server (http://localhost:8080)
./gradlew bootRun

# Development mode – auto-restart on file changes
# Terminal 1:
./gradlew build --continuous -xtest
# Terminal 2:
./gradlew bootRun
```

The H2 in-memory database console is available at `http://localhost:8080/h2-console/`.

### Run tests

```bash
./gradlew test
```

### Docker deployment

All pushes to `main` are built and pushed to Docker Hub automatically via GitHub Actions.

**Manual pull & run:**
```bash
docker pull <dockerhub_username>/<dockerhub_repo_name>
docker run -p 8080:8080 \
  -e GOOGLE_APPLICATION_CREDENTIALS=/creds.json \
  -e PUSHER_APP_ID=... \
  -e PUSHER_KEY=... \
  -e PUSHER_SECRET=... \
  -e PUSHER_CLUSTER=... \
  <dockerhub_username>/<dockerhub_repo_name>
```

Set up Docker Hub secrets in your GitHub repository settings:
- `dockerhub_username`
- `dockerhub_password` (PAT with read/write access)
- `dockerhub_repo_name`

### API testing

Use [Postman](https://www.getpostman.com) to test the REST endpoints. The server exposes JSON over HTTP on port `8080`.

## Roadmap

The following features would be valuable additions for contributors:

1. **Custom word lists** – allow authenticated users to upload their own CSV word lists and select them when creating a lobby, replacing the built-in demo list.

2. **Tournament mode** – extend the game loop to support multi-round, bracket-style tournaments with persistent standings across sessions.

3. **Mobile client support** – the current API is frontend-agnostic; a React Native (or similar) mobile client could be developed against the existing endpoints and Pusher channels with no backend changes required.

## Authors and Acknowledgment

| Name | GitHub |
|---|---|
| Arda Aydın | [@ardaaydin](https://github.com/ardaaydin) |
| Laurin Glarner | [@laurinlarner](https://github.com/lauringlarner) |
| Naren Wallimann | — |[@Wallimann20-914-099](https://github.com/Wallimann20-914-099) |
| Melchior Kneubuehler | — |[@mel-kne](https://github.com/mel-kne)|
| Alessio Martinoli | - |[@AleMarti0](https://github.com/AleMarti0)|

This project was developed as part of the **Software Engineering Lab (SoPra FS26)** at the University of Zurich, Department of Informatics (IFI).

We used the [SoPra FS26 Server Template](https://github.com/HASEL-UZH/sopra-fs26-template-server) as the starting point.

## License

This project is licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for details.
