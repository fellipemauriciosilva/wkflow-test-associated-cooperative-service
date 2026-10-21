# Contexto do projeto — votacao-api

Épico: H90-15-EPICO / run_bfe1226524a9. Slice 3/3 concluído sobre slices 1–2 mergeados. Épico
completo nesta branch. 2 ciclos de review (ver Entregas/Decisões); o 2º corrigiu falha de
segurança (contagem parcial exposta em ABERTA).

## Módulos

- `com.votacao` — `Application` (bean `Clock` system default, único em todo o app).
- `com.votacao.domain.{pauta,sessao,voto}` — entidades (`Pauta`; `Sessao` com `UK_PAUTA_ID`;
  `Voto` com `UK_PAUTA_ASSOCIADO` em `(pauta_id, id_associado)`), repositórios
  (`VotoRepository.existsByPautaIdAndIdAssociado`, `.contarPorTipo` via `@Query` GROUP BY,
  projeção `VotoRepository.ContagemPorTipo`), enum `TipoVoto` (SIM/NAO), enum `StatusSessao`
  (NAO_INICIADA/ABERTA/FECHADA/EXPIRADA — `FECHADA` reservado, valor estável, ver Decisões),
  exceções (`PautaNotFoundException`, `SessaoJaAbertaException`, `VotoDuplicadoException`,
  `SessaoIndisponivelParaVotoException`).
- `com.votacao.application.{pauta,sessao,voto}` — `PautaService`; `SessaoService`/`VotoService`/
  `ResultadoService` (todos `@Transactional`, `Clock` injetado; `apurar` é readOnly).
  `VotoService.registrar`: pauta → sessão aberta → duplicata → salva, backstop de constraint
  única. `ResultadoService.apurar`: pauta (404 se ausente) → sessão opcional → lê `Clock` **uma
  única vez** (`agora`), deriva `StatusSessao`/`timestamp` do mesmo instante (ver Entregas) →
  agrega contagens **só quando `status_sessao` é `EXPIRADA`/`FECHADA`**; `ABERTA`/`NAO_INICIADA`
  retornam zeros sem consultar votos (ver Decisões).
- `com.votacao.api.{pauta,sessao,voto}` — controllers + DTOs (`PautaRequest` com
  `@Size(max=255)`; `VotoRequest` com `@JsonProperty`/`@Positive`; `ResultadoResponse` record
  snake_case, `timestamp` como `OffsetDateTime`). `ResultadoController` em `com.votacao.api.pauta`
  (sub-recurso de pauta, não pacote próprio), `GET /pautas/{id}/resultado`.
- `com.votacao.infrastructure.error` — `ApiErrorHandler`, `ErrorResponse` (`timestamp` ainda
  `LocalDateTime`, não alterado — ver Pendências).

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Spring Web, Spring Data JPA, Bean Validation, H2 em memória.
- Build: Maven. Usar `mvnd` (daemon quente). `mvn` puro também funciona (mais lento).
- `mvnd test` roda a suite. `mvnd spring-boot:run` sobe a API em `/api`.
- **Armadilha de ambiente:** `/tmp` montado `noexec` — jansi/jline (mvnd/mvn CLI) falham ao
  carregar libs nativas. Workaround: exportar
  `JAVA_TOOL_OPTIONS="-Djansi.tmpdir=$(pwd)/.tmp -Djline.tmpdir=$(pwd)/.tmp -Djava.io.tmpdir=$(pwd)/.tmp"`
  antes de `mvnd`/`mvn` (`.tmp/` do projeto, no `.gitignore`). Só flag `-D` do `mvnd` não basta —
  cliente já tenta carregar jansi antes de repassar ao daemon. Não afeta resultado do build, só log.

## Convenções

- Pacotes por camada (`api`/`application`/`domain`/`infrastructure`), sub-pacote por domínio.
  Testes de integração ponta-a-ponta em `com.votacao.integration` (cruzam camadas). Entidades
  JPA sem setters, construtor com campos obrigatórios + construtor protegido sem args. DTOs
  como `record`, snake_case via `@JsonProperty` no campo camelCase.
- Erros: toda exceção vira `{"error","timestamp","path"}` via `ApiErrorHandler`. Cobertura:
  `HttpMessageNotReadableException`/`MethodArgumentTypeMismatchException` (400),
  `NoHandlerFoundException`/`NoResourceFoundException` (404),
  `HttpRequestMethodNotSupportedException` (405), `VotoDuplicadoException` (409),
  `SessaoJaAbertaException`/`SessaoIndisponivelParaVotoException` (422), `Exception` genérica
  (500, fallback por último). `PautaNotFoundException` reutilizada por `ResultadoService`.
