# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Tour de JUG is a Spring Boot 4 web application that visualizes worldwide Java User Groups (JUGs) on a map and lets speakers maintain public profiles of their JUG talks. Authentication is GitHub OAuth2 only. Frontend is server-rendered with Thymeleaf + Tailwind CSS + vanilla JavaScript.

## Build & Run Commands

```bash
# Build (skipping tests)
./mvnw package -DskipTests

# Run unit tests only (Surefire)
./mvnw test

# Run all tests including integration + E2E (Surefire + Failsafe)
./mvnw verify

# Run a single test class
./mvnw test -Dtest=JugImportServiceIT

# Run a single integration/E2E test class
./mvnw verify -Dit.test=EntryPageE2EIT

# Local dev (auto-starts PostgreSQL via Docker Compose support)
./mvnw spring-boot:run
```

Requires Docker running for tests (Testcontainers for PostgreSQL) and for local dev (Spring Boot Docker Compose support auto-manages `compose.yaml`). E2E tests require Chrome installed.

## Architecture

- **Java 21, Spring Boot 4.0.2, Maven**
- **Database**: PostgreSQL 16 with Flyway migrations (`src/main/resources/db/migration/`)
- **Schema managed by Flyway only** — JPA is set to `validate`, never generates DDL
- **JUG data**: Seeded from `src/main/resources/jugs.yaml` on startup via `JugImportService` (delta sync: adds new, updates existing, deactivates removed)
- **Security**: GitHub OAuth2 login configured in `SecurityConfig`; public endpoints are `/`, `/profiles/**`, `/jugs/**`, `/api/**`
- **Test infrastructure**:
  - `TestcontainersConfiguration` provides a shared PostgreSQL container for all tests
  - E2E tests extend `BaseE2EIT` which sets up headless Chrome (Selenide) + WireMock to simulate GitHub OAuth2
  - Integration tests use `*IT` suffix (picked up by Failsafe plugin)
- **Deployment**: Docker multi-stage build → deployed on VPS with nginx reverse proxy + Let's Encrypt (see `docker-compose.prod.yml`, `terraform/`, `deploy/`)
- **CI**: GitHub Actions runs `./mvnw verify` on push/PR to main

## Key Packages

```
digital.pragmatech.tour_de_jug
├── config/      # Spring Security (OAuth2) configuration
├── domain/      # JPA entities: AppUser, JavaUserGroup, TalkEvent, TalkStatus
├── repository/  # Spring Data JPA repositories
├── service/     # JugImportService, OAuthUserService, ProfileService
└── web/         # MVC controllers: Home, Jug, Profile, SpeakingEvent, Admin
```

## Important Conventions

- Database schema changes go through Flyway migrations (V{N}__description.sql), never through JPA auto-DDL
- JUG data is maintained in `jugs.yaml` — community contributes via PR; `JugImportService` handles the sync
- Talk events follow an approval workflow: speakers submit (PENDING), JUG admins approve/reject (see `TalkStatus` enum and `jug_admin` join table)
- OAuth2 test stubs: `GitHubMockServer` + WireMock replaces real GitHub endpoints in E2E tests
