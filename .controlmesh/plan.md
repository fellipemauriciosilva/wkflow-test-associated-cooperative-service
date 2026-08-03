<think></think># Test Implementation Plan — `associated-cooperative-service` (S1: Setup + Associate CRUD)

## 0. Pre-Flight Blocker (must fix before any test can compile)

The two files supplied do not match the real domain API described in the ROOT CAUSE note:

- `Associate` domain factory is `Associate.create(name, document, email)` — **no** `Associate.restore(...)`, **no** `update(...)`, **no** `changeName/changeDocument/changeEmail` mutators are confirmed to exist.
- `AssociateJpaEntity` uses a **constructor**, not setters — `AssociateMapper.toJpaEntity` and `AssociateService.update` were written against an assumed mutable API that doesn't exist on disk.

**Action required before test execution:** align `AssociateService.update()` and `AssociateMapper` with the real `Associate`/`AssociateJpaEntity` constructors (e.g., `Associate.create(...)` re-invoked with new values + original `id`/`createdAt` preserved via a real reconstruction factory, and `new AssociateJpaEntity(...)` in the mapper). Until this compiles, no unit/integration test in this plan can run — this is called out explicitly as **TC-000** below so it is tracked, not silently assumed fixed.

---

## 1. Scope & Test Levels

| Level | Target | Tooling |
|---|---|---|
| Unit — Domain | `Associate` validation rules | JUnit 5, plain (no Spring) |
| Unit — Application | `AssociateService` (create/find/update/delete) | JUnit 5 + Mockito, mocked `AssociateRepositoryPort` |
| Unit — Infrastructure | `AssociateMapper` (domain ⇄ JPA entity) | JUnit 5, plain |
| Integration | REST endpoints + persistence + Flyway | Spring Boot Test + Testcontainers PostgreSQL 16 |
| Failure/Negative | Validation, 404, 409/400 conflict, malformed input | Integration layer (MockMvc/WebTestClient over real app context) |
| Security | Input handling, error disclosure, IDOR-style enumeration | Integration layer + targeted unit checks |
| Architecture (Could Have) | `domain` package isolation | ArchUnit |

Nothing beyond the spec's Must/Should scope is tested (no auth, no Kafka functional flow, no Assembleias).

---

## 2. Unit Tests — Domain (`Associate`)

File: `domain/AssociateTest.java` — **zero Spring/JPA imports**, matches AC-3.4.

| ID | Case | Expectation |
|---|---|---|
| TC-D01 | `create(name=null, document, email)` | throws domain exception (e.g. `IllegalArgumentException`/custom `InvalidAssociateException`) |
| TC-D02 | `create(name="", document, email)` | throws same validation exception |
| TC-D03 | `create(name="  ", document, email)` | blank/whitespace-only rejected (boundary case not explicit in AC but implied by "nome obrigatório") |
| TC-D04 | `create(name, document, email="not-an-email")` | throws validation exception |
| TC-D05 | `create(name, document, email="user@example.com")` | succeeds, `getEmail()` returns value |
| TC-D06 | `create(name, document, email=null)` | succeeds, `getEmail()` returns null/empty (optional) |
| TC-D07 | `create(name, document=null/blank, email)` | document not-null assumption — validate whatever rule domain declares (document required per DB constraint even if not in the 5-case minimum) |
| TC-D08 | happy path: valid name/document/email | id assigned (or null pre-persist depending on design), `createdAt` set, no exceptions |
| TC-D09 (regression for blocker) | Whatever reconstruction/update mechanism the fixed domain exposes | new instance/state reflects updated name/document/email while preserving original `id` and `createdAt` |

**Assertion technique:** reflection-free — call public API only; assert via `assertThrows` + message/type, not framework annotations.

---

## 3. Unit Tests — Application (`AssociateService`)

File: `application/AssociateServiceTest.java` — Mockito mock of `AssociateRepositoryPort`.

