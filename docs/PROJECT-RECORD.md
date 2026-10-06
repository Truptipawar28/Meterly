# Meterly — Project Record

A running log of **every file in the project**: why it exists, how it was added, and what the code means.
A new section is added at the end of every step.

---

## Roadmap

| # | Step | Status |
|---|------|--------|
| 1 | Project skeleton | ✅ Done |
| 2 | PostgreSQL + Flyway + V1 schema (+ HomeController at `/`) | ✅ Done |
| 3 | JPA entities | ✅ Done |
| 4 | Usage ingestion API + Idempotency-Key | ✅ Done |
| 5 | Concurrency: `@Version`, retries, multi-thread test | ✅ Done |
| 6 | Cost aggregation endpoint → service → account | ✅ Done |
| 7 | Period close & immutable invoice | ✅ Done |
| 8 | Spring Security + JWT (OAuth2 resource server, writer/reader roles) | ✅ Done |
| 9 | OpenAPI docs + tests (JUnit 5, Mockito, Testcontainers) | ✅ Done |
| 10 | Dockerfile (app + Postgres in compose) + GitHub Actions CI + README badge | |
| 11 | React + TypeScript portal (Vite, React Query) | |

---

## File map (all files so far)

| File | Added in | Purpose |
|------|----------|---------|
| `pom.xml` | Step 1 | Maven build file: Java version, libraries (dependencies) and plugins |
| `src/main/java/com/meterly/MeterlyApplication.java` | Step 1 | Entry point: the `main` method that starts Spring Boot |
| `src/main/java/com/meterly/HomeController.java` | Step 2 | Simple REST endpoint at `/` that proves the app is running |
| `src/main/resources/application.properties` | Step 1–2 | App settings: port, database URL/user/password, JPA behaviour |
| `compose.yaml` | Step 2 | Docker Compose file that starts PostgreSQL 17 locally |
| `src/main/resources/db/migration/V1__init_schema.sql` | Step 2 | Flyway migration that creates all database tables |
| `.gitignore` | Step 1 | Tells Git which files not to track (`target/`, IDE folders, local AI notes) |
| `README.md` | Step 1 | Short project description + how to run |
| `docs/Meterly-Learning-Journal.docx` | Step 1 | Your personal learning notes (Word) |
| `docs/PROJECT-RECORD.md` | Step 3 | This file |
| `src/main/java/com/meterly/domain/Metric.java` | Step 3 | Enum of billable metrics (API_CALLS, AI_TOKENS, STORAGE, EGRESS) |
| `src/main/java/com/meterly/domain/PeriodStatus.java` | Step 3 | Enum OPEN / CLOSED for billing periods |
| `src/main/java/com/meterly/domain/Account.java` | Step 3 | Entity → `account` table |
| `src/main/java/com/meterly/domain/ServiceEntity.java` | Step 3 | Entity → `service` table |
| `src/main/java/com/meterly/domain/Endpoint.java` | Step 3 | Entity → `endpoint` table |
| `src/main/java/com/meterly/domain/MetricPrice.java` | Step 3 | Entity → `metric_price` table |
| `src/main/java/com/meterly/domain/BillingPeriod.java` | Step 3 | Entity → `billing_period` table (with `@Version`) |
| `src/main/java/com/meterly/domain/UsageEvent.java` | Step 3 | Entity → `usage_event` table |
| `src/main/java/com/meterly/repository/*Repository.java` | Step 3 | 6 Spring Data interfaces: save / find / delete for each entity, with no code needed |
| `mvnw`, `mvnw.cmd` | Step 3 | Maven Wrapper scripts (Linux/Mac and Windows): run Maven without installing it |
| `.mvn/wrapper/maven-wrapper.properties` | Step 3 | Which Maven version the wrapper downloads (3.9.11) |
| `.gitattributes` | Step 4 | Forces LF line endings for `mvnw` so it runs on Linux (Docker, CI) |
| `api/ApiException.java` | Step 4 | Our "expected error" type that carries an HTTP status (404, 409, 422) |
| `api/ApiExceptionHandler.java` | Step 4 | Converts exceptions into JSON error responses (problem details) |
| `api/dto/CatalogDtos.java` | Step 4 | Request/response records for accounts, services, endpoints, periods |
| `api/dto/UsageDtos.java` | Step 4 | Request/response records for usage ingestion |
| `api/CatalogController.java` | Step 4 | REST: create and list accounts, services, endpoints, periods |
| `api/UsageController.java` | Step 4 | REST: `POST /api/usage` with the `Idempotency-Key` header |
| `service/CatalogService.java` | Step 4 | Business logic for the hierarchy; a new account gets an OPEN period |
| `service/UsageService.java` | Step 4 | Records one usage event in one transaction (idempotency check + insert) |
| `service/UsageIngestion.java` | Step 4–5 | Non-transactional wrapper: retries on concurrency conflicts, handles duplicate-key races |
| `src/main/resources/db/migration/V2__period_usage_counters.sql` | Step 5 | New table `period_usage`: running totals with a `version` column |
| `domain/PeriodUsage.java` | Step 5 | Entity for the running totals, with `@Version` |
| `repository/PeriodUsageRepository.java` | Step 5 | Find a counter by period + endpoint + metric |
| `src/test/java/com/meterly/TestcontainersConfig.java` | Step 5 | Starts PostgreSQL 17 in Docker for tests |
| `src/test/java/com/meterly/IntegrationTest.java` | Step 5 | Base class for full-app tests (skipped when Docker is off) |
| `src/test/java/com/meterly/service/ConcurrentUsageTest.java` | Step 5 | Multi-thread test: totals stay exact, duplicate keys stored once |
| `service/cost/UsageTotal.java` | Step 6 | Row of the GROUP BY query: quantity per endpoint + metric |
| `service/cost/CostReport.java` | Step 6 | The cost tree returned by the API (account → services → endpoints → lines) |
| `service/cost/CostCalculator.java` | Step 6 | Pure money math: line amounts, roll-up, allowance, rounding |
| `service/cost/CostService.java` | Step 6 | Loads data for a report; reconciliation of counters vs events |
| `api/CostController.java` | Step 6 | REST: current cost, cost per period, reconcile |
| `src/test/java/com/meterly/service/cost/CostCalculatorTest.java` | Step 6 | Unit tests for the cost math (no Spring, no DB) |
| `src/main/resources/db/migration/V3__invoices.sql` | Step 7 | Tables `invoice` + `invoice_line`, triggers that forbid UPDATE/DELETE |
| `domain/Invoice.java`, `domain/InvoiceLine.java` | Step 7 | `@Immutable` entities; `Invoice.issue(...)` freezes a cost report |
| `repository/InvoiceRepository.java` | Step 7 | Load an invoice with its lines; list invoices of an account |
| `service/InvoiceService.java` | Step 7 | Close period → invoice → open next period, in one transaction |
| `api/dto/InvoiceDtos.java` | Step 7 | Invoice summary / detail response records |
| `api/InvoiceController.java` | Step 7 | REST: close period, list invoices, get invoice |
| `src/test/java/com/meterly/service/InvoiceFlowTest.java` | Step 7 | Integration test: close flow, immutability via raw SQL |
| `security/SecurityConfig.java` | Step 8 | URL rules per role, JWT decoder/encoder, roles claim → Spring authorities |
| `security/SecurityProperties.java` | Step 8 | Typed `meterly.security.*` settings (secret, token lifetime, demo users) |
| `security/TokenService.java` | Step 8 | Dev token issuer: checks demo password, signs a JWT |
| `security/AccountAccess.java` | Step 8 | Row-level rule: a READER sees only the account in their token |
| `api/AuthController.java` | Step 8 | `POST /api/auth/token` (dev login), `GET /api/me` |
| `config/DemoDataLoader.java` | Step 8 | Fills an empty DB with sample accounts, usage and one invoice |
| `src/test/java/com/meterly/api/SecurityAndApiTest.java` | Step 8 | HTTP tests: 401/403, row-level access, idempotent replay |
| `config/OpenApiConfig.java` | Step 9 | API title/description + "Authorize" (bearer JWT) button in Swagger UI |
| `src/test/java/com/meterly/service/UsageIngestionTest.java` | Step 9 | Mockito unit test of the retry logic |
| `src/test/java/com/meterly/security/AccountAccessTest.java` | Step 9 | Unit test of the row-level access rule |

