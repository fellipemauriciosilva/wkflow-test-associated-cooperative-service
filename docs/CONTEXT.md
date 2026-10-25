# Contexto do projeto

## Módulos

- `br.acme.pauta`: fatia 1 (cadastro e consulta de pauta) e fatia 2 (abertura de sessão de votação). Único pacote até agora.
  - Fatia 1: `Pauta` (entidade JPA), `PautaRepository` (com `findComLockPorId`, `PESSIMISTIC_WRITE`), `PautaService`, `PautaController`.
  - Fatia 1: `PautaRequest` (entrada, `@NotBlank titulo`), `PautaResponse` (saída).
  - Fatia 1: `ErroPadrao` (D2), `TratadorErros` (`@RestControllerAdvice`), `PautaNaoEncontradaException`.
  - Fatia 2: `Sessao` (entidade JPA, FK `pauta_id`), `SessaoRepository` (`countAbertasEm`, `findByPautaId`), `SessaoService`, `SessaoController` (`POST /pautas/{id}/sessao`).
  - Fatia 2: `SessaoRequest` (entrada, `duracaoMinutos` opcional), `SessaoResponse` (saída), `SessaoAbertaException` (409).
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
- Tempo só via bean `Clock` injetado. Em teste, `Clock` controlável via `@TestConfiguration` com `@Primary`.
- Código novo vem com teste no mesmo commit.

## Decisões

- D1: 201 criação, 200 consulta, 400 validação, 404 não encontrado, 409 sessão aberta duplicada.
- D2: erro padrão `{"status", "error", "message", "path"}`. `path` vem de `request.getRequestURI()`.
- D2a: códigos `VALIDATION_ERROR` (400), `NOT_FOUND` (404), `CONFLICT` (409), `METHOD_NOT_ALLOWED` (405), `UNSUPPORTED_MEDIA_TYPE` (415), `INTERNAL_ERROR` (500). Fatia 3 reusa. Pendente de confirmação do dono do ticket para os códigos de 400, 404 e 409.
- D5: sessão aberta duplicada na mesma pauta retorna 409. Não substitui a existente.
- D6: `titulo` obrigatório (não nulo, não vazio após trim, até 255 chars após trim). `descricao` opcional (até 255 chars após trim; vazia ou só espaços vira null). Aparagem com `strip()` no construtor compacto de `PautaRequest`, antes da validação. Pendente de confirmação com dono do ticket.
- D6a: id gerado pelo banco.
- D6b: limite de tamanho N = 255 para `titulo` e `descricao`, medido em unidades UTF-16 (`String.length()`; emoji ocupa 2). `@Size` em `PautaRequest` e `@Column(length)` em `Pauta` usam a mesma constante `Pauta.TAMANHO_MAXIMO`.
- D7: 5xx nunca expõe mensagem da exceção. Body genérico `Erro interno do servidor`, stack logada com `log.error`. Vale para `ErrorResponse` 5xx e exceções genéricas.
- D8: sessão aberta é `inicio <= agora < fim` (fim exclusivo). Sessão com `fim == agora` não conta como aberta. Pendente de confirmação.
- D9: `duracaoMinutos` ausente ou `null` vale 1 minuto. Valor não inteiro (ex.: `1.5`) ou `<= 0` retorna 400 citando o campo `duracaoMinutos`. Pendente de confirmação; não está na spec mestre.
- D10: corpo do 201 de sessão: `id`, `pautaId`, `inicio`, `fim`, `duracaoMinutos`. Pendente de confirmação; não está na spec mestre.
- `open-in-view: false` em `application.yml`.

## Entregas

