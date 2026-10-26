# Contexto do projeto

## Módulos

- `br.acme.wkflow.pauta`: entidade `Pauta`, `PautaRepository` (com `buscarComLock`), `PautaController` (`POST /pautas`, `GET /pautas/{id}`), DTOs `PautaRequest`, `PautaCriadaResponse`, `PautaResponse`, exceção `PautaNaoEncontradaException`.
- `br.acme.wkflow.sessao`: entidade `Sessao` (`pautaId`, `inicio`, `fim`), `SessaoRepository` (`existsByPautaIdAndFimGreaterThanEqual`), `SessaoController` (`POST /pautas/{id}/sessao`), DTOs `SessaoRequest` (`duracaoMinutos`, opcional) e `SessaoResponse` (`pautaId`, `inicio`, `fim`), exceção `SessaoJaAbertaException`.
- `br.acme.wkflow.voto`: entidade `Voto` (`pautaId`, `associadoId`, `valor`, unique em `pauta_id`+`associado_id`), enum `ValorVoto` (SIM, NAO, `de(String)` lança `ValorVotoInvalidoException`), `VotoRepository` (`countByPautaIdAndValor`), `VotoController` (`POST /pautas/{id}/votos`, `GET /pautas/{id}/resultado`), DTOs `VotoRequest`, `VotoResponse`, `ResultadoResponse`, exceções `SessaoNaoAbertaException`, `VotoDuplicadoException`, `ValorVotoInvalidoException`.
- `br.acme.wkflow.tempo`: `ConfiguracaoTempo` expõe bean `Clock` (`systemDefaultZone`). Testes sobrescrevem com `@Primary` para `Clock.fixed`.
- `br.acme.wkflow.erro`: `TratamentoErrosGlobal` (`@RestControllerAdvice`), converte erros em `ProblemDetail`, inclui handlers de voto e catch-all 500.
- `br.acme.wkflow`: `WkflowApplication` (entrypoint Spring Boot).

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Spring MVC, Spring Data JPA, Bean Validation, H2 em memória, Maven.
- Testes: `mvnd test` (suite inteira, sem `clean`). Um teste: `mvnd test -Dtest='Classe#metodo'`.
- Dependências resolvem pelo proxy interno (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`). Sem internet.
- Relatórios Surefire em `target/surefire-reports/*.txt` são a fonte confiável do resultado; a saída do `mvnd` vem truncada.

## Convenções

- Testes de controller com `@SpringBootTest` + `@AutoConfigureMockMvc`, nomeados por critério de aceite: `ac11_...` a `ac15_...` na pauta, `ac21_...` a `ac28_...` na sessão, `ac31_...` a `ac38_...` nos votos (`ac37` = resultado 404, `ac38` = ordem de validação do voto).
- Erros de API sempre `application/problem+json` via `ProblemDetail`. `spring.mvc.problemdetails.enabled=true` cobre erros do framework (404, 405, 415, type mismatch).
- Pacote por feature (`pauta`, `sessao`, `voto`), tratamento de erro global em `erro`, infraestrutura de tempo em `tempo`.

## Decisões

- Id gerado pelo banco (`IDENTITY`). H2 em memória aceito para o épico.
- `descricao` validada com `@NotBlank` no DTO de request. Vazia, só espaços, nula ou ausente retornam 400.
- Tamanho máximo de `descricao`: 255 caracteres. `@Size(max = 255)` no DTO e `@Column(length = 255)` na entidade. Acima disso retorna 400.
- POST `/pautas` retorna 201 com header `Location` e body `{"id": n}`.
- GET `/pautas/{id}` retorna `{"id": n, "descricao": "..."}`. Id inexistente retorna 404 ProblemDetail.
- Erro inesperado retorna 500 ProblemDetail com detail genérico, loga a exceção e nunca expõe `ex.getMessage()`.
- Entidade `Pauta` com construtor `protected` para JPA e construtor público para o domínio.
- `POST /pautas/{id}/sessao` retorna 201 com body `{"pautaId", "inicio", "fim"}` (formato assumido, ticket não define). Sem header `Location`.
- `inicio` = `Instant.now(clock)`; `fim` = `inicio + duracaoMinutos`. Duração ausente ou `null` = 1 minuto.
- Duração validada com `@Min(1)` e `@Max(1440)` no `SessaoRequest`. 0, negativos e acima de 1440 retornam 400, nada persistido.
- Coerção Jackson desligada para inteiros em `br.acme.wkflow.json.ConfiguracaoJson`: `5.5` e `"5"` retornam 400. `"abc"` também 400.
- Pauta inexistente no POST de sessão retorna 404: `buscarComLock` (`PESSIMISTIC_WRITE`) dentro de `@Transactional` em `SessaoController.abrir`.
- Reabertura: POST com sessão ainda aberta da mesma pauta (`fim >= agora`) retorna 409 (`SessaoJaAbertaException`). Sessão expirada não bloqueia: nova linha criada (`ac27`).
- Corrida na abertura: o lock de pauta serializa dois POSTs concorrentes; o segundo vê a sessão do primeiro e recebe 409. Sem teste de concorrência real; a garantia vem do lock.
- Ordem de validação na sessão: `@Valid` roda antes da busca da pauta. Pauta inexistente + duração inválida retorna 400 (`ac25_validacaoAntesDeExistenciaDaPauta`).
- Content-Type errado com corpo no POST de sessão retorna 415 ProblemDetail (`ac25_contentTypeErrado...`).
- Sem autenticação, sem listagem.
- `Sessao` é entidade JPA (persistência assumida, ticket não define).
- Voto: sessão aberta = `fim >= agora` (`existsByPautaIdAndFimGreaterThanEqual`), coerente com "aberta quando now <= fim" do ticket.
- Ordem de validação do voto: pauta (404) → sessão (409) → valor (400) → duplicado (409). Duplicado: checagem prévia `existsByPautaIdAndAssociadoId`; constraint `uk_voto_pauta_associado` como rede de segurança. 409 só quando a causa é essa constraint (`ehViolacaoDeUnicidade`); outra violação de integridade propaga.
- `associadoId` normalizado com `strip()` no construtor de `VotoRequest`, antes de validar e persistir. `"assoc-1 "` após `"assoc-1"` retorna 409.
- `detail` do 409 de voto é genérico (`Associado ja votou nesta pauta`), sem `associadoId`.
- Sessão aberta, para voto e reabertura, é o mesmo critério: `fim >= agora` (`existsByPautaIdAndFimGreaterThanEqual`). `fim == agora` conta como aberta.
- `associadoId` string livre no body, `@NotBlank` e `@Size(max = 255)`. Sem validação de cadastro (gap 1 assumido).
- `GET /pautas/{id}/resultado` retorna `{"sim", "nao", "total"}` com contagem parcial, consultável com sessão aberta (gap 2 assumido). Pauta inexistente 404.
- Voto com sessão fechada ou inexistente: 409 (gap 3 assumido).
- Voto 201 retorna body `{"pautaId", "associadoId", "valor"}` (formato assumido). Sem header `Location`.

## Entregas

- Fatia 1/3 (cadastro e consulta de pauta): `POST /pautas`, `GET /pautas/{id}`, erros 400, 404, 415 e 500 em ProblemDetail. Testes AC-1.1 a AC-1.5 em `PautaControllerTest`, mais casos de erro de framework.
- Fatia 2/3 (abertura de sessão por pauta): `POST /pautas/{id}/sessao` com duração padrão 1 min, validação `1..1440`, 404 para pauta inexistente, bean `Clock`, reabertura 409. Testes AC-2.1 a AC-2.5 em `SessaoControllerTest` (`ac21` a `ac28`).
- Fatia 3/3 (votos e resultado): `POST /pautas/{id}/votos` e `GET /pautas/{id}/resultado`, entidade `Voto` com constraint única, handlers de 409/400 em `TratamentoErrosGlobal`. Testes AC-3.1 a AC-3.8 em `VotoControllerTest` (`ac31` a `ac38`), mais teste de constraint no banco (`ac35_constraintUnica...`), validação de `associadoId` em branco e resultado sem votos. Suite completa: 46 testes verdes (9 pauta, 14 sessão, 23 voto), conferido nos relatórios Surefire.

## Armadilhas

- `spring.jpa.open-in-view` está ativo por padrão (warning no log). Desligar antes de existir relação lazy.
- `JsonPath` usado no teste via import (`com.jayway.jsonpath.JsonPath`) para ler id do POST.
- `ProblemDetail` do 400 de validação usa detail genérico `Requisicao invalida`. Detalhe por campo não exposto.
- `ProblemDetail.forStatusAndDetail` não preenche `title`. Usar o helper `problema(...)` do `TratamentoErrosGlobal`.
- Ids sequenciais e sem autenticação: `GET /pautas/{n}` e `GET /pautas/{n}/resultado` são enumeráveis. Aceito até autenticação.
- `Location` montado com path relativo (`/pautas/{id}`). Pode quebrar atrás de proxy com context path.
- Sem request body cap no código. Limitar no proxy/gateway.
- `@RequestBody(required = false)` no POST de sessão: body ausente cai no padrão de 1 min.
- Lock pessimista só vale dentro da transação de `abrir`. Fora de `@Transactional`, `buscarComLock` não trava.
- `existsByPautaIdAndFimGreaterThanEqual` sem índice em `Sessao.pautaId`: full scan.
- Pauta removida entre checagem e `save` cria sessão órfã. Sem remoção de pauta hoje; aceito.
- `Instant` no JSON depende de `write-dates-as-timestamps` estar off. `ac22`/`ac23` quebram se mudar.
- Voto: a checagem de sessão não trava a pauta. Voto concorrente com fechamento de sessão pode passar; sessão é só por `fim`, sem estado de fechamento explícito.
- Voto: constraint `uk_voto_pauta_associado` é checada por nome. H2 devolve o nome com schema e sufixo (`PUBLIC.UK_VOTO_PAUTA_ASSOCIADO_INDEX_2`), então a comparação é por `contains`, não igualdade.
- Voto: `Voto` usa `@Table(name = "voto")` com `pauta_id` e `associado_id` explícitos. Se mudar o naming strategy, a constraint do teste `ac35_constraintUnica...` precisa acompanhar.
- Resultado conta com duas queries `count`. Sem índice composto além da constraint única (que indexa `pauta_id`+`associado_id`).

## Pendências

- Confirmar com o ticket: `@Valid` (S1), Spring Data JPA e formato do body do GET, do 201 de sessão e do 201 de voto (assumidos).
- Gap 1 (votos): identidade do associado. Assumido `associadoId` no body, sem validação de cadastro. Risco alto: qualquer um vota como qualquer associado. Sem auth (W1).
- Gap 2 (votos): resultado com sessão aberta. Assumido consultável e parcial. Confirmar; afeta contrato do GET.
- Gap 3 (votos): código para sessão fechada ou inexistente. Assumido 409. Confirmar.
- Gap 5: campos da pauta além de `descricao` não definidos. Só `descricao` implementado.
- Reabertura decidida: 409 com sessão aberta (W4 do master em aberto, confirmar com o time).
- Índice em `Sessao.pautaId` ainda pendente.
- Teto de duração 1440 min é placeholder de 1 dia. Confirmar com dono do ticket.
- Escopo global do Jackson (`ConfiguracaoJson` afeta todo ObjectMapper): qualquer campo `Integer` futuro rejeita `5.5` e `"5"`.
- Sem autenticação nem limite de votos ou de criação de sessões: risco de pré-auth aceito.
- Fora de escopo (não implementado): normalizar espaços de `descricao` com `strip()`; `ServletUriComponentsBuilder` para `Location`; `spring.jpa.open-in-view=false`; varredura de dependências (SCA) contra o proxy interno antes do merge.
- Voto (review, decisão do time): resultado parcial público durante sessão aberta. Risco de integridade (quem vê o placar pode ajustar voto). Recomendado retornar só com sessão encerrada; hoje é parcial. Dono a definir.
- Voto (review, premissa): `ValorVoto.de` é case-sensitive (`"sim"` retorna 400). `associadoId` sem política de case.
- Voto (review, premissa): sessão expirada entra na contagem do resultado.
- Voto (review, pré-produção): índice em `Sessao(pautaId, fim)` e query agrupada no resultado em vez de duas `count`.
- Voto (review, armadilha): `catch` do `saveAndFlush` marca a transação como rollback-only. Se alguém engolir a exceção e retornar 201, o commit lança `UnexpectedRollbackException`.
- Voto (review, armadilha): sem FK entre `voto.pauta_id` e `pauta`/`sessao`.
- Voto (review, armadilha): 404 de pauta inexistente vs 409 de sem sessão permite enumerar pautas existentes e abertas. Aceito até auth.
- Voto (review): ordem `@Valid` antes de pauta: `associadoId` em branco com pauta inexistente retorna 400, não 404. Doc de ordem é do domínio de `valor`/sessão/duplicado.
- Épico `wkflow-test-associated-cooperative-service` fecha com esta fatia; pendências acima precisam de confirmação do time antes de produção.
