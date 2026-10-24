# Contexto do projeto

## Módulos

- `README.md`: estado atual, endpoints da fatia 1 (pautas) e mapeamento AC-1.x para testes.
- `docs/SPEC.md`: escopo, modelo de ameaças e critérios de aceite (rascunho).
- `src/main/java/com/acme/pautas/domain`: entidade `Pauta` (id `Long` gerado pelo banco, `titulo` até 255, `descricao` até 2000).
- `src/main/java/com/acme/pautas/repository`: `PautaRepository` (Spring Data JPA).
- `src/main/java/com/acme/pautas/service`: `PautaService` (cadastrar, buscar) e `PautaNaoEncontradaException`.
- `src/main/java/com/acme/pautas/web`: `PautaController`, `PautaRequest` (`@NotBlank` e `@Size` em `titulo`, `@Size` em `descricao`), `PautaResponse`, `ErroResponse`, `GlobalExceptionHandler` (`@RestControllerAdvice`), `LimiteTamanhoCorpoFilter` (corta body acima de 64 KB com 413).
- `src/main/resources`: `application.yml` (ddl `none`) e `schema.sql` (tabela `pauta`).
- `src/test`: `PautaControllerTest` (MockMvc, AC-1.1 a AC-1.4 e AC-1.6), `GlobalExceptionHandlerTest` (500 sem vazar mensagem), `PautasApplicationTests` (contexto e tabela `PAUTA`, AC-1.5).

## Stack e comandos

- Java 21, Spring Boot 3.3.13, Maven, H2 em memória, JUnit 5, MockMvc.
- Pacote base: `com.acme.pautas`. Artefato: `pautas`.
- Testes: `mvnd test` (sem `clean`). Rodar um teste: `mvnd test -Dtest='Classe#metodo'`.
- Verificação de CVE: plugin `dependency-check-maven` 12.2.2 declarado no `pom.xml`, sem fase vinculada. Rodar com `mvnd org.owasp:dependency-check-maven:check`.
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
- Códigos: 400/405/406/415 usam `VALIDACAO_INVALIDA`; 413 usa `PAYLOAD_MUITO_GRANDE`; 404 usa `NAO_ENCONTRADO`; 500 usa `ERRO_INTERNO` com mensagem fixa.
- Autenticação fora de escopo nesta fatia (ver README). Endpoints de voto exigirão identificação antes da fatia 2.
- `open-in-view` desligado.
- Endpoints e regras de quórum/pauta de votação ficam em pendência até o solicitante enviar lista.

## Entregas

- `ba5b192` README inicial com descrição da API.
- Ciclo de correção do review: README ajustado para estado planejado e `docs/SPEC.md` criado com modelo de ameaças e critérios de aceite.
- Fatia 1 (épico `run_94bb0d49e2da`): módulo Maven Spring Boot 3 / Java 21 / H2, `POST /pautas`, `GET /pautas/{id}`, handler global de erros.
- `c1d43ff` correções do review: JSON malformado, 405/415/406/413/500 no formato padrão, `@Size`, limite de body, schema fora do Hibernate, H2 com nome único por contexto, Boot 3.3.13. 17 testes verdes.

## Armadilhas

- Ticket (cenário A) prevê só `README.md` no diff; o review pediu spec. Spec foi adicionado em `docs/` por exigência do review.
- Critérios AC-1.3 do ticket dizem "só README.md"; diffs desde então adicionam também `docs/SPEC.md` e `docs/CONTEXT.md`.
- `HttpMessageNotReadableException` precisa de handler; sem ele corpo malformado vira 500.
- Teste de contexto usa `@SpringBootTest` com H2 em memória; sobe em ~45 s. Não usar Testcontainers (sem Docker).
- Cada contexto Spring usa H2 próprio (`pautas-${random.uuid}`). Não voltar a nome fixo, senão contextos se atrapalham.
- `schema.sql` roda em qualquer perfil. Ao criar perfil de produção, revisar isso.
- Limite de body é filtro de servidor, não do proxy. Documentado aqui; proxy externo pode ter limite próprio.
- Dependency-check não roda no `mvnd test`. Versão 12.2.2 não verificada no mirror.

## Pendências

- Confirmar cenário (A ou B) e prioridade com o solicitante.
- Confirmar limites de `titulo` (255), `descricao` (2000) e body (64 KB).
- Confirmar formato de erro para o épico inteiro.
- Confirmar se autenticação entra no épico; hoje está fora de escopo desta fatia.
- Lista de endpoints e regras de votação (quórum, voto único, pauta).
- Rodar dependency-check e registrar CVEs encontrados.
- Fatia 2: sessão de votação. Fatia 3: votos e resultado (409 usa o mesmo formato de erro).
