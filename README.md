# SpeedCubeBattle

SpeedCubeBattle is a competitive web platform where players solve a virtual 3x3 Rubik’s Cube — either solo or in real-time 1v1 online matches.

The project focuses on production-grade backend architecture rather than just game mechanics. It is built around server-authoritative match logic, real-time communication via WebSockets, and event-driven processing (Elo, leaderboards, statistics) handled by a separate worker service.

---

## Purpose

This project is made for learning and demonstration of:

- Realtime system design
- Server-authoritative multiplayer architecture
- Event-driven backend processing
- Clean architecture principles with Spring Boot
- Containerized local development using Docker
- Production-oriented infrastructure design

---

## Core Services

### Frontend
Built with Next.js and Three.js.

Responsible for:
- Rendering the 3D cube
- Handling user input
- Connecting to the backend via WebSockets
- Calling REST APIs for authentication and match data

### Backend
Built with Spring Boot.

Responsible for:
- REST API endpoints
- WebSocket realtime communication
- Authoritative cube and match state management
- Move validation
- Match lifecycle control
- Publishing domain events to RabbitMQ

### Worker
Built with Spring Boot.

Responsible for:
- Consuming events from RabbitMQ
- Calculating Elo rating updates
- Updating leaderboards and statistics
- Writing processed results to Postgres

### Postgres
Primary relational database.

Stores:
- Users
- Matches
- Results
- Ratings
- History data

### Rabbitmq
Message broker for asynchronous event processing.

Used to:
- Decouple backend from worker
- Buffer match-finished events
- Ensure reliable event processing

---

## How the Application Works (End State)

1. A player joins a solo session or a 1v1 match.
2. The client sends cube moves to the backend via WebSocket.
3. The backend validates each move and updates the cube state.
4. When a match finishes, the backend publishes a MatchFinished event to RabbitMQ.
5. The worker consumes the event and updates Elo ratings and leaderboards.
6. The frontend retrieves updated rankings through REST endpoints.

---

## Local Development (Initial Setup)

1. Create a `.env` file based on `.env.example`
2. Start infrastructure services:  `docker compose up -d`

3. Backend, frontend, and worker services will be added and connected in later stages.