- Fatia 1 (Pautas, `run_b490238a9dfb`): entidade, repositório, DTOs, erro padrão D2, service, controller, `application.yml`, testes AC-1.1 a AC-1.4 e caso de título em branco (S1). `mvnd test`: 5 testes, verdes.
- Ciclo de correção do review da fatia 1: teste MockMvc de 500 (`ResponseStatusException` com "segredo" não vaza no body) em `PautaControllerTest`. `log.error` com stack no ramo 5xx de `TratadorErros.inesperado`. `mvnd test`: 21 testes, verdes.
- Fatia 2 (Sessão, `wt-orqaliz-implement-spec`): `POST /pautas/{id}/sessao` com `Clock` injetado, lock `PESSIMISTIC_WRITE` na pauta antes da checagem de sessão aberta, 404 para pauta inexistente (checada antes do lock), 409 para sessão aberta duplicada, 400 para duração inválida (D9). Testes em `SessaoControllerTest` cobrindo AC-2.1 a AC-2.5, o limite de D8 (`fim == agora`) e validações de D9. `mvnd test`: 32 testes, verdes.
- Fora da fatia 2: votos e resultado (Fatia 3), `GET /pautas/{id}/sessoes` (C1, não implementado), início agendado, fechamento manual, estender ou reabrir sessão.
- Fora desta fatia (W1 a W4 da fatia 1): listagem, atualização, remoção, autenticação, persistência durável.

## Armadilhas

- `curl` não existe na imagem. Para checar o proxy use `mvnd dependency:resolve`.
- Saída do `mvnd` pode vir truncada. Confirmar sucesso pelo exit code e por `BUILD SUCCESS` em log (`> arquivo 2>&1`).
- `@Valid` lança `MethodArgumentNotValidException`. O handler pega só o primeiro `FieldError`.
- Handler `Exception` genérico captura tudo. Exceções do Spring com `ErrorResponse` mantêm o status original; o resto vira 500 `INTERNAL_ERROR` com log do servidor.
- H2 em memória com `DB_CLOSE_DELAY=-1`: o contexto de teste compartilha o banco entre casos. Testes não devem depender de id fixo.
- `SessaoRequest` com `@Min`/`@Digits` aceita `null` no corpo (`required = false`). `intValueExact` só roda depois da validação, então não lança para valores fracionários.
- Teste de sessão usa `MutableClock` com `@Primary` via `@Import`. Contexto próprio por `@Import`; não vaza para `PautaControllerTest`.
- Concorrência de abertura de sessão não tem teste de integração (caro em H2). Lacuna declarada: cobertura é o lock pessimista, sem teste que o exercite em paralelo.

## Pendências

- MERGE CONDICIONADO: não fazer merge até o dono do ticket confirmar D6, D2a (incluindo 405, 409, 415 e 500 novos), D8, D9 e D10. Confirmação não existe no código; registrar aqui quando ocorrer.
- Confirmar D6 (campos da pauta) com dono do ticket. Se houver outros campos obrigatórios, ajustar `PautaRequest` e testes antes do merge.
- Confirmar D2a (códigos de `error`) antes da fatia 3, pois viram contrato. Framework 400s não mapeados (ex.: parâmetro ausente) saem como `BAD_REQUEST`, não `VALIDATION_ERROR`.
- Confirmar D8, D9 e D10 com dono do ticket (extensão fora da spec mestre).
- `descricao` só com espaços vira null (D6). Confirmar com dono do ticket.
- Limite de body JSON (request) não aplicado na app. Fazer no gateway/proxy (ex.: `client_max_body_size`).
- Perfil de persistência: H2 em memória é o default único. Mover para profile `dev`/`test` e exigir datasource fora dele.
- Múltiplos erros de campo: `TratadorErros` mostra só o primeiro. Decidir se é contrato.
- Mensagens sem acento (`obrigatorio`, `Pauta nao encontrada`, `Pauta ja possui sessao aberta`) por padrão do projeto. Padronizar em decisão separada.
- Sessão: `GET /pautas/{id}/sessoes` (C1) pendente.
- Sessão: teste de concorrência real do lock pendente ou aceito como lacuna.
- Prioridade da fatia: sugerida alta, não confirmada.
