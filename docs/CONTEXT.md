# Contexto do projeto

## Módulos

- `br.acme.wkflow.pauta`: entidade `Pauta`, `PautaRepository`, `PautaController` (`POST /pautas`, `GET /pautas/{id}`), DTOs `PautaRequest`, `PautaCriadaResponse`, `PautaResponse`, exceção `PautaNaoEncontradaException`.
- `br.acme.wkflow.erro`: `TratamentoErrosGlobal` (`@RestControllerAdvice`), converte erros em `ProblemDetail`, inclui catch-all 500.
- `br.acme.wkflow`: `WkflowApplication` (entrypoint Spring Boot).

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Spring MVC, Spring Data JPA, Bean Validation, H2 em memória, Maven.
- Testes: `mvnd test` (suite inteira, sem `clean`). Um teste: `mvnd test -Dtest='Classe#metodo'`.
- Dependências resolvem pelo proxy interno (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`). Sem internet.

## Convenções

- Testes de controller com `@SpringBootTest` + `@AutoConfigureMockMvc`, nomeados por critério de aceite (`ac11_...` a `ac15_...`).
- Erros de API sempre `application/problem+json` via `ProblemDetail`. `spring.mvc.problemdetails.enabled=true` cobre erros do framework (404, 405, 415, type mismatch).
- Pacote por feature (`pauta`), tratamento de erro global em `erro`.

## Decisões

- Id gerado pelo banco (`IDENTITY`). H2 em memória aceito para o épico.
- `descricao` validada com `@NotBlank` no DTO de request. Vazia, só espaços, nula ou ausente retornam 400.
- Tamanho máximo de `descricao`: 255 caracteres. `@Size(max = 255)` no DTO e `@Column(length = 255)` na entidade, para que o limite de validação e o do banco coincidam. Acima disso retorna 400.
- POST retorna 201 com header `Location` e body `{"id": n}`.
- GET retorna `{"id": n, "descricao": "..."}`. Id inexistente retorna 404 ProblemDetail.
- Erro inesperado retorna 500 ProblemDetail com detail genérico, loga a exceção e nunca expõe `ex.getMessage()`.
- Sem `Clock`, sem autenticação, sem listagem nesta fatia.
- Entidade `Pauta` com construtor `protected` para JPA e construtor público para o domínio.

## Entregas

- Fatia 1/3 (cadastro e consulta de pauta): `POST /pautas`, `GET /pautas/{id}`, erros 400, 404, 415 e 500 em ProblemDetail. Testes AC-1.1 a AC-1.5 em `PautaControllerTest`, mais casos de erro de framework (GET id não numérico, Content-Type errado, descrição de 256 caracteres).

## Armadilhas

- `spring.jpa.open-in-view` está ativo por padrão (warning no log). Não afeta esta fatia. Desligar antes de existir relação lazy.
- `JsonPath` usado no teste via import (`com.jayway.jsonpath.JsonPath`) para ler id do POST.
- `ProblemDetail` do 400 de validação usa detail genérico `Requisicao invalida`. Detalhe por campo não exposto nesta fatia.
- `ProblemDetail.forStatusAndDetail` não preenche `title`. Usar o helper `problema(...)` do `TratamentoErrosGlobal`.
- Ids sequenciais e sem autenticação: `GET /pautas/{n}` é enumerável e gaps do IDENTITY vazam contagem de linhas. Aceito na fatia 1, registrar antes da fatia 2 ou da autenticação.
- `Location` montado com path relativo (`/pautas/{id}`). Pode quebrar atrás de proxy com context path. Ver pendências.
- Sem request body cap no código. Limitar no proxy/gateway. `max-http-form-post-size` não cobre body JSON.

## Pendências

- Confirmar com o ticket: `@Valid` (S1), Spring Data JPA e formato do body do GET (assumidos).
- Gap 5: campos da pauta além de `descricao` não definidos. Só `descricao` implementado.
- Fatia 2 (sessão e votos) depende de `Clock` (S2), ainda não introduzido.
- Fora de escopo nesta correção (não implementado): normalizar espaços de `descricao` com `strip()` no DTO; `ServletUriComponentsBuilder` para `Location`; `spring.jpa.open-in-view=false`; varredura de dependências (SCA) contra o proxy interno antes do merge.
