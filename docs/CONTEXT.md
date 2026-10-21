# Contexto do projeto — votacao-api

Épico: H90-15-EPICO / run_bfe1226524a9. Slice 3/3 concluído (resultado agregado) sobre slices 1–2
mergeados. Épico completo nesta branch.

## Módulos

- `com.votacao` — `Application` (bean `Clock` system default, único em todo o app).
- `com.votacao.domain.{pauta,sessao,voto}` — entidades (`Pauta`; `Sessao` com `UK_PAUTA_ID`;
  `Voto` com `UK_PAUTA_ASSOCIADO` em `(pauta_id, id_associado)`), repositórios
  (`VotoRepository.existsByPautaIdAndIdAssociado`, `.contarPorTipo` via `@Query` GROUP BY
  retornando projeção `VotoRepository.ContagemPorTipo`), enum `TipoVoto` (SIM/NAO), enum
  `StatusSessao` (NAO_INICIADA/ABERTA/FECHADA/EXPIRADA — `FECHADA` reservado, não emitido ainda),
  exceções (`PautaNotFoundException`, `SessaoJaAbertaException`, `VotoDuplicadoException`,
  `SessaoIndisponivelParaVotoException`).
- `com.votacao.application.{pauta,sessao,voto}` — `PautaService` (criar, buscarPorId);
  `SessaoService`/`VotoService` (`@Transactional`, `Clock` injetado); `VotoService.registrar`:
  busca pauta → valida sessão aberta → checa duplicata → salva, com backstop de constraint única;
  `ResultadoService.apurar`: busca pauta (404 se ausente) → busca sessão opcional → deriva
  `StatusSessao` pelo `Clock` → agrega contagens só se sessão existe (senão zera).
- `com.votacao.api.{pauta,sessao,voto}` — controllers + DTOs (`PautaRequest` com
  `@Size(max=255)`; `VotoRequest` com `id_associado`/`voto` via `@JsonProperty`, `@Positive`;
  `ResultadoResponse` record com `@JsonProperty` snake_case). `ResultadoController` em
  `com.votacao.api.pauta` (não em pacote `resultado` próprio — endpoint é sub-recurso de pauta),
  mapeado em `GET /pautas/{id}/resultado`.
- `com.votacao.infrastructure.error` — `ApiErrorHandler` (`@RestControllerAdvice`), `ErrorResponse`.

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Spring Web, Spring Data JPA, Bean Validation, H2 em memória.
- Build: Maven. Usar `mvnd` (daemon quente). `mvn` puro também funciona (mais lento).
- `mvnd test` roda a suite. `mvnd spring-boot:run` sobe a API em `/api`.
- **Armadilha de ambiente:** `/tmp` está montado `noexec` neste ambiente — jansi/jline
  (usados pelo mvnd/mvn CLI) falham ao carregar libs nativas em `/tmp`. Workaround: exportar
  `JAVA_TOOL_OPTIONS="-Djansi.tmpdir=$(pwd)/.tmp -Djline.tmpdir=$(pwd)/.tmp -Djava.io.tmpdir=$(pwd)/.tmp"`
  antes de chamar `mvnd`/`mvn` (diretório `.tmp/` dentro do projeto, ignorado no `.gitignore`).
  Passar as propriedades só como argumento `-D` do próprio `mvnd` não bastou — o processo
  cliente já tenta carregar jansi antes de repassar argumentos ao daemon. Não afeta o resultado
  do build, só a saída de log/warning.

## Convenções

- Pacotes por camada (`api` / `application` / `domain` / `infrastructure`), sub-pacote por
  domínio (`pauta`, `sessao`, `voto`). Testes de integração ponta-a-ponta ficam em
  `com.votacao.integration` (não espelham pacote de produção, pois cruzam camadas).
- Entidades JPA imutáveis do ponto de vista de API pública: sem setters, construtor com
  campos obrigatórios, construtor protegido sem args para o Hibernate.
- DTOs como `record`. Campos em snake_case no JSON (`duracao_minutos`, `id_associado`,
  `votos_sim`, `status_sessao`) mapeados via `@JsonProperty` no campo Java em camelCase.
- Erros: todas as exceções capturadas em `ApiErrorHandler`, resposta sempre
  `{"error", "timestamp", "path"}`. Cobertura: `HttpMessageNotReadableException` (400, JSON
  malformado/enum inválido), `MethodArgumentTypeMismatchException` (400), `NoHandlerFoundException`
  /`NoResourceFoundException` (404), `HttpRequestMethodNotSupportedException` (405),
  `VotoDuplicadoException` (409), `SessaoJaAbertaException`/`SessaoIndisponivelParaVotoException`
  (422), `Exception` genérica (500, fallback — deve ficar por último para não mascarar os
  específicos). `PautaNotFoundException` (404) reutilizada por `ResultadoService` sem handler
  novo.
- Testes de service: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), `Clock.fixed`
  para determinismo. Projeção de repositório (`ContagemPorTipo`) mockada com classe anônima.
- Testes de controller: `@SpringBootTest` + `@AutoConfigureMockMvc`. **MockMvc NÃO aplica
  `server.servlet.context-path`** — testes usam paths sem `/api` (`/pautas/...`), incl. `$.path`
  do erro. Limitação do ambiente de teste, não do comportamento em produção.
- Para validar `/api` de fato e concorrência real, usar `@SpringBootTest(webEnvironment =
  RANDOM_PORT)` + `TestRestTemplate` (+ `ExecutorService` para concorrência) — ver
  `ContextPathIntegrationTest`, `SessaoConcorrenciaIntegrationTest`, `VotoConcorrenciaIntegrationTest`,
  `ResultadoIntegrationTest` (happy path completo, sem concorrência).

