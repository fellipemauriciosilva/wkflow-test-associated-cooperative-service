# Contexto do projeto

## Módulos

- `br.acme.wkflow.pauta`: entidade `Pauta`, `PautaRepository`, `PautaController` (`POST /pautas`, `GET /pautas/{id}`), DTOs `PautaRequest`, `PautaCriadaResponse`, `PautaResponse`, exceção `PautaNaoEncontradaException`.
- `br.acme.wkflow.sessao`: entidade `Sessao` (`pautaId`, `inicio`, `fim`), `SessaoRepository`, `SessaoController` (`POST /pautas/{id}/sessao`), DTOs `SessaoRequest` (`duracaoMinutos`, opcional) e `SessaoResponse` (`pautaId`, `inicio`, `fim`).
- `br.acme.wkflow.tempo`: `ConfiguracaoTempo` expõe bean `Clock` (`systemDefaultZone`). Testes sobrescrevem com `@Primary` para `Clock.fixed`.
- `br.acme.wkflow.erro`: `TratamentoErrosGlobal` (`@RestControllerAdvice`), converte erros em `ProblemDetail`, inclui catch-all 500.
- `br.acme.wkflow`: `WkflowApplication` (entrypoint Spring Boot).

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Spring MVC, Spring Data JPA, Bean Validation, H2 em memória, Maven.
- Testes: `mvnd test` (suite inteira, sem `clean`). Um teste: `mvnd test -Dtest='Classe#metodo'`.
- Dependências resolvem pelo proxy interno (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`). Sem internet.
- Relatórios Surefire em `target/surefire-reports/*.txt` são a fonte confiável do resultado; a saída do `mvnd` vem truncada.

## Convenções

- Testes de controller com `@SpringBootTest` + `@AutoConfigureMockMvc`, nomeados por critério de aceite (`ac11_...` a `ac15_...` na pauta, `ac21_...` a `ac26_...` na sessão; `ac26` = tipo inválido da duração).
- Erros de API sempre `application/problem+json` via `ProblemDetail`. `spring.mvc.problemdetails.enabled=true` cobre erros do framework (404, 405, 415, type mismatch).
- Pacote por feature (`pauta`, `sessao`), tratamento de erro global em `erro`, infraestrutura de tempo em `tempo`.

## Decisões

- Id gerado pelo banco (`IDENTITY`). H2 em memória aceito para o épico.
- `descricao` validada com `@NotBlank` no DTO de request. Vazia, só espaços, nula ou ausente retornam 400.
- Tamanho máximo de `descricao`: 255 caracteres. `@Size(max = 255)` no DTO e `@Column(length = 255)` na entidade, para que o limite de validação e o do banco coincidam. Acima disso retorna 400.
- POST `/pautas` retorna 201 com header `Location` e body `{"id": n}`.
- GET `/pautas/{id}` retorna `{"id": n, "descricao": "..."}`. Id inexistente retorna 404 ProblemDetail.
- Erro inesperado retorna 500 ProblemDetail com detail genérico, loga a exceção e nunca expõe `ex.getMessage()`.
- Entidade `Pauta` com construtor `protected` para JPA e construtor público para o domínio.
- `POST /pautas/{id}/sessao` retorna 201 com body `{"pautaId", "inicio", "fim"}` (formato assumido, ticket não define). Sem header `Location`.
- `inicio` = `Instant.now(clock)`; `fim` = `inicio + duracaoMinutos`. Duração ausente ou `null` = 1 minuto.
- Duração validada com `@Min(1)` no `SessaoRequest` (0 e negativos retornam 400).
- Coerção Jackson desligada para inteiros em `br.acme.wkflow.json.ConfiguracaoJson`: `5.5` (ACCEPT_FLOAT_AS_INT off) e `"5"` (String para Integer = Fail) retornam 400. `"abc"` também 400.
- Pauta inexistente no POST de sessão retorna 404: `buscarComLock` (`PESSIMISTIC_WRITE`) dentro de `@Transactional` em `SessaoController.abrir`.
- Reabertura: POST com sessão ainda aberta da mesma pauta (`fim > agora`) retorna 409 ProblemDetail (`SessaoJaAbertaException`). Sessão expirada não bloqueia: nova linha criada (`ac27`).
- Corrida na abertura: o lock de pauta serializa dois POSTs concorrentes; o segundo vê a sessão do primeiro e recebe 409. Sem teste de concorrência real (H2 e MockMvc single-thread); a garantia vem do lock, não de teste.
- Ordem de validação: `@Valid` roda antes da busca da pauta. Pauta inexistente + duração inválida retorna 400 (pinado por `ac25_validacaoAntesDeExistenciaDaPauta`).
- Content-Type errado com corpo no POST de sessão retorna 415 ProblemDetail (`ac25_contentTypeErrado...`).
- Duração de sessão entre 1 e 1440 minutos: `@Min(1)` e `@Max(1440)` no `SessaoRequest`. Acima de 1440 retorna 400 e nada é persistido (`ac28`). Teto 1440 é placeholder (1 dia), confirmar com dono do ticket.
- Sem autenticação, sem listagem.
- `Sessao` é entidade JPA (persistência assumida, ticket não define).

## Entregas

- Fatia 1/3 (cadastro e consulta de pauta): `POST /pautas`, `GET /pautas/{id}`, erros 400, 404, 415 e 500 em ProblemDetail. Testes AC-1.1 a AC-1.5 em `PautaControllerTest`, mais casos de erro de framework (GET id não numérico, Content-Type errado, descrição de 256 caracteres).
- Fatia 2/3 (abertura de sessão por pauta): `POST /pautas/{id}/sessao` com duração padrão 1 min, validação `>= 1`, 404 para pauta inexistente, bean `Clock`. Testes AC-2.1 a AC-2.5 em `SessaoControllerTest` (`Clock` fixo), mais casos de duração não inteira (`"abc"`, `5.5`, `"5"`), `duracaoMinutos: null` explícito e asserção de persistência por `pautaId` no AC-2.1. Reabertura com sessão aberta: 409 (`ac27`). Teto de duração 1440 min com 400 e sem persistência (`ac28`). Testes de sessão nomeados `ac21` a `ac28` (ac25 = validação e 415, ac26 = tipo inválido da duração, ac27 = reabertura, ac28 = teto).

## Armadilhas

- `spring.jpa.open-in-view` está ativo por padrão (warning no log). Não afeta esta fatia. Desligar antes de existir relação lazy.
- `JsonPath` usado no teste via import (`com.jayway.jsonpath.JsonPath`) para ler id do POST.
- `ProblemDetail` do 400 de validação usa detail genérico `Requisicao invalida`. Detalhe por campo não exposto nesta fatia.
- `ProblemDetail.forStatusAndDetail` não preenche `title`. Usar o helper `problema(...)` do `TratamentoErrosGlobal`.
- Ids sequenciais e sem autenticação: `GET /pautas/{n}` é enumerável e gaps do IDENTITY vazam contagem de linhas. Aceito na fatia 1, registrar antes da fatia 2 ou da autenticação.
- `Location` montado com path relativo (`/pautas/{id}`). Pode quebrar atrás de proxy com context path. Ver pendências.
- Sem request body cap no código. Limitar no proxy/gateway. `max-http-form-post-size` não cobre body JSON.
- `@RequestBody(required = false)` no POST de sessão: body ausente cai no padrão de 1 min. `Content-Type` errado continua 415.
- Lock pessimista só vale dentro da transação de `abrir`. Se alguém chamar `buscarComLock` fora de `@Transactional`, o lock não existe.
- `existsByPautaIdAndFimAfter` sem índice em `pautaId`: full scan. Índice ainda pendente.
- Pauta removida entre `existsById` e `save` cria sessão órfã. Sem remoção de pauta hoje; aceito.
- `Instant` no JSON depende de Jackson não escrever timestamp numérico (`write-dates-as-timestamps` default off). `ac22`/`ac23` quebram se mudar.

## Pendências

- Confirmar com o ticket: `@Valid` (S1), Spring Data JPA e formato do body do GET e do 201 de sessão (assumidos).
- Gap 5: campos da pauta além de `descricao` não definidos. Só `descricao` implementado.
- Reabertura decidida: 409 com sessão aberta (W4 do master em aberto, confirmar com o time). Ainda sem índice em `Sessao.pautaId`.
- Teto de duração 1440 min (`@Max`) é placeholder de 1 dia. Valor real a confirmar com dono do ticket; sem confirmação, manter 1440.
- Escopo global do Jackson (`ConfiguracaoJson` afeta todo ObjectMapper): qualquer campo `Integer` futuro rejeita `5.5` e `"5"`. Hoje nenhum DTO é afetado além de `SessaoRequest`.
- Sem autenticação nem limite de criação de sessões por pauta: risco de pré-auth aceito nesta fatia.
- Confirmar: duração `null` explícita como padrão (assumido); teto de duração 1440 (placeholder).
- Fatia 3 (votos) depende de `Sessao` e do comportamento de reabertura.
- Fora de escopo nesta correção (não implementado): normalizar espaços de `descricao` com `strip()` no DTO; `ServletUriComponentsBuilder` para `Location`; `spring.jpa.open-in-view=false`; varredura de dependências (SCA) contra o proxy interno antes do merge.