---

## Step 1 — Project skeleton

**Goal:** a Spring Boot app that compiles and starts.

### How it was added
1. Created the folder layout Maven expects:
   ```
   src/main/java/com/meterly/      ← Java code (package com.meterly)
   src/main/resources/             ← config files, SQL
   src/test/java/                  ← tests (later)
   ```
2. Wrote `pom.xml` by hand (same thing start.spring.io generates).
3. Wrote `MeterlyApplication.java`.
4. Run with `mvn spring-boot:run`.

### `pom.xml` — key parts

| Code | Meaning |
|------|---------|
| `<parent> spring-boot-starter-parent 3.5.6` | Inherit Spring Boot's tested library versions, so we don't write versions ourselves |
| `<groupId>com.meterly</groupId>` `<artifactId>meterly</artifactId>` | The project's "name and address" in the Maven world |
| `<java.version>21</java.version>` | Compile for Java 21 |
| `spring-boot-starter-web` | Embedded Tomcat web server + REST controllers + JSON (Jackson) |
| `spring-boot-starter-data-jpa` | Hibernate/JPA: map Java classes ↔ database tables |
| `postgresql` (scope `runtime`) | JDBC driver so Java can talk to PostgreSQL; only needed when running |
| `flyway-core` + `flyway-database-postgresql` | Runs versioned SQL files on startup to build/upgrade the schema |
| `spring-boot-starter-test` (scope `test`) | JUnit 5, AssertJ, Mockito, Spring test helpers |
| `spring-boot-maven-plugin` | Enables `mvn spring-boot:run` and builds a runnable jar |

### `MeterlyApplication.java`

```java
@SpringBootApplication
public class MeterlyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeterlyApplication.class, args);
    }
}
```

