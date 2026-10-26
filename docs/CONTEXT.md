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

- Testes de controller com `@SpringBootTest` + `@AutoConfigureMockMvc`, nomeados por critério de aceite (`ac11_...` a `ac15_...` na pauta, `ac21_...` a `ac25_...` na sessão).
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
- Duração validada com `@Min(1)` no `SessaoRequest` (0 e negativos retornam 400). Tipo não inteiro retorna 400 via desserialização.
- Pauta inexistente no POST de sessão retorna 404 via `existsById` antes de criar a sessão.
- Sem autenticação, sem listagem, sem limite superior de duração (ticket não define teto).
- `Sessao` é entidade JPA (persistência assumida, ticket não define).

## Entregas

- Fatia 1/3 (cadastro e consulta de pauta): `POST /pautas`, `GET /pautas/{id}`, erros 400, 404, 415 e 500 em ProblemDetail. Testes AC-1.1 a AC-1.5 em `PautaControllerTest`, mais casos de erro de framework (GET id não numérico, Content-Type errado, descrição de 256 caracteres).
- Fatia 2/3 (abertura de sessão por pauta): `POST /pautas/{id}/sessao` com duração padrão 1 min, validação `>= 1`, 404 para pauta inexistente, bean `Clock`. Testes AC-2.1 a AC-2.5 em `SessaoControllerTest` (`Clock` fixo), mais caso de duração não inteira. Reabertura de sessão NÃO implementada (gap bloqueante, ver Pendências).

## Armadilhas

- `spring.jpa.open-in-view` está ativo por padrão (warning no log). Não afeta esta fatia. Desligar antes de existir relação lazy.
- `JsonPath` usado no teste via import (`com.jayway.jsonpath.JsonPath`) para ler id do POST.
- `ProblemDetail` do 400 de validação usa detail genérico `Requisicao invalida`. Detalhe por campo não exposto nesta fatia.
- `ProblemDetail.forStatusAndDetail` não preenche `title`. Usar o helper `problema(...)` do `TratamentoErrosGlobal`.
- Ids sequenciais e sem autenticação: `GET /pautas/{n}` é enumerável e gaps do IDENTITY vazam contagem de linhas. Aceito na fatia 1, registrar antes da fatia 2 ou da autenticação.
- `Location` montado com path relativo (`/pautas/{id}`). Pode quebrar atrás de proxy com context path. Ver pendências.
- Sem request body cap no código. Limitar no proxy/gateway. `max-http-form-post-size` não cobre body JSON.
- `@RequestBody(required = false)` no POST de sessão: body ausente cai no padrão de 1 min. `Content-Type` errado continua 415.
- Segunda abertura de sessão para a mesma pauta cria outra linha sem checar a anterior. Comportamento de reabertura (substituir, estender ou 409) pendente.

## Pendências

- Confirmar com o ticket: `@Valid` (S1), Spring Data JPA e formato do body do GET e do 201 de sessão (assumidos).
- Gap 5: campos da pauta além de `descricao` não definidos. Só `descricao` implementado.
- Gap bloqueante (fatia 2): reabrir sessão já aberta. Substituir, estender ou rejeitar com 409 (assumido no master, W4 em aberto). Não implementado; sem teste para o caso.
- Confirmar: duração `null` explícita como padrão (assumido); teto superior de duração (não definido).
- Fatia 3 (votos) depende de `Sessao` e do comportamento de reabertura.
- Fora de escopo nesta correção (não implementado): normalizar espaços de `descricao` com `strip()` no DTO; `ServletUriComponentsBuilder` para `Location`; `spring.jpa.open-in-view=false`; varredura de dependências (SCA) contra o proxy interno antes do merge.
