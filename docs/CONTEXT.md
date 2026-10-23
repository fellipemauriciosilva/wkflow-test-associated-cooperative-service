# Contexto do projeto

## Módulos
- `br.coop.cooperative.pauta`: serviço de pautas (controller, entidade JPA, repositório).

## Stack e comandos
- Java 21, Spring Boot 3.3.5, Maven, Spring MVC, Spring Data JPA, Hibernate 6.5, H2 in-memory, JUnit 5, MockMvc.
- Testes: `mvnd test` (sem `clean`). Teste único: `mvnd test -Dtest='PautaControllerTest#metodo'`.

## Convenções
- Testes de controller em `@SpringBootTest` + `@AutoConfigureMockMvc`.
- Records aninhados no controller para request/response.

## Decisões
- Título validado com `@NotBlank` + `@Size(max = 200)`; descrição com `@Size(max = 1000)`. Mesmos limites em `length` na entidade (evita `varchar(255)` estourar em 500). 400 via `MethodArgumentNotValidException`.
- 404 de `GET /pautas/{id}` via `ResponseStatusException`.
- Id `Long` com `GenerationType.IDENTITY`.
- `POST /pautas` retorna 201 com body `{"id": n}`. Header `Location` pendente.
- `spring.jpa.open-in-view=false`; schema por `ddl-auto=create-drop`.

## Entregas
- Fase 1: `POST /pautas` (201, 400 em título inválido), `GET /pautas/{id}` (200/404), testes MockMvc.
- Ciclo de correção do review: limites 200/1000 alinhados (request + entidade); testes de limite (1000 ok, 1001 e título 201 → 400); asserção de `MethodArgumentNotValidException` e de `ResponseStatusException` 404.

## Armadilhas
- Teste de título em branco com tab precisa escapar `\\t` no JSON, senão cai em parse error (400 por motivo errado).
- Asserção só de status 400/404 é vacuosa: sempre checar `getResolvedException()`.

## Pendências
- Header `Location` no 201 (S1).
- Limites de tamanho: título 200, descrição 1000 (C1). Aplicados em código; falta confirmação do PO.
- Sem autenticação em `POST`/`GET /pautas` (review). Decidir se é escopo intencional e registrar.
- Id sequencial `IDENTITY` exposto em `GET /pautas/{id}` (enumeração). Reavaliar UUID ou authz.
- `ddl-auto=create-drop` em `application.properties` sem profile. Mover para perfil de teste.
- Casos negativos não testados: JSON malformado, Content-Type ausente (415), body vazio.
- Espaços em título não normalizados (`strip()` pendente).
- Status 400 vs 409 confirmar com PO. Prioridade e formato de id também em aberto.
- Clock injetável (C2) não implementado.
