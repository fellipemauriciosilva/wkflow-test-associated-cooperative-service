# Contexto do projeto

## Módulos

- `domain.pauta`: entidade `Pauta` (JPA) com validação de `titulo`/`descricao` no construtor,
  `PautaRepository` (interface), `PautaInvalidaException`, `PautaNaoEncontradaException`.
- `application.pauta`: `PautaService` (criar, buscarPorId).
- `api.pauta`: `PautaController` (`POST /pautas`, `GET /pautas/{id}`), `PautaExceptionHandler`
  (`@RestControllerAdvice` escopado só ao `PautaController`, cobre 400/404/415), DTOs em
  `api.pauta.dto` (`PautaRequest`, `PautaResponse`, `ErroResponse`).
- `api`: `TamanhoMaximoBodyFilter` (`jakarta.servlet.Filter` global, `@Component`), limita body
  a 16 KB em duas camadas (ver Convenções/Decisões); `MetodoNaoSuportadoHandler`
  (`@RestControllerAdvice` global, não escopado) para 405.
- `infrastructure.persistence`: `PautaJpaRepository` (Spring Data) e `PautaRepositoryImpl`
  (adapta para a interface de domínio).

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Maven.
- H2 em memória (`application.yml`), `ddl-auto: update`, sem Flyway nesta fatia (greenfield,
  schema trivial).
- `mvnd test` — suite completa (34 testes, ~45s, dominado pelo primeiro boot do Spring Context
  no `PautaControllerTest`).
- `mvnd compile` — build rápido pra checar sintaxe.
- Ambiente sem Docker/rede externa: nada de Testcontainers aqui.

## Convenções

- Validação de regras de negócio (título obrigatório, tamanhos máximos) vive no construtor de
  `Pauta`, não em Bean Validation do DTO — AC-1.6 exige validação no domínio.
- Exceções de domínio (`PautaInvalidaException` → 400, `PautaNaoEncontradaException` → 404)
  traduzidas para `ErroResponse{status, mensagem}` via `PautaExceptionHandler`. O mesmo handler
  também cobre `HttpMessageNotReadableException` (JSON malformado/body ausente → 400),
  `MethodArgumentTypeMismatchException` (`GET /pautas/abc` → 400) e
  `HttpMediaTypeNotSupportedException` (Content-Type errado → 415).
- `HttpRequestMethodNotSupportedException` (405) **não** pode ser tratada por um
  `@RestControllerAdvice(assignableTypes = ...)`: essa exceção é lançada durante a resolução
  de rota, antes de o Spring saber qual `HandlerMethod`/controller seria chamado, então um
  advice escopado nunca a intercepta. Por isso vive em `MetodoNaoSuportadoHandler`, um advice
  global separado — único handler desta fatia que não é escopado ao `PautaController`.
- `PautaExceptionHandler` usa `assignableTypes = PautaController.class` pra não vazar pra
  outros controllers que virão nas fatias 2/3.
- Validação de `titulo`/`descricao` usa `isBlank()`/`strip()` (não `trim()`) e
  `codePointCount()` (não `length()`): `trim()` só remove `<= U+0020` e `length()` conta
  UTF-16 units, não caracteres — emoji (surrogate pairs) seriam contados em dobro.
- Título também passa por uma checagem de "tem caractere visível" via `Character.getType`
  (rejeita Zs/Zl/Zp/Cc/Cf/surrogate/unassigned se for só isso) — ver Decisões.
- Body size: `TamanhoMaximoBodyFilter` tem duas defesas independentes, não uma só. Rejeição
  rápida por `Content-Length` é otimização (corta antes de alocar qualquer buffer), não a
  proteção real; a proteção real é o `HttpServletRequestWrapper`/`ServletInputStream`
  customizado que conta bytes lidos e lança exceção ao passar do limite, funcionando mesmo
  sem `Content-Length` (chunked, HTTP/2, etc.).