| Code | Meaning |
|------|---------|
| `@SpringBootApplication` | = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`. Turns on auto-setup and scans `com.meterly` and its sub-packages for components |
| `SpringApplication.run(...)` | Starts the Spring container, the web server and everything else |

> Rule: every class must live in `com.meterly` or a sub-package (e.g. `com.meterly.domain`), or Spring won't find it.

---

## Step 2 — PostgreSQL + Flyway + V1 schema

**Goal:** a real database, with tables created automatically and version-controlled.

### How it was added
1. `compose.yaml`: defines a `postgres:17` container.
2. `application.properties`: points the app at that database.
3. `V1__init_schema.sql`: Flyway runs it once on the first startup.
4. `HomeController.java`: a quick "is it alive?" endpoint.

Commands:
```bash
docker compose up -d          # start PostgreSQL in the background
mvn spring-boot:run           # start the app → Flyway creates tables
# open http://localhost:9091/  → {"app":"Meterly","status":"running"}
docker compose down           # stop (data is kept in the volume)
```

### `compose.yaml`

| Code | Meaning |
|------|---------|
| `image: postgres:17` | Official PostgreSQL 17 image |
| `container_name: meterly-db` | A fixed name, so `docker logs meterly-db` works |
| `POSTGRES_DB / USER / PASSWORD: meterly` | Creates the database and user on first start |
| `ports: "5432:5432"` | Host port 5432 → container port 5432 |
| `volumes: meterly-data:/var/lib/postgresql/data` | Data survives container restarts |

### `application.properties`

| Setting | Meaning |
|---------|---------|
| `server.port=9091` | The app listens on port 9091 |
| `spring.datasource.url=jdbc:postgresql://localhost:5432/meterly` | Where the database is |
| `spring.datasource.username/password=meterly` | Must match `compose.yaml` |
| `spring.jpa.hibernate.ddl-auto=validate` | Hibernate **does not** create tables; it only checks that entities match the tables Flyway made. If not, the app refuses to start |
| `spring.jpa.open-in-view=false` | Don't keep a DB session open during web rendering (avoids hidden lazy-loading queries) |

### `V1__init_schema.sql` — the tables

File name rule: `V<version>__<description>.sql` (two underscores). Flyway records applied files in the
`flyway_schema_history` table. **Never edit an applied migration**: add `V2__...sql` instead.

| Table | Purpose | Notable rules |
|-------|---------|---------------|
| `account` | A customer | `plan_included` NUMERIC: the free allowance |
| `service` | A product/service owned by an account | `UNIQUE (account_id, name)` |
| `endpoint` | An API endpoint within a service; usage is recorded here | `UNIQUE (service_id, name)` |
| `metric_price` | Price per unit for each metric | Seeded: API_CALLS, AI_TOKENS, STORAGE, EGRESS |
| `billing_period` | A billing month for an account | `status` OPEN/CLOSED, `version` for optimistic locking, partial unique index → only **one OPEN period per account** |
| `usage_event` | Raw usage reports: the source of truth for cost | `idempotency_key UNIQUE` → a retried request is counted once; `quantity > 0` |

Hierarchy: **account → service → endpoint → usage_event** (each event also points to a billing_period and a metric).

### `HomeController.java`

| Code | Meaning |
|------|---------|
| `@RestController` | This class handles HTTP requests; return values become JSON |
| `@GetMapping("/")` | `GET /` calls this method |
| `return Map.of("app","Meterly","status","running")` | A Map is turned into the JSON object `{"app":"Meterly","status":"running"}` |

---

## Step 3 — JPA entities + repositories

**Goal:** one Java class per table, so we can read and write rows as objects instead of writing SQL.

### How it was added
1. Created package `com.meterly.domain` with 2 enums + 6 entity classes.
2. Created package `com.meterly.repository` with 6 repository interfaces.
3. No new dependencies: `spring-boot-starter-data-jpa` was already in `pom.xml` from Step 1.

```
src/main/java/com/meterly/
├── domain/        ← entities (what the data looks like)
│   ├── Metric.java, PeriodStatus.java          (enums)
│   └── Account, ServiceEntity, Endpoint, MetricPrice, BillingPeriod, UsageEvent
└── repository/    ← how we load/save it
    └── AccountRepository, ServiceRepository, EndpointRepository,
        MetricPriceRepository, BillingPeriodRepository, UsageEventRepository
```

How to verify:
```bash
docker compose up -d
.\mvnw spring-boot:run        # Windows  (Linux/Mac: ./mvnw spring-boot:run)
```
`ddl-auto=validate` compares every entity with its table at startup. A wrong column name or type stops the
app with an error like `Schema-validation: missing column [...]`. **A clean start = every mapping is correct.**

### Entity annotations (used in every entity)

| Code | Meaning |
|------|---------|
| `@Entity` | "This class is stored in the database." Hibernate manages its objects |
| `@Table(name = "account")` | Which table it maps to (default would be the class name) |
| `@Id` | The primary key field |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | The database generates the id (our `BIGSERIAL`); Java leaves it `null` until saved |
| `@Column(name = "plan_included", nullable = false, precision = 19, scale = 6)` | Field ↔ column. Needed when the names differ (Java `planIncluded`, SQL `plan_included`) |
| `protected Account() {}` | JPA must create empty objects when loading rows. `protected` stops our own code from creating half-filled objects |
| Public constructor with required fields | The way *we* create a valid new object |
| Getters only, no setters | Fields can't be changed from anywhere at random; changes go through meaningful methods like `BillingPeriod.close()` |

### Java types chosen for SQL types

| SQL | Java | Why |
|-----|------|-----|
| `BIGSERIAL` / `BIGINT` | `Long` / `long` | 64-bit number. `Long` (object) for ids because it's `null` before saving |
| `VARCHAR` | `String` | Text |
| `NUMERIC(19,6)` | `BigDecimal` | Exact decimals. `double` gives rounding errors (0.1 + 0.2 ≠ 0.3), which is unacceptable for money |
| `TIMESTAMPTZ` | `Instant` | An exact moment in time (UTC) |
| `DATE` | `LocalDate` | A calendar date with no time |
| `VARCHAR` holding fixed values | `enum` + `@Enumerated(EnumType.STRING)` | Type-safe: the compiler rejects `"API_CALSS"`. `STRING` stores the name; the default `ORDINAL` stores 0,1,2…, which breaks if the enum is reordered |

