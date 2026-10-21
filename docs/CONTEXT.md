# Contexto do projeto — votacao-api

Épico: H90-15-EPICO / run_bfe1226524a9. Slice 1/3, ciclo de correção de review aplicado.

## Módulos

- `com.votacao` — `Application` (bean `Clock` system default).
- `com.votacao.domain.pauta` — entidade `Pauta`, `PautaRepository`, `PautaNotFoundException`.
- `com.votacao.domain.sessao` — entidade `Sessao` (constante `UK_PAUTA_ID` com nome da
  constraint única), `SessaoRepository`, `SessaoJaAbertaException`.
- `com.votacao.application.pauta` — `PautaService` (criar, buscarPorId).
- `com.votacao.application.sessao` — `SessaoService` (abrir, `@Transactional`, `Clock` injetado).
- `com.votacao.api.pauta` — `PautaController` + DTOs (`PautaRequest` com `@Size(max=255)` em
  `titulo`/`descricao`, `PautaResponse`).
- `com.votacao.api.sessao` — `SessaoController` + DTOs (`SessaoRequest`, `SessaoResponse`).
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
  domínio (`pauta`, `sessao`). Testes de integração ponta-a-ponta ficam em
  `com.votacao.integration` (não espelham pacote de produção, pois cruzam camadas).
- Entidades JPA imutáveis do ponto de vista de API pública: sem setters, construtor com
  campos obrigatórios, construtor protegido sem args para o Hibernate.
- DTOs como `record`. Campo `duracao_minutos` em JSON mapeado via `@JsonProperty` em
  `SessaoRequest` (campo Java `duracaoMinutos`).
- Erros: todas as exceções (de domínio e de infraestrutura/framework) capturadas em
  `ApiErrorHandler`, resposta sempre `{"error", "timestamp", "path"}`. Cobertura inclui
  `HttpMessageNotReadableException` (400, JSON malformado), `MethodArgumentTypeMismatchException`
  (400, path/query param com tipo errado), `NoHandlerFoundException`/`NoResourceFoundException`
  (404, rota inexistente), `HttpRequestMethodNotSupportedException` (405, verbo não suportado) e
  `Exception` genérica (500, fallback) — handler catch-all deve ficar por último na classe para
  não mascarar os mais específicos.
- Testes de service: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), `Clock.fixed`
  para determinismo em `SessaoServiceTest`.
- Testes de controller: `@SpringBootTest` + `@AutoConfigureMockMvc`. **`MockMvc` com contexto
  Spring Boot completo NÃO aplica `server.servlet.context-path` automaticamente** — o
  `DispatcherServlet` de teste roda sem o prefixo. Por isso os testes `PautaControllerTest` e
  `SessaoControllerTest` usam paths sem `/api` (`/pautas/...`) e o campo `$.path` do erro também
  sai sem o prefixo (`/pautas`, não `/api/pautas`). Isso é uma limitação do ambiente de teste, não
  do comportamento em produção.
- Para validar o comportamento real sob `/api` (prefixo de contexto aplicado de fato, inclusive no
  campo `path` do erro) use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
  — ver `com.votacao.integration.ContextPathIntegrationTest`.
- Teste de concorrência real (múltiplas threads batendo no mesmo endpoint) usa
  `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` + `ExecutorService` — ver
  `com.votacao.integration.SessaoConcorrenciaIntegrationTest`. Testes unitários com mock de
  `DataIntegrityViolationException` continuam existindo para cobrir a lógica de tradução de
  exceção isoladamente, mas não provam a constraint de BD por si só.

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
- `PautaRequest.titulo`/`descricao` limitados a 255 caracteres via `@Size(max = 255)`, alinhado ao
  `length = 255` explícito nas colunas de `Pauta`. Sem o limite, payload grande causava
  `DataIntegrityViolationException` não capturada (500) em vez de 400.
- Duração de sessão: sem teto superior implementado (épico não define limite). Pendência aberta,
  listada abaixo.

## Entregas

- Slice 1/3: CRUD básico de pauta (criar, consultar) + abertura de sessão de votação com duração
  parametrizável. 32 testes (unitários + MockMvc + integração com servidor real), suite verde.
- Endpoints: `POST /api/pautas`, `GET /api/pautas/{id}`, `POST /api/pautas/{id}/sessao`.
- Ciclo de correção de review aplicado: formato de erro padrão estendido a toda exceção não
  prevista (JSON malformado, tipo de parâmetro inválido, rota/método inexistente, erro genérico),
  `@Size` em `PautaRequest`, `@Transactional` + tradução seletiva de constraint em `SessaoService`,
  testes de integração reais para context-path e concorrência.

## Armadilhas

- `/tmp` noexec quebra jansi/jline do `mvnd`/`mvn` CLI — ver seção Stack e comandos acima
  (usar `JAVA_TOOL_OPTIONS`, não só flags `-D` do `mvnd`).
- `SessaoRequest` com corpo HTTP vazio (sem `Content-Type`/body): controller trata `request == null`
  criando `SessaoRequest(null)` manualmente, pois `@RequestBody(required = false)` com body
  ausente chega como `null` em vez de lançar erro.
- MockMvc não aplica `server.servlet.context-path` — não assumir isso em testes futuros de
  controller; usar integração com servidor real quando o comportamento do prefixo importar.
- Nomear `@UniqueConstraint` explicitamente nem sempre garante que o nome apareça na mensagem de
  erro do H2 (pode reportar um índice interno autogerado tipo `CONSTRAINT_INDEX_9`). Detecção de
  "qual constraint violou" via `getMostSpecificCause().getMessage()` deve ter um fallback por
  tabela/coluna além do nome.

## Pendências

- Limite superior para `duracao_minutos`: não implementado, não definido pelo épico. Confirmar
  com stakeholder antes do merge final (mencionado no `task.md` original como questão em aberto).
- Sem autenticação/autorização: qualquer requisição cria pauta e abre sessão. Pré-requisito a
  avaliar antes do slice 2 (voto), que precisa de identidade de quem vota.
- `SessaoResponse.from` hardcoda `status = "ABERTA"`. Ok enquanto não existe fechamento de sessão;
  derivar do estado real (comparando `fechamento` com `now`) deve entrar no slice de resultado.
- Pré-requisito para slice 2 (voto): `Pauta` e `Sessao` estáveis, `PautaRepository.findById`
  disponível — atendido.
