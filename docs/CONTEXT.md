# Contexto do projeto

## Spec

Épico H90-17 (votação de assembleia), 3 fatias. Fatia 1 (pautas) e fatia 2 (sessão)
entregues, fatia 2 já com um ciclo de correção de review. AC → teste:

| AC | Descrição | Teste(s) |
|---|---|---|
| AC-1.1–1.6 | Cadastro/consulta de pauta | `PautaControllerTest`, `PautaTest` |
| AC-2.1 | POST sessão com duração informada → 2xx com id | `SessaoControllerTest.abrir_comDuracaoInformada_retorna2xxComId` |
| AC-2.2 | Duração omitida/`null` → default 1 minuto | `SessaoControllerTest.abrir_semCorpo_...`/`abrir_comDuracaoNula_...`, `SessaoServiceTest.abrir_comDuracaoNula_assumeUmMinuto` |
| AC-2.3 | Duração <= 0 → 400 | `SessaoControllerTest.abrir_comDuracaoInvalida_retorna400`, `SessaoTest.validarDuracao_comDuracaoInvalida_lancaExcecao` |
| AC-2.4 | Pauta inexistente → 404 | `SessaoControllerTest.abrir_comPautaInexistente_retorna404` |
| AC-2.5 | Sessão já aberta → 409 | `SessaoControllerTest.abrir_comSessaoJaAberta_retorna409`; concorrência real (lock) em `SessaoConcorrenciaTest` |
| Should | Duração não inteira/não numérica (`"abc"`, `1.5`, `"5"`) → 400 | `SessaoControllerTest.abrir_comDuracaoNaoInteira_retorna400`, `abrir_comDuracaoComoStringNumerica_retorna400` |
| Review c.1 | 413/400/415/405 em `/pautas/{id}/sessao` | `SessaoControllerTest.abrir_comBodyMuitoGrande_retorna413` e `abrir_comIdNaoNumerico/ContentTypeNaoSuportado/buscarSessao_metodoNaoSuportado` |
| Review c.1 | Teto de duração (1440 min) | `SessaoTest.validarDuracao_comDuracaoNoTetoERejeitandoAcimaDoTeto` |
| Review c.1 | Construtor `Sessao` rejeita duração/abertura inválida | `SessaoTest.construtor_comDuracaoInvalida/AberturaNula_lancaExcecao` |

Decisões sobre NBSP/zero-width/emoji na fatia 1 não vêm do task.md — extrapolação da regra
"titulo obrigatório, não vazio". Ver Decisões.

## Módulos

- `domain.pauta`: `Pauta` (JPA, valida `titulo`/`descricao` no construtor), `PautaRepository`
  (`buscarPorId` + `buscarPorIdComLock` com `PESSIMISTIC_WRITE`, usado pelo fluxo de sessão),
  `PautaInvalidaException`, `PautaNaoEncontradaException`.
- `domain.sessao`: `Sessao` (JPA, `@ManyToOne` pra `Pauta`, valida duração — mín. 1, máx.
  `DURACAO_MAXIMA_MINUTOS` = 1440 — e abertura não nula no construtor; `validarDuracao`
  estático chamado pelo service antes de buscar a pauta, pra manter ordem 400→404→409),
  `SessaoRepository`, `SessaoInvalidaException`, `SessaoJaAbertaException`.
- `application.pauta`: `PautaService` (criar, buscarPorId). `application.sessao`:
  `SessaoService` (`abrir`, `@Transactional`, `Clock` injetado, usa `buscarPorIdComLock` pra
  serializar concorrência na mesma pauta).
- `api.pauta` / `api.sessao`: Controllers, `*ExceptionHandler` escopados (400 de domínio,
  404, 409, tipo de parâmetro, content-type — NUNCA `HttpMessageNotReadableException`, ver
  Convenções), DTOs em `dto/`. `api`: `TamanhoMaximoBodyFilter` (filtro global, 16 KB),
  `CorpoExcedeTamanhoMaximoHandler` (global, único lugar que trata
  `HttpMessageNotReadableException`), `MetodoNaoSuportadoHandler` (global, 405).
- `infrastructure.persistence`: `PautaJpaRepository` (`buscarPorIdComLock` via `@Lock` +
  `@Query`), `SessaoJpaRepository` (`findByPautaId`).
