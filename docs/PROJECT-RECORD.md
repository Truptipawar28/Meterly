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
| 5 | Concurrency: `@Version`, retries, multi-thread test | |
| 6 | Cost aggregation endpoint → service → account | |
| 7 | Period close & immutable invoice | |
| 8 | Spring Security + JWT (OAuth2 resource server, writer/reader roles) | |
| 9 | OpenAPI docs + tests (JUnit 5, Mockito, Testcontainers) | |
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
| `service/UsageIngestion.java` | Step 4 | Non-transactional wrapper: handles the "two identical requests at once" race |

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
