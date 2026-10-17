# Cooperative Assembly Voting System

Backend service for managing cooperative assembly agendas and voting sessions.

## Slice 1: Agenda Management (Pautas)

This slice implements the foundational domain for assembly agendas
("pautas"): entity, repository, service, REST controller, validation,
error handling, and tests. It does **not** include voting session or
vote registration functionality (reserved for later slices).

### Setup

- Java 21
- Maven 3.8+ (use `mvnd` if available for faster rebuilds)

### Build

```bash
mvnd clean verify
# or
mvn clean verify
```

### Run tests

```bash
mvnd test
# run a single test class
mvnd test -Dtest=PautaServiceTest
# run a single test method
mvnd test -Dtest='PautaControllerTest#testGetPautaById_Success'
```

### Run the application

```bash
mvnd spring-boot:run
```

The app starts on `http://localhost:8080` with an in-memory H2 database
(schema auto-created via Hibernate DDL, no SQL scripts).

### API examples

Create a pauta:

```bash
curl -X POST http://localhost:8080/pautas \
  -H "Content-Type: application/json" \
  -d '{"title":"Pauta 1","description":"Descrição","date":"2026-02-15T10:00:00"}'
```

Retrieve a pauta:

```bash
curl -X GET http://localhost:8080/pautas/1
```

### Error response format

```json
{"error": "Validation failed", "details": ["title: must not be blank"]}
```

```json
{"error": "Pauta with id 99999 not found"}
```

### Coverage

JaCoCo report is generated at `target/site/jacoco/index.html` after
running `mvnd test`. A coverage check (≥80% line coverage) runs
automatically for `Pauta`, `PautaService`, and `PautaController`.