### Relationships — `@ManyToOne`

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "account_id", nullable = false)
private Account account;
```

| Code | Meaning |
|------|---------|
| `@ManyToOne` | Many services → one account. In Java we hold the **object** `Account`; in SQL it's the number `account_id` |
| `@JoinColumn(name = "account_id")` | The foreign-key column that stores the link |
| `fetch = FetchType.LAZY` | Don't load the account until `getAccount()` is actually used. The default for `@ManyToOne` is EAGER, which runs extra queries you didn't ask for |
| `optional = false` | Every service must have an account (matches `NOT NULL`) |

Used in: `ServiceEntity → Account`, `Endpoint → ServiceEntity`, `BillingPeriod → Account`,
`UsageEvent → Endpoint` and `UsageEvent → BillingPeriod`.

### Special cases

| Class | Detail | Why |
|-------|--------|-----|
| `ServiceEntity` | Not called `Service` | Spring already has a `@Service` annotation; the same name would be confusing |
| `MetricPrice` | `@Id` is the `Metric` enum, not a generated number | The SQL primary key is the metric name itself |
| `UsageEvent.metric` | Mapped as the enum, not `@ManyToOne MetricPrice` | We only need the value; prices are looked up when calculating cost (Step 6) |
| `BillingPeriod.version` | `@Version` | **Optimistic locking**: every UPDATE becomes `... WHERE id=? AND version=?` and increases the version. If another thread changed the row first, 0 rows match and Hibernate throws `OptimisticLockException` instead of silently losing data (used in Step 5) |
| `createdAt`, `receivedAt` | `= Instant.now()` + `updatable = false` | Set once in Java when the object is created, and never changed |

### Repositories

```java
public interface AccountRepository extends JpaRepository<Account, Long> {
}
```

| Code | Meaning |
|------|---------|
| `interface ... extends JpaRepository<Account, Long>` | `<entity type, id type>`. We write **no implementation**: Spring generates a class at startup |
| What we get for free | `save()`, `findById()`, `findAll()`, `count()`, `deleteById()`, paging & sorting… |
| `MetricPriceRepository extends JpaRepository<MetricPrice, Metric>` | The id type is `Metric` because that entity's key is the enum |

Later steps add **derived queries**: Spring writes the SQL from the method name, e.g.
`Optional<BillingPeriod> findByAccountIdAndStatus(Long accountId, PeriodStatus status)`.

### Interview Q&A

- **Why `BigDecimal` and not `double` for money?** `double` is binary floating point and can't represent 0.1 exactly, so totals drift. `BigDecimal` is exact.
- **LAZY vs EAGER?** LAZY loads the related object on first use; EAGER loads it immediately with extra joins/queries. Prefer LAZY and fetch explicitly when needed (it avoids the N+1 query problem).
- **Why `ddl-auto=validate` instead of `update`?** The schema belongs to versioned Flyway migrations. `update` lets Hibernate change production tables silently and can't do renames or data moves.
- **What does `@Version` do?** Optimistic locking: it detects concurrent updates by checking a version number in the UPDATE's WHERE clause, so it needs no database locks.
- **Why does JPA need a no-arg constructor?** Hibernate creates the object first, then fills its fields from the row (using reflection).

### Mini-task
Add `Optional<BillingPeriod> findByAccountIdAndStatus(Long accountId, PeriodStatus status);` to
`BillingPeriodRepository` and predict the SQL that Spring will generate.

### Tooling set up in this step (Windows)

| Tool | How it was installed | Check |
|------|----------------------|-------|
| Java 21 (Eclipse Temurin) | `winget install EclipseAdoptium.Temurin.21.JDK` | `java -version` |
| Maven 3.9.11 | **Maven Wrapper** in the project: no install. The first `.\mvnw` run downloads Maven into `~/.m2/wrapper` | `.\mvnw -v` |
| Docker Desktop | `winget install Docker.DockerDesktop`, then open Docker Desktop once | `docker -v` |

**Why the Maven Wrapper?** Everyone (you, teammates, GitHub Actions) builds with the *same* Maven version,
and nobody has to install Maven. That's why Spring Initializr projects always include `mvnw`.
From now on: use `.\mvnw ...` wherever docs say `mvn ...`.

---

## Step 4 — Usage ingestion API + Idempotency-Key

**Goal:** workers send usage over HTTP; a retried request is **counted once**.

> Paths below are under `src/main/java/com/meterly/`.

### How it was added
1. `pom.xml`: added `spring-boot-starter-validation` (for `@NotBlank`, `@Positive`, `@Valid`).
2. New package `api/` (controllers, DTOs, error handling) and `service/` (business logic).
3. Repository queries added: `findByIdempotencyKey`, `findByAccountIdAndStatus`, `findWithServiceAndAccount` (join fetch)…

### The layers (a request travels top → bottom)

| Layer | Class | Job |
|-------|-------|-----|
| Controller | `UsageController` | HTTP only: read header + JSON body, validate, choose status code |
| Facade | `UsageIngestion` | Not transactional; can react when a transaction fails |
| Service | `UsageService` | `@Transactional` business logic |
| Repository | `UsageEventRepository` … | Database access |

### Try it
```bash
# create an account (gets an OPEN period automatically), a service, an endpoint
curl -X POST localhost:9091/api/accounts -H "Content-Type: application/json" -d '{"name":"Acme","planIncluded":5}'
curl -X POST localhost:9091/api/accounts/1/services -H "Content-Type: application/json" -d '{"name":"search"}'
curl -X POST localhost:9091/api/services/1/endpoints -H "Content-Type: application/json" -d '{"name":"GET /search"}'

