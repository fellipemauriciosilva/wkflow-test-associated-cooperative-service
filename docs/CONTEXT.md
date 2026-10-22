# Contexto do projeto

## Módulos

- `com.acme.votacao` — classe main (`VotacaoPautasApplication`).
- `com.acme.votacao.pauta` — pacote único, Fatias 1+2: `Pauta`/`PautaRepository`/`PautaRequest`/`PautaResponse`/`PautaIdResponse`/`PautaService`/`PautaController`/`PautaNaoEncontradaException` (Fatia 1). `Sessao` (entidade JPA), `SessaoRepository`, `SessaoRequest`/`SessaoResponse` (DTOs), `SessaoService` (abertura com lock pessimista + isolamento fixado), `SessaoController` (`POST /pautas/{id}/sessao`), `SessaoJaAbertaException`, `ClockConfig` (bean `Clock.systemUTC()`), `GanchoConcorrenciaSessao`/`GanchoConcorrenciaSessaoNoOp` (ponto de extensão no-op em produção, usado só em teste para provar o lock) (Fatia 2). `ErroResponse` (corpo padronizado de erro) e `GlobalExceptionHandler` compartilhados pelas duas fatias, cobrindo também 415/405/404-de-rota/500.

## Stack e comandos

- Java 21, Spring Boot 3.3.4 (web, data-jpa, validation), H2 em memória, Maven.
- Build/teste: `mvnd test` (sem `clean`). Build isolado: `mvnd compile`.
- Rodar local: `mvnd spring-boot:run`. Com console H2: `mvnd spring-boot:run -Dspring-boot.run.profiles=dev`.
- H2: `jdbc:h2:mem:votacao`, `ddl-auto: update`, sem migração (Flyway não usado — fora de escopo para greenfield pequeno com H2 em memória).

## Convenções

- DTOs como `record` (Java 21), sem Lombok (não adicionado ao pom).
- Validação via Bean Validation (`@NotBlank`/`@Size(max=255)` em `PautaRequest`; `@Positive` + `@Max(1440)` em `SessaoRequest.duracaoMinutos`). `GlobalExceptionHandler` captura `MethodArgumentNotValidException`/`HttpMessageNotReadableException`/`MethodArgumentTypeMismatchException` → 400, `PautaNaoEncontradaException` → 404, `SessaoJaAbertaException` → 409, `HttpMediaTypeNotSupportedException` → 415, `HttpRequestMethodNotSupportedException` → 405, `NoResourceFoundException` → 404, `Exception` genérico → 500 (com log do stack trace no servidor, mensagem fixa no corpo). Corpo de erro agora é `ErroResponse` (`timestamp`, `status`, `error`, `message`, `path`) para **todos** os handlers (RF-12 implementado nesta fatia, retroagindo também sobre os erros da Fatia 1 — README atualizado, `PautaControllerTest` cobre 400/404 com asserts de corpo completo).
- Handlers de 400 nunca usam `ex.getMessage()` bruto: `handleValidacao` monta `message` a partir de `field` + `defaultMessage` de cada `FieldError` (`ex.getBindingResult().getFieldErrors()`); `handleTipoInvalido` usa mensagem fixa com só `ex.getName()`. Evita vazar nomes de classe/método internos do Spring ao cliente.
- `GlobalExceptionHandler` injeta `Clock` (não usa `Instant.now()` direto) para que `timestamp` do erro seja determinístico em teste — provado em `SessaoControllerTest.erro_timestamp_vemDoClockInjetado` e no assert exato de timestamp em `abrir_sessaoJaAberta_retorna409`.
- `spring.jackson.deserialization.accept-float-as-int: false` em `application.yml`: sem isso, Jackson trunca `1.5` para `1` silenciosamente em campo `Integer`; com a flag, lança `InvalidFormatException` → `HttpMessageNotReadableException` → 400 (mesmo tratamento de `"abc"` em campo numérico).
- `Clock` é bean de produção (`ClockConfig`, `Clock.systemUTC()`), nunca `Instant.now()` direto em código de produção — só assim os testes conseguem fixar "agora" (`Clock.fixed` em teste de serviço; `MutableClock` + `@TestConfiguration TestClockConfig` com `@Primary` em teste de controller, permite avançar o tempo entre requisições no mesmo teste MockMvc).
- Pacote único `pauta` (sem camadas `api/application/domain/infrastructure` da skill genérica — escopo pequeno não justificou a separação).
- H2 console desabilitado no profile padrão (`spring.h2.console.enabled: false`); habilitado só sob profile `dev`.
- Limite de 255 em `titulo`/`descricao` conta code units UTF-16 (ver README) — detalhe da Fatia 1, sem mudança.

## Decisões

