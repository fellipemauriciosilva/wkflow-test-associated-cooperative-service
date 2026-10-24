# Contexto do projeto

## Módulos

- `README.md`: estado atual e endpoints da fatia 1 (pautas).
- `docs/SPEC.md`: escopo, modelo de ameaças e critérios de aceite (rascunho).
- `src/main/java/com/acme/pautas/domain`: entidade `Pauta` (id `Long` gerado pelo banco, `titulo`, `descricao`).
- `src/main/java/com/acme/pautas/repository`: `PautaRepository` (Spring Data JPA).
- `src/main/java/com/acme/pautas/service`: `PautaService` (cadastrar, buscar) e `PautaNaoEncontradaException`.
- `src/main/java/com/acme/pautas/web`: `PautaController`, `PautaRequest` (record com `@NotBlank` em `titulo`), `PautaResponse`, `ErroResponse`, `GlobalExceptionHandler` (`@RestControllerAdvice`).
- `src/test`: `PautaControllerTest` (MockMvc, cobre AC-1.1 a AC-1.4 e AC-1.6) e `PautasApplicationTests` (contexto e tabela `PAUTA`, AC-1.5).

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Maven, H2 em memória, JUnit 5, MockMvc.
- Pacote base: `com.acme.pautas`. Artefato: `pautas`.
- Testes: `mvnd test` (sem `clean`). Rodar um teste: `mvnd test -Dtest='Classe#metodo'`.
- Dependências vêm do mirror interno via `settings.xml` do sistema (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`).
- Subir: `mvnd spring-boot:run`.

## Convenções

- Documentação em português.
- Código novo entra sempre com teste no mesmo commit.
- Camadas: controller, service, repository. Erros tratados só no `GlobalExceptionHandler`.
- Schema gerado pelo Hibernate (`ddl-auto: create-drop`) na subida.

## Decisões

- Entrega de 24/10 tratada como README inicial com spec, sem código (cenário A do ticket).
- Fatia 1 implementada: módulo Maven, cadastro e consulta de pautas, erros padronizados.
- `titulo` vazio ou só espaços responde 400 (`@NotBlank`). Premissa do task.md.
- Formato de erro `{status, erro, mensagem, path}` vale para o épico. Premissa a confirmar.
- Corpo JSON malformado responde 400 com `erro` `VALIDACAO_INVALIDA`.
- `open-in-view` desligado.
- Endpoints e regras de quórum/pauta de votação ficam em pendência até o solicitante enviar lista.

## Entregas

- `ba5b192` README inicial com descrição da API.
- Ciclo de correção do review: README ajustado para estado planejado e `docs/SPEC.md` criado com modelo de ameaças e critérios de aceite.
- Fatia 1 (épico `run_94bb0d49e2da`): módulo Maven Spring Boot 3 / Java 21 / H2, `POST /pautas`, `GET /pautas/{id}`, handler global de erros, 7 testes verdes.

## Armadilhas

- Ticket (cenário A) prevê só `README.md` no diff; o review pediu spec. Spec foi adicionado em `docs/` por exigência do review.
- Critérios AC-1.3 do ticket dizem "só README.md"; diffs desde então adicionam também `docs/SPEC.md` e `docs/CONTEXT.md`.
- `GlobalExceptionHandler` precisa cobrir `HttpMessageNotReadableException`; sem isso corpo malformado vira 500.
- Teste de contexto usa `@SpringBootTest` com H2 em memória; sobe em ~35 s. Não usar Testcontainers (sem Docker).

## Pendências

- Confirmar cenário (A ou B) e prioridade com o solicitante.
- Confirmar tamanho máximo de `titulo` e `descricao` (hoje sem limite além do `VARCHAR` padrão).
- Confirmar formato de erro para o épico inteiro.
- Lista de endpoints e regras de votação (quórum, voto único, pauta).
- Fatia 2: sessão de votação. Fatia 3: votos e resultado (409 usa o mesmo formato de erro).
