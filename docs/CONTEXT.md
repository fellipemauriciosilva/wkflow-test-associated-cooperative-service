# Contexto do projeto

## Módulos

- `README.md`: estado atual, endpoints da fatia 1 (pautas) e mapeamento AC-1.x para testes.
- `docs/SPEC.md`: escopo, modelo de ameaças e critérios de aceite (rascunho).
- `src/main/java/com/acme/pautas/domain`: `Pauta` (id `Long` gerado pelo banco, `titulo` até 255, `descricao` até 2000), `Sessao` (`pautaId`, `abertura`, `fim` como `Instant`), `Voto` (`pautaId`, `associadoId`, `opcao`) e `OpcaoVoto` (`SIM`, `NAO`).
- `src/main/java/com/acme/pautas/config`: `RelogioConfig` (bean `Clock`, UTC do sistema).
- `src/main/java/com/acme/pautas/repository`: `PautaRepository` (`findWithLockById` com `PESSIMISTIC_WRITE`), `SessaoRepository`, `VotoRepository` (`existsByPautaIdAndAssociadoId`, `contarPorOpcao` em JPQL com `GROUP BY`).
- `src/main/java/com/acme/pautas/service`: `PautaService`, `SessaoService` (abrir), `VotoService` (registrar, resultado), exceções `PautaNaoEncontradaException`, `SessaoJaAbertaException`, `SemSessaoAbertaException`, `VotoDuplicadoException`, e record `ResultadoVotacao`.
- `src/main/java/com/acme/pautas/web`: `PautaController`, `SessaoController`, `VotoController` (`POST /pautas/{id}/votos`, `GET /pautas/{id}/resultado`), DTOs `*Request`/`*Response` (`VotoRequest` com `@NotBlank` e `@Size(255)` em `associadoId`, `@NotNull` em `voto`), `ErroResponse`, `GlobalExceptionHandler` (`@RestControllerAdvice`), `LimiteTamanhoCorpoFilter` (corta body acima de 64 KB com 413).
- `src/main/resources`: `application.yml` (ddl `none`) e `schema.sql` (tabelas `pauta`, `sessao` com FK, `voto` com FK e `UNIQUE (pauta_id, associado_id)`).
- `src/test`: `PautaControllerTest`, `GlobalExceptionHandlerTest`, `PautasApplicationTests`, `SessaoControllerTest` (AC-2.1 a AC-2.5), `SessaoConcorrenciaTest` (AC-2.6), `VotoControllerTest` (AC-3.1 a AC-3.11), `VotoRepositoryTest` (AC-3.12, constraint), `VotoConcorrenciaTest` (AC-3.13, 8 threads), `RelogioDeTeste` (`@TestConfiguration` com relógio ajustável, `@Primary`).
- `docs/TASK-fatia-1.md`: cópia do task.md da fatia 1 (requisitos, ACs, premissas).

## Stack e comandos

