# Contexto do projeto — votacao-api

Épico: H90-15-EPICO / run_bfe1226524a9. Slice 2/3 concluído (voto + duplicidade, review aplicado), sobre slice 1 mergeada.

## Módulos

- `com.votacao` — `Application` (bean `Clock` system default).
- `com.votacao.domain.{pauta,sessao,voto}` — entidades (`Pauta`; `Sessao` com `UK_PAUTA_ID`;
  `Voto` com `UK_PAUTA_ASSOCIADO` em `(pauta_id, id_associado)`), repositórios
  (`VotoRepository.existsByPautaIdAndIdAssociado`), enum `TipoVoto` (SIM/NAO), exceções
  (`PautaNotFoundException`, `SessaoJaAbertaException`, `VotoDuplicadoException`,
  `SessaoIndisponivelParaVotoException`).
- `com.votacao.application.{pauta,sessao,voto}` — `PautaService` (criar, buscarPorId);
  `SessaoService`/`VotoService` (`@Transactional`, `Clock` injetado); `VotoService.registrar`:
  busca pauta → valida sessão aberta → checa duplicata → salva, com backstop de constraint única.
- `com.votacao.api.{pauta,sessao,voto}` — controllers + DTOs (`PautaRequest` com
  `@Size(max=255)`; `VotoRequest` com `id_associado`/`voto` via `@JsonProperty`, `@Positive`).
- `com.votacao.infrastructure.error` — `ApiErrorHandler` (`@RestControllerAdvice`), `ErrorResponse`.

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Spring Web, Spring Data JPA, Bean Validation, H2 em memória.
- Build: Maven. Usar `mvnd` (daemon quente). `mvn` puro também funciona (mais lento).
- `mvnd test` roda a suite. `mvnd spring-boot:run` sobe a API em `/api`.
- **Armadilha de ambiente:** `/tmp` está montado `noexec` neste ambiente — jansi/jline
  (usados pelo mvnd/mvn CLI) falham ao carregar libs nativas em `/tmp`. Workaround: exportar
  `JAVA_TOOL_OPTIONS="-Djansi.tmpdir=$(pwd)/.tmp -Djline.tmpdir=$(pwd)/.tmp -Djava.io.tmpdir=$(pwd)/.tmp"`
  antes de chamar `mvnd`/`mvn` (diretório `.tmp/` dentro do projeto, ignorado no `.gitignore`).
  Passar as propriedades só como argumento `-D` do próprio `mvnd` não bastou nesta sessão — o
  processo cliente do mvnd já tenta carregar jansi antes de repassar os argumentos ao daemon;
  `JAVA_TOOL_OPTIONS` é lido por qualquer JVM que o comando dispare. Não afeta o resultado do
  build, só a saída de log/warning.

## Convenções

- Pacotes por camada (`api` / `application` / `domain` / `infrastructure`), sub-pacote por
  domínio (`pauta`, `sessao`, `voto`). Testes de integração ponta-a-ponta ficam em
  `com.votacao.integration` (não espelham pacote de produção, pois cruzam camadas).
- Entidades JPA imutáveis do ponto de vista de API pública: sem setters, construtor com
  campos obrigatórios, construtor protegido sem args para o Hibernate.
- DTOs como `record`. Campos em snake_case no JSON (`duracao_minutos`, `id_associado`) mapeados
  via `@JsonProperty` no campo Java em camelCase.
- Erros: todas as exceções capturadas em `ApiErrorHandler`, resposta sempre
  `{"error", "timestamp", "path"}`. Cobertura: `HttpMessageNotReadableException` (400, JSON
  malformado/enum inválido), `MethodArgumentTypeMismatchException` (400), `NoHandlerFoundException`
  /`NoResourceFoundException` (404), `HttpRequestMethodNotSupportedException` (405),
  `VotoDuplicadoException` (409), `SessaoJaAbertaException`/`SessaoIndisponivelParaVotoException`
  (422), `Exception` genérica (500, fallback — deve ficar por último para não mascarar os
  específicos).
- Testes de service: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), `Clock.fixed`
  para determinismo em `SessaoServiceTest`/`VotoServiceTest`.
