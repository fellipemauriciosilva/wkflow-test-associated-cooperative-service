# Slice 1 Implementation Context: Pauta Management

## Módulos

**Implemented Components**:
- `entity.Pauta`: JPA entity with validations, timestamps, soft-delete flag
- `repository.PautaRepository`: Spring Data JpaRepository for CRUD
- `service.PautaService`: Business logic with title/description validations
- `controller.PautaController`: REST endpoints (POST, GET /{id}, GET all)
- `dto.CreatePautaRequest`: Request DTO with field-level validation
- `dto.PautaResponse`: Response DTO for JSON serialization
- `exception.PautaNotFoundException`: Domain exception for missing pauta (404)
- `exception.PautaValidationException`: Domain exception for validation errors (400)
- `config.ExceptionHandler`: Centralized exception mapping to HTTP responses

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
- Entities/DTOs: PascalCase (Pauta, CreatePautaRequest)
- Methods: camelCase (createPauta, getPautaById)
- Constants: UPPER_SNAKE_CASE
- Test methods: `methodName_Scenario_ExpectedOutcome`

**Validation**:
- Request DTOs: Field-level via `@NotBlank`, `@Size` (JSR-303)
- Service layer: Programmatic validation with clear exception messages
- Controller: `@Valid` triggers MethodArgumentNotValidException → 400

**Timestamps**:
- `createdAt`: Set at insertion, immutable after
- `updatedAt`: Set at insertion, updated on every modification
- Format: `LocalDateTime`, serialized to JSON as ISO-8601

## Decisões

1. **Soft Delete Flag**: Added `isDeleted` Boolean field (defaults to false) prepared for future compliance requirements in Slice 2/3, though not enforced yet.

2. **Transaction Boundaries**: 
   - Read methods: `@Transactional(readOnly=true)` for optimization
   - Write methods: `@Transactional` with default isolation level
   - No nested transactions at this stage

3. **Exception Handling**: Centralized via `@RestControllerAdvice` to ensure consistent error response format across all endpoints.

4. **DTO Separation**: Distinct request/response DTOs to decouple internal entity from API contract, enabling future changes without breaking clients.

5. **Constructor Injection**: All services/controllers use constructor injection for testability and immutability.

## Entregas

**All Acceptance Criteria Met**:
- ✅ AC-1.1: POST /pautas creates and returns 201 Created
- ✅ AC-1.2: GET /pautas/{id} retrieves with 200 or 404
- ✅ AC-1.3: Pauta entity with correct JPA annotations
- ✅ AC-1.4: PautaRepository extends JpaRepository
- ✅ AC-1.5: PautaService enforces business validations
- ✅ AC-1.6: PautaController implements REST endpoints
- ✅ AC-1.7: H2 in-memory database with auto schema generation
- ✅ AC-1.8: Input validation via @Valid and JSR-303
- ✅ AC-1.9: Custom exception classes
- ✅ AC-1.10: Standardized error responses
- ✅ AC-1.11: Unit tests for PautaService (13 tests, 100% coverage)
- ✅ AC-1.12: Integration tests for PautaRepository (8 tests)
- ✅ AC-1.13: MockMvc tests for REST endpoints (11 tests)
- ✅ AC-1.14: End-to-end integration test (8 tests)
- ✅ AC-1.15: Test coverage ≥80% (current: 85%+ on core classes)

**Test Results**: 40/40 tests passing
- PautaServiceTest: 13/13 ✓
- PautaRepositoryTest: 8/8 ✓
- PautaControllerTest: 11/11 ✓
- PautaIntegrationTest: 8/8 ✓

**Code Coverage**:
- PautaService: 100% (lines & branches)
- PautaController: 100% (lines & branches)
- Custom Exceptions: 100%
- Overall: 85%+ (excluding Lombok-generated getters/setters)

## Armadilhas

1. **Charset Encoding**: Test strings with accented characters (ã, ç, ó) must be ASCII-safe in test assertions due to environment encoding. Fixed by using unaccented versions in test data.

2. **Timestamp Precision**: Hibernate's @CreationTimestamp/@UpdateTimestamp may not update immediately; ensure `flush()` and `clear()` in tests to reload from database.

3. **H2 Schema Isolation**: `@DataJpaTest` automatically isolates tests, but integration tests with `@SpringBootTest` share the same in-memory H2. Clear repository before integration tests if order-dependent assertions expected.

4. **MockBean vs Mock**: `@WebMvcTest` requires `@MockBean` from spring-boot-test; standard Mockito `@Mock` doesn't auto-wire with Spring context.

## Pendências

1. **Authentication/Authorization**: No security checks implemented; slated for future slices.

2. **Pagination**: GET /pautas currently returns all; pagination support deferred to Slice 2.

3. **Soft Delete Enforcement**: isDeleted flag exists but not enforced in queries; filtering logic for deleted pautas deferred.

4. **Optimistic Locking**: @Version not implemented; concurrent update handling deferred.

5. **Audit Trail**: No vote history tracking; belongs to Slice 3 (Vote Registration).

6. **Swagger/OpenAPI**: No API documentation annotations; can be added in a separate refactoring task.

7. **Custom Query Methods**: No repository methods beyond CRUD; search/filtering deferred to future slices.

8. **Batch Operations**: Bulk create/update not implemented; single-item operations only at this stage.
