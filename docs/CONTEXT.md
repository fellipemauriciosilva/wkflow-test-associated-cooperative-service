# Contexto do projeto

## Spec

Fatia 1/3 do épico H90-17 (cadastro e consulta de pautas). AC-1.1 a AC-1.6 → teste:

| AC | Descrição | Teste(s) |
|---|---|---|
| AC-1.1 | POST válido → 201 com id | `PautaControllerTest.criar_comDadosValidos_retorna201ComId` |
| AC-1.2 | POST sem campo obrigatório → 400 | `PautaControllerTest.criar_semTitulo_retorna400`/`criar_comTituloEmBranco_retorna400`, `PautaTest.construtor_comTituloInvalido_lancaExcecao` (parametrizado) |
| AC-1.3 | GET existente → 200 | `PautaControllerTest.buscarPorId_comPautaExistente_retorna200` |
| AC-1.4 | GET inexistente → 404 | `PautaControllerTest.buscarPorId_comPautaInexistente_retorna404` |
| AC-1.5 | 400/404 com `{status, mensagem}` | asserções `jsonPath("$.status")`/`"$.mensagem"` em `PautaControllerTest` (400/404/413/415/405) |
| AC-1.6 | Título obrigatório validado no domínio | `PautaTest` inteiro (23 testes) |

Decisões sobre NBSP/zero-width/emoji não vêm do task.md — extrapolação da regra "titulo
obrigatório, não vazio" (AC-1.2/1.6) pra evitar bypass por caractere invisível. Ver Decisões.

## Módulos

- `domain.pauta`: `Pauta` (JPA, valida `titulo`/`descricao` no construtor), `PautaRepository`,
  `PautaInvalidaException`, `PautaNaoEncontradaException`.
- `application.pauta`: `PautaService` (criar, buscarPorId).
- `api.pauta`: `PautaController`, `PautaExceptionHandler` (escopado ao `PautaController`,
  cobre 400/404/413/415 — ver Convenções sobre o 413), DTOs em `api.pauta.dto`.
- `api`: `TamanhoMaximoBodyFilter` (filtro global, limite de 16 KB), `CorpoExcedeTamanhoMaximoHandler`
  (advice global, fallback — ver Convenções), `MetodoNaoSuportadoHandler` (advice global, 405).
- `infrastructure.persistence`: `PautaJpaRepository` + `PautaRepositoryImpl`.

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Maven, H2 em memória (`ddl-auto: update`), sem Flyway.
- `mvnd test` — suite completa (46 testes, ~50s; boot de Spring Context em
  `PautaControllerTest` e Tomcat real em `CorpoExcedeTamanhoMaximoE2ETest`).
- Sem Docker/rede externa: nada de Testcontainers. Teste e2e de chunked usa
  `@SpringBootTest(webEnvironment = RANDOM_PORT)` (Tomcat embarcado) + `java.net.Socket` crudo.

## Convenções

- Regras de negócio no construtor de `Pauta`, não em Bean Validation do DTO (AC-1.6 exige
  validação no domínio).
- `PautaExceptionHandler` traduz `PautaInvalidaException`→400, `PautaNaoEncontradaException`→404,
  `HttpMessageNotReadableException`→400 (JSON malformado/ausente),
  `MethodArgumentTypeMismatchException`→400 (`GET /pautas/abc`), `HttpMediaTypeNotSupportedException`→415.
- **413 tem três pontos de interceptação, propositais, não redundância acidental:**
  1. `PautaExceptionHandler.tratarJsonInvalido` desembrulha a cadeia de causas de
     `HttpMessageNotReadableException` procurando `CorpoExcedeTamanhoMaximoException` antes de
     decidir 400 vs 413. Necessário porque Jackson (via `RequestResponseBodyMethodProcessor`)
     embrulha QUALQUER exceção lançada durante a leitura do `InputStream` — inclusive a nossa —
     em `JsonMappingException`→`HttpMessageNotReadableException`. Sem desembrulhar, o limite de
     body sempre caía como "400 JSON inválido", nunca 413, no caminho real de `@RequestBody`
     (só descoberto com teste e2e real; o teste unitário do filtro isolado não pegava isso).
  2. `CorpoExcedeTamanhoMaximoHandler` (global, não escopado): fallback pra rotas sem um
     handler `PautaExceptionHandler`-like escopado (relevante pras fatias 2/3).
  3. Catch em `TamanhoMaximoBodyFilter.doFilter`: última linha de defesa, caso a exceção
     escape do ciclo de `DispatcherServlet.doDispatch` sem passar por nenhum advice.
