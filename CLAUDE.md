# Meterly — context for Claude

SaaS metered billing & invoicing engine, built step by step as a **learning project**.
The owner is relearning Java/Spring Boot. Explain the key ideas plainly, add one step at a time, and pause between steps.

## Working style
- One step at a time. Explain the "why", verify that it runs, then wait for "next".
- After each step, append a section to `docs/Meterly-Learning-Journal.docx` (python-docx: open the existing file and append; never regenerate it, because the user writes notes in it). Each section gets side-by-side code/meaning tables, commands, a mini-task, interview Q&A and blank notes lines. Also update the roadmap status table (`tables[0]`) and the glossary (`tables[1]`).
- Commit after every step.

## Stack
Java 21, Spring Boot 3.5, Spring Data JPA, PostgreSQL 17 (Docker, `compose.yaml`), Flyway, later Spring Security JWT (writer/reader), springdoc-openapi, JUnit 5 + Testcontainers, React + TS portal.

## Core design
- Hierarchy: account → service → endpoint. Usage is recorded per endpoint and rolled up.
- `usage_event.idempotency_key` UNIQUE, so a retried request is counted once.
- `billing_period.version` is for optimistic locking (`@Version`). Only one OPEN period per account (partial unique index).
- Money is NUMERIC / BigDecimal. Cost is derived from events × `metric_price`, not stored.
- A finalized invoice is immutable.

## Roadmap
1. ✅ Project skeleton
2. ✅ PostgreSQL + Flyway + V1 schema (+ HomeController at `/`)
3. ⏳ JPA entities
4. Usage ingestion API + Idempotency-Key
5. Concurrency: @Version, retries, multi-thread test
6. Cost aggregation endpoint → service → account
7. Period close & immutable invoice
8. Spring Security + JWT
9. OpenAPI docs + Testcontainers tests
10. React + TypeScript portal

## Run
```bash
docker compose up -d
mvn spring-boot:run      # port set in application.properties (currently 9091)
```