# record usage — run it twice: 1st = 201 Created, 2nd = 200 OK + "Idempotent-Replayed: true"
curl -i -X POST localhost:9091/api/usage -H "Content-Type: application/json" -H "Idempotency-Key: evt-001" \
     -d '{"endpointId":1,"metric":"API_CALLS","quantity":100}'
```
(From Step 8 on, these calls need a JWT: see Step 8.)

### Idempotency: three layers of protection

| Situation | What happens | Code |
|-----------|--------------|------|
| Retry arrives later | `findByIdempotencyKey` finds it → return the old event, **200** + `Idempotent-Replayed: true` | `UsageService.record` step 1 |
| Two copies arrive **at the same instant** | Both see "not used yet", both INSERT; the `UNIQUE` constraint rejects one → `DataIntegrityViolationException` → the loser looks up the winner's event and returns it | `UsageIngestion.ingest` |
| Same key, **different** body | Client bug → **422 Unprocessable Entity** | `UsageService.findExisting` |

**Why the database constraint matters:** the "check then insert" in Java has a race window between the check and the insert. Only the database can make "unique" truly unique.

### Key code

| Code | Meaning |
|------|---------|
| `@RestController` + `@RequestMapping("/api/usage")` | All methods in this class handle URLs starting with `/api/usage` |
| `@RequestHeader("Idempotency-Key") String key` | Reads the header; if it's missing, Spring answers 400 automatically |
| `@Valid @RequestBody UsageRequest request` | JSON → record, then check `@NotNull`/`@Positive`; on failure → 400 with details |
| `ResponseEntity.status(201).body(...)` | Full control over status code + headers + body |
| `@Transactional` | Everything in the method commits together or rolls back together |
| `@Transactional(readOnly = true)` | Hint for reads: Hibernate skips dirty checking |
| `saveAndFlush(event)` | `save` + send the SQL **now** (normally it's sent at commit), so the UNIQUE error happens inside our try/catch |
| `join fetch` in `@Query` | Load endpoint + service + account in one SQL query |
| `record UsageRequest(...)` | Java 16+ immutable data class: constructor, getters (`request.quantity()`), `equals`, `toString` generated |
| `@RestControllerAdvice` + `@ExceptionHandler` | One central place that turns exceptions into HTTP responses |
| `ProblemDetail` | Standard JSON error format (RFC 9457): `status`, `title`, `detail` |

### Why `UsageIngestion` is separate from `UsageService`
When a `@Transactional` method throws, its transaction is marked **rollback-only**: you can't continue
using it. So the "try again / look up the winner" logic must sit **outside**, in a non-transactional
bean, and call the service again to get a **fresh** transaction. (Calling a method on the same class
wouldn't work either: Spring's transaction proxy only intercepts calls coming *from another bean*.)

### Interview Q&A
- **What is idempotency?** Doing an operation twice has the same effect as doing it once. Needed because networks fail and clients retry.
- **Why 200 instead of 201 on replay?** Nothing new was created; the client still gets the same body, so a retry is safe.
- **Why DTOs instead of returning entities?** The API stays stable when tables change, there are no lazy-loading surprises during JSON conversion, and internal fields aren't exposed.
- **What's the self-invocation problem?** `this.someTransactionalMethod()` bypasses Spring's proxy, so `@Transactional` is ignored.

---

## Step 5 — Concurrency: `@Version`, retries, multi-thread test

**Goal:** many workers report usage for the same endpoint at the same moment; totals must stay exact.

### How it was added
1. `V2__period_usage_counters.sql`: new table `period_usage` (running total per period + endpoint + metric, with `version`).
2. `PeriodUsage` entity + repository.
3. `UsageService.record` now also adds to the running total, in the **same transaction** as the event insert.
4. `UsageIngestion` got a **retry loop**.
5. `BillingPeriodRepository` got two locking queries (`FOR SHARE` / `FOR UPDATE`).
6. `pom.xml`: Testcontainers test dependencies; first integration test.

### The problem: lost updates
```
Thread A reads quantity = 100      Thread B reads quantity = 100
Thread A writes 100 + 5 = 105      Thread B writes 100 + 7 = 107   ← A's +5 is lost!
```

### Solution 1: optimistic locking (`@Version`) + retry — for the hot counter rows
```sql
UPDATE period_usage SET quantity = 107, version = 6 WHERE id = 1 AND version = 5;
-- Thread A already changed version to 6 → 0 rows match → Hibernate throws an exception
```
- No locks are held while working, so it's fast when conflicts are rare.
- On conflict the **whole transaction rolls back** (the event insert too), so event and counter never disagree.
- `UsageIngestion` catches the exception, waits a random 1–5×attempt ms (**jitter**) and tries again in a **new transaction**: it re-reads the fresh value and adds to it.

### Solution 2: pessimistic locks — between ingestion and closing a period

| Who | Query | Lock | Effect |
|-----|-------|------|--------|
| Recording usage | `findOpenForUsage` | `PESSIMISTIC_READ` → `SELECT … FOR SHARE` | Many ingesters at once, no waiting between them |
| Closing the period (Step 7) | `findOpenForClose` | `PESSIMISTIC_WRITE` → `SELECT … FOR UPDATE` | Waits for running ingesters; new ones wait for the close |

Without it, an event could commit into a period *just after* its invoice was calculated, and that usage would never be billed.

### What gets retried (and why)

| Exception | Cause | Action |
|-----------|-------|--------|
| `OptimisticLockingFailureException` | Someone updated the same counter first | Retry |
| `DataIntegrityViolationException` | UNIQUE race: same Idempotency-Key → return the winner's event; first counter row for endpoint+metric → retry (the row exists now) | Replay or retry |
| `PessimisticLockingFailureException` | Deadlock/lock timeout reported by PostgreSQL | Retry |
| After `meterly.usage.max-attempts` (20) | Extreme contention | 409 "please retry" |

### The multi-thread test (`ConcurrentUsageTest`)
- 8 threads × 25 events, each sent **twice** (retry storm), all on the **same counter row**.
- A `CountDownLatch` releases all threads at the same instant to maximise collisions.
- Asserts: 200 events, quantity = 200 × 3, event_count = 200, version = 199.
- Second test: 10 threads send the **same key** at once → exactly 1 stored, 1 `created=true`.

Run: `.\mvnw test` (needs Docker running; otherwise these tests are skipped).

### Key code

| Code | Meaning |
|------|---------|
| `@Version private Long version;` | Hibernate adds `AND version = ?` to UPDATEs and increments it |
| `@Lock(LockModeType.PESSIMISTIC_READ)` | Adds `FOR SHARE` to the query |
| `@Value("${meterly.usage.max-attempts:20}")` | Inject a setting from `application.properties` (20 if missing) |
| `ThreadLocalRandom.current().nextLong(...)` | Random back-off, so threads don't retry in lockstep |
| `@ServiceConnection` on a container bean | Spring Boot points the datasource at the test container automatically |
| `@Testcontainers(disabledWithoutDocker = true)` | Skip, instead of failing, when Docker isn't available |

### Interview Q&A
- **Optimistic vs pessimistic locking?** Optimistic: no lock, detect conflicts at write time (version check), retry. Good when conflicts are rare. Pessimistic: lock the row first (`FOR UPDATE`), others wait. Good when conflicts are common or a retry is expensive.
- **Why retry in a new transaction?** The failed transaction is rolled back and its snapshot is stale; only a fresh one sees the winner's value.
- **Alternative for counters?** An atomic `UPDATE … SET quantity = quantity + ?` has no read-modify-write and no conflicts. We used `@Version` to learn optimistic locking; the atomic update is often the production choice for pure counters.
- **Why is the double-send test important?** It proves idempotency and concurrency control work *together*.

---

## Step 6 — Cost aggregation: endpoint → service → account

**Goal:** show what an account owes right now, broken down by service, endpoint and metric.

### How it was added
1. `UsageEventRepository.sumByEndpointAndMetric`: one `GROUP BY` query over the raw events.
2. New package `service/cost/`: `UsageTotal` (query row), `CostReport` (result tree), `CostCalculator` (math), `CostService` (loading).
3. `CostController` with 3 endpoints.
4. `CostCalculatorTest`: first pure unit test.

### Endpoints

| Method + URL | Returns |
|--------------|---------|
| `GET /api/accounts/{id}/cost` | Cost tree of the current OPEN period |
| `GET /api/periods/{id}/cost` | Cost tree of any period |
| `GET /api/periods/{id}/reconcile` | Counters (`period_usage`) vs events: `[]` = consistent |

### How the cost is computed
```
usage_event rows ──GROUP BY endpoint, metric──▶ UsageTotal rows (SQL does this)
UsageTotal × metric_price.unit_price ─────────▶ LineCost.amount  (exact, 8 decimals)
sum of lines ─▶ endpoint subtotal ─▶ service subtotal ─▶ usageTotal
amountDue = max(0, usageTotal − planIncluded) rounded to cents (HALF_EVEN)
```

**The double-counting trap:** if you computed the service total from events *and* added its endpoints' totals, or summed
an endpoint row and a service row, every event would count twice. Rule: **compute money only at the leaf, then only add up.**

**Why cost isn't stored:** the price × quantity is derived when needed, so a price fix or a bug fix never leaves stale totals
behind. (The invoice in Step 7 *does* store a snapshot, because an invoice must never change.)

### Key code

| Code | Meaning |
|------|---------|
| `select new com.meterly.service.cost.UsageTotal(...)` | JPQL **constructor expression**: each result row becomes a record directly |
| `sum(u.quantity) ... group by ...` | Aggregation runs in PostgreSQL, so only a few summary rows travel to Java |
| `BigDecimal.multiply / add` | Exact decimal math |
| `setScale(2, RoundingMode.HALF_EVEN)` | Round to cents; ties go to the even digit (0.125 → 0.12, 0.135 → 0.14) to avoid bias |
| `isEqualByComparingTo("2.50")` | AssertJ: compares BigDecimal by value (`2.5` equals `2.50`); `equals` would also compare scale |
| `LinkedHashMap` | A map that keeps insertion order, so the report keeps the SQL sort order |
| `@Component` on `CostCalculator` | It's a Spring bean, but has no dependencies, so tests can just `new` it |

### Interview Q&A
- **Why round only at the end?** Rounding every line and then summing accumulates errors (many lines of 0.004 would all round to 0.00).
- **What is HALF_EVEN?** Banker's rounding: .5 goes to the nearest even digit, so over many values the rounding errors cancel out.
- **Why a pure calculator class?** Business rules can be tested in milliseconds without a database; the service only does I/O.
- **What is reconciliation?** An independent check that two representations of the same data (counters vs events) agree.

---

## Step 7 — Period close & immutable invoice

**Goal:** at month end, freeze the period's cost into an invoice that can never change, and start the next period.

### How it was added
1. `V3__invoices.sql`: `invoice` + `invoice_line` tables and two **triggers** that reject UPDATE/DELETE.
2. Entities `Invoice` / `InvoiceLine` with Hibernate's `@Immutable`.
3. `InvoiceService.closePeriod`, `InvoiceController`, response DTOs.
4. `BillingPeriod.close()` now refuses to close twice.

### Endpoints

| Method + URL | What it does |
|--------------|--------------|
| `POST /api/accounts/{id}/close-period` | Close the open period → **201** + the invoice |
| `GET /api/accounts/{id}/invoices` | List invoices (summary) |
| `GET /api/invoices/{id}` | Invoice with all lines |

### The close, step by step (one transaction)

| # | Code | Why |
|---|------|-----|
| 1 | `findOpenForClose` → `SELECT … FOR UPDATE` | Waits for in-flight usage writes, blocks new ones: nothing slips in after the total is computed |
| 2 | `costs.report(period)` | Same calculator as the live cost (Step 6) |
| 3 | `period.close(); saveAndFlush(period)` | Flush **now**: Hibernate runs INSERTs before UPDATEs at commit, so the new OPEN period would break the "one open period" index |
| 4 | `Invoice.issue(period, report)` + `save` | Snapshot: copies names, unit prices, amounts |
| 5 | `new BillingPeriod(...next month...)` | Usage keeps flowing into the new period |

If anything fails, **everything** rolls back: no half-closed period, and no period without an invoice.

### Immutability: three layers

| Layer | How |
|-------|-----|
| Java | No setters; the only way to create one is `Invoice.issue(...)`; `getLines()` returns an unmodifiable list |
| Hibernate | `@Immutable`: changes to the object are ignored, no UPDATE is ever generated |
| PostgreSQL | `BEFORE UPDATE OR DELETE` trigger → `RAISE EXCEPTION`, even from psql or another app |

**Why a snapshot?** If the invoice only referenced `metric_price`, a price change next year would silently change last year's invoice.

### Key code

| Code | Meaning |
|------|---------|
| `@OneToMany(mappedBy = "invoice", cascade = CascadeType.PERSIST)` | Invoice → many lines; the FK lives in `invoice_line.invoice_id` (`mappedBy` points to that field); saving the invoice saves its lines |
| `@OrderBy("id")` | Lines always load in insertion order |
| `left join fetch i.lines` | Load invoice + lines in one query (avoids LazyInitializationException, since `open-in-view=false`) |
| `select distinct i` | The join returns one row per line; `distinct` gives one Invoice object |
| `CREATE FUNCTION … LANGUAGE plpgsql` | A small function stored in PostgreSQL; `TG_TABLE_NAME` / `TG_OP` say which table and operation fired it |

### Interview Q&A
- **How do you make data immutable?** In several layers: the API (no update endpoint), the domain (no setters), the ORM (`@Immutable`), the database (triggers/permissions). The DB layer is the one that can't be bypassed.
- **How do you correct a wrong invoice then?** Issue a **credit note** (a new document with negative amounts), never edit the original. That's standard accounting practice.
- **What's Hibernate's flush order?** Inserts → updates → collection changes → deletes. When a constraint depends on order, flush explicitly.

---

## Step 8 — Spring Security + JWT (OAuth2 resource server)

**Goal:** every API call must prove who it is. Internal services may **write** usage; customers may only **read** their own account.

### How it was added
1. `pom.xml`: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-security-test`.
2. New package `security/`: `SecurityConfig`, `SecurityProperties`, `TokenService`, `AccountAccess`.
3. `AuthController`: dev login + "who am I".
4. `@PreAuthorize` / `access.check(...)` added to cost and invoice endpoints.
5. `application.properties`: JWT secret, token lifetime, demo users.
6. `DemoDataLoader`: sample data, so the demo `client` user (account 1) has something to see.