## Decisões

- Context-path `/api` fixado em `application.properties` (ver Convenções para limitação do
  MockMvc).
- Concorrência (sessão e voto): dupla barreira — check no service (rejeita cedo) + constraint
  única de BD como backstop (`Sessao.UK_PAUTA_ID`, `Voto.UK_PAUTA_ASSOCIADO`). Tradução de
  `DataIntegrityViolationException` tenta nome exato da constraint via
  `ConstraintViolationException#getConstraintName()`, com fallback por mensagem (nome em
  substring OU token `CONSTRAINT_INDEX` + colunas da constraint, nunca só nome de tabela — H2
  pode reportar índice autogerado). Detalhes completos nos commits do slice 1/2; ver Armadilhas.
- Ordem de validação em `VotoService.registrar` (primeira falha vence): pauta existe (404) →
  sessão aberta (422) → duplicata (409) → payload/Bean Validation antes do controller (400).
- **Status de sessão derivado na leitura, sem job/scheduler** (`ResultadoService.apurar`): sem
  sessão → `NAO_INICIADA` (200, contagens zeradas, não 404); com sessão, `agora < fechamento` →
  `ABERTA`, senão → `EXPIRADA`. `FECHADA` reservado para fechamento manual futuro, não emitido
  neste slice (master spec ambíguo entre "FECHADA ou EXPIRADA"; decisão: usar `EXPIRADA`).
- Contagem agregada no banco via `@Query` JPQL `GROUP BY v.tipo`, não em memória — projeção de
  interface (`ContagemPorTipo`) em vez de `Object[]` para tipagem forte. Query só executa se
  sessão existe (`ResultadoService` pula a chamada ao repositório quando `Optional` vazio,
  evitando round-trip desnecessário no caso NAO_INICIADA).
- `timestamp` do resultado = `LocalDateTime.now(clock)` no momento da consulta, não da sessão.
- Duplicata mapeada para 409, sessão inválida para 422, pauta ausente para 404 — decisões
  herdadas dos slices 1–2, reaplicadas ao resultado.
- Duração de sessão: sem teto superior implementado (épico não define limite). Pendência aberta.

## Entregas

- Slice 1/3: CRUD de pauta + abertura de sessão (`POST /api/pautas`, `GET /api/pautas/{id}`,
  `POST /api/pautas/{id}/sessao`).
- Slice 2/3: registro de voto (`POST /api/pautas/{id}/votos`) com validação de pauta, sessão
  aberta e duplicata (service + constraint de BD). Riscos aceitos (identidade sem auth,
  enumeração 404/422/409) documentados em Pendências.
- Slice 3/3: resultado agregado (`GET /api/pautas/{id}/resultado`) — `ResultadoService`,
  `ResultadoController`, `ResultadoResponse`, `StatusSessao`, `VotoRepository.contarPorTipo`.
  `ResultadoServiceTest` (5 casos: aberta, limite exato de fechamento, expirada mantém contagem,
  sem sessão, pauta inexistente propaga 404), `ResultadoControllerTest` (3 casos MockMvc: sem
  sessão, aberta com votos mistos, pauta inexistente), `ResultadoIntegrationTest` (happy path
  completo via `TestRestTemplate`: pauta → sessão → 3 SIM/2 NAO → resultado 5 total). Sem
  alteração de schema (usa entidades e `Clock` já existentes).
- Suite completa: 60 testes, `mvnd test` → BUILD SUCCESS, 0 failures, 0 errors, 0 skipped.

## Armadilhas

- `/tmp` noexec quebra jansi/jline do `mvnd`/`mvn` CLI — ver Stack e comandos (usar
  `JAVA_TOOL_OPTIONS`, não só flags `-D` do `mvnd`).
- MockMvc não aplica `server.servlet.context-path` — não assumir isso em testes futuros de
  controller; usar integração com servidor real quando o prefixo importar.
- Nomear `@UniqueConstraint` explicitamente nem sempre garante que o nome apareça na mensagem de
  erro do H2 — pode reportar índice autogerado (`CONSTRAINT_INDEX_N`). Ao adicionar entidades com
  unicidade, sempre incluir fallback por token `CONSTRAINT_INDEX` + colunas (não só nome de
  tabela, que mascara violação em outra coluna da mesma tabela).
- Projeção de repositório Spring Data (`interface ContagemPorTipo`) não pode ser instanciada
  normalmente em teste — mockar com classe anônima implementando a interface, não com construtor.

## Pendências

- Limite superior para `duracao_minutos`: não implementado, não definido pelo épico. Confirmar
  com stakeholder antes do merge final.
- **Risco aceito formalmente — identidade do voto/pauta/sessão sem autenticação** (bloqueio de
  design levantado em review de segurança, slice 2). `id_associado` cru no payload: qualquer
  cliente vota em nome de qualquer associado. Aceito pois não há entidade de associado nem login
  no sistema. **Prazo: resolver antes de exposição fora de teste/demo.** Decorrência aceita:
  404/409/422 juntos enumeram estado de pauta/voto/sessão para qualquer chamador — revisitar
  junto com a auth.
- `SessaoResponse.from` hardcoda `status = "ABERTA"` (não usa `StatusSessao` novo); alinhar com
  `ResultadoService` ficaria para um slice de polimento, fora do escopo do slice 3 (COULD C2 do
  task.md, não implementado).
- `VotoRequest.idAssociado` rejeita não positivo (`@Positive`), mas não valida associado real
  (sem entidade de associado) — revisitar com cadastro de associados e auth acima.
- `StatusSessao.FECHADA` existe no enum mas nunca é emitido (reservado para fechamento manual
  futuro, fora do escopo deste épico).
