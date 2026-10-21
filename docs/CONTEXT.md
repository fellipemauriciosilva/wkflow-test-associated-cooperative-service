# Contexto do projeto — votacao-api

Épico: H90-15-EPICO / run_bfe1226524a9. Slice 2/3 concluído (voto + validação de duplicidade),
sobre slice 1 já mergeada (ciclo de correção de review aplicado).

## Módulos

- `com.votacao` — `Application` (bean `Clock` system default).
- `com.votacao.domain.pauta` — entidade `Pauta`, `PautaRepository`, `PautaNotFoundException`.
- `com.votacao.domain.sessao` — entidade `Sessao` (constante `UK_PAUTA_ID` com nome da
  constraint única), `SessaoRepository`, `SessaoJaAbertaException`.
- `com.votacao.domain.voto` — entidade `Voto` (constante `UK_PAUTA_ASSOCIADO` com nome da
  constraint única em `(pauta_id, id_associado)`), enum `TipoVoto` (SIM/NAO), `VotoRepository`
  (`existsByPautaIdAndIdAssociado`), `VotoDuplicadoException`, `SessaoIndisponivelParaVotoException`.
- `com.votacao.application.pauta` — `PautaService` (criar, buscarPorId).
- `com.votacao.application.sessao` — `SessaoService` (abrir, `@Transactional`, `Clock` injetado).
- `com.votacao.application.voto` — `VotoService` (registrar, `@Transactional`, `Clock` injetado):
  busca pauta → valida sessão aberta → checa duplicata → salva, com backstop de constraint única.
- `com.votacao.api.pauta` — `PautaController` + DTOs (`PautaRequest` com `@Size(max=255)` em
  `titulo`/`descricao`, `PautaResponse`).
- `com.votacao.api.sessao` — `SessaoController` + DTOs (`SessaoRequest`, `SessaoResponse`).
- `com.votacao.api.voto` — `VotoController` (`POST /pautas/{id}/votos`) + DTOs (`VotoRequest`
  com `id_associado`/`voto` via `@JsonProperty`, `VotoResponse`).
- `com.votacao.infrastructure.error` — `ApiErrorHandler` (`@RestControllerAdvice`), `ErrorResponse`.
  Agora também mapeia `VotoDuplicadoException` (409) e `SessaoIndisponivelParaVotoException` (422).

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
- Erros: todas as exceções (de domínio e de infraestrutura/framework) capturadas em
  `ApiErrorHandler`, resposta sempre `{"error", "timestamp", "path"}`. Cobertura inclui
  `HttpMessageNotReadableException` (400, JSON malformado — também cobre enum inválido em
  `TipoVoto`), `MethodArgumentTypeMismatchException` (400, path/query param com tipo errado),
  `NoHandlerFoundException`/`NoResourceFoundException` (404, rota inexistente),
  `HttpRequestMethodNotSupportedException` (405, verbo não suportado), `VotoDuplicadoException`
  (409), `SessaoJaAbertaException`/`SessaoIndisponivelParaVotoException` (422) e `Exception`
  genérica (500, fallback) — handler catch-all deve ficar por último na classe para não mascarar
  os mais específicos.
- Testes de service: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), `Clock.fixed`
  para determinismo em `SessaoServiceTest`/`VotoServiceTest`.
- Testes de controller: `@SpringBootTest` + `@AutoConfigureMockMvc`. **`MockMvc` com contexto
  Spring Boot completo NÃO aplica `server.servlet.context-path` automaticamente** — o
  `DispatcherServlet` de teste roda sem o prefixo. Por isso os testes `PautaControllerTest`,
  `SessaoControllerTest` e `VotoControllerTest` usam paths sem `/api` (`/pautas/...`) e o campo
  `$.path` do erro também sai sem o prefixo. Isso é uma limitação do ambiente de teste, não do
  comportamento em produção.
- Para validar o comportamento real sob `/api` (prefixo de contexto aplicado de fato, inclusive no
  campo `path` do erro) use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
  — ver `com.votacao.integration.ContextPathIntegrationTest`.
- Teste de concorrência real (múltiplas threads batendo no mesmo endpoint) usa
  `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` + `ExecutorService` — ver
  `SessaoConcorrenciaIntegrationTest` e `VotoConcorrenciaIntegrationTest`. Testes unitários com
  mock de `DataIntegrityViolationException` continuam existindo para cobrir a lógica de tradução
  de exceção isoladamente, mas não provam a constraint de BD por si só.

## Decisões

- Context-path `/api` fixado em `application.properties`. Em MockMvc, os testes batem direto em
  `/pautas/...` (prefixo não aplicado pelo `DispatcherServlet` de teste — ver Convenções). Em
  produção e nos testes de integração com servidor real, o prefixo `/api` é aplicado de fato.
- Concorrência na abertura de sessão: dupla barreira — check `findByPautaId` no service (rejeita
  cedo) + constraint única nomeada (`Sessao.UK_PAUTA_ID` = `uk_sessao_pauta_id`) em
  `sessao.pauta_id`. `abrir()` é `@Transactional`. No `catch` de `DataIntegrityViolationException`,
  só traduz para `SessaoJaAbertaException` se a mensagem da causa raiz referenciar a constraint
  (pelo nome, ou por conter `sessao`+`pauta_id` — o H2 às vezes reporta um índice interno
  autogerado em vez do nome declarado); qualquer outra violação de integridade é relançada sem
  tradução, para não mascarar bugs reais (ex.: coluna NOT NULL nula) como "sessão já existente".
  Coberto por teste unitário (`SessaoServiceTest`, cenário de violação não relacionada) e por teste
  de integração com 10 requisições concorrentes reais (`SessaoConcorrenciaIntegrationTest`),
  afirmando exatamente um 201 e os demais 422.
