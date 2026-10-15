# Assembly Voting System - Pauta Management (Slice 1)

Cooperative voting system with complete CRUD functionality for managing voting agendas (Pautas). This is the foundational module establishing the data model, repository layer, business service, and REST API.

## Quick Start

### Prerequisites
- Java 21 LTS
- Maven 3.8+
- H2 Database (in-memory, included)

### Build & Test
```bash
mvn clean test
```

### Run Application
```bash
mvn spring-boot:run
```

Application starts on `http://localhost:8080/api`

## Architecture

### Package Structure
```
src/main/java/br/coop/assembly/
├── entity/          # JPA entities
├── repository/      # Spring Data repositories
├── service/         # Business logic layer
├── controller/      # REST endpoints
├── dto/             # Request/Response DTOs
├── exception/       # Domain exceptions
└── config/          # Global configuration & exception handling
```

### Technology Stack
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 21
- **ORM**: Hibernate (Spring Data JPA)
- **Database**: H2 (in-memory)
- **Testing**: JUnit 5, Mockito, MockMvc
- **Coverage**: JaCoCo 0.8.10

## API Endpoints

### Create Pauta
```http
POST /api/pautas
Content-Type: application/json

{
  "title": "Pauta Assembléia Q1 2024",
  "description": "Votação de novos projetos"
}

Response: 201 Created
{
  "id": 1,
  "title": "Pauta Assembléia Q1 2024",
  "description": "Votação de novos projetos",
  "createdAt": "2024-01-15T10:30:45",
  "updatedAt": "2024-01-15T10:30:45"
}
```

### Get Pauta by ID
```http
GET /api/pautas/1

Response: 200 OK
{
  "id": 1,
  "title": "Pauta Assembléia Q1 2024",
  "description": "Votação de novos projetos",
  "createdAt": "2024-01-15T10:30:45",
  "updatedAt": "2024-01-15T10:30:45"
}
```

### Get All Pautas
```http
GET /api/pautas

Response: 200 OK
[
  {
    "id": 1,
    "title": "Pauta 1",
    "description": "Description 1",
    "createdAt": "2024-01-15T10:30:45",
    "updatedAt": "2024-01-15T10:30:45"
  },
  ...
]
```

## Validation Rules

### Pauta Title
- **Required**: Must be provided
- **Min Length**: 3 characters
- **Max Length**: 255 characters
- **Blank Check**: Cannot be empty or whitespace-only

### Pauta Description
- **Optional**: May be null
- **Max Length**: 1000 characters

### Error Response Format
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "timestamp": "2024-01-15T10:30:45",
  "path": "/api/pautas",
  "errors": [
    {
      "field": "title",
      "message": "must not be blank",
      "rejectedValue": ""
    }
  ]
}
```

## Testing

### Test Coverage
Run the following command to generate the coverage report:
```bash
mvn clean test jacoco:report
```

Coverage report location: `target/site/jacoco/index.html`

**Current Coverage Metrics**:
- PautaService: 100% (lines & branches)
- PautaController: 100% (lines & branches)
- PautaRepository: 100% (via @DataJpaTest)
- Custom Exceptions: 100%
- Entity: ~60% (Lombok-generated code excluded)

### Test Classes
- **PautaServiceTest**: Unit tests for business logic with mocked repository
- **PautaRepositoryTest**: Integration tests with real H2 database
- **PautaControllerTest**: MockMvc tests for REST endpoints
- **PautaIntegrationTest**: End-to-end tests with full Spring context

## Configuration

### Application Properties
File: `src/main/resources/application.properties`

```properties
# Server
server.port=8080
server.servlet.context-path=/api

# H2 Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop

# Logging
logging.level.br.coop.assembly=DEBUG
```

## Database Schema

### Pauta Table
```sql
CREATE TABLE pauta (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  description VARCHAR(1000),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  is_deleted BOOLEAN DEFAULT FALSE
)
```

## Development Notes

### Timestamp Handling
- `createdAt`: Auto-populated on entity creation via `@CreationTimestamp`
- `updatedAt`: Auto-updated on every modification via `@UpdateTimestamp`
- Both stored as `LocalDateTime` in UTC timezone
- Serialized to JSON in ISO-8601 format

### Soft Delete
- `isDeleted` field prepared for future compliance requirements
- Currently defaults to `FALSE`
- Can be extended in future slices for logical deletion

### Error Handling
- Centralized via `@RestControllerAdvice` in `ExceptionHandler.java`
- Maps domain exceptions to HTTP status codes:
  - `PautaNotFoundException` → 404 Not Found
  - `PautaValidationException` → 400 Bad Request
  - `MethodArgumentNotValidException` → 400 Bad Request (validation errors)
  - General exceptions → 500 Internal Server Error

## Future Enhancements (Slices 2 & 3)

- **Slice 2**: Voting Session Management
  - Session creation, opening, closing
  - Pauta-Session association
  - Session state management

- **Slice 3**: Vote Registration & Results
  - Vote casting
  - Vote counting & results calculation
  - Vote audit trail

## Conventions

### Code Style
- Google Java Style Guide
- IDE auto-format (Maven enforcer plugin ready)
- Naming: PascalCase for classes, camelCase for methods/variables

### Testing
- Test method naming: `methodName_Scenario_ExpectedOutcome`
- Uses DisplayName annotations for readable test descriptions
- Test classes co-located with implementation packages

### Logging
- SLF4J via Logback
- DEBUG level for detailed flow tracking
- INFO level for important business events
- WARN/ERROR for issues

## Troubleshooting

### Tests Timeout
If tests hang during execution, ensure:
1. No external service dependencies (all mocked)
2. H2 in-memory database properly configured
3. JaCoCo agent properly configured in surefire plugin

### Database Connection Issues
- Check `application.properties` datasource URL
- Verify H2 dependency in pom.xml
- Ensure database isolation between tests via `@DataJpaTest`

### Compilation Errors
- Verify Java 21 is set as compiler target
- Run `mvn clean compile` to diagnose issues
- Check Lombok annotation processing is enabled

## References

- [Spring Boot 3 Documentation](https://spring.io/projects/spring-boot)
- [JPA/Hibernate Documentation](https://hibernate.org/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
