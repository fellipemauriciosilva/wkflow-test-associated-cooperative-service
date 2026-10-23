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
- Título validado com `@NotBlank` (400 via `MethodArgumentNotValidException`).
- 404 de `GET /pautas/{id}` via `ResponseStatusException`.
- Id `Long` com `GenerationType.IDENTITY`.
- `POST /pautas` retorna 201 com body `{"id": n}`. Header `Location` pendente.
- `spring.jpa.open-in-view=false`; schema por `ddl-auto=create-drop`.

## Entregas
- Fase 1: `POST /pautas` (201, 400 em título inválido), `GET /pautas/{id}` (200/404), testes MockMvc.

## Armadilhas
- Teste de título em branco com tab precisa escapar `\\t` no JSON, senão cai em parse error (400 por motivo errado).

## Pendências
- Header `Location` no 201 (S1).
- Limites de tamanho: título 200, descrição 1000 (C1). Não confirmados.
- Status 400 vs 409 confirmar com PO. Prioridade e formato de id também em aberto.
- Clock injetável (C2) não implementado.
