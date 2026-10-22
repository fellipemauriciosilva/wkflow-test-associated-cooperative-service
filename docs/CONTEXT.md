# Contexto do projeto

## Módulos

- `domain.pauta`: entidade `Pauta` (JPA) com validação de `titulo`/`descricao` no construtor,
  `PautaRepository` (interface), `PautaInvalidaException`, `PautaNaoEncontradaException`.
- `application.pauta`: `PautaService` (criar, buscarPorId).
- `api.pauta`: `PautaController` (`POST /pautas`, `GET /pautas/{id}`), `PautaExceptionHandler`
  (`@RestControllerAdvice` escopado só ao `PautaController`), DTOs em `api.pauta.dto`
  (`PautaRequest`, `PautaResponse`, `ErroResponse`).
- `api`: `TamanhoMaximoBodyFilter` (`jakarta.servlet.Filter` global, `@Component`), rejeita
  `Content-Length` > 16 KB com 413 antes de o body ser desserializado.
- `infrastructure.persistence`: `PautaJpaRepository` (Spring Data) e `PautaRepositoryImpl`
  (adapta para a interface de domínio).

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Maven.
- H2 em memória (`application.yml`), `ddl-auto: update`, sem Flyway nesta fatia (greenfield,
  schema trivial).
- `mvnd test` — suite completa (27 testes, ~45s, dominado pelo primeiro boot do Spring Context
  no `PautaControllerTest`).
- `mvnd compile` — build rápido pra checar sintaxe.
- Ambiente sem Docker/rede externa: nada de Testcontainers aqui.

## Convenções

- Validação de regras de negócio (título obrigatório, tamanhos máximos) vive no construtor de
  `Pauta`, não em Bean Validation do DTO — AC-1.6 exige validação no domínio.
- Exceções de domínio (`PautaInvalidaException` → 400, `PautaNaoEncontradaException` → 404)
  traduzidas para `ErroResponse{status, mensagem}` via `PautaExceptionHandler`. O mesmo handler
  também cobre `HttpMessageNotReadableException` (JSON malformado/body ausente → 400) e
  `MethodArgumentTypeMismatchException` (`GET /pautas/abc` → 400), pra nenhum erro de contrato
  cair no formato default do Spring.
- `PautaExceptionHandler` usa `assignableTypes = PautaController.class` pra não vazar pra
  outros controllers que virão nas fatias 2/3.
- Validação de `titulo`/`descricao` usa `isBlank()`/`strip()` (não `trim()`) e
  `codePointCount()` (não `length()`): `trim()` só remove `<= U+0020` e `length()` conta
  UTF-16 units, não caracteres — emoji (surrogate pairs) seriam contados em dobro.

## Decisões

- Assumidos defaults do task.md sem resposta do PO: `titulo` obrigatório (máx. 200),
  `descricao` opcional (máx. 1000), erro `{status, mensagem}`.
- `titulo` e `descricao` têm `strip()` aplicado na persistência (espaços Unicode nas
  extremidades não contam pro limite nem ficam salvos; ver nota sobre NBSP/zero-width abaixo).
- Sem Bean Validation (`@Valid`/`@NotBlank`) no `PautaRequest`: delegado 100% pro domínio
  (`Pauta`), evitando duplicar regra em duas camadas.
- NBSP (U+00A0) e zero-width space (U+200B) não são tratados como espaço em branco: decisão
  consciente de usar `Character.isWhitespace` (via `isBlank()`/`strip()` do JDK) em vez de
  normalização Unicode mais agressiva (ex.: `\p{Z}` ou remoção de zero-width). Um título
  composto só por esses caracteres é aceito como "não vazio" e persistido como está. Mudar
  se o PO/produto reportar título "fantasma" em produção.
- Limite de tamanho de body: filtro `TamanhoMaximoBodyFilter` (16 KB), checa só
  `Content-Length`. Não cobre chunked transfer encoding sem esse header — aceito nesta fatia
  porque é defesa em profundidade (corpo real é no máximo ~1.2 KB com os limites de
  título/descrição), não a única camada de proteção.
- Sem autenticação/autorização e sem ofuscação de id (`IDENTITY` sequencial) em
  `POST /pautas`/`GET /pautas/{id}`: decisão registrada no README (seção "Segurança"), não
  implementada. Reavaliar obrigatoriamente antes da fatia 3 (voto/resultado), onde IDOR por
  enumeração de id passa a ter impacto real.

## Entregas

- Fatia 1/3 (H90-17) completa: AC-1.1 a AC-1.6 cobertos, mais ciclo de correção de review
  (handler de erro, Unicode, limite de body, mensagens exatas).
  - `PautaTest` (16 testes): validação de domínio (título nulo/vazio/espaços, limites de
    tamanho por code point, strip, NBSP/em space/zero-width space, emoji no limite).
  - `PautaControllerTest` (11 testes): contrato HTTP via MockMvc — 201, 400 (sem título,
    título em branco, título/descrição acima do limite, JSON malformado, id não numérico),
    200, 404, 413 (body grande), descrição no limite exato.
- Suite completa verde: `mvnd test` → 27 testes, 0 falhas.

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
- Auth e exposição de id sequencial em `GET /pautas/{id}` (ver seção Decisões): decidido
  "aceitar nesta fatia", mas obrigatório revisitar antes de implementar voto/resultado na
  fatia 3 — ou seja, antes da PRÓXIMA fatia, não depois.
- `spring-boot-starter-validation` no `pom.xml` sem uso (nenhuma anotação Bean Validation):
  dependência morta apontada no review, não removida neste ciclo por não estar na lista de
  mudanças obrigatórias. Remover quando tocar no `pom.xml` de novo.
- `PautaNaoEncontradaException(Long id)`: parâmetro `id` ainda ignorado (mensagem hardcoded
  "Pauta nao encontrada"). Apontado como não bloqueante no review; não tocado neste ciclo.