- `infrastructure.config`: `ClockConfig` (`Clock.systemUTC()`, substituído por `@MockBean
  Clock` nos testes), `JacksonConfig` (coerção customizada — ver Convenções).

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Maven, H2 em memória (`ddl-auto: update`), sem Flyway.
- `mvnd test` — suite completa (82 testes, ~2min; boot de Spring Context em
  `PautaControllerTest`/`SessaoControllerTest`/`SessaoConcorrenciaTest` e Tomcat real em
  `CorpoExcedeTamanhoMaximoE2ETest`).
- Sem Docker/rede externa: nada de Testcontainers. Teste e2e de chunked usa
  `@SpringBootTest(webEnvironment = RANDOM_PORT)` (Tomcat embarcado) + `java.net.Socket` crudo.
  Teste de concorrência (`SessaoConcorrenciaTest`) usa threads reais + `CountDownLatch` contra
  o H2 real, sem mocks de repositório — só assim o lock pessimista é exercitado de verdade.

## Convenções

- Regras de negócio no construtor das entidades (`Pauta`, `Sessao`), não em Bean Validation
  do DTO.
- **`HttpMessageNotReadableException` (JSON malformado ou 413 disfarçado) é tratada SÓ no
  `CorpoExcedeTamanhoMaximoHandler` global, nunca em handler escopado por controller.**
  Jackson embrulha qualquer exceção do `InputStream` (incluindo
  `CorpoExcedeTamanhoMaximoException`) nesse tipo, e um `@RestControllerAdvice` escopado tem
  precedência sobre o global. Bug real do review ciclo 1: `PautaExceptionHandler` e
  `SessaoExceptionHandler` cada um tinha handler próprio desse tipo, só o de `Pauta`
  desembrulhava a causa — `POST /pautas/{id}/sessao` com body > 16 KB respondia 400 em vez de
  413. Fix: desembrulho e decisão 400-ou-413 centralizados no global; escopados não tratam
  mais esse tipo. Catch em `TamanhoMaximoBodyFilter.doFilter` continua como defesa residual
  fora do fluxo do `DispatcherServlet`.
- `HttpRequestMethodNotSupportedException` (405) é lançada antes do Spring saber o controller
  → `MetodoNaoSuportadoHandler` é global, preenche `Allow` via `ex.getSupportedHttpMethods()`.
- Fatia 1: validação usa `isBlank()`/`strip()`/`codePointCount()` (não `trim()`/`length()`) e
  checagem de "caractere visível" por categoria Unicode — detalhes e gaps em Decisões.
- Fatia 2: ordem de validação em `SessaoService.abrir` é corpo (400) → pauta existe (404) →
  sessão aberta (409, checado iterando `sessaoRepository.buscarPorPautaId` e
  `Sessao.estaAberta(clock.instant())`). "Aberta" = `abertura.plus(duracaoMinutos,
  MINUTES).isAfter(agora)` — estritamente posterior, instante exato do fim já é encerrada.
- **`JacksonConfig`:** Jackson por padrão coage silenciosamente `Float→Integer` (`1.5`→`1`)
  e `String→Integer` (`"5"`→`5`). Ambos forçados a `CoercionAction.Fail` globalmente — campo
  numérico no contrato não deve aceitar truncamento nem string (decisão do review ciclo 1
  pro segundo caso). Qualquer `Integer`/`Long` novo em DTO herda a proteção.
- `SessaoControllerTest` substitui o bean `Clock` por `@MockBean Clock` — necessário pra
  AC-2.5 (duas aberturas, Clock avança determinística e controladamente) e pro cálculo de
  `fimEm` previsível.
- **Lock do 409 (review ciclo 1):** `SessaoService.abrir` é `@Transactional`, busca a `Pauta`
  via `buscarPorIdComLock` (`@Lock(PESSIMISTIC_WRITE)`). Antes, check-then-act sem lock nem
  transação deixava duas requisições concorrentes criarem duas sessões abertas. Decisão vs.
  `@Version` otimista: pessimista evita retry/exception-translation no caller, mais simples
  de raciocinar, custo de uma linha bloqueada por request (aceitável, volume baixo esperado).
  Provado com 10 threads reais em `SessaoConcorrenciaTest`.
- **Teto de duração (review ciclo 1):** `Sessao.DURACAO_MAXIMA_MINUTOS = 1440` (24h). Sem
  teto, `Integer` aceitava ~4000 anos — sem auth, qualquer request bloqueava a pauta com 409
  indefinidamente (DoS funcional). Valor de trabalho, não confirmado com PO.

