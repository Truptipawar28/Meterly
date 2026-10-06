# Meterly
SaaS metered billing & invoicing engine: it takes in usage events from distributed workers, adds up each customer's cost in real time, and issues an immutable monthly invoice. It uses optimistic locking and idempotency keys to keep totals exact when many reports arrive at once.

**Stack:** Java 21 · Spring Boot 3 · Spring Data JPA · PostgreSQL · Flyway · Testcontainers

## Run locally
```bash
docker compose up -d      # start PostgreSQL
mvn spring-boot:run       # start the app
```

Learning notes: [docs/Meterly-Learning-Journal.docx](docs/Meterly-Learning-Journal.docx)
