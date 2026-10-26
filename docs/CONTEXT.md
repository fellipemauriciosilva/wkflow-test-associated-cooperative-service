# Contexto do projeto

## Módulos

- `br.acme.wkflow.pauta`: entidade `Pauta`, `PautaRepository`, `PautaController` (`POST /pautas`, `GET /pautas/{id}`), DTOs `PautaRequest`, `PautaCriadaResponse`, `PautaResponse`, exceção `PautaNaoEncontradaException`.
- `br.acme.wkflow.erro`: `TratamentoErrosGlobal` (`@RestControllerAdvice`), converte erros em `ProblemDetail`.
- `br.acme.wkflow`: `WkflowApplication` (entrypoint Spring Boot).

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Spring MVC, Spring Data JPA, Bean Validation, H2 em memória, Maven.
- Testes: `mvnd test` (suite inteira, sem `clean`). Um teste: `mvnd test -Dtest='Classe#metodo'`.
- Dependências resolvem pelo proxy interno (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`). Sem internet.

## Convenções

- Testes de controller com `@SpringBootTest` + `@AutoConfigureMockMvc`, nomeados por critério de aceite (`ac11_...` a `ac15_...`).
- Erros de API sempre `application/problem+json` via `ProblemDetail`.
- Pacote por feature (`pauta`), tratamento de erro global em `erro`.

## Decisões

- Id gerado pelo banco (`IDENTITY`). H2 em memória aceito para o épico.
- `descricao` validada com `@NotBlank` no DTO de request. Vazia, só espaços, nula ou ausente retornam 400.
- POST retorna 201 com header `Location` e body `{"id": n}`.
- GET retorna `{"id": n, "descricao": "..."}`. Id inexistente retorna 404 ProblemDetail.
- Sem `Clock`, sem autenticação, sem listagem nesta fatia.
- Entidade `Pauta` com construtor `protected` para JPA e construtor público para o domínio.

## Entregas

- Fatia 1/3 (cadastro e consulta de pauta): `POST /pautas`, `GET /pautas/{id}`, erros 400 e 404 em ProblemDetail. Testes AC-1.1 a AC-1.5 em `PautaControllerTest`.

## Armadilhas

- `spring.jpa.open-in-view` está ativo por padrão (warning no log). Não afeta esta fatia.
- `JsonPath` usado no teste via FQN (`com.jayway.jsonpath.JsonPath`) para ler id do POST.
- `ProblemDetail` do 400 de validação usa título genérico. Detalhe por campo não exposto nesta fatia.

## Pendências

- Confirmar com o ticket: `@Valid` (S1), Spring Data JPA e formato do body do GET (assumidos).
- Gap 5: campos da pauta além de `descricao` não definidos. Só `descricao` implementado.
- Fatia 2 (sessão e votos) depende de `Clock` (S2), ainda não introduzido.
