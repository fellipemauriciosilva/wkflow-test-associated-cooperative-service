# votacao-service

Serviço REST para votação em assembleias de cooperativa. Fatias entregues: cadastro e
consulta de pautas; abertura de sessão de votação por pauta.

## Pré-requisitos

- JDK 21
- Maven 3.9+ (ou `mvnd`)

## Instalar dependências

```bash
mvnd dependency:resolve
```

## Rodar localmente

```bash
mvnd spring-boot:run
```

Banco H2 em memória, sem configuração adicional.

## Rodar os testes

```bash
mvnd test
```

## Endpoints

### `POST /pautas`

Request:
```json
{"titulo": "Eleicao de diretoria", "descricao": "Votacao anual"}
```

- `201 Created` com `id`, `titulo`, `descricao` quando `titulo` é válido.
- `400 Bad Request` quando `titulo` está ausente, vazio, só espaços (ou só caracteres
  invisíveis, ver abaixo), titulo/descricao acima do tamanho máximo (200/1000 caracteres,
  contados por code point — emoji contam como 1), JSON malformado ou body ausente.
- `413 Payload Too Large` quando o body excede 16 KB, mesmo sem `Content-Length` confiável
  (`TamanhoMaximoBodyFilter`, ver "Segurança" abaixo).
- `415 Unsupported Media Type` quando `Content-Type` não é `application/json`.
- `405 Method Not Allowed` quando o método HTTP não é suportado pela rota (resposta inclui o
  header `Allow` com os métodos aceitos, conforme RFC 9110).

Limites de tamanho usam `codePointCount`, não `length()`, então 200 emojis são aceitos como
título (`length()` seria 400 por serem surrogate pairs). Validação de espaço em branco usa
`isBlank()`/`strip()` (critério `Character.isWhitespace`) mais uma checagem adicional de
"caractere visível" por categoria Unicode (`Character.getType`): título composto só por
espaços de separação (Zs/Zl/Zp, incluindo NBSP U+00A0), caracteres de formato invisíveis
(Cf, incluindo zero-width space U+200B e BOM U+FEFF) ou controle é rejeitado como "titulo e
obrigatorio". NBSP/zero-width continuam aceitos quando há pelo menos um caractere visível no
título (ex.: usados como separador no meio do texto).

### `GET /pautas/{id}`

- `200 OK` com o corpo da pauta, se existir.
- `404 Not Found` se não existir.
- `400 Bad Request` se `{id}` não for numérico.

### `POST /pautas/{id}/sessao`

Abre sessão de votação para a pauta `{id}`. Request:
```json
{"duracaoMinutos": 5}
```

- `201 Created` com `id`, `pautaId`, `duracaoMinutos`, `abertura`, `fimEm` quando aberta com
  sucesso. `duracaoMinutos` ausente, `null`, ou corpo ausente assume 1 minuto.
- `400 Bad Request` quando `duracaoMinutos` é <= 0, acima do teto de 1440 minutos (24h), não
  inteiro (ex. `"abc"`, `1.5`, `"5"` — string numérica também é rejeitada, não coagida) ou
  JSON malformado.
- `404 Not Found` quando a pauta `{id}` não existe.
- `409 Conflict` quando a pauta já tem uma sessão aberta (fim ainda não alcançado). Sessão
  encerrada não bloqueia nova abertura (reabertura de sessão encerrada é fora de escopo desta
  fatia — ver `docs/CONTEXT.md`).
- `413 Payload Too Large`, `415 Unsupported Media Type`, `405 Method Not Allowed`: mesmo
  comportamento de `/pautas` (ver acima).

Ordem de validação: corpo (400) → pauta existe (404) → sessão aberta (409). Instante de
abertura vem de um `java.time.Clock` injetável (bean `Clock.systemUTC()` em produção,
substituível por `Clock.fixed(...)` ou `@MockBean` em testes). A checagem de sessão aberta
roda dentro de uma transação com lock pessimista (`PESSIMISTIC_WRITE`) na pauta, para que
duas requisições concorrentes para a mesma pauta não abram duas sessões (ver
`docs/CONTEXT.md`, Convenções).

### Formato de erro (400, 404, 409, 413, 415, 405)

```json
{"status": 404, "mensagem": "Pauta nao encontrada"}
```

## Variáveis de ambiente

Nenhuma necessária nesta fatia (H2 em memória, sem segredos).

## Segurança (decisões desta fatia)

- Sem autenticação/autorização em `POST /pautas` e `GET /pautas/{id}`. IDs são sequenciais
  (`IDENTITY`), então `GET /pautas/{id}` permite enumerar todas as pautas. Aceito nesta fatia
  porque não há voto/resultado ainda e a entidade exposta é só o conteúdo da pauta (sem dado
  de participante). Reavaliar obrigatoriamente antes da fatia 3 (voto): se o resultado também
  for exposto por id sequencial, enumeração vira problema real (IDOR). **Risco ainda não
  mitigado:** sem autenticação, nada limita quantas pautas um cliente cria nem a taxa de
  requisições — aceito nesta fatia como limitação conhecida, não como decisão de produto.
- Limite de tamanho de body (16 KB) reforçado em duas camadas: rejeição imediata se
  `Content-Length` já declara acima do limite, e um `HttpServletRequestWrapper` que conta
  bytes efetivamente lidos do `InputStream` e interrompe a leitura ao estourar o limite —
  cobre chunked transfer encoding e requisições sem `Content-Length` confiável, onde o header
  sozinho não protegeria nada (gap identificado em review de segurança e corrigido). O 413
  foi confirmado de ponta a ponta com um teste e2e que fala HTTP/1.1 chunked via socket cru
  contra um servidor real (`CorpoExcedeTamanhoMaximoE2ETest`) — não só contra o filtro
  isolado. HTTP/2 não foi testado. Detalhes da correção e do bug que ela fechou (Jackson
  mascarando o limite como "400 JSON inválido" no caminho real) em `docs/CONTEXT.md`.
