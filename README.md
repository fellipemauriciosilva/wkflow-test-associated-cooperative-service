# votacao-pautas

Cadastro e consulta de pautas (Fatia 1) e abertura de sessão de votação (Fatia 2) do épico Votação Pautas.

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

### Corpo de erro (RF-12)

Todos os erros da API — `400`/`404`/`409` nos dois recursos, mais `415` (Content-Type não suportado), `405` (método não suportado na rota), `404` de rota inexistente e `500` (erro interno) — retornam o mesmo corpo padronizado:

```json
{"timestamp": "2026-01-01T10:00:00Z", "status": 409, "error": "Conflict", "message": "Já existe sessão aberta para a pauta: 1", "path": "/pautas/1/sessao"}
```

`message` em erros de validação (`400`) é construído só a partir de `field` + mensagem de validação de cada campo rejeitado — nunca com `exception.getMessage()` bruto, para não expor nomes de classe/método internos do Spring ao cliente.

### Teto de duração de sessão

`duracaoMinutos` aceita no máximo **1440** (24 horas). Sem teto, uma requisição anônima com `duracaoMinutos` enorme (ex. `Integer.MAX_VALUE`) deixaria a pauta com sessão "aberta" por ~4000 anos — negação de serviço de negócio sem nenhuma autenticação, já que `POST /pautas/{id}/sessao` não exige auth nesta fatia. O valor 1440 é um teto provisório (decisão registrada, não confirmada com o PO); ajustar exige só mudar `SessaoRequest.DURACAO_MAXIMA_MINUTOS`.

### Concorrência na abertura de sessão

Duas requisições concorrentes de abertura de sessão para a mesma pauta usam lock pessimista (`SELECT ... FOR UPDATE` via `EntityManager.find` com `PESSIMISTIC_WRITE`) na linha de `Pauta` dentro da transação, com isolamento **fixado explicitamente em `READ_COMMITTED`** (`@Transactional(isolation = Isolation.READ_COMMITTED)` em `SessaoService.abrir`, independente do default do datasource). A segunda transação só adquire o lock após o commit da primeira e, nesse ponto, já enxerga a sessão recém-criada — garantindo que só uma abertura por vez sucede (a outra recebe `409`). Sem constraint de unicidade simples na tabela, porque reabertura após fechamento é permitida.

Essa garantia depende do isolamento ser READ_COMMITTED (default do H2 e do Postgres, mas **não universal**): em REPEATABLE READ/SERIALIZABLE, o snapshot da transação pode ser fixado na própria instrução com lock, fazendo a segunda transação não ver a sessão da primeira mesmo após o commit — por isso o isolamento é fixado explicitamente no método, não deixado como "o que o datasource usar por padrão". O teste `SessaoControllerTest.abrir_duasChamadasConcorrentes_exatamenteUmaSucede` usa um gancho de teste (`GanchoConcorrenciaSessaoControlavel`) para forçar a sobreposição real das duas transações e provar que, sem o lock, a segunda transação alcança o ponto pós-lock enquanto a primeira ainda está retida — comportamento que o lock impede.

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
