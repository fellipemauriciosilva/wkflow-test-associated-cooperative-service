# CONTEXT

## Módulos

- `br.coop.pautas.PautasApplication`: entrypoint Spring Boot.
- `br.coop.pautas.pauta`: fatia 1, cadastro e consulta de pautas.
  - `Pauta` (entidade JPA, tabela `pauta`, ID `Long` IDENTITY, `titulo` obrigatório, `descricao` opcional).
  - `PautaRepository` (Spring Data JPA).
  - `PautaRequest` (record de entrada, `titulo` com `@NotBlank`).
  - `PautaResponse` (record de saída: `id`, `titulo`, `descricao`).
  - `PautaController`: `POST /pautas` e `GET /pautas/{id}`.

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Maven. Starters: web, data-jpa, validation, test. H2 runtime.
- Build e testes: `mvnd test` (sem `clean`). Teste único: `mvnd test -Dtest='PautaControllerTest#metodo'`.
- Dependências resolvem pelo proxy interno (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`). Sem `settings.xml` no ambiente; o Maven resolveu sem configuração extra.
- Sem `curl` nem `python3` no ambiente.

## Convenções

- Pacote por feature (`pauta`), não por camada.
- DTO como record; entidade com construtor protegido para JPA.
- Erros via `ProblemDetail` (`spring.mvc.problemdetails.enabled=true`).
- Testes: `@SpringBootTest` + `@AutoConfigureMockMvc`, sem servidor real, H2 in-memory.

## Decisões

- Campos da pauta: `titulo` obrigatório, `descricao` opcional (proposta do ticket, decisão 1 em aberto).
- `POST /pautas` retorna 201 com `Location: /pautas/{id}` (proposta, decisão 2 em aberto).
- ID `Long` IDENTITY (proposta, decisão 3 em aberto).
- Persistência JPA/Hibernate (proposta, decisão 4 em aberto).
- Validação: campo ausente ou vazio retorna 400 em `ProblemDetail` (proposta, decisão 5 em aberto).
- `ddl-auto: create-drop` com H2 `jdbc:h2:mem:pautas;DB_CLOSE_DELAY=-1`.
- GET com ID inexistente lança `ResponseStatusException(NOT_FOUND)`, respondido como `ProblemDetail`.

## Entregas

- Fatia 1 (cadastro e consulta de pautas): AC-1.1 a AC-1.5 cobertos por `PautaControllerTest` (5 testes).

## Armadilhas

- `ResponseStatusException` precisa de `spring.mvc.problemdetails.enabled` para sair como `ProblemDetail`.
- Com H2 e `create-drop`, o contexto de teste recria o schema; usar `DB_CLOSE_DELAY=-1` para não perder dados entre conexões.
- `mvnd test` sem `clean` é o fluxo esperado neste ambiente.

## Pendências

- Decisões 1 a 4 do `task.md` (seção 6) não confirmadas pelo time; implementado com as propostas.
- Fatias 2 (sessão e votos) e 3 (resultado) dependem do contrato de `{id}` (tipo `Long`).
- C1 (Springdoc) fica no epic.