- Voto segue o mesmo padrão de dupla barreira: check `existsByPautaIdAndIdAssociado` no
  `VotoService` (rejeita cedo com 409) + constraint única nomeada (`Voto.UK_PAUTA_ASSOCIADO` =
  `uk_voto_pauta_associado`) em `(pauta_id, id_associado)`. `registrar()` é `@Transactional`. A
  tradução de `DataIntegrityViolationException` usa o mesmo fallback por nome/tabela/coluna que
  `SessaoService` — necessário porque, nesta sessão, o H2 reportou o índice como
  `UK_VOTO_PAUTA_ASSOCIADO_INDEX_2` (sufixo autogerado), não o nome exato da constraint; o
  fallback por `contains("voto") && contains("pauta_id") && contains("id_associado")` cobre esse
  caso. Coberto por `VotoServiceTest` (cenário de violação não relacionada) e por
  `VotoConcorrenciaIntegrationTest` (10 requisições concorrentes do mesmo associado/pauta,
  exatamente um 201 e os demais 409).
- Ordem de validação em `VotoService.registrar` (primeira falha vence, conforme task.md):
  1. pauta existe (`PautaService.buscarPorId`, senão `PautaNotFoundException` → 404)
  2. sessão aberta (`abertura <= now < fechamento`, senão `SessaoIndisponivelParaVotoException` → 422)
  3. duplicata (`existsByPautaIdAndIdAssociado`, senão `VotoDuplicadoException` → 409)
  4. payload (Bean Validation no DTO, antes mesmo do controller chamar o service) → 400
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

- Slice 1/3: CRUD básico de pauta (criar, consultar) + abertura de sessão de votação com duração
  parametrizável. Endpoints: `POST /api/pautas`, `GET /api/pautas/{id}`, `POST /api/pautas/{id}/sessao`.
  Ciclo de correção de review aplicado: formato de erro padrão estendido a toda exceção não
  prevista, `@Size` em `PautaRequest`, `@Transactional` + tradução seletiva de constraint em
  `SessaoService`, testes de integração reais para context-path e concorrência.
- Slice 2/3: registro de voto (`POST /api/pautas/{id}/votos`) com validação de pauta existente,
  sessão aberta e duplicata (service + constraint de BD como backstop). Entidade `Voto`,
  `VotoRepository`, `VotoService`, `VotoController` + DTOs, `ApiErrorHandler` estendido para 409
  (duplicata) e 422 (sessão inválida). Testes: `VotoServiceTest` (8 casos: válido, duplicata,
  sem sessão, sessão expirada, sessão não iniciada, pauta inexistente, violação de constraint
  relacionada e não relacionada), `VotoControllerTest` (6 casos MockMvc: 201/409/422/404/400
  payload incompleto/400 enum inválido), `VotoConcorrenciaIntegrationTest` (10 requisições reais
  concorrentes do mesmo associado, exatamente 1×201 e 9×409).
- Suite completa: 47 testes, `mvnd test` → BUILD SUCCESS, 0 failures, 0 errors, 0 skipped.

## Armadilhas

- `/tmp` noexec quebra jansi/jline do `mvnd`/`mvn` CLI — ver seção Stack e comandos acima
  (usar `JAVA_TOOL_OPTIONS`, não só flags `-D` do `mvnd`).
- `SessaoRequest` com corpo HTTP vazio (sem `Content-Type`/body): controller trata `request == null`
  criando `SessaoRequest(null)` manualmente, pois `@RequestBody(required = false)` com body
  ausente chega como `null` em vez de lançar erro.
- MockMvc não aplica `server.servlet.context-path` — não assumir isso em testes futuros de
  controller; usar integração com servidor real quando o comportamento do prefixo importar.
- Nomear `@UniqueConstraint` explicitamente nem sempre garante que o nome apareça na mensagem de
  erro do H2 (pode reportar um índice interno autogerado tipo `CONSTRAINT_INDEX_9`, confirmado
  também para `Voto.UK_PAUTA_ASSOCIADO` nesta sessão). Detecção de "qual constraint violou" via
  `getMostSpecificCause().getMessage()` deve sempre ter um fallback por tabela/coluna além do nome
  — não confiar só no nome declarado da constraint ao adicionar novas entidades com unicidade.

## Pendências

- Limite superior para `duracao_minutos`: não implementado, não definido pelo épico. Confirmar
  com stakeholder antes do merge final (mencionado no `task.md` original como questão em aberto).
- Sem autenticação/autorização: qualquer requisição cria pauta, abre sessão e vota em nome de
  qualquer `id_associado` informado no payload (não validado contra identidade real). Avaliar
  antes de produção; fora do escopo do slice 2 conforme MoSCoW (C2 "could").
- `SessaoResponse.from` hardcoda `status = "ABERTA"`. Ok enquanto não existe fechamento de sessão;
  derivar do estado real (comparando `fechamento` com `now`) deve entrar no slice de resultado.
- Slice 3 (resultado agregado, master AC não detalhado neste task.md): depende de `VotoRepository`
  para contagem de SIM/NAO por pauta — `Voto` e `TipoVoto` já atendem esse pré-requisito.
- `VotoRequest` não valida se `id_associado` corresponde a um associado real (não há entidade de
  associado no sistema ainda) — qualquer `Long` positivo ou negativo é aceito como válido pelo
  Bean Validation (`@NotNull` apenas). Mencionar se slice futuro adicionar cadastro de associados.