- Concorrência na abertura de sessão: lock pessimista (`EntityManager.find(Pauta.class, id, LockModeType.PESSIMISTIC_WRITE)`) dentro de `@Transactional(isolation = Isolation.READ_COMMITTED) SessaoService.abrir`, não constraint de unicidade nem verificação otimista. Escolhido porque reabertura após sessão encerrada é permitida (RF-05 fala só em "sessão aberta"), então não há coluna/combinação sempre-única para constraint simples. A segunda transação concorrente bloqueia no `find` até a primeira commitar; ao destravar, já enxerga a sessão recém-inserida via `findFirstByPautaIdAndFimAfter` e lança `SessaoJaAbertaException` (409). Isolamento fixado explicitamente em READ_COMMITTED (não herda o default do datasource): em REPEATABLE READ/SERIALIZABLE o snapshot poderia ser fixado antes do lock, fazendo a segunda transação não ver a sessão da primeira mesmo após o lock liberar — ver README.
- Prova de concorrência determinística: `GanchoConcorrenciaSessao` (no-op em produção, `GanchoConcorrenciaSessaoNoOp`) é chamado por `SessaoService.abrir` logo após obter o lock. Em teste, `GanchoConcorrenciaSessaoControlavel` (bean `@Primary`) retém a primeira thread que chega ali até o teste liberar, permitindo confirmar que uma segunda chamada concorrente não alcança o mesmo ponto enquanto a primeira está retida — prova real de bloqueio, não probabilística. Confirmado manualmente: trocando `PESSIMISTIC_WRITE` por `NONE`, `SessaoControllerTest.abrir_duasChamadasConcorrentes_exatamenteUmaSucede` falha de forma reproduzível.
- "Sessão aberta" = existe `Sessao` com `pautaId` e `fim > agora` (sem scheduler, comparação pura por `Clock` injetado). Fechamento por tempo é implícito: não há job que marca sessão como fechada.
- `SessaoController` mapeado em `/pautas/{id}/sessao` (não em classe `Sessao` própria de rota) — reaproveita `@RequestMapping("/pautas")` como prefixo, controller separado de `PautaController` por classe (não por rota), seguindo a separação por agregado.
- `PESSIMISTIC_WRITE` via `EntityManager` direto (não `@Lock` em método de `PautaRepository`) para não tocar em `PautaRepository`/`PautaService` da Fatia 1 (fora de escopo, ver task.md seção 2).
- RF-12 (corpo de erro padronizado) implementado nesta fatia, embora fosse "Should" na Fatia 1 e "Should" aqui — decisão: já que o handler precisava mudar para `SessaoJaAbertaException` (409), estender para todos os handlers teve custo marginal baixo e evita dois formatos de erro coexistindo na mesma API.
- Body de `POST /pautas/{id}/sessao` ausente, `{}` ou `{"duracaoMinutos": null}` tratados identicamente como "omitido" (padrão 1 min) — `@RequestBody(required = false)` + fallback manual em `SessaoController.abrir`, pois Spring não injeta body vazio como objeto com campo null automaticamente quando não há content-type/body.
- Reabertura após sessão encerrada permitida sem 409 (confirma premissa do task.md seção 4) — AC-2.6 testado literalmente.

## Entregas

- Fatia 1 (sem alteração nesta rodada, exceto corpo de erro): ver histórico de commits anteriores a este.
- Fatia 2: `POST /pautas/{id}/sessao` com 201/400/404/409, AC-2.1 a AC-2.6 cobertos.
- Ciclo de correção pós-review (10 mudanças obrigatórias, todas aplicadas):
  - Teste de concorrência tornado determinístico via `GanchoConcorrenciaSessao`/`GanchoConcorrenciaSessaoControlavel` (barreira real entre as duas transações); `ExecutorService` com try/finally + `shutdownNow()`.
  - `PautaControllerTest` ganhou asserts de `$.status`/`$.error`/`$.path`/`$.timestamp` em 400 e 404.
  - `SessaoControllerTest.erro_timestamp_vemDoClockInjetado` prova timestamp vindo do `Clock`, não do relógio real; 409 ganhou asserts completos de `error`/`message`/`timestamp`.
  - `SessaoRequest.duracaoMinutos` ganhou `@Max(1440)`; testes cobrem teto exato (1440 → 201), acima por pouco (1441 → 400) e `Integer.MAX_VALUE` (400, zero sessões).
  - `SessaoService.abrir` com `@Transactional(isolation = Isolation.READ_COMMITTED)` explícito; README documenta a limitação em outros níveis de isolamento.
  - `GlobalExceptionHandler` não usa mais `ex.getMessage()` em handlers de 400; cobre também 415/405/404-de-rota/500 com `ErroResponse`.
  - CONTEXT.md corrigido: contaminação de teste é só entre métodos de `SessaoControllerTest`, não entre classes.
  - `SessaoServiceTest` (JUnit 5 + Mockito, 4 testes): duração padrão, duração informada, pauta inexistente, sessão já aberta.
  - `SessaoControllerTest` (MockMvc + H2 real + `MutableClock`, 19 testes): sem body, body `{}`, duração informada, `duracaoMinutos: null`, 409 em sessão já aberta (corpo completo), 404 pauta inexistente, 400 em zero/negativo/`1.5`/`"abc"`/acima-do-teto/no-teto/acima-por-pouco, reabertura após `Clock` avançar além do `fim`, teste de concorrência determinístico, timestamp via Clock, 415/405/404-de-rota.
  - Suite completa: 40 testes (14 `PautaControllerTest` + 3 `PautaServiceTest` + 19 `SessaoControllerTest` + 4 `SessaoServiceTest`), `mvnd test` → BUILD SUCCESS.