### Roles

| Role | Who | Allowed |
|------|-----|---------|
| `WRITER` | Internal usage-logging services | `POST /api/usage` |
| `READER` | A customer's dashboard | `GET` cost / invoices / account **of the account in their token only** |
| `ADMIN` | Operators | Everything (catalog, close period, reconcile, all accounts) |

Demo logins (dev only): `admin/admin123`, `worker/worker123`, `client/client123` (account 1).

### Try it
```bash
# 1. get a token
curl -X POST localhost:9091/api/auth/token -H "Content-Type: application/json" \
     -d '{"username":"client","password":"client123"}'
# → {"accessToken":"eyJhbGciOiJIUzI1NiJ9...","tokenType":"Bearer","expiresIn":28800,...}

# 2. use it
curl localhost:9091/api/accounts/1/cost -H "Authorization: Bearer eyJhbGciOi..."
curl localhost:9091/api/accounts/2/cost -H "Authorization: Bearer eyJhbGciOi..."   # → 403
```
Paste a token into https://jwt.io to see what's inside (header, claims, signature).

### How a request is checked
```
Authorization: Bearer <jwt>
   │
   ▼ BearerTokenAuthenticationFilter
   ▼ JwtDecoder: signature valid (HS256 secret)? not expired? issuer = "meterly"?   ── no → 401
   ▼ JwtAuthenticationConverter: claim roles ["READER"] → authority ROLE_READER
   ▼ authorizeHttpRequests: URL rule, e.g. GET /api/accounts/*/cost needs READER or ADMIN ── no → 403
   ▼ @PreAuthorize("@access.canRead(authentication, #accountId)"): own account?          ── no → 403
   ▼ controller method
```

