# votacao-pautas

Cadastro e consulta de pautas (Fatia 1), abertura de sessão de votação (Fatia 2) e registro de votos com resultado (Fatia 3) do épico Votação Pautas.

## Pré-requisitos

- JDK 21
- Maven (ou `mvnd`)

## Instalar dependências

```bash
mvnd dependency:resolve
```

## Rodar localmente

```bash
mvnd spring-boot:run
```

API disponível em `http://localhost:8080`.

## Endpoints

- `POST /pautas` — cria pauta. Body: `{"titulo": "string obrigatória", "descricao": "string opcional"}`. Retorna `201` com `{"id": 1}`. Payload inválido (titulo ausente/nulo/em branco, titulo ou descricao acima de 255 caracteres, JSON malformado) retorna `400`.
- `GET /pautas/{id}` — retorna `200` com `{"id", "titulo", "descricao"}`, `404` se não existir, ou `400` se `{id}` não for numérico.
- `POST /pautas/{id}/sessao` — abre sessão de votação para a pauta `{id}`. Body opcional `{"duracaoMinutos": <inteiro entre 1 e 1440>}`; ausente, `{}` ou `null` usam padrão de 1 minuto. Retorna `201` com `{"id", "pautaId", "inicio", "fim", "duracaoMinutos"}` (`inicio`/`fim` em ISO-8601 UTC). `404` se a pauta não existir. `409` se já existir sessão aberta para a pauta (`fim > agora`). `400` se `duracaoMinutos` for zero, negativo, maior que 1440, não inteiro ou de tipo inválido — nesses casos nenhuma sessão é criada. Reabertura após sessão encerrada é permitida (retorna `201`).
- `POST /pautas/{id}/votos` — registra voto do associado na pauta `{id}`. Body: `{"associadoId": "string obrigatória, não vazia, até 255 caracteres", "voto": "SIM"|"NAO"}`. `associadoId` é normalizado (trim + lowercase) antes de qualquer checagem e antes de salvar — `"associado-1"`, `"Associado-1"` e `" associado-1 "` são tratados como o mesmo associado. Retorna `201` com `{"id", "pautaId", "associadoId", "voto"}` (`associadoId` já normalizado). `400` se `voto` estiver ausente/fora de SIM/NAO, ou `associadoId` ausente/em branco/acima de 255 caracteres. `404` se a pauta não existir. `422` se não houver sessão aberta para a pauta (sessão inexistente, encerrada, ou `agora >= fim`) — checado antes do duplicado, então associado já votado com sessão fechada retorna `422`, não `409`. `409` se o associado já tiver votado nessa pauta (checado no serviço, atomicamente com a checagem de sessão, e garantido pela constraint `UNIQUE (pauta_id, associado_id)` do banco sob concorrência real).

  **Risco aceito, registrado pelo PO:** `associadoId` é string livre no body, sem autenticação e sem verificação de elegibilidade/existência do associado. A constraint de unicidade impede repetir exatamente o mesmo `associadoId` normalizado na mesma pauta, mas não impede um cliente votar múltiplas vezes trocando o `associadoId` a cada chamada — a regra "um voto por associado" só vale se o chamador for honesto sobre sua própria identidade. Vincular identidade a autenticação real ou a uma lista de associados aptos está fora do escopo desta fatia (task.md, seção 2 — "fora"); mitigação completa exigiria pelo menos autenticação.
- `GET /pautas/{id}/resultado` — retorna `200` com `{"pautaId", "totalSim", "totalNao", "status"}`. `status` é `ABERTA` (sessão existe e `agora < fim`), `ENCERRADA` (sessão existe e `agora >= fim`) ou `NAO_INICIADA` (pauta sem nenhuma sessão — totais sempre zero nesse caso). `404` se a pauta não existir.

### Corpo de erro (RF-12)

Todos os erros da API — `400`/`404`/`409`/`422` nos três recursos, mais `415` (Content-Type não suportado), `405` (método não suportado na rota), `406` (Accept não aceitável), `404` de rota inexistente e `500` (erro interno) — retornam o mesmo corpo padronizado:

```json
{"timestamp": "2026-01-01T10:00:00Z", "status": 409, "error": "Conflict", "message": "Já existe sessão aberta para a pauta: 1", "path": "/pautas/1/sessao"}
```

`message` em erros de validação (`400`) é construído só a partir de `field` + mensagem de validação de cada campo rejeitado — nunca com `exception.getMessage()` bruto, para não expor nomes de classe/método internos do Spring ao cliente. `500` tem mensagem fixa ("erro interno inesperado"), nunca o texto da exceção.