| ID | Case | Expectation |
|---|---|---|
| TC-A01 | `create()` with non-duplicate document | `repository.save` invoked once with domain-valid `Associate`; returns saved entity |
| TC-A02 | `create()` with `existsByDocument(document) == true` | throws `DuplicateDocumentException`; `save` never called |
| TC-A03 | `findById()` existing id | returns mapped `Associate` |
| TC-A04 | `findById()` missing id | throws `AssociateNotFoundException` |
| TC-A05 | `findAll()` | delegates to `repository.findAll()`, returns list unmodified |
| TC-A06 | `update()` existing id, no document collision | repository queried, `save` called with updated fields, original `id`/`createdAt` preserved |
| TC-A07 | `update()` missing id | throws `AssociateNotFoundException` before touching document uniqueness check |
| TC-A08 | `update()` where new document collides with **another** associate (`existsByDocumentAndIdNot`) | throws `DuplicateDocumentException` |
| TC-A09 | `update()` where document unchanged (belongs to same id) | succeeds — regression guard against false-positive from `existsByDocumentAndIdNot` |
| TC-A10 | `delete()` existing id | `repository.deleteById` invoked |
| TC-A11 | `delete()` missing id | throws `AssociateNotFoundException`; `deleteById` never invoked |
| TC-A12 | Transactionality smoke check | `@Transactional` present on write methods (reflection/annotation check, or covered implicitly via integration rollback test) |

---

## 4. Unit Tests — Infrastructure Mapper (`AssociateMapper`)

File: `infrastructure/persistence/AssociateMapperTest.java`

| ID | Case | Expectation |
|---|---|---|
| TC-M01 | `toJpaEntity(domainAssociate)` | resulting `AssociateJpaEntity` fields (id, name, document, email, createdAt) equal source; **must use the real constructor signature**, not setters |
| TC-M02 | `toDomain(jpaEntity)` | resulting `Associate` fields match entity, via whatever real reconstruction API exists after fix (not `Associate.restore` unless confirmed present) |
| TC-M03 | Round-trip `toDomain(toJpaEntity(a)) == a` (field-wise equals) | no data loss/mutation across mapping |
| TC-M04 | Null email round-trip | mapper doesn't NPE on null email either direction |
| TC-M05 | Mapper has no `@Autowired` mutable state / is stateless & thread-safe (Virtual Threads context) | single instance reused safely across concurrent calls |

---

## 5. Integration Tests — REST + Persistence (Testcontainers PostgreSQL 16)

File: `infrastructure/rest/AssociateIntegrationTest.java` — `@SpringBootTest(webEnvironment=RANDOM_PORT)` + `@Testcontainers`, real Postgres container, Flyway runs against it, no mocks/H2 (per AC-3.5).

### 5.1 Infrastructure/Boot Checks
| ID | Case |
|---|---|
| TC-I01 | Context loads; Flyway applies `V1__create_associates_table.sql`; `flyway_schema_history` has success row for version 1 |
| TC-I02 | Table `associates` schema: `name` NOT NULL, `document` UNIQUE + NOT NULL, `email` nullable, `id`, `created_at` present (query `information_schema`) |
| TC-I03 | `GET /actuator/health` → 200, body `{"status":"UP"}` |