### Key code

| Code | Meaning |
|------|---------|
| `SecurityFilterChain` bean | The security rules for all HTTP requests |
| `csrf.disable()` + `STATELESS` | No cookies or sessions; the token travels with every request, so CSRF attacks don't apply |
| `.requestMatchers(POST, "/api/usage").hasAnyRole("WRITER","ADMIN")` | URL-level rule; **first matching rule wins**, so order matters |
| `.anyRequest().hasRole("ADMIN")` | Deny-by-default: anything not listed is admin-only |
| `oauth2ResourceServer(o -> o.jwt(...))` | "This API accepts JWT bearer tokens" |
| `NimbusJwtDecoder.withSecretKey(key)` | Verifies the HMAC signature with our secret |
| `JwtValidators.createDefaultWithIssuer("meterly")` | Also checks the `exp`/`nbf` times and the `iss` claim |
| `@EnableMethodSecurity` + `@PreAuthorize("...")` | A rule on a single method, written in SpEL; `#accountId` = the method parameter, `@access` = the bean named "access" |
| `@ConfigurationProperties(prefix="meterly.security")` record | Typed settings; `users.client.account-id` → `users.get("client").accountId()` |
| `${METERLY_JWT_SECRET:dev-only...}` | Use an environment variable if set, else the dev default |
| `MessageDigest.isEqual(a, b)` | Constant-time comparison (no timing leak) |
| `@ConditionalOnProperty(...)` on `DemoDataLoader` | The bean only exists when the property is `true` (tests set it to `false`) |
| `ApplicationRunner` | Code that runs once after the app has started |