## Decisões

- Assumidos defaults do task.md sem resposta do PO: `titulo` obrigatório (máx. 200),
  `descricao` opcional (máx. 1000), erro `{status, mensagem}`.
- `titulo` e `descricao` têm `strip()` aplicado na persistência (espaços Unicode nas
  extremidades não contam pro limite nem ficam salvos).
- Sem Bean Validation (`@Valid`/`@NotBlank`) no `PautaRequest`: delegado 100% pro domínio
  (`Pauta`), evitando duplicar regra em duas camadas.
- **Revertida** a decisão anterior sobre NBSP/zero-width (ver histórico git se precisar do
  raciocínio original — foi apontado como achado de segurança M3 em review e corrigido neste
  ciclo). Agora: título composto só por espaços de separação Unicode (Zs/Zl/Zp, incluindo
  NBSP U+00A0), caracteres de formato invisíveis (Cf, incluindo zero-width space U+200B e BOM
  U+FEFF), controle (Cc) ou code points não atribuídos/surrogate é rejeitado como "titulo e
  obrigatorio" — não basta `!isBlank()`, tem que ter pelo menos 1 code point "visível"
  (`Pauta.temCaractereVisivel`). Motivo: um título assim não aparece pra ninguém (nem na UI,
  nem em notificação), mas passava a validação como preenchido — "pauta fantasma" criável em
  massa. NBSP/zero-width continuam aceitos quando há pelo menos 1 caractere visível no título
  (uso legítimo como separador decorativo).
- Limite de tamanho de body: `TamanhoMaximoBodyFilter` tem duas camadas (ver Convenções). A
  primeira versão só checava `Content-Length` e foi apontada em review (H1, HIGH) como
  bypassável por chunked transfer encoding ou qualquer requisição sem esse header — nesse
  caso `getContentLengthLong()` retorna -1, passava a checagem, e o body fluía sem limite até
  o Jackson, em endpoint sem autenticação (DoS de heap trivial). Corrigido com um
  `HttpServletRequestWrapper` que envolve o `InputStream` e conta bytes efetivamente lidos,
  lançando uma `RuntimeException` interna (não `IOException`, de propósito: o Spring MVC
  converte `IOException` do parsing em `HttpMessageNotReadableException`/400 antes de chegar
  ao filtro — precisava atravessar essa camada sem ser traduzida) ao ultrapassar 16 KB. A
  claim anterior de que isso era "defesa em profundidade, não a única camada" era falsa: não
  havia outra camada. Agora há defesa real.
- Sem autenticação/autorização e sem ofuscação de id (`IDENTITY` sequencial) em
  `POST /pautas`/`GET /pautas/{id}`: decisão registrada no README (seção "Segurança"), não
  implementada. Reavaliar obrigatoriamente antes da fatia 3 (voto/resultado), onde IDOR por
  enumeração de id passa a ter impacto real. Risco correlato não mitigado: sem auth, nada
  limita quantas pautas um cliente cria nem a taxa de requisições (M6 do review) — registrado
  como limitação conhecida, não como decisão de produto confortável.

## Entregas