### 5.2 Create (`POST /associates`)
| ID | Case | Expected |
|---|---|---|
| TC-I04 | Valid name+document+email | 201, Location/body with generated id, echoed fields |
| TC-I05 | Valid name+document, no email | 201 |
| TC-I06 | Missing/blank name | 400 ProblemDetail (`type`,`title`,`status`,`detail`,`instance` all present) |
| TC-I07 | Duplicate document (create twice) | second call → 409 (or 400, per project's documented choice) ProblemDetail; verify only 1 row persisted |
| TC-I08 | Invalid email format (`"invalid"`) | 400 ProblemDetail |
| TC-I09 | Missing document field entirely | 400 ProblemDetail (DB NOT NULL / domain rule enforced pre-persist, not via raw SQL exception leak) |
| TC-I10 | Empty request body / malformed JSON | 400 (generic parse error handled by advice, not raw stack trace) |

### 5.3 Read
| ID | Case | Expected |
|---|---|---|
| TC-I11 | `GET /associates/{id}` existing | 200, correct payload |
| TC-I12 | `GET /associates/{id}` random UUID not in DB | 404 ProblemDetail |
| TC-I13 | `GET /associates/{id}` malformed id (non-UUID string) | 400 ProblemDetail (path variable conversion error handled, not 500) |
| TC-I14 | `GET /associates` with 2+ records | 200, list contains both, correct shape (array or paginated envelope per implementation) |
| TC-I15 | `GET /associates` empty DB | 200, empty list (not 404/error) |

### 5.4 Update (`PUT /associates/{id}`)
| ID | Case | Expected |
|---|---|---|
| TC-I16 | Valid update to existing id | 200, `GET` afterward reflects new values |
| TC-I17 | Update with blank name | 400 ProblemDetail |
| TC-I18 | Update with invalid email | 400 ProblemDetail |
| TC-I19 | Update document to another existing associate's document | 409/400 ProblemDetail; original record unchanged |
| TC-I20 | Update on non-existent id | 404 ProblemDetail |
| TC-I21 | Update keeping same document as itself | 200 success (no false duplicate) — directly exercises the bug area (`existsByDocumentAndIdNot`) |

### 5.5 Delete (`DELETE /associates/{id}`)
| ID | Case | Expected |
|---|---|---|
| TC-I22 | Delete existing id | 204, subsequent `GET` → 404 |
| TC-I23 | Delete non-existent id | 404 ProblemDetail |
| TC-I24 | Delete same id twice | first 204, second 404 |

### 5.6 ProblemDetail Contract Consistency
| ID | Case |
|---|---|
| TC-I25 | Parametrized test iterating all error-producing cases above (TC-I06,07,08,09,12,13,17,18,19,20,23) asserting `Content-Type: application/problem+json` and non-null `type/title/status/detail/instance` |

### 5.7 Cross-cutting infra
| ID | Case |
|---|---|
| TC-I26 | `spring.threads.virtual.enabled=true` reflected in loaded `Environment`/`@Value` check |
| TC-I27 | (Should Have) `/swagger-ui/index.html` reachable if Springdoc included; `/actuator/info` returns 200 if enabled |

---

## 6. Failure / Edge Case Matrix (cross-layer)

| Scenario | Layer(s) exercised | Covered by |
|---|---|---|
| Repository throws `DataIntegrityViolationException` on race-condition duplicate insert (TOCTOU between `existsByDocument` check and `save`) | Application + Infra | TC-I07 variant: concurrent create with same document (optional stretch test using two threads) — ensure it still surfaces as ProblemDetail, not 500 |
| Persistence layer down / container not ready | Infra | Not unit-testable meaningfully; covered by Testcontainers startup itself failing the whole suite loudly |
| Extremely long name/document strings (DB column limits, if any) | Infra + Domain | TC-D10 (add) — boundary length test if column has `VARCHAR(n)`; otherwise document as out-of-scope |
| Unicode/special characters in name | Domain + Integration | TC-D11 / TC-I28 — accepted, stored, retrieved intact |
| Trailing/leading whitespace in email | Domain | assert either trimmed-and-validated or rejected consistently |

---

## 7. Security-Focused Test Cases

| ID | Case | Expectation |
|---|---|---|
| TC-S01 | SQL injection payload in `name`/`document`/`email` fields (`' OR '1'='1`, `; DROP TABLE associates;--`) | Stored/rejected safely via JPA parameter binding; no error, no data corruption; table still exists after test |
| TC-S02 | Error response bodies (400/404/409) never contain raw SQL, stack trace, or internal exception class names in `detail` | Inspect `detail` string content for absence of `org.postgresql`, `SQLException`, `at com.cooperative...` |
| TC-S03 | `GET /associates/{id}` with crafted/invalid UUID does not leak whether documents exist via timing or differing error messages | Response time/shape consistent between "malformed id" and "not found" cases (both structured ProblemDetail, distinguishable only by status 400 vs 404, no internal hints) |
| TC-S04 | XSS-style payload in `name` (`<script>alert(1)</script>`) | Stored/returned as-is (no active execution context — API only), confirms no unsafe HTML rendering assumption; document if front-end escaping is out of scope |
| TC-S05 | Mass-assignment: extra unknown JSON fields (`{"name":"x","document":"y","role":"admin"}`) | Unknown fields ignored, no error, no unintended field set (confirms DTO explicitly maps only expected fields) |
| TC-S06 | Oversized payload (very large `name` string, e.g. 1MB) | Rejected gracefully (400) rather than causing OOM/500, if size limits configured; otherwise flagged as a gap in NFRs (not in original AC, note as observation) |
| TC-S07 | Content-Type fuzzing: `POST` with `Content-Type: text/plain` or missing header | 415/400 handled by Spring, not a 500 |
| TC-S08 | Verify no authentication/authorization is enforced (explicitly out of scope per spec) — smoke test that endpoints are reachable without credentials, documented as accepted risk, not a defect | Passes trivially; recorded so it isn't mistaken for a gap later |

---

## 8. Architecture Tests (Could Have — recommended to start now)

File: `architecture/HexagonalArchitectureTest.java` (ArchUnit)

| ID | Rule |
|---|---|
| TC-X01 | Classes in `..domain..` must not depend on `org.springframework..` |
| TC-X02 | Classes in `..domain..` must not depend on `jakarta.persistence..` |
| TC-X03 | Classes in `..domain..` must not depend on Kafka client packages |
| TC-X04 | Classes in `..domain..` must not depend on `jakarta.servlet..`/HTTP client classes |
| TC-X05 | `..infrastructure.persistence..` is the only package allowed to reference `AssociateJpaEntity` |
| TC-X06 | `..application..` may depend on `..domain..` but not on `..infrastructure..` concrete classes (only ports) |

---

## 9. Test Data & Fixtures

- Builder/`TestDataFactory` for `Associate` valid instances (varying name/document/email) shared by unit + integration suites to avoid drift.
- Testcontainers: single reused `PostgreSQLContainer` (static, `@Container`, singleton pattern) across the integration class to keep suite time reasonable; each test cleans state via `@Transactional` rollback or explicit `DELETE FROM associates` in `@AfterEach` (rollback preferred to avoid disturbing Flyway history).
- Document values must be unique per test (use random suffix) since it's a UNIQUE constraint — avoid cross-test pollution/order-dependence.

---

## 10. Traceability to Acceptance Criteria

| AC | Test IDs |
|---|---|
| AC-1.1 / AC-1.2 | TC-I03 (health only testable in-suite; compose-up itself is a manual/CI smoke step outside unit/integration scope) |
| AC-1.3 | TC-I01, TC-I02 |
| AC-2.1 | TC-I04, TC-I05 |
| AC-2.2 | TC-I06 |
| AC-2.3 | TC-I07 |
| AC-2.4 | TC-I08, TC-I05 |
| AC-2.5 | TC-I11, TC-I12 |
| AC-2.6 | TC-I14, TC-I15 |
| AC-2.7 | TC-I16, TC-I17, TC-I19, TC-I21 |
| AC-2.8 | TC-I22, TC-I24 |
| AC-3.1 | TC-I25 |
| AC-3.2 | TC-X01–TC-X04 |
| AC-3.3 | TC-I26 |
| AC-3.4 | TC-D01–TC-D06 |
| AC-3.5 | Entire §5 (Testcontainers-based) |
| AC-3.6 | Manual/CI reproducibility check — not automatable as a unit/integration test; recommend a CI job step that runs README commands verbatim |

---

## 11. Execution & Tooling

- `mvn test` → runs §2, §3, §4, §8 (fast, no containers).
- `mvn verify` → adds §5, §6, §7 integration tests (Testcontainers, Failsafe plugin binding `*IT.java` or `*IntegrationTest.java`).
- CI gate: unit suite must pass before integration suite starts (fail fast).
- Naming convention: `*Test.java` = unit (Surefire), `*IT.java` = integration (Failsafe), keeping Testcontainers out of the fast feedback loop.

---

## 12. Explicit Gaps / Non-Goals (documented, not silently skipped)

- No Kafka functional/integration test (per spec, deferred to S4) — only implicit coverage via `docker compose up -d` container-health smoke check, not part of this automated plan.
- No auth/authz tests (explicitly out of scope for the whole epic).
- Pagination tests only if implemented (Could Have) — add `TC-I29` conditionally if `page`/`size` params are present in the actual controller.
- Oversized-payload limits (TC-S06) flagged as advisory since not in original AC — implement only if a size limit is actually configured; otherwise report as an open risk, not a failing test.