- `SessaoController.abrir` usa `@RequestBody(required = false)`: body vazio não é erro, vira
  `SessaoRequest(null)` + `DURACAO_PADRAO_MINUTOS` (1). Sem `required = false`, body vazio
  quebra com 400 antes do `@Valid`, mesmo a duração sendo opcional por design.
- `PautaRequest.titulo`/`descricao` com `@Size(max = 255)`, alinhado ao `length = 255` da coluna
  — sem o limite no DTO, payload maior causava `DataIntegrityViolationException` (500) em vez de 400.
- Testes de service: JUnit 5 + Mockito, `Clock.fixed` para determinismo (exceto teste dedicado
  de leitura única, que usa `Clock` custom avançando por chamada — ver Entregas). `ContagemPorTipo`
  mockada com classe anônima. Testes de controller: `@SpringBootTest` + `@AutoConfigureMockMvc`.
  **MockMvc NÃO aplica `server.servlet.context-path`** — paths sem `/api`, incl. `$.path` do
  erro. Limitação do ambiente de teste, não do comportamento em produção.
- Para `/api` de fato e concorrência real: `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`
  — ver `ContextPathIntegrationTest`, `*ConcorrenciaIntegrationTest`, `ResultadoIntegrationTest`.
  Helpers de `ResultadoIntegrationTest` (`criarPauta`/`abrirSessao`/`votar`) checam `HttpStatus`
  antes de seguir; os demais `*IntegrationTest` não foram alterados para isso. Timestamp testado
  por valor: `isEqualTo(...now(clock))` com `Clock` controlado; `isCloseTo(..., within(5s))` em
  integração (sem `Clock` injetável no contexto real).

## Decisões

- Context-path `/api` fixado em `application.properties` (ver Convenções/MockMvc).
- Concorrência (sessão e voto): check no service + constraint única de BD como backstop
  (`Sessao.UK_PAUTA_ID`, `Voto.UK_PAUTA_ASSOCIADO`). Tradução de `DataIntegrityViolationException`
  tenta nome exato via `getConstraintName()`, fallback por mensagem (substring OU token
  `CONSTRAINT_INDEX` + colunas). Implementada em `SessaoService.violaConstraintDeSessaoUnica`/
  `VotoService.violaConstraintDeVotoUnico` (ambos privados, mesma lógica de fallback — ver
  Armadilhas para o detalhe do token `CONSTRAINT_INDEX`). Ordem de validação em
  `VotoService.registrar`: pauta existe (404) → sessão aberta (422) → duplicata (409) → Bean
  Validation antes do controller (400).
- **Status derivado na leitura, sem job**: sem sessão → `NAO_INICIADA` (200, não 404; contagens
  zeradas — ver AC-4.6); `agora < fechamento` → `ABERTA`; senão → `EXPIRADA` (master spec
  ambíguo entre "FECHADA ou EXPIRADA"; decisão: `EXPIRADA` para expiração por tempo). **Travado
  como contrato estável**: `EXPIRADA` = "encerrada pelo tempo", `FECHADA` (futuro, reservado) =
  "encerrada manualmente" — eventos distintos; clientes que querem "sessão não vota mais" devem
  checar `status_sessao` diferente de `ABERTA`/`NAO_INICIADA`, não só `== EXPIRADA`.
- **Contagens ocultas enquanto `ABERTA` (correção de review, HIGH-1 segurança).** Endpoint sem
  autenticação; expor `votos_sim`/`votos_nao` em tempo real permite voto estratégico.
  `ResultadoService.apurar` só agrega (`votoRepository.contarPorTipo`, `GROUP BY v.tipo`) quando
  `status_sessao` é `EXPIRADA`/`FECHADA`; `ABERTA`/`NAO_INICIADA` retornam zeros sem consultar
  votos. **Contradiz AC-4.4 do `task.md`** ("sessão aberta... contagens atuais") — review tratou
  a exposição como falha bloqueante; ocultar prevalece sobre o AC até sign-off formal do produto.
  Coberto por `ResultadoServiceTest.apurar_sessaoAberta_ocultaContagensEStatusAberta`
  (`verify(never())` em `contarPorTipo`), `ResultadoControllerTest`, `ResultadoIntegrationTest`.
- `timestamp` do resultado é `OffsetDateTime`, não `LocalDateTime` — cliente em outro fuso
  precisa do instante absoluto. `ErrorResponse`/demais DTOs seguem em `LocalDateTime` (ver
  Pendências).