## Decisões

- Fatia 1: defaults sem resposta do PO (`titulo` obrigatório máx. 200, `descricao` opcional
  máx. 1000, erro `{status, mensagem}`). Título só com Zs/Zl/Zp/Cf/Cc/unassigned/surrogate é
  rejeitado como "titulo e obrigatorio" (pauta fantasma). **Gap:** checagem por categoria
  Unicode, não visual real — Hangul Filler U+3164, Braille Blank U+2800 passam.
- Fatia 1: limite de body 16 KB via contagem real de bytes lidos, confirmado com e2e real
  contra Tomcat embarcado. HTTP/2 não testado.
- Fatia 2: código 409 (dúvida 1) e teto de duração (dúvida 3, 1440 min, definido pelo review)
  assumidos, não confirmados com PO. Reabertura após encerramento (dúvida 2) fora de escopo:
  bloqueio só enquanto `estaAberta`. `null`/ausente iguais, default 1 (dúvida 4). `Sessao` sem
  cascata de remoção de `Pauta` (fora de escopo, restrição do task.md).
- Sem auth/autorização e sem ofuscação de id (`IDENTITY` sequencial): decisão no README, não
  implementada. **Obrigatório revisitar antes da fatia 3** (IDOR por enumeração de id vira
  problema real com voto/resultado).

## Entregas

- Fatia 1/3 (pautas): AC-1.1–1.6, três ciclos de review (handler de erro, Unicode, limite de
  body).
- Fatia 2/3 (sessão): endpoint `POST /pautas/{id}/sessao`, `Sessao` com `Clock` injetável,
  `SessaoService` 400→404→409, `JacksonConfig` pra duração não inteira.
- Fatia 2/3, ciclo de correção de review (este ciclo): 413 centralizado no global (bug real
  corrigido), 415/405/400-param na rota de sessão com teste, lock pessimista +
  `@Transactional` + `SessaoConcorrenciaTest` pro 409 sob concorrência real, teto de duração
  1440 min, rejeição de string numérica, testes de construtor de `Sessao`.
- Suite completa verde: `mvnd test` → 82 testes, 0 falhas.

## Armadilhas

- `mvn`/`mvnd` imprime warnings de `jline`/`jansi` (`UnsatisfiedLinkError`) — inofensivo.
- Testes com `@SpringBootTest` sobem contexto/Tomcat real; primeira execução de cada ~40s
  (`SessaoConcorrenciaTest` incluso, pelo mesmo motivo, não pelas threads).
- MockMvc sempre deriva `Content-Length` do `byte[]` do payload — nunca produz chunked de
  verdade; e2e de chunked precisa de socket crudo contra servidor real.
- `@MockBean Clock` substitui o bean real do contexto inteiro do teste — toda chamada a
  `clock.instant()` depois do primeiro `when(...).thenReturn(...)` usa valores stubados; se a
  Nª chamada não tem stub cai em default do Mockito (`null`) e gera NPE — sempre stubar
  sequência quando o teste lê o clock mais de uma vez.
- `PESSIMISTIC_WRITE` fora de transação lança `TransactionRequiredException` — por isso
  `SessaoService.abrir` precisa de `@Transactional` junto com `buscarPorIdComLock`.

## Pendências

- Fatia 3 (voto/resultado) não implementada. `GET /pautas` (listagem, Could) não implementado.
- Dúvidas do task.md (fatia 2: código 409, reabertura, teto) seguem assumidas; confirmar com
  PO antes da fatia 3. Auth/id sequencial (ver Decisões): revisitar antes da fatia 3, não depois.
- `spring-boot-starter-validation` no `pom.xml` sem uso — remover ao tocar no `pom.xml`.
  `PautaNaoEncontradaException(Long id)`: parâmetro `id` ignorado (mensagem hardcoded).
- "Título fantasma" (fatia 1): checagem por categoria Unicode, não visual real — decisão de
  produto pendente sobre Hangul Filler/Braille Blank/etc.
- Fatia 2: sem encerramento automático por job — sessão "encerra" só por cálculo em tempo
  real. Fatia futura que precisar filtrar sessões encerradas por SQL direto vai precisar de
  coluna `fim` persistida + índice `(pauta_id, fim)` (apontado no review ciclo 1, fora das 11
  mudanças obrigatórias deste ciclo).
