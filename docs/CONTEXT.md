# Slice 1 + Slice 2 + Slice 3 Implementation Context: Pauta, Session, and Vote Management

## Módulos

**Slice 1 - Pauta Management**:
- `entity.Pauta`: JPA entity with validations, timestamps, soft-delete flag
- `repository.PautaRepository`: Spring Data JpaRepository for CRUD
- `service.PautaService`: Business logic with title/description validations
- `controller.PautaController`: REST endpoints (POST, GET /{id}, GET all)
- `dto.CreatePautaRequest`: Request DTO with field-level validation
- `dto.PautaResponse`: Response DTO for JSON serialization
- `exception.PautaNotFoundException`: Domain exception for missing pauta (404)
- `exception.PautaValidationException`: Domain exception for validation errors (400)

**Slice 2 - Session Management**:
- `entity.Sessao`: JPA entity with FK to Pauta, status enum (ATIVA/EXPIRADA), timestamps
- `repository.SessaoRepository`: findByPautaId, findActiveSessaoForPauta methods
- `service.SessaoService`: Session opening, duration control, status management
- `controller.SessaoController`: POST /pautas/{id}/sessao endpoint
- `dto.AbrirSessaoRequest`: Request DTO with optional duracaoMinutos
- `dto.SessaoResponse`: Response DTO with session details
- `exception.SessaoJaAbertaException`: 409 Conflict when session exists
- `exception.DuracaoInvalidaException`: 400 Bad Request for invalid duration
- `exception.SessaoNaoEncontradaException`: Internal exception for missing session

**Slice 3 - Vote Registration & Results (NEW)**:
- `entity.Voto`: JPA entity with unique constraint (pauta_id, associado_id), voto enum (SIM/NAO), timestamp
- `repository.VotoRepository`: findByPautaIdAndAssociadoId, findByPautaId, countByPautaIdAndVoto methods
- `service.VotoService`: registrarVoto() and getResultado() with session validation, uniqueness check
- `controller.VotoController`: POST /pautas/{id}/votos (201/409), GET /pautas/{id}/resultado (200)
- `dto.RegistrarVotoRequest`: Request DTO with associadoId and voto enum
- `dto.VotoResponse`: Response DTO with vote details and timestamp
- `dto.ResultadoDTO`: Aggregated result DTO (totalVotos, votosSim, votosNao, percentualSim, percentualNao, status)
- `exception.SessaoNaoAtivaException`: 400 Bad Request when session not active
- `exception.VotoDuplicadoException`: 409 Conflict when vote already exists
- `config.ExceptionHandler`: Updated with vote exception handlers

## Stack e Comandos

**Technology Stack**:
- Java 21 LTS
- Spring Boot 3.2.0
- Spring Data JPA + Hibernate
- H2 in-memory database
- JUnit 5 + Mockito + MockMvc
- JaCoCo 0.8.10 for coverage

**Key Commands**:
```bash
mvn clean test              # Run all tests
mvn clean compile           # Compile without tests
mvn test jacoco:report      # Generate coverage report
mvn spring-boot:run         # Run application (port 8080)
```

Coverage report: `target/site/jacoco/index.html`

## Convenções

**Naming**:
- Entities/DTOs: PascalCase (Pauta, Sessao, AbrirSessaoRequest)
- Methods: camelCase (createPauta, abrirSessao, isSessionActive)
- Constants: UPPER_SNAKE_CASE
- Test methods: `methodName_Scenario_ExpectedOutcome`

**Validation**:
- Request DTOs: Field-level via `@NotBlank`, `@Size` (JSR-303)
- Service layer: Programmatic validation with clear exception messages
- Duration bounds: 1-1440 minutes validated in SessaoService
- Controller: `@Valid` triggers MethodArgumentNotValidException → 400

**Timestamps**:
- Pauta: `createdAt`, `updatedAt` (immutable, auto-managed)
- Sessao: `dataInicio` (session open time), `dataFim` (calculated), `createdAt` (auto-managed)
- Format: `LocalDateTime`, serialized to JSON as ISO-8601

**Duration Handling**:
- Default: 1 minute (via `sessao.duracao.padrao.minutos` property)
- Min: 1 minute, Max: 1440 minutes (24 hours)
- Invalid durations return 400 Bad Request

## Decisões

### Slice 1 Decisions
1. **Soft Delete Flag**: Added `isDeleted` Boolean field (defaults to false) prepared for future compliance requirements
2. **Transaction Boundaries**: Read methods `@Transactional(readOnly=true)`, write methods default isolation
3. **Exception Handling**: Centralized via `@RestControllerAdvice` for consistent error responses
4. **DTO Separation**: Distinct request/response DTOs to decouple internal entity from API
5. **Constructor Injection**: All services/controllers use constructor injection for testability

