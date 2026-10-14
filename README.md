# Wkflow Test Associated Cooperative Service

A Spring Boot microservice for workflow testing and cooperative system integration.

## Features

- **Health Details Endpoint**: GET `/health/details` - Returns application metadata including name and version dynamically read from `pom.xml`
- **OpenAPI/Swagger Documentation**: Interactive API documentation available at `/swagger-ui.html`
- **Spring Boot Actuator**: Health check and info endpoints with detailed metrics

## Prerequisites

- Java 11 or higher
- Maven 3.6.0 or higher

## Building the Project

```bash
mvn clean package
```

## Running the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Running Tests

Execute the test suite to validate the implementation:

```bash
mvn test
```

Generate code coverage report:

```bash
mvn jacoco:report
```

Coverage report will be available at `target/site/jacoco/index.html`

## API Endpoints

### Health Details

**Endpoint:** `GET /health/details`

**Description:** Returns application metadata including name and version.

**Response Status:** 200 OK (successful) or 500 Internal Server Error (if metadata unavailable)

**Response Content-Type:** `application/json`

**Example Request:**
```bash
curl -X GET http://localhost:8080/health/details
```

**Example Response (200 OK):**
```json
{
  "name": "wkflow-test-associated-cooperative-service",
  "version": "1.0.0"
}
```

**Example Response (500 Error):**
```json
{
  "name": "unknown",
  "version": "unknown"
}
```

### Swagger/OpenAPI Documentation

**Endpoint:** `GET /swagger-ui.html`

Navigate to `http://localhost:8080/swagger-ui.html` to view the interactive API documentation.

### Health Check

**Endpoint:** `GET /actuator/health`

Returns the application health status.

## Architecture

### Project Structure

```
src/main/java/com/cooperative/wkflow/
├── WkflowApplication.java              # Spring Boot main application
├── controller/
│   └── HealthDetailsController.java    # REST controller for /health/details
├── config/
│   └── ApplicationProperties.java      # Configuration properties binding
└── dto/
    └── HealthDetailsResponse.java      # Response DTO

src/main/resources/
└── application.yml                     # Application configuration with Maven filtering

src/test/java/com/cooperative/wkflow/
├── controller/
│   └── HealthDetailsControllerTest.java    # Unit tests for controller
└── WkflowApplicationIntegrationTest.java   # Integration tests
```

### Dynamic Property Loading

The application uses Maven resource filtering to inject pom.xml properties into the application configuration at build time:

- `pom.xml` defines `<project.name>` and `<project.version>` properties
- Maven resource plugin processes `application.yml` replacing `@project.name@` and `@project.version@` tokens
- Spring Boot's `@ConfigurationProperties` binds these values to the `ApplicationProperties` bean
- Controller injects `ApplicationProperties` and returns values via the REST endpoint

## Configuration

Edit `application.yml` to customize:

- Application name: `app.name`
- Application version: `app.version`
- Management endpoints exposure: `management.endpoints.web.exposure.include`

## Testing

### Unit Tests

Unit tests are located in `src/test/java/com/cooperative/wkflow/controller/HealthDetailsControllerTest.java`

Test coverage includes:
- HTTP 200 response status verification
- JSON response structure validation
- Field presence and non-null checks
- Error handling for missing/blank metadata
- Response body format validation

### Integration Tests

Integration tests are located in `src/test/java/com/cooperative/wkflow/WkflowApplicationIntegrationTest.java`

Tests verify:
- Application context loads successfully
- Health details endpoint is available
- Response contains project name from pom.xml
- Response contains valid semantic versioning (X.Y.Z format)

## Error Handling

The `/health/details` endpoint handles errors gracefully:

- **Missing metadata**: Returns HTTP 500 with fallback values (`"unknown"`, `"unknown"`)
- **Blank metadata**: Returns HTTP 500 with fallback values
- **Unexpected exceptions**: Returns HTTP 500 with error indicators

## Monitoring & Observability

- Logging: Uses SLF4J with Spring Boot logging framework
  - Debug: Health details retrieval
  - Warn: Incomplete metadata
  - Error: Exceptions during retrieval

## Dependencies

- **Spring Boot 2.7.14**: Web and Actuator starters
- **Springdoc OpenAPI 1.7.0**: Swagger/OpenAPI documentation
- **JUnit 5 (Jupiter)**: Testing framework
- **Mockito**: Mocking framework for tests
- **AssertJ**: Fluent assertions for tests
- **JaCoCo**: Code coverage measurement

## Version History

| Version | Date       | Changes                                           |
|---------|------------|---------------------------------------------------|
| 1.0.0   | 2026-10-14 | Initial implementation with /health/details endpoint |

## License

Proprietary - Cooperative Systems

## Support

For issues or questions, please contact the development team.
