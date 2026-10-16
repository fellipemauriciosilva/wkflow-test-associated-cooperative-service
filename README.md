# Voting System - Slice 1: Gerenciamento de Pautas

Sistema de Votação em Assembleias de Cooperativa - Slice 1 implementa o CRUD de pautas de assembleia com validação de dados, persistência em H2 e suite completa de testes.

## 📋 Requisitos

- Java 21+
- Maven 3.8+ (ou mvnd)
- H2 Database (incluído nas dependências)

## 🏗️ Arquitetura

O projeto segue padrão em camadas com packages segregados por domínio:

```
src/main/java/br/coop/voting/
├── domain/pauta/
│   ├── entity/         # Entidade JPA
│   ├── repository/     # Interface JpaRepository
│   ├── service/        # Lógica de negócio
│   ├── controller/     # REST endpoints
│   └── dto/            # Data Transfer Objects
├── shared/
│   ├── exception/      # Exceções customizadas
│   └── handler/        # Global Exception Handler
└── VotingSystemApplication.java
```

## 🚀 Build e Execução

### Compilar sem testes
```bash
mvnd clean compile
```

### Executar todos os testes
```bash
mvnd clean test
```

### Gerar relatório de cobertura JaCoCo
```bash
mvnd clean test jacoco:report
# Abrir: target/site/jacoco/index.html
```

### Rodar aplicação
```bash
mvnd spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080`

## 📡 REST API Endpoints

### Criar Pauta
```bash
curl -X POST http://localhost:8080/pautas \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"Pauta Assembleia 2025","descricao":"Discussão sobre direcionamento estratégico da cooperativa"}' \
  -v
```

**Resposta (201 Created)**:
```json
{
  "id": 1,
  "titulo": "Pauta Assembleia 2025",
  "descricao": "Discussão sobre direcionamento estratégico da cooperativa",
  "createdAt": "2025-01-15T10:00:00",
  "updatedAt": "2025-01-15T10:00:00"
}
```

### Obter Pauta por ID
```bash
curl -X GET http://localhost:8080/pautas/1
```

**Resposta (200 OK)**:
```json
{
  "id": 1,
  "titulo": "Pauta Assembleia 2025",
  "descricao": "Discussão sobre direcionamento estratégico da cooperativa",
  "createdAt": "2025-01-15T10:00:00",
  "updatedAt": "2025-01-15T10:00:00"
}
```

### Listar todas as Pautas
```bash
curl -X GET http://localhost:8080/pautas
```

**Resposta (200 OK)**:
```json
[
  {
    "id": 1,
    "titulo": "Pauta Assembleia 2025",
    "descricao": "Discussão sobre direcionamento estratégico da cooperativa",
    "createdAt": "2025-01-15T10:00:00",
    "updatedAt": "2025-01-15T10:00:00"
  }
]
```

### Buscar Pautas por Título
```bash
curl -X GET 'http://localhost:8080/pautas?titulo=Assembleia'
```

### Atualizar Pauta
```bash
curl -X PUT http://localhost:8080/pautas/1 \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"Pauta Atualizada","descricao":"Nova descrição da pauta"}' \
  -v
```

### Deletar Pauta
```bash
curl -X DELETE http://localhost:8080/pautas/1 -v
```

## ✅ Validações

As pautas devem seguir as regras de validação:

| Campo | Validação | Mensagem |
|-------|-----------|----------|
| titulo | @NotBlank, @Size(3-100) | "Título não pode ser vazio" / "Título deve ter entre 3 e 100 caracteres" |
| descricao | @NotBlank, @Size(10-500) | "Descrição não pode ser vazia" / "Descrição deve ter entre 10 e 500 caracteres" |

### Exemplo de erro (400 Bad Request):
```bash
curl -X POST http://localhost:8080/pautas \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"","descricao":"Descrição curta"}' \
  -v
```

**Resposta (400 Bad Request)**:
```json
{
  "error": "Validação falhou",
  "message": {
    "titulo": "Título não pode ser vazio",
    "descricao": "Descrição deve ter entre 10 e 500 caracteres"
  }
}
```

## 📊 Cobertura de Testes

A suite de testes fornece cobertura de **92%** (325 instruções), superando o mínimo de 80%:

- **Unit Tests**: PautaTest, PautaServiceTest
- **Integration Tests**: PautaRepositoryTest, PautaControllerTest
- **Test Data Builder**: PautaTestDataBuilder para factory de dados

Total: 36 testes passando ✅

## 🔧 Configuração

Arquivo: `src/main/resources/application.properties`

```properties
# H2 Database (em memória)
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop

# Logging
logging.level.br.coop.voting=DEBUG
logging.level.org.springframework.web=DEBUG

# Server
server.port=8080
```

## 📦 Dependências principais

- Spring Boot 3.3.3
- Spring Data JPA
- H2 Database
- Jakarta Validation
- Lombok
- JUnit 5
- Mockito
- JaCoCo (Code Coverage)

## 🎯 Acceptance Criteria

Todos os critérios de aceitação foram implementados:

- ✅ AC-1.1: POST /pautas cria pauta com status 201
- ✅ AC-1.2: GET /pautas/{id} retorna pauta com status 200
- ✅ AC-1.3: Validação falha com status 400
- ✅ AC-1.4: ID inexistente retorna 404
- ✅ AC-1.5: Cobertura de testes ≥80% (92%)
- ✅ AC-1.6: Entidade possui todos os campos obrigatórios
- ✅ AC-1.7: DTOs segregadas da entidade
- ✅ AC-1.8: Global Exception Handler implementado
- ✅ AC-1.9: Logging SLF4J em operações críticas
- ✅ AC-1.10: Tabela criada automaticamente via DDL

## 📚 Estrutura de Testes

```
src/test/java/br/coop/voting/
├── domain/pauta/
│   ├── entity/PautaTest.java               # Testes de entidade
│   ├── repository/PautaRepositoryTest.java  # Testes de persistência
│   ├── service/PautaServiceTest.java        # Testes de lógica
│   └── controller/PautaControllerTest.java  # Testes de API
└── shared/builder/PautaTestDataBuilder.java # Factory de dados teste
```

## 🔗 Próximas Slices

- **Slice 2**: Sessão de Votação (dependente desta slice)
- **Slice 3**: Registro e Consulta de Votos (dependente de Slice 2)

## 📌 Notas

- Banco H2 é criado automaticamente em `create-drop` (testes) e `validate` (produção)
- Todas as pautas são criadas com `createdAt` e `updatedAt` automáticos via Hibernate
- Tratamento global de exceções centralizado em `GlobalExceptionHandler`
- Sem autenticação/autorização (escopo de Slice 1)

---

**Status**: ✅ Slice 1 Completa  
**Cobertura**: 92% (325 instruções)  
**Testes**: 36/36 passando  
**Build**: ✅ Sucesso
