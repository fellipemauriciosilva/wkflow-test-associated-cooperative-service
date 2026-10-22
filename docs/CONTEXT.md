# Contexto do projeto

## Spec

Épico H90-17 (votação de assembleia), 3 fatias, todas entregues. Fatias 2 e 3 cada uma com um ciclo de correção de review (fatia 3 corrigida neste commit, CHANGES_REQUIRED, 13 itens — ver Entregas). AC → teste:

| AC | Descrição | Teste(s) |
|---|---|---|
| AC-1.1–1.6 | Cadastro/consulta de pauta | `PautaControllerTest`, `PautaTest` |
| AC-2.1–2.5 | Sessão: duração/default/inválida, pauta 404, já aberta 409 | `SessaoControllerTest`, `SessaoServiceTest`, `SessaoTest`, `SessaoConcorrenciaTest` |
| Review c.1 | 413/415/405/teto 1440min/duração não inteira na rota de sessão | `SessaoControllerTest.abrir_comBodyMuitoGrande...`/validações de parâmetro, `SessaoTest.validarDuracao...` |
| AC-3.1/3.8 | POST voto 201 (body com id/pautaId/voto); pauta inexistente 404 | `VotoControllerTest.votar_comSessaoAbertaEAssociadosDistintos_retorna201`/`votar_comPautaInexistente_retorna404`, `VotoServiceTest.registrar_comSessaoAbertaEVotoSim_salvaVoto`/`registrar_comPautaInexistente_lancaExcecao` |
| AC-3.2/3.9 | Voto fora de SIM/NAO e associado inválido (nulo/vazio/>50/emoji) → 400 | `VotoControllerTest.votar_comValorDeVotoInvalido_retorna400`/`votar_comAssociadoInvalido_retorna400`, `VotoTest.construtor_comAssociadoInvalido_lancaExcecao`/`construtor_comAssociadoComEmojisAcimaDoLimite_lancaExcecao` |
| AC-3.3/3.4 | Sem sessão aberta ou após fim (exclusivo) → 409 | `VotoControllerTest.votar_semSessaoAberta_retorna409`/`votar_aposFimDaSessao_retorna409`, `VotoServiceTest.registrar_comInstanteIgualAoFim_lancaExcecao` |
| AC-3.5 | Segundo voto (inclusive apos strip) → 409; constraint única no banco; violação de outra coluna propaga | `VotoControllerTest.votar_comAssociadoQueJaVotou_retorna409`, `VotoServiceTest.registrar_comAssociadoQueJaVotou_lancaExcecao`/`registrar_comAssociadoComEspacoQueJaVotouStripado_lancaExcecao`/`registrar_comConstraintViolada_lancaVotoDuplicado`/`registrar_comViolacaoDeOutraColuna_relanca`, `VotoJpaRepositoryTest` |
| AC-3.6/3.7 | GET resultado: contagens + situação; pauta inexistente 404 | `VotoControllerTest.resultado_comSessaoAberta_retornaContagensESituacao`/`resultado_comPautaInexistente_retorna404`, `VotoServiceTest.resultado_comPautaInexistente_lancaExcecao` |
| Review c.2 | 400 (id não numérico)/415/413 na rota de voto; ordem 404→400 | `VotoControllerTest.votar_comIdNaoNumerico_retorna400`/`resultado_comIdNaoNumerico_retorna400`/`votar_comContentTypeNaoSuportado_retorna415`/`votar_comBodyMuitoGrande_retorna413`, `VotoServiceTest.registrar_comPautaInexistenteEAssociadoInvalido_lancaPautaNaoEncontrada` |

Decisões sobre NBSP/zero-width/emoji na fatia 1 (extrapolação, não vêm do task.md) ver Decisões.

## Módulos