- `ResultadoService.apurar` é `@Transactional(readOnly = true)`, alinhado a
  `SessaoService`/`VotoService`: hint de otimização (flush `MANUAL`), mesma transação/conexão
  para as 3 leituras. **Não eleva isolamento** — H2 roda em READ COMMITTED por padrão, não impede
  snapshots diferentes sob concorrência; precisaria de `REPEATABLE_READ` explícito para isso.
  Coberto por teste de reflexão (só a anotação).
- Duplicata 409, sessão inválida 422, pauta ausente 404 — herdadas dos slices 1–2.

## Entregas

- Slice 1/3: CRUD de pauta + abertura de sessão. Review: erro padrão, `@Size` em `PautaRequest`,
  `@Transactional` + tradução seletiva de constraint.
- Slice 2/3: registro de voto com validação de pauta/sessão/duplicata. Review: constraint por
  nome exato e índice autogerado, `VotoConcorrenciaIntegrationTest` (10 reqs concorrentes).
- Slice 3/3: resultado agregado (`ResultadoService`/`Controller`/`Response`, `StatusSessao`,
  `contarPorTipo`). 1º ciclo: `timestamp` testado por valor; `verify(never())` em NAO_INICIADA;
  `readOnly` com teste de reflexão; `EXPIRADA` travado como contrato; `apurar` corrigido para ler
  `clock.instant()` uma só vez (status e timestamp do mesmo instante), sem teste que provasse
  isso ainda. 2º ciclo (review de segurança, HIGH-1): contagem parcial em `ABERTA` era falha, não
  risco aceitável — corrigido ocultando `votos_sim`/`votos_nao` (zerados) até `EXPIRADA`/`FECHADA`
  (ver Decisões); adicionado `ContadorDeLeiturasClock` (Clock que avança por chamada) provando a
  leitura única do 1º ciclo.
- Suite completa: 62 testes, `mvnd test` → BUILD SUCCESS, 0 failures/errors/skipped.

## Armadilhas

- `/tmp` noexec quebra jansi/jline — usar `JAVA_TOOL_OPTIONS` (ver Stack e comandos). MockMvc
  não aplica `server.servlet.context-path` — usar integração com servidor real quando importar.
- `SessaoController.abrir` usa `@RequestBody(required = false)` — replicar em endpoints novos
  com corpo opcional; sem isso, body vazio quebra com 400 antes de qualquer validação rodar.
- `@UniqueConstraint` nomeada nem sempre aparece na mensagem de erro do H2 (índice autogerado
  `CONSTRAINT_INDEX_N`) — fallback por token `CONSTRAINT_INDEX` + colunas, nunca só nome de
  tabela (ver Decisões). Projeção Spring Data (`ContagemPorTipo`) não aceita construtor em
  teste — mockar com classe anônima.
- Ao adicionar lógica que dependa de "agora", capturar o instante uma vez no topo do método
  (`clock.instant()`) — leituras separadas podem divergir se a fronteira relevante (ex.:
  fechamento de sessão) for cruzada entre elas (ver Entregas, slice 3). Teste precisa de `Clock`
  que avança por chamada; `Clock.fixed` não pega essa regressão.

## Pendências

- Limite superior para `duracao_minutos`: não implementado. **Risco aceito — identidade sem
  autenticação** (slice 2, reforçado no slice 3): `id_associado` cru permite votar em nome de
  qualquer associado; aceito por não haver entidade de associado/login. **Prazo: resolver antes
  de exposição fora de teste/demo.** Mesma raiz: 404/409/422 enumeram estado de pauta/voto/sessão.
  Sem teto, sessão pode ficar `ABERTA` indefinidamente (nunca `EXPIRADA`) — voto estratégico via
  contagem parcial foi corrigido (ver Decisões), mas sessão também nunca "fecha"; considerar `@Max`.
- `contarPorTipo` agrega todos os votos sem índice `(pauta_id, tipo)`/cache; roda só em
  `EXPIRADA`/`FECHADA` agora (sem polling em `ABERTA`), mas pautas grandes pagam `GROUP BY` cheio.
- `SessaoResponse.from` hardcoda `status = "ABERTA"` (COULD C2, não implementado);
  `VotoRequest.idAssociado` não valida associado real (cadastro/auth); `StatusSessao.FECHADA`
  nunca é emitido (reservado, travado nas Decisões); `ErrorResponse.timestamp` segue
  `LocalDateTime`, diferente do `OffsetDateTime` de `ResultadoResponse`.
