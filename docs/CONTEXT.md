# Contexto do projeto

## Módulos

- `br.acme.pauta`: fatia 1 (cadastro e consulta de pauta) e fatia 2 (abertura de sessão de votação). Único pacote até agora.
  - Fatia 1: `Pauta` (entidade JPA), `PautaRepository` (com `findComLockPorId`, `PESSIMISTIC_WRITE`), `PautaService`, `PautaController`.
  - Fatia 1: `PautaRequest` (entrada, `@NotBlank titulo`), `PautaResponse` (saída).
  - Fatia 1: `ErroPadrao` (D2), `TratadorErros` (`@RestControllerAdvice`), `PautaNaoEncontradaException`.
  - Fatia 2: `Sessao` (entidade JPA, FK `pauta_id`), `SessaoRepository` (`countAbertasEm`, `findByPautaId`), `SessaoService`, `SessaoController` (`POST /pautas/{id}/sessao`).
  - Fatia 2: `SessaoRequest` (entrada, `duracaoMinutos` opcional, teto `DURACAO_MAXIMA_MINUTOS = 1440`), `SessaoResponse` (saída), `SessaoAbertaException` (409).
  - Fatia 2: `RelogioConfig` expõe bean `Clock` (`systemUTC`). Serviço não usa `Instant.now()`.

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Maven. Persistência H2 em memória (`create-drop`).
- Build e teste: `mvnd test` (sem `clean` no ciclo de correção). Testar um método: `mvnd test -Dtest='Classe#metodo'`.
- Sem Docker e sem internet. Dependências resolvem pelo proxy interno (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`).

## Convenções

- Erros sempre via `TratadorErros`. Nunca montar corpo de erro no controller.
- Entidade não sai na API: entrada por `*Request`, saída por `*Response`.
- Id gerado pelo banco (`Long`, `IDENTITY`). Cliente não envia id.
- Testes de integração: `@SpringBootTest` + `@AutoConfigureMockMvc` com H2 real. Sem mock de repositório.
- Tempo só via bean `Clock` injetado. Em teste, `Clock` controlável via `@TestConfiguration` aninhada com `@Primary`.
- Código novo vem com teste no mesmo commit.

## Decisões

- D1: 201 criação, 200 consulta, 400 validação, 404 não encontrado, 409 sessão aberta duplicada.
- D2: erro padrão `{"status", "error", "message", "path"}`. `path` vem de `request.getRequestURI()`.
- D2a: códigos `VALIDATION_ERROR` (400), `NOT_FOUND` (404), `CONFLICT` (409), `METHOD_NOT_ALLOWED` (405), `UNSUPPORTED_MEDIA_TYPE` (415), `INTERNAL_ERROR` (500). Fatia 3 reusa. Pendente de confirmação do dono do ticket para os códigos de 400, 404 e 409.
- D2b: `TratadorErros.validacao` monta `message` como `campo: mensagem` (primeiro `FieldError`). Testes de `duracaoMinutos` dependem disso.
- D5: sessão aberta duplicada na mesma pauta retorna 409. Não substitui a existente.
- D6: `titulo` obrigatório (não nulo, não vazio após trim, até 255 chars após trim). `descricao` opcional (até 255 chars após trim; vazia ou só espaços vira null). Aparagem com `strip()` no construtor compacto de `PautaRequest`, antes da validação. Pendente de confirmação com dono do ticket.
- D6a: id gerado pelo banco.
- D6b: limite de tamanho N = 255 para `titulo` e `descricao`, medido em unidades UTF-16 (`String.length()`; emoji ocupa 2). `@Size` em `PautaRequest` e `@Column(length)` em `Pauta` usam a mesma constante `Pauta.TAMANHO_MAXIMO`.
- D7: 5xx nunca expõe mensagem da exceção. Body genérico `Erro interno do servidor`, stack logada com `log.error`. Vale para `ErrorResponse` 5xx e exceções genéricas.
- D8: sessão aberta é `inicio <= agora < fim` (fim exclusivo). Sessão com `fim == agora` não conta como aberta. Pendente de confirmação na spec mestre.
- D9: `duracaoMinutos` ausente ou `null` vale 1 minuto. Valor não inteiro (ex.: `1.5`) ou `<= 0` retorna 400 citando o campo `duracaoMinutos`. Teto: `> 1440` retorna 400 citando o campo (teto de 24 h, escolha da fatia; evita sessão que bloqueia a pauta por séculos). Strings numéricas (`"5"`) são aceitas por coerção do Jackson em `BigDecimal`. Strings não numéricas, booleanos e arrays caem em `HttpMessageNotReadableException` e saem como `corpo: JSON invalido` (não cita o campo). Pendente de confirmação; não está na spec mestre.
- D10: corpo do 201 de sessão: `id`, `pautaId`, `inicio`, `fim`, `duracaoMinutos`. Pendente de confirmação; não está na spec mestre.
- `open-in-view: false` em `application.yml`.

## Entregas

- Fatia 1 (Pautas, `run_b490238a9dfb`): entidade, repositório, DTOs, erro padrão D2, service, controller, `application.yml`, testes AC-1.1 a AC-1.4 e caso de título em branco (S1). `mvnd test`: 5 testes, verdes.
- Ciclo de correção do review da fatia 1: teste MockMvc de 500 (`ResponseStatusException` com "segredo" não vaza no body) em `PautaControllerTest`. `log.error` com stack no ramo 5xx de `TratadorErros.inesperado`. `mvnd test`: 21 testes, verdes.
- Fatia 2 (Sessão, `wt-orqaliz-implement-spec`): `POST /pautas/{id}/sessao` com `Clock` injetado, lock `PESSIMISTIC_WRITE` na pauta antes da checagem de sessão aberta, 404 para pauta inexistente (checada antes do lock), 409 para sessão aberta duplicada, 400 para duração inválida (D9) e acima do teto (1440).
- Ciclo de correção do review da fatia 2: `SessaoControllerTest` reescrito com status asserts em todos os 400, teste de teto (1441 → 400, 1440 → 201), tipos JSON inválidos (`"abc"`, `true`, `[1]` → 400), `id` no corpo, `inicio` no caso `fim == agora`, e teste de concorrência com 2 threads contra H2 afirmando 1 × 201 e 1 × 409 com lock pessimista. Banco de teste isolado por `@TestPropertySource` (`jdbc:h2:mem:sessao`). Overload morto `abrirSessao(Long, String)` removido. `mvnd test`: 37 testes, verdes.
- Fora da fatia 2: votos e resultado (Fatia 3), `GET /pautas/{id}/sessoes` (C1, não implementado), início agendado, fechamento manual, estender ou reabrir sessão.
- Fora desta fatia (W1 a W4 da fatia 1): listagem, atualização, remoção, autenticação, persistência durável.

## Armadilhas

- `curl` não existe na imagem. Para checar o proxy use `mvnd dependency:resolve`.
- Saída do `mvnd` pode vir truncada. Confirmar sucesso pelo exit code e por `BUILD SUCCESS` em log (`> arquivo 2>&1`).
- `@Valid` lança `MethodArgumentNotValidException`. O handler pega só o primeiro `FieldError`.
- Handler `Exception` genérico captura tudo. Exceções do Spring com `ErrorResponse` mantêm o status original; o resto vira 500 `INTERNAL_ERROR` com log do servidor.
- H2 em memória com `DB_CLOSE_DELAY=-1`: contextos com a mesma URL compartilham o banco. `SessaoControllerTest` usa URL própria (`jdbc:h2:mem:sessao`) via `@TestPropertySource`; `PautaControllerTest` usa a default (`pauta`). Testes não devem depender de id fixo: o 404 usa `999999L`, que só é seguro com banco não compartilhado.
- `SessaoRequest` com `@Min`/`@Digits`/`@Max` aceita `null` no corpo (`required = false`). `intValueExact` só roda depois da validação, então não lança para valores fracionários.
- Teste de sessão usa `MutableClock` com `@Primary` em `@TestConfiguration` aninhada (detectada automaticamente; sem `@Import`). Clock é resetado no `@BeforeEach`.
- Teste de concorrência (`abrirSessaoConcorrenteNaMesmaPautaUmCriaOutroRecebe409`) usa threads com MockMvc e transação real do service. Se a classe de teste ganhar `@Transactional`, o teste perde validade (falso positivo): as threads passam a compartilhar a transação de teste.

## Pendências

- MERGE CONDICIONADO: não fazer merge até o dono do ticket confirmar D6, D2a (incluindo 405, 409, 415 e 500 novos), D8, D9 (incluindo teto de 1440 e coerção de string numérica) e D10. Confirmação não existe no código; registrar aqui quando ocorrer. Campo `Spec:` do review da fatia 2 ainda vazio: preencher com a referência da spec mestre quando confirmada.
- Confirmar D6 (campos da pauta) com dono do ticket. Se houver outros campos obrigatórios, ajustar `PautaRequest` e testes antes do merge.
- Confirmar D2a (códigos de `error`) antes da fatia 3, pois viram contrato. Framework 400s não mapeados (ex.: parâmetro ausente) saem como `BAD_REQUEST`, não `VALIDATION_ERROR`.
- Confirmar D8, D9 (incluindo teto 1440) e D10 com dono do ticket (extensão fora da spec mestre).
- Mensagem de tipo JSON inválido em `duracaoMinutos` não cita o campo (`corpo: JSON invalido`). D9 pede citar o campo. Decidir se o handler de `HttpMessageNotReadableException` deve nomear o campo ou se o contrato aceita a mensagem genérica.
- Risco aceito, a registrar com dono e prazo: abrir sessão sem autenticação nem autorização (fora da fatia). Com o teto, o dano máximo por requisição é uma pauta bloqueada por até 24 h, mas qualquer cliente pode repetir o POST.
- Índice ausente para o caminho quente: `countAbertasEm` filtra por `pauta_id`, `inicio` e `fim`. Sugerir índice `(pauta_id, inicio, fim)` na migração quando houver migração (em Postgres a FK não cria índice).
- `id` 409 expõe o id interno na mensagem (`Pauta ja possui sessao aberta: <id>`). Baixo risco; confirmar junto com D2a.
- `descricao` só com espaços vira null (D6). Confirmar com dono do ticket.
- Limite de body JSON (request) não aplicado na app. Fazer no gateway/proxy (ex.: `client_max_body_size`).
- Perfil de persistência: H2 em memória é o default único. Mover para profile `dev`/`test` e exigir datasource fora dele.
- Múltiplos erros de campo: `TratadorErros` mostra só o primeiro. Decidir se é contrato.
- Mensagens sem acento (`obrigatorio`, `Pauta nao encontrada`, `Pauta ja possui sessao aberta`) por padrão do projeto. Padronizar em decisão separada.
- Sessão: `GET /pautas/{id}/sessoes` (C1) pendente.
- Sessão: teste de ordem "404 antes do lock" não exercita a ordem explicitamente; cobertura é indireta pelo 404.
- Prioridade da fatia: sugerida alta, não confirmada.
