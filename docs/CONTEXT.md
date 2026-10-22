# Contexto do projeto

## Spec

Épico H90-17 (votação de assembleia), 3 fatias, todas entregues. Fatia 2 com um ciclo de
correção de review; fatia 3 (este ciclo) sem review ainda. AC → teste:

| AC | Descrição | Teste(s) |
|---|---|---|
| AC-1.1–1.6 | Cadastro/consulta de pauta | `PautaControllerTest`, `PautaTest` |
| AC-2.1–2.5 | Sessão: duração/default/inválida, pauta 404, já aberta 409 | `SessaoControllerTest`, `SessaoServiceTest`, `SessaoTest`, `SessaoConcorrenciaTest` |
| Review c.1 | 413/415/405/teto 1440min/duração não inteira na rota de sessão | `SessaoControllerTest.abrir_comBodyMuitoGrande...`/validações de parâmetro, `SessaoTest.validarDuracao...` |
| AC-3.1/3.8 | POST voto 201; pauta inexistente 404 | `VotoControllerTest`, `VotoServiceTest` |
| AC-3.2/3.9 | Voto fora de SIM/NAO e associado inválido → 400 | `VotoControllerTest`, `VotoTest` |
| AC-3.3/3.4 | Sem sessão aberta ou após fim (exclusivo) → 409 | `VotoControllerTest`, `VotoServiceTest` |
| AC-3.5 | Segundo voto → 409; constraint única no banco | `VotoControllerTest`, `VotoServiceTest`, `VotoJpaRepositoryTest` |
| AC-3.6/3.7 | GET resultado: contagens + situação; pauta inexistente 404 | `VotoControllerTest`, `VotoServiceTest` |

Decisões sobre NBSP/zero-width/emoji na fatia 1 (extrapolação, não vêm do task.md) ver Decisões.

## Módulos

- `domain.pauta`/`application.pauta`: `Pauta` (JPA, valida `titulo`/`descricao` no
  construtor), `PautaRepository` (`buscarPorId` + `buscarPorIdComLock` PESSIMISTIC_WRITE,
  usado pelo fluxo de sessão), `PautaInvalidaException`, `PautaNaoEncontradaException`,
  `PautaService` (criar, buscarPorId).
- `domain.sessao`/`application.sessao`: `Sessao` (JPA, `@ManyToOne` pra `Pauta`, valida
  duração — mín. 1, máx. `DURACAO_MAXIMA_MINUTOS`=1440 — e abertura no construtor;
  `validarDuracao` estático, chamado antes de buscar pauta, ordem 400→404→409),
  `SessaoRepository`, `SessaoInvalidaException`, `SessaoJaAbertaException`, `SessaoService`
  (`abrir`, `@Transactional`, `Clock` injetado, `buscarPorIdComLock` serializa concorrência).
- `api.pauta`/`api.sessao`: Controllers, `*ExceptionHandler` escopados (400/404/409/tipo de
  parâmetro/content-type — NUNCA `HttpMessageNotReadableException`, ver Convenções), DTOs em
  `dto/`. `api`: `TamanhoMaximoBodyFilter` (global, 16 KB), `CorpoExcedeTamanhoMaximoHandler`
  (global, único que trata `HttpMessageNotReadableException`), `MetodoNaoSuportadoHandler`
  (global, 405).
- `domain.voto` / `application.voto`: `Voto` (JPA, constraint única `uk_voto_associado_pauta`
  em `(associado, pauta_id)`, valida `associado`/`tipoVoto` no construtor), `TipoVoto` (enum
  SIM/NAO), `VotoRepository`, `ResultadoVotacao` (record calculado, não persistido —
  `situacao` ∈ aberta/encerrada/`nao_iniciada`), `VotoInvalidoException`,
  `VotoDuplicadoException`, `SessaoNaoAbertaException`. `VotoService` (`registrar`,
  `resultado`, `@Transactional` em `registrar`, `Clock` injetado): ordem pauta (404) →
  `new Voto(...)` (400) → sessão aberta (409) → duplicado em memória (409) → `salvar`
  captura `DataIntegrityViolationException` da constraint e remapeia pro mesmo 409.
- `api.voto` / `api.voto.dto`: `VotoController` (`POST /pautas/{id}/votos`,
  `GET /pautas/{id}/resultado`), `VotoExceptionHandler` (mesmo padrão de Pauta/Sessão — não
  trata `HttpMessageNotReadableException`), `VotoRequest`, `VotoResponse`, `ResultadoResponse`.