- `domain.pauta`/`application.pauta`: `Pauta` (JPA, valida `titulo`/`descricao` no construtor), `PautaRepository` (`buscarPorId` + `buscarPorIdComLock` PESSIMISTIC_WRITE, usado pelo fluxo de sessão), `PautaInvalidaException`, `PautaNaoEncontradaException`, `PautaService` (criar, buscarPorId).
- `domain.sessao`/`application.sessao`: `Sessao` (JPA, `@ManyToOne` pra `Pauta`, valida duração — mín. 1, máx. `DURACAO_MAXIMA_MINUTOS`=1440 — e abertura no construtor; `validarDuracao` estático, chamado antes de buscar pauta, ordem 400→404→409), `SessaoRepository`, `SessaoInvalidaException`, `SessaoJaAbertaException`, `SessaoService` (`abrir`, `@Transactional`, `Clock` injetado, `buscarPorIdComLock` serializa concorrência).
- `api.pauta`/`api.sessao`: Controllers, `*ExceptionHandler` escopados (400/404/409/tipo de parâmetro/content-type — NUNCA `HttpMessageNotReadableException`, ver Convenções), DTOs em `dto/`. `api`: `TamanhoMaximoBodyFilter` (global, 16 KB), `CorpoExcedeTamanhoMaximoHandler` (global, único que trata `HttpMessageNotReadableException`), `MetodoNaoSuportadoHandler` (global, 405).
- `domain.voto` / `application.voto`: `Voto` (JPA, constraint única `uk_voto_associado_pauta` em `(associado, pauta_id)`, valida `associado`/`tipoVoto` no construtor, `codePointCount` no limite de 50), `TipoVoto` (enum SIM/NAO), `VotoRepository`, `ResultadoVotacao` (record calculado, não persistido — `situacao` ∈ aberta/encerrada/`nao_iniciada`), `VotoInvalidoException`, `VotoDuplicadoException`, `SessaoNaoAbertaException`. `VotoService` (`registrar`, `resultado`, `@Transactional` em `registrar`, `Clock` injetado): ordem pauta (404) → `new Voto(...)` (400) → sessão aberta (409) → duplicado em memória (409) → `salvar` captura `DataIntegrityViolationException`, checa se a causa é `uk_voto_associado_pauta` e remapeia pro mesmo 409; qualquer outra causa relança (ver Convenções).
- `api.voto` / `api.voto.dto`: `VotoController` (`POST /pautas/{id}/votos`, `GET /pautas/{id}/resultado`), `VotoExceptionHandler` (mesmo padrão de Pauta/Sessão — não trata `HttpMessageNotReadableException`), `VotoRequest`, `VotoResponse`, `ResultadoResponse`.
- `infrastructure.persistence`: `PautaJpaRepository` (`buscarPorIdComLock` via `@Lock` + `@Query`), `SessaoJpaRepository`, `VotoJpaRepository` (ambos `findByPautaId`; `VotoRepositoryImpl.salvar` usa `saveAndFlush` — documentação de intenção, não o que garante a ordem; ver Armadilhas sobre `GenerationType.IDENTITY`).
- `infrastructure.config`: `ClockConfig` (`Clock.systemUTC()`, substituído por `@MockBean Clock` nos testes), `JacksonConfig` (coerção customizada — ver Convenções).

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Maven, H2 em memória (`ddl-auto: update`), sem Flyway.
- `mvnd test` — suite completa (125 testes, ~3min; boot de Spring Context em `PautaControllerTest`/`SessaoControllerTest`/`SessaoConcorrenciaTest`/`VotoControllerTest`, Tomcat real em `CorpoExcedeTamanhoMaximoE2ETest`, `@DataJpaTest` em `VotoJpaRepositoryTest`).
- Sem Docker/rede externa: nada de Testcontainers. E2e de chunked usa `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `java.net.Socket` crudo. `SessaoConcorrenciaTest` usa threads reais + `CountDownLatch` contra H2 real, sem mock de repositório (único jeito de exercitar o lock pessimista de verdade). `VotoJpaRepositoryTest` usa `@DataJpaTest` (H2 real) pra provar a constraint única no banco, não só o check em memória.

## Convenções

- Regras de negócio no construtor das entidades (`Pauta`, `Sessao`, `Voto`), não em Bean Validation do DTO.
- **`HttpMessageNotReadableException` (JSON malformado, enum inválido, ou 413 disfarçado) é tratada SÓ no `CorpoExcedeTamanhoMaximoHandler` global, nunca em handler escopado** — `@RestControllerAdvice` escopado tem precedência sobre o global; bug real do review ciclo 1 de sessão (413 virando 400) corrigido centralizando o desembrulho no global. `Pauta`/`Sessao`/`Voto` ExceptionHandler nunca tratam esse tipo.
- `HttpRequestMethodNotSupportedException` (405) é lançada antes do Spring saber o controller → `MetodoNaoSuportadoHandler` é global, preenche `Allow` via `ex.getSupportedHttpMethods()`.
- Fatia 1: validação usa `isBlank()`/`strip()`/`codePointCount()` (não `trim()`/`length()`) e checagem de "caractere visível" por categoria Unicode — detalhes e gaps em Decisões.
- Fatia 2: ordem em `SessaoService.abrir` é corpo (400) → pauta existe (404) → sessão aberta (409). "Aberta" = `abertura.plus(duracaoMinutos, MINUTES).isAfter(agora)` — fim exclusivo, mesma regra usada em `Sessao.estaAberta` por `VotoService` (fatia 3).
- **`JacksonConfig`:** Jackson coage silenciosamente `Float→Integer` e `String→Integer`, forçados a `CoercionAction.Fail` globalmente — qualquer `Integer`/`Long` novo em DTO herda.
- `*ControllerTest` substitui o bean `Clock` por `@MockBean Clock`.
- **Lock do 409 de sessão (review ciclo 1):** `SessaoService.abrir` é `@Transactional` + `buscarPorIdComLock` (`PESSIMISTIC_WRITE`) pra serializar requisições na mesma pauta, decisão vs. `@Version` otimista (evita retry no caller). Provado com 10 threads reais em `SessaoConcorrenciaTest`. Teto `Sessao.DURACAO_MAXIMA_MINUTOS = 1440` (sem ele, DoS funcional) — não confirmado com PO.
- Fatia 3: ordem em `VotoService.registrar` é pauta (404) → `Voto` (400) → sessão aberta (409) → duplicado (409) — INVERTIDA vs. `SessaoService.abrir`; não unificar sem PO. Constraint única (`uk_voto_associado_pauta`) é a defesa real sob corrida — com `GenerationType.IDENTITY` o INSERT ocorre no `persist`, não no commit, então a violação já aparece no `try/catch` (`saveAndFlush` é só documentação de intenção). `catch` checa a causa contra `uk_voto_associado_pauta` antes de mapear pra `VotoDuplicadoException` (409); outra violação relança sem mascarar (`VotoServiceTest.registrar_comViolacaoDeOutraColuna_relanca`, review ciclo 2 item 4-5). Sem teste de concorrência real; cobertura só via mock.

## Decisões

- Fatia 1: defaults sem resposta do PO (`titulo` máx. 200, `descricao` opcional máx. 1000, erro `{status, mensagem}`). Título só com Zs/Zl/Zp/Cf/Cc/unassigned/surrogate rejeitado (pauta fantasma). **Gap:** checagem por categoria Unicode, não visual real — Hangul Filler U+3164, Braille Blank U+2800 passam. Body 16 KB confirmado com e2e; HTTP/2 não testado.
- Fatia 2: código 409, teto 1440 min e default 1 min assumidos, não confirmados com PO. Reabertura após encerramento fora de escopo.
- **Sem auth/autorização, decisão explícita (review ciclo 2 da fatia 3, item 12): não vai para produção como votação real.** `associado` string livre sem validação contra cadastro, id sequencial sem ofuscação — qualquer cliente vota como qualquer associado e lê resultado de qualquer pauta. Fora de escopo do task.md (Won't: "Autenticação e autorização"), mas PO precisa aprovar formalmente esse estado antes de qualquer uso real. Ver Pendências.
- Fatia 3: dúvidas 1–6 do task.md assumidas como sugerido no ticket (409 p/ sem sessão/após fim, zeros + `"nao_iniciada"` sem sessão, 201, fim exclusivo, `associado` string livre, resultado usa sessão mais recente). Nenhuma confirmada com PO.

## Entregas

- Fatia 1/3 (pautas): AC-1.1–1.6, três ciclos de review (handler de erro, Unicode, body).
- Fatia 2/3 (sessão): `POST /pautas/{id}/sessao`, `SessaoService` 400→404→409; ciclo de review: 413 centralizado no global, 415/405/400-param, lock pessimista + `SessaoConcorrenciaTest`, teto 1440 min, rejeição de string numérica.
- Fatia 3/3 (votação/resultado): `POST /pautas/{id}/votos`, `GET /pautas/{id}/resultado`. `Voto` com constraint única no banco, `VotoService` 404→400→409→409, situação derivada do tempo, usa sessão mais recente. Ciclo de review 1 (CHANGES_REQUIRED, 13 itens, corrigidos neste commit): testes de handler 400/415/413 na rota de voto; catch de `DataIntegrityViolationException` restrito à constraint esperada (demais relançam); `codePointCount` em vez de `length()`; sequência de `clock.instant()` fixada com `verify` em `votar_aposFimDaSessao_retorna409`; AC-3.1 assertando body; doc de `saveAndFlush`/IDENTITY corrigida (Armadilhas e Módulos); auth registrada como decisão explícita (não pendência silenciosa); mapa AC→teste com nomes de teste restaurados.
- Suite completa verde: `mvnd test` → 125 testes, 0 falhas (eram 116, +9 deste ciclo).

## Armadilhas

- `mvn`/`mvnd` imprime warnings de `jline`/`jansi` (`UnsatisfiedLinkError`) — inofensivo.
- `@SpringBootTest`/`@DataJpaTest` sobem contexto/Tomcat real; primeira execução de cada ~40s.
- MockMvc sempre deriva `Content-Length` do `byte[]` do payload — nunca produz chunked de verdade; e2e de chunked precisa de socket crudo contra servidor real.
- `@MockBean Clock` substitui o bean real do contexto inteiro — stubar sequência de retornos se o teste lê o clock mais de uma vez, senão Nª chamada sem stub vira NPE.
- `PESSIMISTIC_WRITE` fora de transação lança `TransactionRequiredException` — por isso `SessaoService.abrir` precisa de `@Transactional` junto com `buscarPorIdComLock`.
- `VotoRepositoryImpl.salvar` usa `saveAndFlush`, não `save` — mas com `GenerationType.IDENTITY` o Hibernate já executa o INSERT no `persist`, não no commit; mesmo com `save` simples a constraint dispararia a tempo do `try/catch`. `saveAndFlush` é só documentação de intenção (correção do review ciclo 2, item 11 — a frase anterior sobre "constraint só dispararia no commit" estava errada).

## Pendências

- **Sem auth/autorização** (decisão explícita, ver Decisões, não pendência silenciosa): `associado` string livre não validada, id sequencial sem ofuscação — qualquer um vota como qualquer associado e lê resultado de qualquer pauta. Bloqueante formal antes de uso como votação real.
- `GET /pautas` (Could fatia 1) e `ultimaSessao` no resultado (Could fatia 3) não implementados.
- Fatia 3: duplicata concorrente só via mock (sem teste real tipo `SessaoConcorrenciaTest`); `associado` string livre vs. ID numérico (dúvida 5); `"nao_iniciada"` é extrapolação não confirmada. Performance: `registrar`/`resultado` carregam todos os votos/sessões da pauta em memória em vez de usar queries dedicadas (`existsByPautaIdAndAssociado`, `countByPautaIdAndTipoVoto`, `findTopByPautaIdOrderByAberturaDesc`) — O(n) por request, não corrigido neste ciclo. Dúvidas 1-6 do task.md seguem assumidas.
- `spring-boot-starter-validation` no `pom.xml` sem uso. `PautaNaoEncontradaException(Long id)`: parâmetro ignorado (mensagem hardcoded). "Título fantasma" (fatia 1): checagem por categoria Unicode, não visual real — pendente decisão de produto.
- Fatia 2: sem encerramento automático por job, só cálculo em tempo real.
