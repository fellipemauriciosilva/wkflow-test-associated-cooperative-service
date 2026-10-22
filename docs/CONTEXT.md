# Contexto do projeto

## Módulos

- `domain.pauta`: entidade `Pauta` (JPA) com validação de `titulo`/`descricao` no construtor,
  `PautaRepository` (interface), `PautaInvalidaException`, `PautaNaoEncontradaException`.
- `application.pauta`: `PautaService` (criar, buscarPorId).
- `api.pauta`: `PautaController` (`POST /pautas`, `GET /pautas/{id}`), `PautaExceptionHandler`
  (`@RestControllerAdvice` escopado só ao `PautaController`), DTOs em `api.pauta.dto`
  (`PautaRequest`, `PautaResponse`, `ErroResponse`).
- `infrastructure.persistence`: `PautaJpaRepository` (Spring Data) e `PautaRepositoryImpl`
  (adapta para a interface de domínio).

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Maven.
- H2 em memória (`application.yml`), `ddl-auto: update`, sem Flyway nesta fatia (greenfield,
  schema trivial).
- `mvnd test` — suite completa (14 testes, ~45s, dominado pelo primeiro boot do Spring Context
  no `PautaControllerTest`).
- `mvnd compile` — build rápido pra checar sintaxe.
- Ambiente sem Docker/rede externa: nada de Testcontainers aqui.

## Convenções

- Validação de regras de negócio (título obrigatório, tamanhos máximos) vive no construtor de
  `Pauta`, não em Bean Validation do DTO — AC-1.6 exige validação no domínio.
- Exceções de domínio (`PautaInvalidaException` → 400, `PautaNaoEncontradaException` → 404)
  traduzidas para `ErroResponse{status, mensagem}` via `PautaExceptionHandler`.
- `PautaExceptionHandler` usa `assignableTypes = PautaController.class` pra não vazar pra
  outros controllers que virão nas fatias 2/3.

## Decisões

- Assumidos defaults do task.md sem resposta do PO: `titulo` obrigatório (máx. 200),
  `descricao` opcional (máx. 1000), erro `{status, mensagem}`.
- `titulo` tem `trim()` aplicado na persistência (espaços nas extremidades não contam pro
  limite nem ficam salvos).
- Sem Bean Validation (`@Valid`/`@NotBlank`) no `PautaRequest`: delegado 100% pro domínio
  (`Pauta`), evitando duplicar regra em duas camadas.

## Entregas

- Fatia 1/3 (H90-17) completa: AC-1.1 a AC-1.6 cobertos.
  - `PautaTest` (9 testes): validação de domínio (título nulo/vazio/espaços, limites de
    tamanho, trim).
  - `PautaControllerTest` (5 testes): contrato HTTP via MockMvc (201, 400 x2, 200, 404).
- Suite completa verde: `mvnd test` → 14 testes, 0 falhas.

## Armadilhas

- `mvn`/`mvnd` imprime warnings de `jline`/`jansi` (`UnsatisfiedLinkError`, lib em `/tmp`
  não executável) — inofensivo, não afeta o build. Não tentar "corrigir" isso.
- `PautaControllerTest` sobe o Spring Context inteiro (`@SpringBootTest`); primeira execução
  leva ~40s. Normal.

## Pendências

- Fatias 2 (sessão) e 3 (voto/resultado) do épico H90-17 ainda não implementadas.
- `GET /pautas` (listagem) é Could nesta fatia, sem entry point — não implementado.
- Dúvidas do task.md (campos obrigatórios, limites de tamanho, formato de erro) seguem como
  assumidas; confirmar com PO se divergir nas próximas fatias.
