# Contexto do projeto

## Módulos

- `README.md`: estado atual, endpoints da fatia 1 (pautas) e mapeamento AC-1.x para testes.
- `docs/SPEC.md`: escopo, modelo de ameaças e critérios de aceite (rascunho).
- `src/main/java/com/acme/pautas/domain`: entidade `Pauta` (id `Long` gerado pelo banco, `titulo` até 255, `descricao` até 2000).
- `src/main/java/com/acme/pautas/domain`: entidade `Sessao` (`pautaId`, `abertura`, `fim` como `Instant`).
- `src/main/java/com/acme/pautas/config`: `RelogioConfig` (bean `Clock`, UTC do sistema).
- `src/main/java/com/acme/pautas/repository`: `PautaRepository` (Spring Data JPA; `findWithLockById` com `PESSIMISTIC_WRITE`) e `SessaoRepository`.
- `src/main/java/com/acme/pautas/service`: `PautaService` (cadastrar, buscar), `PautaNaoEncontradaException`, `SessaoService` (abrir) e `SessaoJaAbertaException`.
- `src/main/java/com/acme/pautas/web`: `PautaController`, `SessaoController`, `PautaRequest`, `SessaoRequest` (`@Min(1)` em `duracaoMinutos`), `PautaResponse`, `SessaoResponse`, `ErroResponse`, `GlobalExceptionHandler` (`@RestControllerAdvice`), `LimiteTamanhoCorpoFilter` (corta body acima de 64 KB com 413).
- `src/main/resources`: `application.yml` (ddl `none`) e `schema.sql` (tabelas `pauta` e `sessao`, FK `sessao.pauta_id`).
- `src/test`: `PautaControllerTest` (MockMvc, AC-1.1 a AC-1.4 e AC-1.6, 405/415/406), `GlobalExceptionHandlerTest` (500 direto no handler e 500 via rota com controller stub), `PautasApplicationTests` (contexto e tabela `PAUTA`, AC-1.5), `SessaoControllerTest` (AC-2.1 a AC-2.5), `SessaoConcorrenciaTest` (AC-2.6, 8 threads), `RelogioDeTeste` (`@TestConfiguration` com relógio ajustável, `@Primary`).
- `docs/TASK-fatia-1.md`: cópia do task.md da fatia 1 (requisitos, ACs, premissas).

## Stack e comandos

- Java 21, Spring Boot 3.3.13, Maven, H2 em memória, JUnit 5, MockMvc.
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
- Fatia 2: sessão aberta = `fim` depois de agora. Expirada não bloqueia. Premissas do task.md confirmadas na implementação.
- Fatia 2: status 201, `duracaoMinutos` padrão 1, `fim` calculado no servidor pelo bean `Clock`.
- Fatia 2: concorrência por `PESSIMISTIC_WRITE` na linha da pauta (`findWithLockById`), sem constraint única em `sessao.pauta_id`.
- Autenticação fora de escopo nesta fatia (ver README). Endpoints de voto exigirão identificação antes da fatia 2.
- `open-in-view` desligado.
- Endpoints e regras de quórum/pauta de votação ficam em pendência até o solicitante enviar lista.

## Entregas

- `ba5b192` README inicial com descrição da API.
- Ciclo de correção do review: README ajustado para estado planejado e `docs/SPEC.md` criado com modelo de ameaças e critérios de aceite.
- Fatia 1 (épico `run_94bb0d49e2da`): módulo Maven Spring Boot 3 / Java 21 / H2, `POST /pautas`, `GET /pautas/{id}`, handler global de erros.
- `c1d43ff` correções do review: JSON malformado, 405/415/406/413/500 no formato padrão, `@Size`, limite de body, schema fora do Hibernate, H2 com nome único por contexto, Boot 3.3.13. 17 testes verdes.
- Ciclo 2 do review: 405/415/406 com chaves exatas e `mensagem`; 500 via rota MockMvc com controller stub; `dependency-check` vinculado a `verify` (execução bloqueada por falta de internet); `docs/SPEC.md` com AC-1.x mapeados a testes; `docs/TASK-fatia-1.md`. 18 testes verdes.
- Fatia 2 (sessão de votação, épico `run_94bb0d49e2da`): `POST /pautas/{id}/sessao`, tabela `sessao`, `Clock` injetável, 409 via `GlobalExceptionHandler`, lock pessimista. 25 testes verdes.

## Armadilhas

- Ticket (cenário A) prevê só `README.md` no diff; o review pediu spec. Spec foi adicionado em `docs/` por exigência do review.
- Critérios AC-1.3 do ticket dizem "só README.md"; diffs desde então adicionam também `docs/SPEC.md` e `docs/CONTEXT.md`.
- `HttpMessageNotReadableException` precisa de handler; sem ele corpo malformado vira 500.
- Teste de contexto usa `@SpringBootTest` com H2 em memória; sobe em ~45 s. Não usar Testcontainers (sem Docker).
- Cada contexto Spring usa H2 próprio (`pautas-${random.uuid}`). Não voltar a nome fixo, senão contextos se atrapalham.
- `schema.sql` roda em qualquer perfil. Ao criar perfil de produção, revisar isso.
- Limite de body é filtro de servidor, não do proxy. Documentado aqui; proxy externo pode ter limite próprio.
- Dependency-check não roda no `mvnd test`. Em `mvn verify` ele baixa o banco KEV da CISA (`www.cisa.gov`) e falha sem internet (`UnknownHostException`). Plugin 12.2.2 resolve no mirror, mas não há CVE report neste ambiente. Rodar com acesso à internet ou com banco NVD/KEV local.
- Controller stub de teste de 500 fica em `GlobalExceptionHandlerTest`. Não mover para outra classe sem revisar o `standaloneSetup`.
- Testes de sessão usam `RelogioDeTeste` (`@Import` + `@Primary`) para fixar `abertura`/`fim`. Ao criar outro teste de tempo, importar a mesma classe.
- Lock pessimista exige transação ativa: `SessaoService.abrir` é `@Transactional`. Não mover a checagem de sessão para fora dela.
- Teste de concorrência (`SessaoConcorrenciaTest`) sobe contexto próprio e leva ~45 s.

## Pendências

- Confirmar cenário (A ou B) e prioridade com o solicitante.
- Confirmar limites de `titulo` (255), `descricao` (2000) e body (64 KB).
- Confirmar formato de erro para o épico inteiro.
- Confirmar se autenticação entra no épico; hoje está fora de escopo desta fatia.
- Lista de endpoints e regras de votação (quórum, voto único, pauta).
- Rodar dependency-check com acesso ao KEV/NVD e registrar CVEs encontrados (bloqueado neste ambiente).
- Boot 3.3.x sem suporte OSS: decidir migração para 3.4/3.5 ou aceitar o risco registrado em SPEC.
- Autenticação e sign-off de escopo para endpoints abertos de voto (SPEC).
- Achados não bloqueantes do review: `Allow` no 405, filtro 413 fora do `GlobalExceptionHandler`, filtro e form params, `ddl-auto: validate`, catch-all `Exception` engolindo `ErrorResponse`, testes de limite (255/2000, 64 KB).
- Fatia 2 (sessão de votação): fechamento manual (C2) e expiração/limpeza (C1) fora de escopo.
- Fatia 3: votos e resultado (depende da sessão aberta da fatia 2).
- Limite máximo de `duracaoMinutos` sem definição (gap do task.md). Hoje não há teto.
