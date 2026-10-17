# CONTEXT.md

## Módulos

- `com.cooperative.voting` — entry point (`VotingApplication`).
- `com.cooperative.voting.entity` — `Pauta` JPA entity (id, title,
  description, date, createdAt, updatedAt). `date` is write-once
  (no setter path exercised by API after creation); timestamps are
  managed via `@PrePersist`/`@PreUpdate`.
- `com.cooperative.voting.repository` — `PautaRepository extends
  CrudRepository<Pauta, Long>`, no custom queries.
- `com.cooperative.voting.service` — `PautaService`: `createPauta(...)`
  and `findById(...)` (throws `PautaNotFoundException` if missing).
- `com.cooperative.voting.controller` — `PautaController`: `POST
  /pautas`, `GET /pautas/{id}`.
- `com.cooperative.voting.dto` — `PautaRequest` (validated input),
  `ErrorResponse` (`{error, details}`).
- `com.cooperative.voting.exception` — `PautaNotFoundException`.
- `com.cooperative.voting.config` — `GlobalExceptionHandler`
  (`@RestControllerAdvice`) maps `MethodArgumentNotValidException` →
  400 and `PautaNotFoundException` → 404.

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Maven (parent `spring-boot-starter-parent`).
- H2 in-memory (`jdbc:h2:mem:votingdb`), `ddl-auto=create-drop`, schema
  from JPA annotations only (no `schema.sql`/`data.sql`).
- Lombok for entity/DTO boilerplate and `@Slf4j` loggers.
- Build/test: `mvnd clean verify` (use `mvnd`, not `mvn`/`./mvnw`, per
  platform convention — daemon keeps build warm).
- Single test class: `mvnd test -Dtest=PautaServiceTest`.
- Single method: `mvnd test -Dtest='Classe#metodo'`.
- JaCoCo coverage report: `target/site/jacoco/index.html`; a
  `jacoco:check` execution bound to `test` phase enforces ≥80% line
  coverage on `Pauta`, `PautaService`, `PautaController` (fails the
  build if not met). `dto/**` and `VotingApplication` are excluded
  from coverage (trivial/boilerplate).

## Convenções

- Package layout: `entity`, `repository`, `service`, `controller`,
  `dto`, `exception`, `config` under `com.cooperative.voting`.
- Controller → Service → Repository, unidirectional.
- Error body shape: `{"error": "...", "details": [...]}` — `details`
  is a list of `"field: message"` strings for validation errors, empty
  for simple errors (e.g. not-found).
- Jakarta Bean Validation (`@NotBlank`, `@NotNull`) on the request DTO,
  triggered via `@Valid` in the controller.

## Decisões

- Entity serialized directly in responses (no separate
  `PautaResponse` DTO) — acceptable per spec ("COULD have" DTO
  pattern), kept this slice minimal.
- `PautaRepositoryTest` (`@DataJpaTest`) added beyond the spec's
  minimum test list specifically to exercise the `@PreUpdate`
  lifecycle callback on `Pauta`, which is otherwise unreachable from
  service/controller tests and was needed to clear the 80% line
  coverage bar on the entity class.
- JaCoCo exclusions (`VotingApplication`, `dto/**`) are for trivial
  bootstrap/DTO code with no branching logic; everything exercising
  business logic is included and measured.

## Entregas

- Slice 1 completo: entity, repository, service, controller, DTOs,
  exception handling, `application.properties`, unit tests
  (`PautaServiceTest`, 3 cases), integration tests
  (`PautaControllerTest`, 5 cases via `@SpringBootTest` +
  `MockMvc`), plus `PautaRepositoryTest` (1 case) — 9 tests total,
  all green.
- `mvnd clean verify` succeeds (BUILD SUCCESS, jar repackaged at
  `target/wkflow-test-associated-cooperative-service-0.1.0.jar`).
- JaCoCo `check` goal passes (≥80% line coverage on the three
  required classes).

## Armadilhas

- `mvnd`/Maven native library (jansi) logs a harmless
  `UnsatisfiedLinkError` on `/tmp/jansi-*.so` (noexec partition) —
  cosmetic, does not affect build/test results.
- Surefire logs `[ERROR] Surefire is going to kill self fork JVM...`
  after tests finish — this is Surefire's fork-JVM shutdown watchdog
  reacting to Spring Boot's context shutdown hook calling
  `System.exit`-adjacent cleanup; it appears even on fully green runs
  and is not a test failure. Confirm pass/fail via the `Tests run:
  X, Failures: 0, Errors: 0` and `BUILD SUCCESS` lines, not via
  presence/absence of `[ERROR]` strings alone.
- `CrudRepository` has no `flush()`; use `TestEntityManager.flush()`
  in `@DataJpaTest` tests when you need to force the `@PreUpdate`
  callback synchronously.
- `@WebMvcTest` was intentionally not used for `PautaControllerTest`
  despite being mentioned in the spec's "how to verify" sections;
  `@SpringBootTest` + `@AutoConfigureMockMvc` was used instead so the
  test exercises the real H2-backed repository end-to-end (the spec's
  own AC-1.6 explicitly requires "real H2 database, not mocked",
  which plain `@WebMvcTest` would not provide without extra wiring).

## Pendências

- Slice 2 (Voting Sessions) and Slice 3 (Vote Registration) not
  started — `PautaService`/`PautaRepository` are stable and ready for
  injection into future session/vote services per the spec's
  "Slice Dependencies" section.
- No Swagger/OpenAPI, no custom `@ValidDate`, no Actuator — all
  "COULD have" items, intentionally skipped for this slice.