- Java 21, Spring Boot 3.3.13, Maven, H2 em memória, JUnit 5, MockMvc, Mockito.
- Pacote base: `com.acme.pautas`. Artefato: `pautas`.
- Testes: `mvnd test` (sem `clean`). Rodar um teste: `mvnd test -Dtest='Classe#metodo'`.
- Verificação de CVE: plugin `dependency-check-maven` 12.2.2 vinculado ao goal `check` na fase `verify` (`pom.xml`). Roda em `mvn verify`, não em `mvnd test`. Rodar isolado: `mvnd org.owasp:dependency-check-maven:check`.
- Dependências vêm do mirror interno (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`). Sem `curl` nem `python3` no ambiente; para consultar o mirror use programa Java.
- Subir: `mvnd spring-boot:run`.

## Convenções

- Documentação em português.
- Código novo entra sempre com teste no mesmo commit.
- Camadas: controller, service, repository. Erros tratados só no `GlobalExceptionHandler`, sempre no formato `ErroResponse`.
- Schema vem de `schema.sql`; Hibernate não altera schema (`ddl-auto: none`).

## Decisões

- Entrega de 24/10 tratada como README inicial com spec, sem código (cenário A do ticket).
- Fatia 1 implementada: módulo Maven, cadastro e consulta de pautas, erros padronizados.
- `titulo` vazio ou só espaços responde 400 (`@NotBlank`). Premissa do task.md.
- Limites: `titulo` 255, `descricao` 2000, body 64 KB. Valores propostos, a confirmar com o solicitante.
- Formato de erro `{status, erro, mensagem, path}` vale para o épico. Premissa a confirmar.
- Códigos: 400/405/406/415 usam `VALIDACAO_INVALIDA`; 413 usa `PAYLOAD_MUITO_GRANDE`; 404 usa `NAO_ENCONTRADO`; 409 usa `CONFLITO`; 500 usa `ERRO_INTERNO` com mensagem fixa.
- Fatia 2: sessão aberta = `fim` depois de agora. Expirada não bloqueia. Status 201, `duracaoMinutos` padrão 1, `fim` calculado no servidor pelo bean `Clock`.
- Fatia 2: concorrência por `PESSIMISTIC_WRITE` na linha da pauta (`findWithLockById`), sem constraint única em `sessao.pauta_id`.
- Fatia 2: `duracaoMinutos` com teto de 1440 (24 h). Acima disso, 400 `VALIDACAO_INVALIDA`, nada gravado. Valor proposto.
- Fatia 3: ordem de checagem do voto fixa: pauta (404) → body (400) → sessão aberta (409) → duplicidade (409).
- Fatia 3: `VotoService.registrar` usa `findWithLockById` na pauta, igual à sessão. Serializa votos concorrentes do mesmo par sem depender só da constraint.
- Fatia 3: `DataIntegrityViolationException` de `saveAndFlush` do voto é convertida em `VotoDuplicadoException` (409 `CONFLITO`) dentro de `VotoService`. O handler global não trata mais essa exceção, então violação de outra constraint continua 500.
- Fatia 3: `associadoId` é normalizado antes de checar e gravar: `strip()` + NFC. Caixa é preservada (`a1` e `A1` são associados distintos). Decisão de caixa a confirmar com o solicitante.
- Fatia 3: `associadoId` é string livre, não vazia, até 255 caracteres (após strip). Proposta a confirmar.
- Fatia 3: resultado sem campo de status. Contagem por `GROUP BY` no banco, não lista em memória.
- Autenticação fora de escopo nesta fatia (ver README). Endpoints de voto exigirão identificação antes de produção.
- `open-in-view` desligado.
- Endpoints e regras de quórum/pauta de votação ficam em pendência até o solicitante enviar lista.

## Entregas

- `ba5b192` README inicial com descrição da API.
- Ciclo de correção do review: README ajustado para estado planejado e `docs/SPEC.md` criado com modelo de ameaças e critérios de aceite.
- Fatia 1 (épico `run_94bb0d49e2da`): módulo Maven Spring Boot 3 / Java 21 / H2, `POST /pautas`, `GET /pautas/{id}`, handler global de erros.
- `c1d43ff` correções do review: JSON malformado, 405/415/406/413/500 no formato padrão, `@Size`, limite de body, schema fora do Hibernate, H2 com nome único por contexto, Boot 3.3.13. 17 testes verdes.
- Ciclo 2 do review: 405/415/406 com chaves exatas e `mensagem`; 500 via rota MockMvc com controller stub; `dependency-check` vinculado a `verify`; `docs/SPEC.md` com AC-1.x mapeados; `docs/TASK-fatia-1.md`. 18 testes verdes.
- Fatia 2 (sessão de votação): `POST /pautas/{id}/sessao`, tabela `sessao`, `Clock` injetável, 409 via `GlobalExceptionHandler`, lock pessimista. 25 testes verdes.
- Fatia 3 (votos e resultado): `POST /pautas/{id}/votos`, `GET /pautas/{id}/resultado`, tabela `voto` com constraint única, lock pessimista na pauta, testes MockMvc, de repositório e de concorrência. Suite completa 45 testes verdes. Ciclo de correção do review: 55 testes verdes (normalização, unicidade por pauta, limites 255/256, precedência do body, teste de concorrência com asserts separados).

## Armadilhas

- Ticket (cenário A) prevê só `README.md` no diff; o review pediu spec. Spec foi adicionado em `docs/` por exigência do review.
- Critérios AC-1.3 do ticket dizem "só README.md"; diffs desde então adicionam também `docs/SPEC.md` e `docs/CONTEXT.md`.
- `HttpMessageNotReadableException` precisa de handler; sem ele corpo malformado vira 500.
- Teste de contexto usa `@SpringBootTest` com H2 em memória; sobe em ~45 s. Não usar Testcontainers (sem Docker).
- Cada contexto Spring usa H2 próprio (`pautas-${random.uuid}`). Não voltar a nome fixo, senão contextos se atrapalham.
- `schema.sql` roda em qualquer perfil. Ao criar perfil de produção, revisar isso.
- Limite de body (64 KB) é filtro de servidor (`LimiteTamanhoCorpoFilter`), não do proxy. Proxy externo pode ter limite próprio. Limite documentado aqui e no SPEC.
- Dependency-check não roda no `mvnd test`. Em `mvn verify` ele baixa o banco KEV da CISA (`www.cisa.gov`) e falha sem internet (`UnknownHostException`). Plugin 12.2.2 resolve no mirror, mas não há CVE report neste ambiente. Rodar com acesso à internet ou com banco NVD/KEV local.
- Controller stub de teste de 500 fica em `GlobalExceptionHandlerTest`. Não mover sem revisar o `standaloneSetup`.
- Premissas do task.md (`docs/TASK-fatia-1.md`) confirmadas na implementação: `titulo` vazio responde 400, `fim` calculado no servidor, sessão aberta = `agora < fim`.
- Testes de tempo usam `RelogioDeTeste` (`@Import` + `@Primary`) para fixar relógio. Importar a mesma classe em testes novos.
- Lock pessimista exige transação ativa: `SessaoService.abrir` e `VotoService.registrar` são `@Transactional`. Não mover checagens para fora.
- Testes de concorrência abrem transação via `TransactionTemplate` por thread, porque o lock exige transação ativa.
- Mutação sem lock em sessão (`findById` no lugar de `findWithLockById`): 3 de 3 runs falham com `expected: <1> but was: <8>`. Mutação sem lock em voto (`findById` no lugar de `findWithLockById`): `VotoConcorrenciaTest` falha em `votosSimultaneosDoMesmoAssociadoGravamUmSoVoto:106` com `expected: <0> but was: <7>`. Sete votos caem na constraint, não na checagem da aplicação. Lock comprovado como necessário para a checagem.
- Mutação `agora == fim` (`Instant.now(clock).minusNanos(1)` em `VotoService`): `VotoControllerTest#rejeitaVotoAposFimDaSessao:92` falha com `expected:<409> but was:<201>`. Fronteira exata coberta.
- Spy com `@SpyBean` em interface Spring Data falha com "Cannot call abstract real method". Usar `mock(..., delegatesTo(repo))` e montar o service à mão.
- Teste de repositório de voto precisa de pauta real: `pauta_id` sem linha na FK falha antes da constraint e dá falso positivo.

## Pendências

- Confirmar cenário (A ou B) e prioridade com o solicitante.
- Confirmar limites de `titulo` (255), `descricao` (2000), body (64 KB) e `associadoId` (255).
- Confirmar formato de erro para o épico inteiro.
- Confirmar código 409 para voto fora de sessão (spec usa 409; alternativa 400).
- Confirmar se autenticação entra no épico; hoje está fora de escopo.
- Lista de endpoints e regras de votação (quórum, voto único, pauta).
- Rodar dependency-check com acesso ao KEV/NVD e registrar CVEs encontrados (bloqueado neste ambiente).
- Boot 3.3.x sem suporte OSS: decidir migração para 3.4/3.5 ou aceitar o risco registrado em SPEC.
- Autenticação e sign-off de escopo para endpoints abertos de voto (SPEC).
- Achados não bloqueantes do review: `Allow` no 405, filtro 413 fora do `GlobalExceptionHandler`, filtro e form params, `ddl-auto: validate`, catch-all `Exception` engolindo `ErrorResponse`, testes de limite (255/2000, 64 KB).
- Fatia 2: fechamento manual (C2) e expiração/limpeza (C1) fora de escopo.
- Fatia 3: validação de existência do associado (C1), notificação (W2) e status do resultado fora de escopo.
- Teto de `duracaoMinutos` (1440) é proposta; confirmar com o solicitante.
- Lock sem timeout (`jakarta.persistence.lock.timeout`) e exceção de lock caindo em 500 `ERRO_INTERNO`.
- Invariante "uma sessão ativa por pauta" só no service. Índice `(pauta_id, fim)` pendente.
- Mensagem do `@Min`/`@Max` e do `@NotBlank`/`@Size` de voto não é asserted nos testes.
- Lock sem timeout em voto e sessão (`jakarta.persistence.lock.timeout`). Contenção não medida; pool Hikari padrão 10. Sem carga, não há garantia.
- Resultado público durante sessão aberta: `GET /resultado` não checa `fim` e não tem auth. Decidir visibilidade (bloquear até fechamento ou documentar como aceito).
- Sem limite de volume de votos por pauta ou origem (sem auth, sem rate limit).
- Endpoint de voto não pode ir a produção sem fatia de identificação do associado (bloqueio de deploy).
- Lista de endpoints e regras de votação (quórum, pauta) continua pendente.
- Lock de voto talvez redundante com a constraint para unicidade; mantido para checagem clara. Reavaliar com medição.
