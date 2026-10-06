# Meterly

[![CI](https://github.com/Truptipawar28/Meterly/actions/workflows/ci.yml/badge.svg)](https://github.com/Truptipawar28/Meterly/actions/workflows/ci.yml)

SaaS metered billing & invoicing engine: it takes in usage events from distributed workers, adds up each customer's cost in real time, and issues an immutable monthly invoice. It uses optimistic locking and idempotency keys to keep totals exact when many reports arrive at once.

**Stack:** Java 21 · Spring Boot 3.5 · Spring Data JPA · PostgreSQL 17 · Flyway · Spring Security (JWT) · springdoc-openapi · JUnit 5 · Mockito · Testcontainers · Docker · GitHub Actions

## Run locally

**Option A: develop (database in Docker, app from source)**
```bash
docker compose up -d          # start PostgreSQL
.\mvnw spring-boot:run        # start the app (Windows; ./mvnw on Linux/Mac)
```

**Option B: everything in Docker**
```bash
docker compose --profile app up -d --build
```

Then:
- API root: http://localhost:9091/
- Swagger UI: http://localhost:9091/swagger-ui.html
- Health: http://localhost:9091/actuator/health

Demo logins for `POST /api/auth/token`: `admin/admin123`, `worker/worker123`, `client/client123`.

## Tests
```bash
.\mvnw test        # unit tests always; integration tests when Docker is running
```

Project record (every file and what it does): [docs/PROJECT-RECORD.md](docs/PROJECT-RECORD.md)
