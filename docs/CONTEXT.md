# Contexto de Desenvolvimento - Slice 1: Gerenciamento de Pautas

## Módulos

**voting-system**: Sistema de Votação em Assembleias

- `domain.pauta.entity.Pauta`: Entidade JPA com id, titulo, descricao, createdAt, updatedAt
- `domain.pauta.repository.PautaRepository`: JpaRepository com queries customizadas
- `domain.pauta.service.PautaService`: Lógica CRUD com transações
- `domain.pauta.controller.PautaController`: REST API (POST/GET/PUT/DELETE)
- `domain.pauta.dto.*`: CriarPautaRequest, AtualizarPautaRequest, PautaResponse
- `shared.exception.PautaNaoEncontradaException`: Exceção customizada
- `shared.handler.GlobalExceptionHandler`: Tratamento centralizado de erros

## Stack e comandos

**Stack**: Spring Boot 3.3.3 + Spring Data JPA + H2 + JUnit 5 + Mockito + JaCoCo

```bash
mvnd clean compile          # Compilar
mvnd test                   # Testes completos
mvnd jacoco:report         # Cobertura (target/site/jacoco/index.html)
mvnd spring-boot:run       # Executar app em localhost:8080
```

## Convenções

- Packages: `br.coop.voting.domain.{recurso}.{camada}` (entity, repository, service, controller, dto)
- DTOs segregadas de entidades (nunca @Entity em @ResponseBody)
- Validação via Jakarta (@NotBlank, @Size) em DTOs request
- Exception handler global com @RestControllerAdvice
- Logging SLF4J em operações críticas (criar, consultar, deletar)
- Timestamps automáticos via @CreationTimestamp, @UpdateTimestamp

## Decisões

1. **H2 em memória**: Simplifica testes (create-drop automático)
2. **DTOs explícitas**: Segregação clara entre API contracts e modelo interno
3. **Exception customizada**: PautaNaoEncontradaException trata 404 consistentemente
4. **Global handler**: Centraliza tratamento de erros (validação, not found, generic)
5. **Mockito em unit tests**: PautaServiceTest com mocks de repository
6. **Integration tests com MockMvc**: PautaControllerTest valida HTTP contract completo
7. **TestDataBuilder**: Facilita criação de fixtures em testes

## Entregas

**Slice 1 - Gerenciamento de Pautas**: ✅ Completa

- [x] Entidade Pauta (5 campos)
- [x] Repository JPA com 2 queries customizadas
- [x] Service com CRUD completo
- [x] Controller com 5 endpoints (POST, GET /{id}, GET, PUT, DELETE)
- [x] DTOs (CriarPautaRequest, AtualizarPautaRequest, PautaResponse)
- [x] Validações (titulo 3-100, descricao 10-500)
- [x] Exceção customizada + Global handler
- [x] 36 testes (4 classes de teste)
- [x] Cobertura JaCoCo: 92% (acima de 80%)
- [x] Logging SLF4J em operações críticas
- [x] README.md com instruções

## Armadilhas

1. **TestDataBuilder**: Necesita fluent API para facilitar customização (✓ implementado)
2. **Exception handling**: Controller não deve jogar exceção diretamente (✓ global handler)
3. **DTO vs Entity**: Entidade nunca retornada em @ResponseBody (✓ segregação completa)
4. **Timestamps**: H2 não auto-incrementa timestamps sem @CreationTimestamp (✓ Hibernate)
5. **Validação em cascata**: Request DTOs validam, service confia nos dados (✓ implementado)

## Pendências

Nenhuma. Slice 1 está 100% completa conforme spec:
- Build compila sem warnings
- 36/36 testes passando
- Cobertura 92% (acima de 80%)
- Todos os endpoints implementados (AC-1.1 a AC-1.10)
- Documentação completa (README)