- `infrastructure.persistence`: `PautaJpaRepository` (`buscarPorIdComLock` via `@Lock` +
  `@Query`), `SessaoJpaRepository`, `VotoJpaRepository` (ambos `findByPautaId`;
  `VotoRepositoryImpl.salvar` usa `saveAndFlush` pra a constraint disparar dentro da
  transação do service, não só no commit).
- `infrastructure.config`: `ClockConfig` (`Clock.systemUTC()`, substituído por `@MockBean
  Clock` nos testes), `JacksonConfig` (coerção customizada — ver Convenções).

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Maven, H2 em memória (`ddl-auto: update`), sem Flyway.
- `mvnd test` — suite completa (116 testes, ~3min; boot de Spring Context em
  `PautaControllerTest`/`SessaoControllerTest`/`SessaoConcorrenciaTest`/`VotoControllerTest`,
  Tomcat real em `CorpoExcedeTamanhoMaximoE2ETest`, `@DataJpaTest` em `VotoJpaRepositoryTest`).
- Sem Docker/rede externa: nada de Testcontainers. E2e de chunked usa
  `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `java.net.Socket` crudo.
  `SessaoConcorrenciaTest` usa threads reais + `CountDownLatch` contra H2 real, sem mock de
  repositório (único jeito de exercitar o lock pessimista de verdade). `VotoJpaRepositoryTest`
  usa `@DataJpaTest` (H2 real) pra provar a constraint única no banco, não só o check em memória.

## Convenções

- Regras de negócio no construtor das entidades (`Pauta`, `Sessao`, `Voto`), não em Bean
  Validation do DTO.
- **`HttpMessageNotReadableException` (JSON malformado, enum inválido, ou 413 disfarçado) é
  tratada SÓ no `CorpoExcedeTamanhoMaximoHandler` global, nunca em handler escopado** —
  `@RestControllerAdvice` escopado tem precedência sobre o global; bug real do review ciclo 1
  de sessão (413 virando 400) corrigido centralizando o desembrulho no global.
  `Pauta`/`Sessao`/`Voto` ExceptionHandler nunca tratam esse tipo.
- `HttpRequestMethodNotSupportedException` (405) é lançada antes do Spring saber o controller
  → `MetodoNaoSuportadoHandler` é global, preenche `Allow` via `ex.getSupportedHttpMethods()`.
- Fatia 1: validação usa `isBlank()`/`strip()`/`codePointCount()` (não `trim()`/`length()`) e
  checagem de "caractere visível" por categoria Unicode — detalhes e gaps em Decisões.
- Fatia 2: ordem em `SessaoService.abrir` é corpo (400) → pauta existe (404) → sessão aberta
  (409). "Aberta" = `abertura.plus(duracaoMinutos, MINUTES).isAfter(agora)` — fim exclusivo,
  mesma regra usada em `Sessao.estaAberta` por `VotoService` (fatia 3).
- **`JacksonConfig`:** Jackson coage silenciosamente `Float→Integer` e `String→Integer`,
  forçados a `CoercionAction.Fail` globalmente — qualquer `Integer`/`Long` novo em DTO herda.
- `*ControllerTest` substitui o bean `Clock` por `@MockBean Clock`.
- **Lock do 409 de sessão (review ciclo 1):** `SessaoService.abrir` é `@Transactional` +
  `buscarPorIdComLock` (`PESSIMISTIC_WRITE`) pra serializar requisições na mesma pauta,
  decisão vs. `@Version` otimista (evita retry no caller). Provado com 10 threads reais em
  `SessaoConcorrenciaTest`. Teto `Sessao.DURACAO_MAXIMA_MINUTOS = 1440` (sem ele, DoS
  funcional) — não confirmado com PO.
- Fatia 3: ordem em `VotoService.registrar` é pauta (404) → `Voto` (400) → sessão aberta
  (409) → duplicado (409) — INVERTIDA vs. `SessaoService.abrir`, por spec própria de cada
  fatia; não unificar sem PO. Duplicata concorrente: check em memória cobre caso comum,
  constraint única (`uk_voto_associado_pauta`) é defesa real sob corrida — `salvar` usa
  `saveAndFlush` pra violação disparar no `try/catch`. Sem teste de concorrência real; cobertura só via mock.

## Decisões

- Fatia 1: defaults sem resposta do PO (`titulo` obrigatório máx. 200, `descricao` opcional
  máx. 1000, erro `{status, mensagem}`). Título só com Zs/Zl/Zp/Cf/Cc/unassigned/surrogate é
  rejeitado (pauta fantasma). **Gap:** checagem por categoria Unicode, não visual real —
  Hangul Filler U+3164, Braille Blank U+2800 passam. Limite de body 16 KB via contagem real
  de bytes, confirmado com e2e contra Tomcat embarcado; HTTP/2 não testado.
- Fatia 2: código 409, teto de duração (1440 min) e default 1 min (`null`/ausente) assumidos,
  não confirmados com PO. Reabertura após encerramento fora de escopo. `Sessao` sem cascata
  de remoção de `Pauta` (restrição do task.md).
- Sem auth/autorização e sem ofuscação de id (`IDENTITY` sequencial): decisão no README, não
  implementada. Fatia 3 entregue sem revisitar — IDOR por enumeração de id é problema real
  agora que voto/resultado existem. Ver Pendências.
- Fatia 3: dúvidas 1–6 do task.md assumidas como sugerido no ticket (409 p/ sem sessão/após
  fim, zeros + `"nao_iniciada"` sem sessão nenhuma, 201, fim exclusivo, `associado` string
  livre, resultado usa sessão mais recente). Nenhuma confirmada com PO.

## Entregas

- Fatia 1/3 (pautas): AC-1.1–1.6, três ciclos de review (handler de erro, Unicode, body).
- Fatia 2/3 (sessão): `POST /pautas/{id}/sessao`, `SessaoService` 400→404→409; ciclo de
  review: 413 centralizado no global, 415/405/400-param, lock pessimista +
  `SessaoConcorrenciaTest`, teto 1440 min, rejeição de string numérica.
- Fatia 3/3 (votação/resultado, este ciclo): `POST /pautas/{id}/votos`,
  `GET /pautas/{id}/resultado`. `Voto` com constraint única no banco, `VotoService`
  404→400→409→409, situação derivada do tempo, usa sessão mais recente. Sem review ainda.
- Suite completa verde: `mvnd test` → 116 testes, 0 falhas.

## Armadilhas

- `mvn`/`mvnd` imprime warnings de `jline`/`jansi` (`UnsatisfiedLinkError`) — inofensivo.
- `@SpringBootTest`/`@DataJpaTest` sobem contexto/Tomcat real; primeira execução de cada ~40s.
- MockMvc sempre deriva `Content-Length` do `byte[]` do payload — nunca produz chunked de
  verdade; e2e de chunked precisa de socket crudo contra servidor real.
- `@MockBean Clock` substitui o bean real do contexto inteiro — stubar sequência de retornos
  se o teste lê o clock mais de uma vez, senão Nª chamada sem stub vira NPE.
- `PESSIMISTIC_WRITE` fora de transação lança `TransactionRequiredException` — por isso
  `SessaoService.abrir` precisa de `@Transactional` junto com `buscarPorIdComLock`.
- `VotoRepositoryImpl.salvar` usa `saveAndFlush`, não `save` — com `save` a constraint só
  dispararia no commit, depois do método já ter retornado 201.

## Pendências

- Sem auth/autorização: `associado` string livre não validada, id sequencial sem ofuscação —
  qualquer um vota como qualquer associado e lê resultado de qualquer pauta. Discutir
  produto/segurança antes de expor.
- `GET /pautas` (Could fatia 1) e `ultimaSessao` no resultado (Could fatia 3) não implementados.
- Fatia 3 sem ciclo de review. Candidatos: duplicata concorrente só via mock (sem teste real
  tipo `SessaoConcorrenciaTest`); `associado` string livre vs. ID numérico (dúvida 5);
  `"nao_iniciada"` é extrapolação não confirmada. Dúvidas 1-6 do task.md seguem assumidas.
- `spring-boot-starter-validation` no `pom.xml` sem uso. `PautaNaoEncontradaException(Long
  id)`: parâmetro ignorado (mensagem hardcoded). "Título fantasma" (fatia 1): checagem por
  categoria Unicode, não visual real — pendente decisão de produto.
- Fatia 2: sem encerramento automático por job, só cálculo em tempo real.
