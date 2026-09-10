# Cooperative Voting System - Domain & Data Layer Foundation

## Overview

This project implements the **H49-08: Domain & Data Layer Foundation** slice for the Cooperative Voting System. It establishes a clean, framework-agnostic domain layer with JPA mappings, PostgreSQL persistence via Flyway migrations, and repository interfaces.

### What's Included

- **Domain Layer:** Framework-free entities (Associate, Assembly, Agenda, Vote) with clean separation of concerns
- **Enums & Exceptions:** AgendaStatus, VoteOption, and domain-specific exceptions
- **JPA Mappings:** Complete entity relationships with constraints and cascade behavior
- **Flyway Migrations:** Schema versioning (V001–V005) for reproducible database initialization
- **Repository Interfaces:** Spring Data JPA repositories for data access
- **Integration Tests:** Testcontainers-based tests validating schema constraints and data integrity
- **Unit Tests:** Domain entity tests without Spring context

## Architecture

### Package Structure

```
com.example.cooperativevoting/
├── domain/
│   ├── entities/
│   │   ├── Associate.java
│   │   ├── Assembly.java
│   │   ├── Agenda.java
│   │   └── Vote.java
│   ├── enums/
│   │   ├── AgendaStatus.java
│   │   └── VoteOption.java
│   └── exceptions/
│       ├── AgendaNotOpenException.java
│       ├── DuplicateAssociateException.java
│       └── DuplicateVoteException.java
├── infrastructure/
│   └── persistence/
│       ├── AssociateRepository.java
│       ├── AssemblyRepository.java
│       ├── AgendaRepository.java
│       └── VoteRepository.java
└── CooperativeVotingSystemApplication.java
```

### Domain Model

```
Associate (1) ──→ (many) Vote
Assembly (1) ──→ (many) Agenda
Agenda (1) ──→ (many) Vote
```

## Technology Stack

- **Java:** 21
- **Spring Boot:** 3.4.3
- **Spring Data JPA:** Latest
- **PostgreSQL:** 16
- **Flyway:** 10.x
- **Testcontainers:** 1.20.1
- **Maven:** 3.9+

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 21
- Maven 3.9+

### 1. Start PostgreSQL

```bash
docker-compose up -d postgres
```

Verify the database is ready:

```bash
docker-compose ps
```

### 2. Build the Project

```bash
mvn clean compile
```

### 3. Run Tests

**Unit Tests:**
```bash
mvn test -Dtest='*Test'
```

**Integration Tests (requires running PostgreSQL):**
```bash
mvn test -Dtest='*IT' -DargLine='-Dspring.profiles.active=test'
```

**All Tests:**
```bash
mvn test
```

### 4. Generate Coverage Report

```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080/api`.

## Database Schema

### Associates Table
```sql
CREATE TABLE associates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    document VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Assemblies Table
```sql
CREATE TABLE assemblies (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    scheduled_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Agendas Table
```sql
CREATE TABLE agendas (
    id UUID PRIMARY KEY,
    assembly_id UUID NOT NULL REFERENCES assemblies(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    duration_in_minutes INTEGER NOT NULL DEFAULT 1,
    opened_at TIMESTAMP,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Votes Table
```sql
CREATE TABLE votes (
    id UUID PRIMARY KEY,
    agenda_id UUID NOT NULL REFERENCES agendas(id) ON DELETE CASCADE,
    associate_id UUID NOT NULL REFERENCES associates(id) ON DELETE RESTRICT,
    option VARCHAR(20) NOT NULL,
    event_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(agenda_id, associate_id)
);
```

### Shedlock Table
```sql
CREATE TABLE shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_at TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    lock_until TIMESTAMP NOT NULL
);
```

## Key Features

### Domain Layer Independence

- Zero Spring imports in `domain/` package
- All entities are POJOs instantiable without Spring context
- Exceptions extend `java.lang.Exception`, not Spring exceptions

### Entity Relationships

- **Associates:** Participate in votes for agendas
- **Assemblies:** Contain multiple agendas
- **Agendas:** Belong to an assembly; contain multiple votes
- **Votes:** Link associates to agendas with a ballot option
- Bidirectional navigation where applicable
- Lazy loading by default; cascade delete on aggregate root deletion

### Data Integrity

- **Unique Constraints:**
  - `associates.document` – prevents duplicate voter registration
  - `votes(agenda_id, associate_id)` – prevents duplicate votes by same associate on same agenda
  - `votes.event_id` – supports idempotent Kafka consumption

- **Foreign Key Constraints:**
  - Agenda → Assembly (ON DELETE CASCADE)
  - Vote → Agenda (ON DELETE CASCADE)
  - Vote → Associate (ON DELETE RESTRICT – voting record preserved)

- **Not-Null Constraints:** All required fields enforced at database level

### Repository Queries

- `AssociateRepository.findByDocument()` – lookup associate by document
- `AgendaRepository.findByAssemblyId()` – find agendas in an assembly
- `AgendaRepository.findByStatus()` – filter agendas by status
- `VoteRepository.findByAgendaIdAndAssociateId()` – detect duplicate votes
- `VoteRepository.findByEventId()` – idempotent vote consumption

## Configuration

### Environment Variables

```bash
DB_HOST=localhost          # PostgreSQL host
DB_PORT=5432              # PostgreSQL port
DB_NAME=cooperative_voting # Database name
DB_USER=postgres          # Database user
DB_PASSWORD=postgres      # Database password
```

### application.yml

Located in `src/main/resources/application.yml`. Key settings:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway manages schema; Hibernate validates only
  flyway:
    enabled: true
    locations: classpath:db/migration
```