- `HttpRequestMethodNotSupportedException` (405) é lançada na resolução de rota, antes do
  Spring saber o controller — um advice escopado nunca intercepta. Por isso
  `MetodoNaoSuportadoHandler` é global, e preenche `Allow` via `ex.getSupportedHttpMethods()`
  (RFC 9110 §15.5.6 exige isso; um `@ExceptionHandler` customizado não preenche de graça).
- Validação usa `isBlank()`/`strip()` (não `trim()`, que só remove `<= U+0020`) e
  `codePointCount()` (não `length()`, que conta UTF-16 units — emoji dobrariam).
- Título passa por checagem de "tem caractere visível" (`Character.getType`, rejeita
  Zs/Zl/Zp/Cc/Cf/surrogate/unassigned se for só isso) — ver Decisões. Cada categoria tem teste
  em `PautaTest` (NBSP Zs, U+2028/U+2029 Zl/Zp, ZWSP/BOM Cf, NEL U+0085 Cc, U+0378 unassigned,
  U+D800 surrogate).
- `TamanhoMaximoBodyFilter.RequisicaoComLimiteDeBytes` cacheia UM stream contado por request
  (sem isso, `getInputStream()` chamado duas vezes resetava a cota) e sobrescreve `getReader()`
  pra delegar a `getInputStream()` (sem isso, leitura via `getReader()` furava o contador —
  `HttpServletRequestWrapper.getReader()` por padrão ignora o wrapper). `Content-Length` acima
  do limite é só otimização (corta antes de alocar buffer); a proteção real é a contagem de
  bytes efetivamente lidos.

## Decisões

- Defaults assumidos sem resposta do PO: `titulo` obrigatório (máx. 200), `descricao` opcional
  (máx. 1000), erro `{status, mensagem}`. `strip()` aplicado na persistência.
- Sem Bean Validation no `PautaRequest`: delegado 100% ao domínio.
- Título só com Zs/Zl/Zp (incl. NBSP)/Cf (incl. ZWSP/BOM)/Cc/unassigned/surrogate é rejeitado
  como "titulo e obrigatorio" — não aparece pra ninguém, mas passava `!isBlank()` como
  preenchido ("pauta fantasma"). NBSP/ZWSP aceitos se houver ≥1 caractere visível (uso como
  separador decorativo). **Gap conhecido:** é checagem por categoria Unicode, não visual real —
  Hangul Filler U+3164, U+115F/U+1160, Braille Blank U+2800 (categoria `Lo`/`So`) passam como
  "visíveis" apesar de invisíveis na prática. Decidir com PO se vale lista de exceção.
- Limite de body 16 KB via contagem real de bytes lidos, não só `Content-Length` — cobre
  chunked/sem header confiável. **Confirmado com e2e real** (`CorpoExcedeTamanhoMaximoE2ETest`,
  socket crudo chunked contra Tomcat embarcado de verdade) que o 413 chega ao cliente — a
  claim anterior de "funciona sem Content-Length" só tinha sido testada no filtro isolado, que
  não passava pelo `DispatcherServlet`/Jackson e não pegava o bug do ponto 1 de Convenções.
  HTTP/2 não é testado em lugar nenhum — não afirmar que funciona.
- Sem auth/autorização e sem ofuscação de id (`IDENTITY` sequencial): decisão registrada no
  README ("Segurança"), não implementada. **Obrigatório revisitar antes da fatia 3** (IDOR por
  enumeração de id vira problema real com voto/resultado). Rate limiting também depende de
  haver auth primeiro — não implementado.

## Entregas

