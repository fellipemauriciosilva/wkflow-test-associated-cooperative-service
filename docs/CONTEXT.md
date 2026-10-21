# Contexto do projeto — votacao-api

Épico: H90-15-EPICO / run_bfe1226524a9. Slice 3/3 concluído (resultado agregado) sobre slices 1–2
mergeados. Épico completo nesta branch. Ciclo de correção do review aplicado (ver Decisões).

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
  `ResultadoService` (todos `@Transactional`, `Clock` injetado; `ResultadoService.apurar` é
  `@Transactional(readOnly = true)`). `VotoService.registrar`: pauta → sessão aberta → duplicata
  → salva, com backstop de constraint única. `ResultadoService.apurar`: pauta (404 se ausente) →
  sessão opcional → lê `Clock` **uma única vez** (`agora`), deriva `StatusSessao` e `timestamp`
  do mesmo instante (ver Armadilhas) → agrega contagens só se sessão existe, sem consultar
  `votoRepository` quando não há sessão.
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
  — sem o limite no DTO, payload maior causava `DataIntegrityViolationException` (500) em vez
  de 400; `@Size` converte erro de banco em erro de validação de entrada.
- Testes de service: JUnit 5 + Mockito, `Clock.fixed` para determinismo. `ContagemPorTipo`
  mockada com classe anônima (projeção Spring Data não aceita construtor em teste). Testes de
  controller: `@SpringBootTest` + `@AutoConfigureMockMvc`. **MockMvc NÃO aplica
  `server.servlet.context-path`** — paths sem `/api`, incl. `$.path` do erro. Limitação do
  ambiente de teste, não do comportamento em produção.
- Para `/api` de fato e concorrência real: `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`
  (+ `ExecutorService`) — ver `ContextPathIntegrationTest`, `SessaoConcorrenciaIntegrationTest`,
  `VotoConcorrenciaIntegrationTest`, `ResultadoIntegrationTest`. Helpers desses testes
  (`criarPauta`/`abrirSessao`/`votar`) sempre checam `HttpStatus` antes de seguir. Timestamp de
  API testado por valor, não só presença: `isEqualTo(...now(clock))` em unitário/MockMvc com
  `Clock` controlado; `isCloseTo(OffsetDateTime.now(), within(5, SECONDS))` em integração (sem
  `Clock` injetável no contexto real).

## Decisões

- Context-path `/api` fixado em `application.properties` (ver Convenções/MockMvc).
- Concorrência (sessão e voto): check no service + constraint única de BD como backstop
  (`Sessao.UK_PAUTA_ID`, `Voto.UK_PAUTA_ASSOCIADO`). Tradução de `DataIntegrityViolationException`
  tenta nome exato via `getConstraintName()`, fallback por mensagem (substring OU token
  `CONSTRAINT_INDEX` + colunas). Implementada em `SessaoService.violaConstraintDeSessaoUnica`/
  `VotoService.violaConstraintDeVotoUnico` — ver Armadilhas. Ordem de validação em
  `VotoService.registrar`: pauta existe (404) → sessão aberta (422) → duplicata (409) → Bean
  Validation antes do controller (400).
- **Status derivado na leitura, sem job**: sem sessão → `NAO_INICIADA`; `agora < fechamento` →
  `ABERTA`; senão → `EXPIRADA` (master spec ambíguo entre "FECHADA ou EXPIRADA"; decisão:
  `EXPIRADA` para expiração por tempo). **Travado como contrato estável**: `EXPIRADA` =
  "encerrada pelo tempo", `FECHADA` (futuro, reservado) = "encerrada manualmente" — eventos
  distintos; clientes que querem "sessão não vota mais" devem checar `status_sessao` diferente
  de `ABERTA`/`NAO_INICIADA`, não só `== EXPIRADA`.
- Contagem agregada no banco (`GROUP BY v.tipo`), projeção tipada. Query só executa com sessão
  presente — coberto por `verify(votoRepository, never()).contarPorTipo(...)`.
- `ResultadoService.apurar` lê `Clock` **uma única vez** (`clock.instant()`), deriva status e
  timestamp do mesmo instante — duas leituras separadas podiam ficar inconsistentes se a
  fronteira de fechamento fosse cruzada entre elas (risco de review).
- `timestamp` do resultado é `OffsetDateTime` (ISO-8601 com offset), não `LocalDateTime` —
  cliente em outro fuso precisa do instante absoluto. `ErrorResponse`/demais DTOs seguem em
  `LocalDateTime`, fora do escopo (ver Pendências).
