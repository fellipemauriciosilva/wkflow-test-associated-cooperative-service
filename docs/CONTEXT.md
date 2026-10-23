# CONTEXT

## Módulos
- `br.coop.acme.pauta` — aplicação Spring Boot (`PautaServiceApplication`).
- `controller` — `PautaController`: `POST /pautas` (201 com `{"id"}`), `GET /pautas/{id}` (200 ou 404). Records `PautaRequest`, `PautaCriadaResponse`, `PautaResponse` ficam aninhados no controller.
- `service` — `PautaService`: `cadastrar`, `buscarPorId`.
- `repository` — `PautaRepository` (Spring Data JPA).
- `entity` — `Pauta` (id Long, titulo, descricao). Sem timestamp.

## Stack e comandos
- Java 21, Spring Boot 3.3.5, Maven, Spring Web MVC, Spring Data JPA (Hibernate 6.5), Bean Validation, H2 em memória.
- Testes: JUnit 5, MockMvc (`@SpringBootTest` + `@AutoConfigureMockMvc`).
- Build e testes: `mvnd test` (sem `clean`). Para um teste: `mvnd test -Dtest='PautaControllerTest#metodo'`.

## Convenções
- Pacotes por camada: controller, service, repository, entity.
- Nomes de domínio em português (`titulo`, `descricao`, `cadastrar`, `buscarPorId`).
- Entrada validada com `@Valid` no request record; sem validação manual no service.

## Decisões
- Título obrigatório via `@NotBlank` (rejeita nulo, vazio e só espaços) -> 400.
- Descrição opcional, sem validação, persiste null. Testes 201 sem descrição fazem GET e asserem `$.descricao` null.
- Erro 400 no formato `ProblemDetail` (RFC 7807) via `spring.mvc.problemdetails.enabled=true`; campo `status` = 400.
- `spring.jpa.open-in-view=false`.
- Sem `Clock`: entidade não tem timestamp.

## Entregas
- Projeto Maven criado do zero (repo antes só com `.gitignore`).
- Endpoints `POST /pautas` e `GET /pautas/{id}`.
- Suíte `PautaControllerTest` (8 testes): POST 201 com id, POST 201 sem descrição (persistência null verificada via GET), POST 201 com descrição null (persistência null verificada via GET), POST 400 para título null/vazio/espaços, GET 200 com dados, GET 404.
- Review ciclo 1: correção de testes vácuos de descrição nula aplicada.

## Armadilhas
- Teste e código dependem de H2 em memória; não há banco externo nem Docker.
- Build usa proxy interno de dependências; sem internet.
- `@NullSource` com `@ValueSource` no POST 400: título null gera corpo sem o campo `titulo`.

## Pendências
- Confirmar com PO: tamanho máximo de título e descrição (não definido, nada validado hoje).
- Confirmar formato do erro 400 (usando ProblemDetail por padrão).
- Confirmar corpo do 201 `{"id": ...}` (assumido).
- Confirmar prioridade (inferida média) e tipo do ticket (LATENCIA vs feature).
- Requisito de latência não existe no corpo do ticket (Won't).
- Confirmar com PO/arquitetura: autenticação (IDs sequenciais permitem enumerar GET /pautas/{id}).
- Limite de tamanho de título/descrição: coluna varchar(255) estoura em 500 se passar; falta `@Size` (400) e teste.
- Backlog de segurança/operação: perfil prod com datasource explícito (hoje H2 em memória), varredura de dependências (SCA), idempotência do POST, `trim` do título, teste de JSON malformado, isolamento de estado entre testes.