- Fatia 1/3: AC-1.1–1.6 cobertos (ver Spec), três ciclos de correção de review.
  - Ciclo 1: handler de erro, Unicode básico, limite de body v1 (só Content-Length).
  - Ciclo 2: 415/405, "título fantasma" parcial, limite de body v2 (stream contado, mas só
    testado isolado — não ponta a ponta).
  - Ciclo 3 (este): 413 não chegava ao cliente no caminho real de `@RequestBody` (Jackson
    mascarava como 400) — corrigido com desembrulho de causa + teste e2e real (socket chunked
    contra Tomcat embarcado). `getReader()` bypassável e contador resetável por
    `getInputStream()` repetido, nunca exercidos em teste — corrigidos e testados. Header
    `Allow` em 405. Categorias Unicode faltantes (Zl/Zp/Cc/unassigned/surrogate) + mensagem
    exata em todas as variantes de título inválido.
- `PautaTest` (23): domínio — título nulo/vazio/espaço/toda categoria Unicode "fantasma" (com
  mensagem exata), limites por code point, strip, emoji no limite/acima.
- `PautaControllerTest` (14): contrato HTTP — 201/400/200/404/413/415/405(com Allow).
- `TamanhoMaximoBodyFilterTest` (6): filtro isolado — corte acima do limite, boundary exato de
  16 KB, `getInputStream()` 2x não reseta, `getReader()` cortado, exceção não relacionada
  propaga inalterada.
- `CorpoExcedeTamanhoMaximoE2ETest` (3, novo): Tomcat real + socket chunked — 413 chunked
  acima do limite, 201 chunked dentro do limite, JSON malformado chunked continua 400.
- Suite completa verde: `mvnd test` → 46 testes, 0 falhas.

## Armadilhas

- `mvn`/`mvnd` imprime warnings de `jline`/`jansi` (`UnsatisfiedLinkError`) — inofensivo.
- `PautaControllerTest`/`CorpoExcedeTamanhoMaximoE2ETest` sobem contexto/Tomcat real; primeira
  execução de cada ~40s. Normal.
- MockMvc sempre deriva `Content-Length` do `byte[]` do payload — nunca produz chunked de
  verdade. Pra testar o filtro isolado nesse cenário, usar `MockHttpServletRequest`
  subclassado; pra provar ponta a ponta (incluindo `DispatcherServlet`/Jackson), é preciso
  socket crudo contra servidor real — o teste isolado do filtro NÃO prova o caminho real (foi
  assim que o bug do Jackson mascarando a exceção passou batido em ciclos anteriores).
- Teste e2e com JSON malformado: usar corpo inválido desde o primeiro byte (ex. `"a".repeat(N)`
  sem aspas) faz o Jackson falhar rápido por token, não por tamanho — mascara bug de limite.
  Pra "corpo grande e válido o bastante pra estourar o limite", usar JSON válido com campo
  string gigante.
- Não dá pra testar "16 KB exatos → 201" com payload de domínio legítimo: `descricao` tem
  limite de 1000 caracteres, nenhum JSON válido chega perto de 16 KB. O boundary exato É
  testado, mas no filtro isolado com corpo sintético fora dos limites de domínio.

## Pendências

- Fatias 2 (sessão) e 3 (voto/resultado) não implementadas. `GET /pautas` (listagem, Could)
  não implementado.
- Dúvidas do task.md seguem assumidas; confirmar com PO se divergir nas próximas fatias.
- Auth/id sequencial (ver Decisões): obrigatório revisitar antes da fatia 3, não depois.
- `spring-boot-starter-validation` no `pom.xml` sem uso — remover quando tocar no `pom.xml`.
- `PautaNaoEncontradaException(Long id)`: parâmetro `id` ignorado (mensagem hardcoded).
- `TamanhoMaximoBodyFilter` não drena o corpo antes de responder 413: e2e deste ciclo prova
  que no Tomcat embarcado padrão a resposta chega, mas não testado contra proxy/LB
  intermediário. Revisitar se houver relato de 413 não chegando ao cliente em produção.
- "Título fantasma" é checagem por categoria Unicode, não visual real (ver Decisões) —
  decisão de produto pendente sobre Hangul Filler/Braille Blank/etc.