## Armadilhas

- `mvnd`/`mvn` neste ambiente resolvem dependências via proxy interno (`orqaliz-artifact-proxy`); primeira execução de `mvnd test` é lenta (download de plugins/deps). Sem internet real — se um artefato não existir no proxy, não há como obtê-lo de outra forma.
- Warnings de `jlinenative`/`jansi` ao rodar `mvnd` no console são ruído do daemon (terminal "dumb"), não indicam falha de build.
- `@Size(max=255)` sem `@Column(length=255)` explícito funciona por coincidência (default do Hibernate também é 255); se um dos dois limites mudar sozinho, o campo que passa a validação mas excede a coluna gera 500 em vez de 400. Sempre mudar os dois juntos.
- `SessaoControllerTest` e `PautaControllerTest` usam contextos Spring distintos (`@Import(TestClockConfig.class, ...)` só em `SessaoControllerTest`, cada classe de teste `@SpringBootTest` ganha contexto próprio). A contaminação real é só **entre métodos da mesma classe** `SessaoControllerTest`: sem rollback entre testes (`@SpringBootTest` sem `@Transactional` de teste), `sessaoRepository.count()` global soma writes de métodos anteriores. Usar `countByPautaId(pautaId)` escopado à pauta local do `@BeforeEach`, nunca `count()` sozinho, em asserções de "nenhuma sessão criada".
- Teste de concorrência exige retenção real da primeira transação dentro do código de produção (via `GanchoConcorrenciaSessao`) para ser determinístico: duas threads MockMvc batendo ao mesmo tempo sem barreira podem não se sobrepor de fato (o lock do H2 é rápido o bastante para a segunda nem notar contenção), dando teste flaky que passa com ou sem o lock. A barreira precisa estar *depois* do `entityManager.find(..., PESSIMISTIC_WRITE)` e *antes* do commit.
- Jackson trunca float para int silenciosamente por padrão (`ACCEPT_FLOAT_AS_INT` habilitado); sem desabilitar via `spring.jackson.deserialization.accept-float-as-int: false`, `1.5` em campo `Integer` vira `1` sem erro, e o AC-2.5 (400 para duração não inteira) falha silenciosamente.

## Pendências

- Fatia 3 (votos/resultado) do épico `run_2057fd08c4e4`, fora do escopo desta entrega.
- RF-13 (listagem paginada) ainda não implementado (Could, fora de escopo em ambas as fatias).
- Teto de `duracaoMinutos` fixado em 1440 (24h) nesta correção — provisório, não confirmado com o PO. Ajustar via `SessaoRequest.DURACAO_MAXIMA_MINUTOS` quando o PO decidir o valor definitivo.
- Decisão de reabertura após fechamento (permitida) afeta AC-3.3 da Fatia 3 (sessão fechada vs pauta sem sessão) — confirmar antes de iniciar a Fatia 3.
- Confirmar com PO o limite de 255 code units (UTF-16) em `titulo`/`descricao` (herdado da Fatia 1, ainda não confirmado).
- Itens não bloqueantes do review de segurança, fora de escopo, revisitar antes de qualquer deploy com exposição real: limite de tamanho de corpo de requisição, auth/rate-limit em `POST /pautas` e `POST /pautas/{id}/sessao`, trim de espaços em `titulo`/`descricao`, ids sequenciais enumeráveis (IDOR se pautas/sessões ganharem regra de visibilidade).
- Itens não bloqueantes do ciclo de correção, fora deste merge: índice `(pauta_id, fim)` e FK em `sessao.pauta_id` (sem índice, `findFirstByPautaIdAndFimAfter` fica mais lento conforme a tabela cresce; sem FK, não há integridade referencial garantida pelo banco); centralizar `@Import(TestClockConfig.class)` numa base class de teste se mais classes precisarem de `MutableClock`.