- Testes de controller: `@SpringBootTest` + `@AutoConfigureMockMvc`. **MockMvc NÃO aplica
  `server.servlet.context-path`** — testes usam paths sem `/api` (`/pautas/...`), incl. `$.path`
  do erro. Limitação do ambiente de teste, não do comportamento em produção.
- Para validar `/api` de fato (inclusive `path` do erro) e concorrência real (múltiplas threads),
  usar `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` (+ `ExecutorService`
  para concorrência) — ver `ContextPathIntegrationTest`, `SessaoConcorrenciaIntegrationTest`,
  `VotoConcorrenciaIntegrationTest`. Testes unitários com mock de
  `DataIntegrityViolationException` cobrem a lógica de tradução isoladamente, mas não provam a
  constraint de BD por si só.

## Decisões

- Context-path `/api` fixado em `application.properties` (ver Convenções para limitação do
  MockMvc).
- Concorrência na abertura de sessão: dupla barreira — check `findByPautaId` no service (rejeita
  cedo) + constraint única nomeada (`Sessao.UK_PAUTA_ID` = `uk_sessao_pauta_id`) em
  `sessao.pauta_id`. `abrir()` é `@Transactional`. Tradução de `DataIntegrityViolationException`
  só vira `SessaoJaAbertaException` se a mensagem referenciar a constraint (nome, ou
  `sessao`+`pauta_id`); outra violação relança sem tradução. Coberto por `SessaoServiceTest`
  (violação não relacionada) e `SessaoConcorrenciaIntegrationTest` (10 reqs, 1×201 + 9×422).
- Voto segue o mesmo padrão de dupla barreira: check `existsByPautaIdAndIdAssociado` no
  `VotoService` (rejeita cedo com 409) + constraint única nomeada (`Voto.UK_PAUTA_ASSOCIADO` =
  `uk_voto_pauta_associado`) em `(pauta_id, id_associado)`. `registrar()` é `@Transactional`.
  Tradução de `DataIntegrityViolationException` tenta, em ordem: (1)
  `org.hibernate.exception.ConstraintViolationException#getConstraintName()` via `getCause()`
  comparado com `Voto.UK_PAUTA_ASSOCIADO` (`equalsIgnoreCase`) — só confirma se bater exato, não
  usado para negar; (2) fallback por mensagem (sempre roda, pois o H2 às vezes reporta um índice
  autogerado mesmo em `getConstraintName()`): nome da constraint em substring, OU token
  `CONSTRAINT_INDEX` + colunas `pauta_id`/`id_associado` juntas (cobre
  `CONSTRAINT_INDEX_9 ON PUBLIC.VOTO(PAUTA_ID, ID_ASSOCIADO)`). Removido o critério frouxo
  `contains("voto")` isolado (mascarava violação em coluna diferente da mesma tabela). Coberto por
  `VotoServiceTest` (nome exato, índice autogerado com colunas certas, índice autogerado com
  coluna diferente, violação não relacionada) e `VotoConcorrenciaIntegrationTest`.
- Ordem de validação em `VotoService.registrar` (primeira falha vence): 1. pauta existe (404)
  2. sessão aberta (422) 3. duplicata (409) 4. payload/Bean Validation, antes do controller
  chamar o service (400).
- Duplicata mapeada para 409 (conflito com recurso existente), sessão inválida para 422 — conforme
  decisões documentadas no `task.md` do slice 2.
- `TipoVoto` é enum (`SIM`/`NAO`) mapeado via Jackson; valor fora do enum já cai em
  `HttpMessageNotReadableException` (400) no deserializer padrão, sem handler extra necessário.
- `PautaRequest.titulo`/`descricao` limitados a 255 caracteres via `@Size(max = 255)`, alinhado ao
  `length = 255` explícito nas colunas de `Pauta`. Sem o limite, payload grande causava
  `DataIntegrityViolationException` não capturada (500) em vez de 400.
- Duração de sessão: sem teto superior implementado (épico não define limite). Pendência aberta,
  listada abaixo.

## Entregas

- Slice 1/3: CRUD básico de pauta + abertura de sessão. Endpoints `POST /api/pautas`,
  `GET /api/pautas/{id}`, `POST /api/pautas/{id}/sessao`. Review aplicado: erro padrão em toda
  exceção, `@Size` em `PautaRequest`, `@Transactional` + tradução seletiva de constraint.