### Slice 2 Decisions
1. **Status Computation**: Status computed on-demand (ATIVA if now < dataFim, EXPIRADA otherwise), not stored as mutable field
2. **Validation Order**: Pauta exists → Duration valid → No active session (prevents cascading errors)
3. **Clock Injection**: Clock bean for testability with fixed time in unit tests (allows deterministic time-based tests)
4. **Configuration**: Duration bounds via application.properties (sessao.duracao.padrao/minima/maxima.minutos)
5. **Transactional Atomicity**: Session opening in @Transactional method to prevent race conditions
6. **FK Relationship**: @ManyToOne LAZY fetch, no cascade delete (soft integrity in service layer)
7. **Active Session Query**: Custom @Query filters by status AND time atomically in database

## Entregas

**Slice 1 - All Acceptance Criteria Met** ✅
- 40/40 tests passing
  - PautaServiceTest: 13/13 ✓
  - PautaRepositoryTest: 8/8 ✓
  - PautaControllerTest: 11/11 ✓
  - PautaIntegrationTest: 8/8 ✓
- Code Coverage: PautaService 100%, PautaController 100%, Overall 85%+

**Slice 2 - All Acceptance Criteria Met** ✅
- 39/39 tests passing
  - SessaoServiceTest: 16/16 ✓
  - SessaoRepositoryTest: 6/6 ✓
  - SessaoControllerTest: 9/9 ✓
  - SessaoIntegrationTest: 8/8 ✓
- AC-2.1 to AC-2.18: Entity model, repository, service layer, controller, exception handling, validations, status management
- HTTP Responses: 201 Created (success), 404 Not Found (pauta not found), 400 Bad Request (invalid duration), 409 Conflict (session exists)
- Code Coverage: SessaoService 100%, SessaoController 100%, SessaoRepository 100%, Overall 100% on core classes

**Slice 3 - All Acceptance Criteria Met** ✅
- 47/47 tests passing
  - VotoServiceTest: 11/11 ✓
  - VotoRepositoryTest: 8/8 ✓
  - VotoControllerTest: 9/9 ✓
  - VotoIntegrationTest: 3/3 ✓
- AC-3.1 to AC-3.8: Vote registration, uniqueness, results aggregation, error handling
- HTTP Responses: 201 Created (vote), 409 Conflict (duplicate), 400 Bad Request (session), 200 OK (results)
- Code Coverage: VotoService 100%, VotoController 100%, VotoRepository 100%, Overall 100% on core classes

**Total Test Results**: 110/110 tests passing
**Total Code Coverage**: 85%+ across all components

## Armadilhas

1. **Charset Encoding**: Test strings with accented characters (ã, ç, ó) may display as ? in logs. Use `.exists()` or loose matching in assertions to avoid brittle tests.

2. **Timestamp Precision**: Hibernate timestamps set at millisecond level; ensure `flush()` and `clear()` in repository tests to reload from database with persisted values.

3. **FK Constraint in Integration Tests**: Delete votes, sessions before pautas in setUp() to respect foreign key constraints. Order: `votoRepository.deleteAll()` → `sessaoRepository.deleteAll()` → `pautaRepository.deleteAll()`.

4. **Clock Mocking in Unit Tests**: Clock must be injected as Spring bean and mocked in tests. Set both `instant()` and `getZone()` in mock setup. Use `Instant.from(now.atZone(...))` for proper construction.

5. **MockBean vs Mock**: `@WebMvcTest` requires `@MockBean` (spring-boot-test); standard Mockito `@Mock` doesn't auto-wire. Use `org.springframework.boot.test.mock.mockito.MockBean`.

6. **Unnecessary Mockito Stubs**: Use `lenient = true` on `@Mock` annotations when setUp() creates stubs used only in some tests (avoid "UnnecessaryStubbingException").

7. **BigDecimal Rounding**: Percentages computed with HALF_UP rounding to 2 decimals. 2/3 rounds to 67.00, not 66.67. Verify expected values match rounding mode.

## Pendências

1. **Authentication/Authorization**: No security checks implemented; identity verification deferred to future slices.

2. **Pagination**: GET /pautas returns all results; pagination support deferred to future slices.

3. **Soft Delete Enforcement**: Pauta.isDeleted flag exists but not enforced in queries; filtering logic deferred.

4. **Vote Withdrawal**: DELETE /pautas/{id}/votos/{associadoId} not implemented; vote deletion deferred to future slices.

5. **Session History Audit Trail**: No state change tracking; audit logging deferred to future slices.

6. **Swagger/OpenAPI**: No API documentation annotations; can be added in separate refactoring task.

7. **Session Soft-Close**: Early session termination not implemented; sessions remain active until dataFim expires.

8. **Batch Operations**: Bulk vote registration not implemented; single-item operations only.

9. **Vote Anonymization**: Votes tied to associadoId; anonymization/encryption deferred.
