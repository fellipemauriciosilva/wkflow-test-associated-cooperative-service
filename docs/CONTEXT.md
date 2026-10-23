# CONTEXT

## Módulos

- `pautas` (pacote único, estrutura por feature):
  - `PautasApplication`: bootstrap Spring Boot.
  - `Pauta`: entidade JPA, tabela `pauta`, id `Long` via sequência `pauta_seq`.
  - `PautaRepository`: `JpaRepository<Pauta, Long>`. Reutilizável na fatia 3.
  - `PautaService`: `cadastrar(CriarPautaRequest) -> Long`, `buscar(Long) -> PautaResponse`.
  - `PautaController`: `POST /pautas` (201 + `Location` + `{"id"}`), `GET /pautas/{id}` (200).
  - `CriarPautaRequest` (record, Bean Validation), `PautaResponse`, `PautaCriadaResponse`, `ErroResponse` (com `CampoErro`).
  - `PautaNaoEncontradaException`: lançada pelo service, mapeada para 404.
  - `Sessao`: entidade JPA, tabela `sessao`, sequência `sessao_seq`, FK `pauta_id` com constraint única nomeada `uk_sessao_pauta` (`@UniqueConstraint`), `abertura` e `fim` como `Instant`.
  - `SessaoRepository`: `JpaRepository<Sessao, Long>`, `existsByPautaId`.
  - `SessaoService`: `abrir(Long pautaId, Integer duracaoMinutos) -> SessaoResponse`. Usa `Clock`. Padrão 1 min quando `null`. Só converte `DataIntegrityViolationException` em 409 se a causa for `ConstraintViolationException` cujo nome contém `uk_sessao_pauta`; outras violações são relançadas.
  - `JacksonConfig`: desliga coerção de `Float` e `String` para inteiros (`duracaoMinutos` aceita só inteiro JSON).
  - `SessaoController`: `POST /pautas/{id}/sessao` (201 + `Location` + body `SessaoResponse`). Body opcional (`required = false`).
  - `AbrirSessaoRequest` (record, `@Min(1)` em `duracaoMinutos`), `SessaoResponse` (`id, pautaId, abertura, fim`).
  - `SessaoJaExisteException`: mapeada para 409.
  - `ClockConfig`: bean `Clock.systemUTC()`. Testes substituem por `ClockDeTeste` (`@TestConfiguration` com `@Primary`), mutável e resetado em `@AfterEach`.
  - `GlobalExceptionHandler` (`@RestControllerAdvice`): 400 validação (com `fieldErrors`), 400 corpo ilegível (com `fieldErrors` quando `MismatchedInputException` aponta campo com tipo inválido; vazio em JSON malformado e em corpo que não é objeto), 400 parâmetro com tipo errado, 404 pauta, 405 método não permitido, 409 sessão já existente, 415 mídia não suportada.

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Maven. Dependências: web, data-jpa, validation, H2 (runtime), starter-test.
- H2 em memória: `jdbc:h2:mem:pautas`, `ddl-auto: create-drop`, `open-in-view: false`.
- Testes: `mvnd test` (suite inteira, sem `clean`). Classe única: `mvnd test -Dtest=PautaControllerTest`.
- Sem Docker e sem internet: dependências resolvem pelo mirror Maven (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`, configurado em `/usr/share/maven/conf/settings.xml`).
- Versões fixadas conforme cache local `/orqaliz/cache/m2`: Boot 3.3.5, H2 2.2.224, JUnit 5.10.x.

## Convenções

- Pacote base `pautas`. Mensagens de erro de validação em português, no `@NotBlank`/`@Size`/`@Min` do request.
- Corpo de erro padronizado: `timestamp, status, error, message, path, fieldErrors`. Vale para erros tratados pelo `GlobalExceptionHandler`. Rota inexistente (404 de `/error`) fica fora: o Spring só lança exceção para rota sem handler com `spring.mvc.throw-exception-if-no-handler-found=true` e `spring.web.resources.add-mappings=false`, que não estão configurados. Não habilitado nesta fatia para não afetar outros recursos estáticos.
- Campo `id` do POST é ignorado (não existe no record de request).
- Campo desconhecido no JSON é ignorado (padrão Jackson).
- Testes: `@SpringBootTest` + `@AutoConfigureMockMvc`. Fatia 1 em `PautaControllerTest` (AC-1.1 a AC-1.5). Fatia 2 em `SessaoControllerTest` (AC-2.1 a AC-2.5).
- Erro de sessão segue o `ErroResponse` do projeto, não `ProblemDetail`, para manter um único formato de erro.

## Decisões

- `descricao` opcional, máximo 2000 caracteres. `titulo` obrigatório, sem vazio após trim, máximo 200.
- Pauta duplicada (mesmo título) permitida. Sem regra de unicidade.
- Persistência em H2 em memória, premissa até decisão antes de produção.
- Contrato de API é premissa até confirmação com PO (ver `task.md` seção 3).
- Sessão (fatia 2): nome do campo `duracaoMinutos`; 201 na criação com `Location: /pautas/{id}/sessao`; 409 para pauta que já tem sessão, expirada ou não (reabertura não permitida, testada com `Clock` avançado); só inteiro JSON >= 1 (`1.5`, `"5"`, `1e2` viram 400).
- Sem limite máximo de duração, decisão explícita (review pediu registro). Risco: uma sessão de duração enorme (ex.: `2147483647`) trava a pauta para sempre, porque toda sessão existente gera 409 e não há fechamento nesta fatia. Sem autenticação nem rate limit no endpoint. Revisar antes de produção com PO.
- Corrida entre duas aberturas: checagem `existsByPautaId` mais constraint única `uk_sessao_pauta`. Constraint é a autoridade; a checagem é otimização.
- Sessão é única por pauta na vida toda (não só enquanto ativa). Fatia 3 herda essa regra.

## Entregas

- Fatia 1/3 (cadastro e consulta de pautas): endpoints `POST /pautas` e `GET /pautas/{id}`, handler global, entidade e repositório, 25 testes MockMvc (limites 200/2000 aceitos, duplicata permitida, erros de forma/método/mídia com corpo padronizado, regressão de corpo mal tipado, JSON malformado e array).
- Fatia 2/3 (abertura de sessão de votação): `POST /pautas/{id}/sessao` com duração padrão 1 min ou `duracaoMinutos`, 404/409/400 mapeados, `Clock` injetável, 19 testes MockMvc em `SessaoControllerTest` (AC-2.1 a AC-2.5 mais reabertura negada, corrida via constraint, violação não relacionada não vira 409, coerção estrita e mensagens de erro).

## Armadilhas

- `@ValueSource` não aceita expressões concatenadas (`"a".repeat(n)`). Usar `@MethodSource` para casos gerados.
- Saída do `mvnd` vem truncada pelos avisos do jline. Redirecionar para arquivo (`> log 2>&1`) e filtrar `BUILD|Tests run|ERROR`.
- `curl` não existe na imagem.
- `open-in-view: false`: DTOs são montados dentro do service (`@Transactional(readOnly = true)`), não no controller.
- Body opcional no POST de sessão exige `@RequestBody(required = false)`. Sem isso, POST sem body vira 400.
- `@Min` em `Integer` aceita `null`. O padrão 1 min depende do service tratar `null`, não da validação.
- Nome da constraint que o H2 reporta vem com prefixo de schema e sufixo (`PUBLIC.UK_SESSAO_PAUTA_INDEX_9`). Comparar com `contains`, não igualdade.
- Jackson lança `MismatchedInputException` (não `InvalidFormatException`) para tipo errado em campo de objeto.
- Jackson coage número para `String` por padrão (`{"titulo": 123}` vira 201). Não tratado nesta fatia, afeta a fatia 1.
- Teste com `@SpyBean` em `SessaoRepository`: `doReturn`/`doThrow` resetam após cada teste. Erro de constraint real no H2 sai como `jakarta.servlet.ServletException` com causa `DataIntegrityViolationException`.

## Pendências

- Confirmar contrato com PO (campos, tamanhos, formato de erro, nome de `duracaoMinutos`, 201 vs 200, reabertura).
- Decidir persistência durável antes de produção (H2 em memória perde dados no restart).
- Corrida real (duas threads) não testada. A constraint é exercitada via `existsByPautaId` forçado a `false`.
- Limite máximo de duração e autorização do endpoint: decisão de PO pendente (ver Decisões, risco de pauta travada).
- Pauta com sessão expirada não pode ser reaberta nem a sessão encerrada manualmente: tudo depende da decisão de PO.
- Fatia 3 (votos) reutiliza `Pauta`, `PautaRepository` e `Sessao` (checar sessão aberta via `fim` e `Clock`).
- Atualizar/remover pauta, listagem e fechamento manual de sessão fora desta fatia.