- Slice 2/3: registro de voto (`POST /api/pautas/{id}/votos`) com validação de pauta existente,
  sessão aberta e duplicata (service + constraint de BD como backstop). Entidade `Voto`,
  `VotoRepository`, `VotoService`, `VotoController` + DTOs, `ApiErrorHandler` estendido para 409
  (duplicata) e 422 (sessão inválida). `VotoServiceTest` (10 casos, incl. violação de constraint
  por nome exato, por índice autogerado `CONSTRAINT_INDEX_N` com colunas certas/erradas),
  `VotoControllerTest` (8 casos MockMvc), `VotoConcorrenciaIntegrationTest` (10 reqs concorrentes,
  1×201 + 9×409).
- Ciclo de correção de review aplicado ao slice 2: `VotoRequest.idAssociado` ganhou `@Positive`;
  fallback de constraint restrito (token `CONSTRAINT_INDEX` + colunas, sem `contains("voto")`
  isolado) — ver Decisões. Riscos aceitos (identidade do voto sem auth, enumeração 404/422/409)
  documentados formalmente em Pendências, com prazo.
- Suite completa: 51 testes, `mvnd test` → BUILD SUCCESS, 0 failures, 0 errors, 0 skipped.

## Armadilhas

- `/tmp` noexec quebra jansi/jline do `mvnd`/`mvn` CLI — ver seção Stack e comandos acima
  (usar `JAVA_TOOL_OPTIONS`, não só flags `-D` do `mvnd`).
- `SessaoRequest` com corpo HTTP vazio (sem `Content-Type`/body): controller trata `request == null`
  criando `SessaoRequest(null)` manualmente, pois `@RequestBody(required = false)` com body
  ausente chega como `null` em vez de lançar erro.
- MockMvc não aplica `server.servlet.context-path` — não assumir isso em testes futuros de
  controller; usar integração com servidor real quando o comportamento do prefixo importar.
- Nomear `@UniqueConstraint` explicitamente nem sempre garante que o nome apareça na mensagem de
  erro do H2 nem em `getConstraintName()` do Hibernate — pode reportar índice autogerado
  (`CONSTRAINT_INDEX_N`), confirmado para `Voto.UK_PAUTA_ASSOCIADO` nesta sessão. Ver tratamento
  em Decisões (voto). Ao adicionar novas entidades com unicidade, sempre incluir fallback por
  token `CONSTRAINT_INDEX` + colunas, exigindo as colunas (não só o nome da tabela, que é frouxo
  demais e mascara violação em outra coluna da mesma tabela).

## Pendências

- Limite superior para `duracao_minutos`: não implementado, não definido pelo épico. Confirmar
  com stakeholder antes do merge final (mencionado no `task.md` original como questão em aberto).
- **Risco aceito formalmente — identidade do voto sem autenticação** (bloqueio de design levantado
  em review de segurança). `id_associado` cru no payload: qualquer cliente vota em nome de
  qualquer associado (loop forja resultado). Aceito para o slice 2 — sem entidade de associado nem
  login no sistema ainda, fora do MUST/COULD do `task.md`, sem stakeholder/prazo definido pelo
  épico. **Prazo: resolver antes de exposição fora de teste/demo** (pré-requisito de produção);
  quem adicionar cadastro de associados revisita este item. Mitigação parcial: `@Positive` em
  `id_associado` (não resolve auth). Mesma pendência cobre criar pauta/abrir sessão sem restrição.
  **Decorrência aceita:** 409/404/422 juntos enumeram estado de pauta/voto para qualquer chamador;
  mantido assim porque o problema real é a falta de auth, não o código de status — revisitar junto
  com o item acima.
- `SessaoResponse.from` hardcoda `status = "ABERTA"`; derivar do estado real deve entrar no slice
  de resultado.
- Slice 3 (resultado agregado): depende de `VotoRepository` para contagem SIM/NAO — já atendido.
- `VotoRequest.idAssociado` rejeita não positivo (`@Positive`), mas não valida associado real
  (sem entidade de associado ainda) — revisitar com cadastro de associados e com auth acima.