## Testing

### Unit Tests

Domain entities tested in isolation without Spring:

```bash
mvn test -Dtest='*Test'
```

- `AssociateTest` – entity creation, equality, updates
- `AssemblyTest` – entity creation, agenda relationships
- `AgendaTest` – entity creation, vote relationships, status transitions
- `VoteTest` – entity creation, vote options

### Integration Tests

Testcontainers-based tests with real PostgreSQL:

```bash
mvn test -Dtest='*IT'
```

- `AssociateRepositoryIT` – CRUD ops, document uniqueness
- `VoteRepositoryIT` – composite key uniqueness, event_id idempotency
- `AgendaRepositoryIT` – foreign key constraints, cascade delete
- `FlywayMigrationIT` – schema validation, index presence

### Coverage

JaCoCo generates coverage reports:

```bash
mvn clean test jacoco:report
```

Open `target/site/jacoco/index.html` to view detailed coverage.

## Flyway Migrations

Migrations are versioned and stored in `src/main/resources/db/migration/`:

| Migration | Description |
|-----------|-------------|
| V001__create_associates_table.sql | Creates associates table with unique document |
| V002__create_assemblies_table.sql | Creates assemblies table |
| V003__create_agendas_table.sql | Creates agendas table with FK to assemblies |
| V004__create_votes_table.sql | Creates votes table with composite unique key |
| V005__create_shedlock_table.sql | Creates shedlock table for distributed locking |

Migrations run automatically on application startup (Flyway baseline on first run).

## Troubleshooting

### PostgreSQL Connection Issues

Ensure PostgreSQL is running and accessible:

```bash
docker-compose ps
docker-compose logs postgres
```

Reset PostgreSQL:

```bash
docker-compose down -v
docker-compose up -d postgres
```

### Test Failures

Clear build artifacts and re-run tests:

```bash
mvn clean test
```

Check test configuration in `application-test.yml` (Testcontainers datasource).

### Migration Errors

Validate SQL syntax in migration files:

```bash
cat src/main/resources/db/migration/V*.sql
```

If migrations are stuck, reset the database and re-run:

```bash
docker-compose down -v
docker-compose up -d postgres
mvn clean test
```

## Acceptance Criteria

### Domain Layer Independence (AC-A)

- ✅ Domain package contains zero Spring imports
- ✅ Domain entities are pure POJOs instantiable without Spring
- ✅ Domain exceptions are framework-agnostic

### JPA Mappings (AC-B)

- ✅ All entities properly mapped with @Entity and @Table
- ✅ Relationships use @ManyToOne, @OneToMany with correct mappings
- ✅ Enums mapped via @Enumerated(STRING)
- ✅ Timestamps use java.time.Instant with @Temporal

### Flyway Migrations (AC-C)

- ✅ V001–V005 migrations created and versioned
- ✅ Schema created successfully; Flyway tracking table updated
- ✅ Indexes created on all FK columns

### Repositories (AC-D)

- ✅ All repositories extend JpaRepository
- ✅ Custom query methods implemented via Spring Data
- ✅ No manual SQL needed; derived queries used

### Constraints (AC-E)

- ✅ Document uniqueness enforced at database level
- ✅ Composite vote uniqueness enforced
- ✅ FK constraints prevent orphaned records
- ✅ Cascade delete behavior tested

### Package Structure (AC-F)

- ✅ Hexagonal package layout (domain, application, infrastructure)
- ✅ No circular dependencies
- ✅ Domain self-contained and reusable

### Testing (AC-J)

- ✅ Unit tests run without Spring context
- ✅ Integration tests with Testcontainers
- ✅ >80% code coverage for domain and repository layers
- ✅ Constraint violations properly tested

## Next Steps

This slice provides the foundation for:

1. **REST API Layer** – Controllers accepting vote operations
2. **Kafka Integration** – Events produced/consumed for vote changes
3. **Business Logic** – Service layer enforcing voting rules
4. **Authentication** – Associates identified and authorized

## License

Proprietary – Cooperative Voting System