- Fatia 1/3 (H90-17): AC-1.1 a AC-1.6 cobertos, mais dois ciclos de correção de review.
  - Ciclo 1: handler de erro, Unicode (codePointCount/strip), limite de body (v1, só
    Content-Length), mensagens exatas em 400/404.
  - Ciclo 2 (este): fecha bypass de limite de body via leitura real do stream (H1), reforça
    testes de mensagem exata em 400 de JSON malformado/id inválido e em 413 (B1/B2/M4/M5),
    adiciona handlers de 415/405 sem vazar formato default do Spring (M1), rejeita título
    "fantasma" sem caractere visível (M3).
  - `PautaTest` (18 testes): validação de domínio — título nulo/vazio/espaços, limites de
    tamanho por code point, strip, em space, NBSP/zero-width/BOM isolados (agora rejeitados),
    NBSP/zero-width misturados com conteúdo visível (aceitos), emoji no limite.
  - `PautaControllerTest` (14 testes): contrato HTTP via MockMvc — 201, 400 (sem título,
    título em branco, título/descrição acima do limite, JSON malformado com mensagem exata,
    id não numérico com mensagem exata), 200, 404, 413 (body grande e 1 byte acima do limite,
    com mensagem exata), 415 (Content-Type errado), 405 (DELETE não suportado), descrição no
    limite exato.
  - `TamanhoMaximoBodyFilterTest` (2 testes, novo): testa o filtro isolado com
    `MockHttpServletRequest` customizado que simula `getContentLengthLong() == -1` (cenário
    chunked/sem header) — prova que o corte por bytes lidos funciona mesmo quando o header
    não ajuda. Não dá pra provar isso via MockMvc porque o builder sempre deriva
    Content-Length do array de bytes do payload.
- Suite completa verde: `mvnd test` → 34 testes (18 + 14 + 2), 0 falhas.

## Armadilhas

- `mvn`/`mvnd` imprime warnings de `jline`/`jansi` (`UnsatisfiedLinkError`, lib em `/tmp`
  não executável) — inofensivo, não afeta o build. Não tentar "corrigir" isso.
- `PautaControllerTest` sobe o Spring Context inteiro (`@SpringBootTest`); primeira execução
  leva ~40s. Normal.
- MockMvc (`MockHttpServletRequestBuilder.content(...)`) sempre calcula `Content-Length` a
  partir do array de bytes passado — não existe jeito de simular, por esse caminho, uma
  requisição chunked ou sem esse header. Para testar esse cenário é preciso ir direto no
  filtro com `MockHttpServletRequest` subclassado (ver `TamanhoMaximoBodyFilterTest`).
- Não dá pra testar "16 KB exatos → 201" de ponta a ponta: a `descricao` tem limite de domínio
  de 1000 caracteres, então nenhum JSON de request legítimo chega perto de 16 KB. O limite de
  body é maior que qualquer payload válido de propósito (margem de segurança), não um limite
  "justo".

## Pendências

- Fatias 2 (sessão) e 3 (voto/resultado) do épico H90-17 ainda não implementadas.
- `GET /pautas` (listagem) é Could nesta fatia, sem entry point — não implementado.
- Dúvidas do task.md (campos obrigatórios, limites de tamanho, formato de erro) seguem como
  assumidas; confirmar com PO se divergir nas próximas fatias.
- Auth e exposição de id sequencial em `GET /pautas/{id}` (ver seção Decisões): decidido
  "aceitar nesta fatia", mas obrigatório revisitar antes de implementar voto/resultado na
  fatia 3 — ou seja, antes da PRÓXIMA fatia, não depois.
- Rate limiting / limite de quantidade de pautas por cliente (M6 do review): não implementado,
  sem auth não há "cliente" identificável pra limitar de forma significativa. Decisão real
  depende de haver autenticação primeiro.
- `spring-boot-starter-validation` no `pom.xml` sem uso (nenhuma anotação Bean Validation):
  dependência morta apontada no review, não removida neste ciclo por não estar na lista de
  mudanças obrigatórias. Remover quando tocar no `pom.xml` de novo.
- `PautaNaoEncontradaException(Long id)`: parâmetro `id` ainda ignorado (mensagem hardcoded
  "Pauta nao encontrada"). Apontado como não bloqueante no review; não tocado neste ciclo.
- `TamanhoMaximoBodyFilter` não drena o corpo da requisição antes de responder 413 (apontado
  como L5 no review): alguns containers podem resetar a conexão em vez de entregar a resposta
  em certas condições de rede. Não verificado contra um container real (só contra o servlet
  stack de teste do Spring); revisitar se houver relato de 413 não chegando ao cliente.