`GlobalExceptionHandler` estende `ResponseEntityExceptionHandler` (não usa só `@ExceptionHandler(Exception.class)` avulso) e sobrescreve `handleExceptionInternal`, preservando o status que o próprio Spring MVC já calculou para suas exceções (`MissingServletRequestParameterException` → 400, `HttpMediaTypeNotAcceptableException` → 406, etc.) em vez de um catch-all genérico convertê-las em 500 antes do resolver padrão do framework agir. Para exceções detectadas na negociação de conteúdo (ex. `HttpMediaTypeNotAcceptableException`, quando o `Accept` do cliente não aceita nada que o servidor produza), o corpo de erro é escrito diretamente na resposta via `ObjectMapper`, bypassando os message converters — deixar a negociação de conteúdo decidir como serializar o próprio corpo de erro reproduziria o mesmo erro na escrita da resposta de erro. `PautaControllerTest` cobre 406 (`Accept: application/xml`) e 400 de parâmetro obrigatório ausente contra um controller só de teste (`DiagnosticoTestController`), e `SessaoControllerTest` cobre 500 forçando uma `RuntimeException` via gancho de teste.

### Teto de duração de sessão

`duracaoMinutos` aceita no máximo **1440** (24 horas). Sem teto, uma requisição anônima com `duracaoMinutos` enorme (ex. `Integer.MAX_VALUE`) deixaria a pauta com sessão "aberta" por ~4000 anos — negação de serviço de negócio sem nenhuma autenticação, já que `POST /pautas/{id}/sessao` não exige auth nesta fatia. O valor 1440 é um teto provisório (decisão registrada, não confirmada com o PO); ajustar exige só mudar `SessaoRequest.DURACAO_MAXIMA_MINUTOS`.

### Concorrência na abertura de sessão

Duas requisições concorrentes de abertura de sessão para a mesma pauta usam lock pessimista (`SELECT ... FOR UPDATE` via `EntityManager.find` com `PESSIMISTIC_WRITE`) na linha de `Pauta` dentro da transação, com isolamento **fixado explicitamente em `READ_COMMITTED`** (`@Transactional(isolation = Isolation.READ_COMMITTED)` em `SessaoService.abrir`, independente do default do datasource). A segunda transação só adquire o lock após o commit da primeira e, nesse ponto, já enxerga a sessão recém-criada — garantindo que só uma abertura por vez sucede (a outra recebe `409`). Sem constraint de unicidade simples na tabela, porque reabertura após fechamento é permitida.

Essa garantia depende do isolamento ser READ_COMMITTED (default do H2 e do Postgres, mas **não universal**): em REPEATABLE READ/SERIALIZABLE, o snapshot da transação pode ser fixado na própria instrução com lock, fazendo a segunda transação não ver a sessão da primeira mesmo após o commit — por isso o isolamento é fixado explicitamente no método, não deixado como "o que o datasource usar por padrão". O teste `SessaoControllerTest.abrir_duasChamadasConcorrentes_exatamenteUmaSucede` usa um gancho de teste (`GanchoConcorrenciaSessaoControlavel`) para forçar a sobreposição real das duas transações e provar que, sem o lock, a segunda transação alcança o ponto pós-lock enquanto a primeira ainda está retida — comportamento que o lock impede. O mesmo teste também lê `TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()` dentro do gancho (guardado num `AtomicReference`, asserido na thread do teste) e confirma que o nível efetivo é `ISOLATION_READ_COMMITTED` — essa parte prova que a anotação `@Transactional(isolation = ...)` está de fato em vigor, não o comportamento do banco sob outro isolamento (isso exigiria um teste separado forçando REPEATABLE READ, fora do escopo desta fatia).

### Limite de tamanho de titulo/descricao

Ambos aceitam no máximo 255 **code units UTF-16** (`String.length()` em Java), não bytes nem code points. Isso é o que `@Size(max = 255)` do Bean Validation mede e o que o H2 `VARCHAR(255)` também conta (caracteres, não bytes) — para a maioria dos alfabetos (incluindo acentuados) os dois coincidem. A única divergência é para caracteres fora do plano básico (ex. muitos emojis), representados por par substituto (2 code units): `"😀".repeat(128)` tem 256 code units e é rejeitado com `400`, mesmo "parecendo" 128 caracteres visuais. Essa é a regra efetivamente aplicada nesta fatia; se o PO quiser contagem por code point (grafemas), trocar `@Size` por um validador customizado com `codePointCount`.

## Rodar os testes

```bash
mvnd test
```

## Banco de dados

H2 em memória (`jdbc:h2:mem:votacao`), schema gerado via JPA (`ddl-auto: update`). Sem persistência entre execuções. Console H2 **desabilitado por padrão** (risco de execução remota de código via `CREATE ALIAS`); disponível em `/h2-console` só com o profile `dev` ativo (`--spring.profiles.active=dev`).

## Variáveis de ambiente

Nenhuma obrigatória nesta fatia.