- `ResultadoService.apurar` é `@Transactional(readOnly = true)`, alinhado a
  `SessaoService`/`VotoService` — sem isso, as 3 queries rodavam em conexões implícitas
  separadas, risco de snapshots diferentes sob concorrência. Coberto por teste de reflexão.
- **Contagem parcial em sessão ABERTA: mantida visível, decisão explícita.** Review de segurança
  apontou voto estratégico. Mantida pois AC-4.4 do `task.md` exige contagens atuais com
  `status_sessao=ABERTA` — requisito funcional, não lacuna. Risco em Pendências com a falta de auth.
- Duplicata 409, sessão inválida 422, pauta ausente 404 — herdadas dos slices 1–2.

## Entregas

- Slice 1/3: CRUD de pauta + abertura de sessão. Review: erro padrão em toda exceção, `@Size`
  em `PautaRequest`, `@Transactional` + tradução seletiva de constraint.
- Slice 2/3: registro de voto com validação de pauta/sessão/duplicata. Review: `VotoServiceTest`
  cobre constraint por nome exato e por índice autogerado, `VotoConcorrenciaIntegrationTest`
  (10 reqs concorrentes). Riscos aceitos documentados.
- Slice 3/3: resultado agregado (`ResultadoService`/`Controller`/`Response`, `StatusSessao`,
  `contarPorTipo`). Ciclo de correção: `timestamp` (`OffsetDateTime`) testado por valor;
  `verify(never())` para `contarPorTipo` em NAO_INICIADA; `@Transactional(readOnly = true)` com
  teste de reflexão; `Clock` lido uma vez; `EXPIRADA` travado como contrato estável; contagem
  parcial em ABERTA mantida por exigência do `task.md`. `ResultadoServiceTest` (6 casos),
  `ResultadoControllerTest` (3 casos, assert de timestamp), `ResultadoIntegrationTest` (happy
  path, timestamp + `HttpStatus` dos helpers). Sem alteração de schema.
- Suite completa: 61 testes, `mvnd test` → BUILD SUCCESS, 0 failures/errors/skipped.

## Armadilhas

- `/tmp` noexec quebra jansi/jline — usar `JAVA_TOOL_OPTIONS` (ver Stack e comandos). MockMvc
  não aplica `server.servlet.context-path` — usar integração com servidor real quando o prefixo
  importar.
- `SessaoController.abrir` usa `@RequestBody(required = false)` — replicar em endpoints novos
  com corpo opcional; sem isso, body vazio quebra com 400 antes de qualquer validação rodar.
- `@UniqueConstraint` nomeada nem sempre aparece na mensagem de erro do H2 (pode reportar
  índice autogerado `CONSTRAINT_INDEX_N`) — sempre incluir fallback por token `CONSTRAINT_INDEX`
  + colunas, nunca só nome de tabela (mascara violação em outra coluna). Ver Decisões
  (concorrência). Projeção Spring Data (`ContagemPorTipo`) não aceita construtor em teste —
  mockar com classe anônima implementando a interface.
- `ResultadoService.apurar` lia `Clock` duas vezes (status e timestamp separados) — corrigido
  lendo `clock.instant()` uma vez. Ao adicionar lógica que dependa de "agora", capturar o
  instante uma vez no topo do método, nunca ler o relógio mais de uma vez por requisição.

## Pendências

- Limite superior para `duracao_minutos`: não implementado, não definido pelo épico. **Risco
  aceito — identidade sem autenticação** (slice 2, reforçado no slice 3): `id_associado` cru no
  payload permite votar em nome de qualquer associado. Aceito por não haver entidade de
  associado/login. **Prazo: resolver antes de exposição fora de teste/demo.** Mesma raiz:
  404/409/422 juntos enumeram estado de pauta/voto/sessão; contagem parcial em ABERTA permite
  voto estratégico (mantida por exigência do AC-4.4 — revisitar se virar "ocultar até fechar").
- `SessaoResponse.from` hardcoda `status = "ABERTA"` (COULD C2 do task.md, não implementado).
  `VotoRequest.idAssociado` rejeita não positivo mas não valida associado real — revisitar com
  cadastro de associados e auth.
- `StatusSessao.FECHADA` existe mas nunca é emitido (reservado para fechamento manual futuro,
  nome/semântica já travados nas Decisões — não reaproveitar `EXPIRADA` para esse caso).
- `ErrorResponse.timestamp` continua `LocalDateTime`, diferente de `ResultadoResponse.timestamp`
  (`OffsetDateTime`) — revisitar se a API padronizar formato de timestamp globalmente.