### 401 vs 403
- **401 Unauthorized** = "I don't know who you are": the token is missing, invalid or expired.
- **403 Forbidden** = "I know who you are, but you're not allowed to do this."

### Production notes
- Tokens would come from an identity provider (Keycloak/Auth0/Okta). Only `jwt-secret` → `spring.security.oauth2.resourceserver.jwt.issuer-uri` changes (RS256 public keys via JWKS); the URL rules and `@PreAuthorize` stay the same.
- `/api/auth/token` and the demo users would be removed.

### Interview Q&A
- **What's in a JWT?** `header.payload.signature`, each Base64URL-encoded. The payload (claims) is **readable by anyone**: it's signed, not encrypted. Never put secrets in it.
- **HS256 vs RS256?** HS256: one shared secret signs and verifies. RS256: a private key signs, a public key verifies, so many services can verify without being able to issue tokens.
- **How do you revoke a JWT?** Short lifetimes + refresh tokens, or a deny-list. Stateless tokens can't simply be "logged out".
- **URL rules vs `@PreAuthorize`?** URL rules for coarse role checks; method security for data-dependent rules (whose account is it?).

---

## Step 9 — OpenAPI docs + tests (JUnit 5, Mockito, Testcontainers)

**Goal:** live API documentation generated from the code, and a test suite at three levels.

### How it was added
1. `pom.xml`: `springdoc-openapi-starter-webmvc-ui` 2.8.17 (the 2.8 line is for Spring Boot 3.x).
2. `OpenApiConfig`: title, description, JWT "Authorize" button.
3. `@Operation` / `@Parameter` on `POST /api/usage` for richer docs.
4. Mockito unit test (`UsageIngestionTest`) + access rule test (`AccountAccessTest`).
5. Surefire loads Mockito as a Java agent (removes a JDK warning).

### Swagger UI
Open **http://localhost:9091/swagger-ui.html** → `POST /api/auth/token` → "Try it out" → copy `accessToken`
→ **Authorize** (top right) → paste → now every "Try it out" call sends the token.
The raw spec (JSON) is at `/v3/api-docs`; tools can generate clients from it.

### The test pyramid in this project

| Level | Tests | Tools | Speed | Needs Docker |
|-------|-------|-------|-------|--------------|
| Unit (pure logic) | `CostCalculatorTest`, `AccountAccessTest` | JUnit 5 + AssertJ | ms | No |
| Unit with mocks | `UsageIngestionTest` | Mockito | ms | No |
| Integration (service + real DB) | `ConcurrentUsageTest`, `InvoiceFlowTest` | `@SpringBootTest` + Testcontainers | seconds | Yes |
| API over HTTP | `SecurityAndApiTest` | `TestRestTemplate` + random port | seconds | Yes |

Run all: `.\mvnw test`. Without Docker, the integration tests are **skipped** (not failed).

### Mockito in 4 lines

| Code | Meaning |
|------|---------|
| `@ExtendWith(MockitoExtension.class)` + `@Mock UsageService usageService` | Creates a fake `UsageService` with no real behaviour |
| `when(mock.record(...)).thenThrow(x).thenReturn(y)` | Script it: 1st call throws, 2nd returns |
| `verify(mock, times(2)).record(...)` | Assert how often it was called |
| `mock(UsageEvent.class)` | A dummy object when only its identity matters |

**Why mock here?** A real optimistic-lock conflict is hard to trigger on demand; a mock produces one every time, so each
retry branch (conflict → retry, duplicate key → replay, counter race → retry, give up → 409) gets its own deterministic test.

### Why Testcontainers and not H2?
H2 is an in-memory database that only *imitates* PostgreSQL. Our code relies on real PostgreSQL behaviour: partial unique
indexes, `FOR SHARE` locks, plpgsql triggers, `TIMESTAMPTZ`. Testcontainers runs the **same** `postgres:17` image as production.

### Interview Q&A
- **Unit vs integration test?** A unit test checks one class in isolation (dependencies mocked); an integration test checks real collaboration (Spring + DB + HTTP).
- **When not to mock?** Don't mock what you're testing, or things you don't own like the database: test those with the real thing (Testcontainers).
- **What is OpenAPI?** A standard, machine-readable description of a REST API (paths, parameters, schemas, auth). Swagger UI is one viewer for it.
