# SpeedCubeBattle – Architecture Overview

## 1. System Overview

SpeedCubeBattle is built as a containerized, event-driven, server-authoritative realtime system.

The architecture separates:

- Realtime match processing
- Asynchronous background processing
- Persistence
- Infrastructure concerns

The system is designed for scalability, decoupling, and production readiness.

---

## 2. Core Components

Frontend
- Next.js
- Three.js (3D cube rendering)
- WebSocket client
- REST client

Backend (Authoritative Server)
- Java + Spring Boot
- REST API
- WebSocket endpoint
- Match lifecycle management
- Cube engine & move validation
- Publishes domain events

Worker (Async Processor)
- Java + Spring Boot
- Consumes events from RabbitMQ
- Calculates Elo updates
- Updates leaderboards & statistics
- Writes to database

Database
- PostgreSQL
- Stores users, matches, ratings, history

Message Broker
- RabbitMQ
- Buffers domain events
- Decouples backend from worker

Infrastructure
- Docker / Docker Compose (local development)
- AWS Lightsail (production deployment)
- Optional Nginx reverse proxy

---

## 3. High-Level Communication Flow

### Realtime Match Flow

Frontend  
    ↓ (WebSocket)  
Backend  
    ↓  
PostgreSQL

- Client sends moves
- Backend validates & applies moves
- Backend synchronizes match state
- Backend determines match result

---

### Event-Driven Flow

Backend  
    ↓ (Publish event)  

RabbitMQ  
    ↓ (Consume event)  

Worker  
    ↓  
PostgreSQL

- Backend emits MatchFinished event
- Worker processes event asynchronously
- Elo & leaderboard updates are stored

---

## 4. Deployment Model

Local Development:
Docker Compose runs:
- PostgreSQL
- RabbitMQ

Backend, Worker, Frontend connect via environment variables.

Production (AWS Lightsail):
- Containerized services
- Separate DEV and PROD databases
- Environment-based configuration
- Optional reverse proxy (Nginx)

---

## 5. Architectural Principles

Server-Authoritative
- The backend is the single source of truth.
- Clients never determine match results.

Event-Driven
- Match results trigger domain events.
- Background processing is fully decoupled.

Separation of Concerns
- Realtime match handling and background computation are isolated.
- Infrastructure is separated from application logic.

Scalability
- Backend and worker can scale independently.
- RabbitMQ provides buffering and resilience.