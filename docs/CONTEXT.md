# Contexto do projeto

## Módulos

- `br.acme.pauta`: fatia 1 (cadastro e consulta de pauta). Único pacote até agora.
  - `Pauta` (entidade JPA), `PautaRepository`, `PautaService`, `PautaController`.
  - `PautaRequest` (entrada, `@NotBlank titulo`), `PautaResponse` (saída).
  - `ErroPadrao` (D2), `TratadorErros` (`@RestControllerAdvice`), `PautaNaoEncontradaException`.

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Maven. Persistência H2 em memória (`create-drop`).
- Build e teste: `mvnd test` (sem `clean` no ciclo de correção). Testar um método: `mvnd test -Dtest='Classe#metodo'`.
- Sem Docker e sem internet. Dependências resolvem pelo proxy interno (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`).

## Convenções

- Erros sempre via `TratadorErros`. Nunca montar corpo de erro no controller.
- Entidade não sai na API: entrada por `PautaRequest`, saída por `PautaResponse`.
- Id gerado pelo banco (`Long`, `IDENTITY`). Cliente não envia id.
- Testes de integração: `@SpringBootTest` + `@AutoConfigureMockMvc` com H2 real. Sem mock de repositório.
- Código novo vem com teste no mesmo commit.

## Decisões

- D1: 201 criação, 200 consulta, 400 validação, 404 não encontrado.
- D2: erro padrão `{"status", "error", "message", "path"}`. `path` vem de `request.getRequestURI()`.
- D2a: códigos `VALIDATION_ERROR` (400), `NOT_FOUND` (404), `METHOD_NOT_ALLOWED` (405), `UNSUPPORTED_MEDIA_TYPE` (415), `INTERNAL_ERROR` (500). Fatias 2 e 3 reusam. Pendente de confirmação do dono do ticket para os códigos de 400 e 404.
- D6: `titulo` obrigatório (não nulo, não vazio após trim, até 255 chars após trim). `descricao` opcional (até 255 chars após trim; vazia ou só espaços vira null). Aparagem com `strip()` no construtor compacto de `PautaRequest`, antes da validação. Pendente de confirmação com dono do ticket.
- D6b: limite de tamanho N = 255 para `titulo` e `descricao`, medido em unidades UTF-16 (`String.length()`; emoji ocupa 2). `@Size` em `PautaRequest` e `@Column(length)` em `Pauta` usam a mesma constante `Pauta.TAMANHO_MAXIMO`.
- D7: 5xx nunca expõe mensagem da exceção. Body genérico `Erro interno do servidor`, stack logada com `log.error`. Vale para `ErrorResponse` 5xx e exceções genéricas.
- D6a: id gerado pelo banco.
- Sem `Clock` nesta fatia (pauta não tem campo de tempo).
- `open-in-view: false` em `application.yml`.

## Entregas

- Fatia 1 (Pautas, `run_b490238a9dfb`): entidade, repositório, DTOs, erro padrão D2, service, controller, `application.yml`, testes AC-1.1 a AC-1.4 e caso de título em branco (S1). `mvnd test`: 5 testes, todos verdes.
- Fora desta fatia (W1 a W4): listagem, atualização, remoção, autenticação, persistência durável, header `Location` só como pendência de Could (C1 já aplicado no 201).
- Ciclo de correção do review: teste MockMvc de 500 (`ResponseStatusException` com "segredo" não vaza no body) em `PautaControllerTest`. `log.error` com stack já existe no ramo 5xx de `TratadorErros.inesperado`. `mvnd test`: 21 testes, verdes.

## Armadilhas

- `curl` não existe na imagem. Para checar o proxy use `mvnd dependency:resolve`.
- Saída do `mvnd` pode vir truncada. Confirmar sucesso pelo exit code e por `BUILD SUCCESS` em log (`> arquivo 2>&1`).
- `@Valid` lança `MethodArgumentNotValidException`. O handler pega só o primeiro `FieldError`.
- Handler `Exception` genérico captura tudo. Exceções do Spring com `ErrorResponse` mantêm o status original; o resto vira 500 `INTERNAL_ERROR` com log do servidor.
- H2 em memória com `DB_CLOSE_DELAY=-1`: o contexto de teste compartilha o banco entre casos. Testes não devem depender de id fixo.

## Pendências

- MERGE CONDICIONADO: não fazer merge até o dono do ticket confirmar D6 e D2a (incluindo 405, 415 e 500 novos). Confirmação não existe no código; registrar aqui quando ocorrer.
- Confirmar D6 (campos da pauta) com dono do ticket. Se houver outros campos obrigatórios, ajustar `PautaRequest` e testes antes do merge.
- Confirmar D2a (códigos de `error`) antes de iniciar fatia 2, pois viram contrato. Framework 400s não mapeados (ex.: parâmetro ausente) saem como `BAD_REQUEST`, não `VALIDATION_ERROR`.
- `descricao` só com espaços vira null (D6). Confirmar com dono do ticket.
- Limite de body JSON (request) não aplicado na app. Fazer no gateway/proxy (ex.: `client_max_body_size`).
- Perfil de persistência: H2 em memória é o default único. Mover para profile `dev`/`test` e exigir datasource fora dele.
- Múltiplos erros de campo: `TratadorErros` mostra só o primeiro. Decidir se é contrato.
- Mensagens sem acento (`obrigatorio`, `Pauta nao encontrada`) por padrão do projeto. Padronizar em decisão separada.
- Prioridade da fatia: sugerida alta, não confirmada.
- 409 entra na fatia 2 (não se aplica aqui).
