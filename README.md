# votacao-service

Serviço REST para votação em assembleias de cooperativa. Nesta fatia: cadastro e consulta de pautas.

## Pré-requisitos

- JDK 21
- Maven 3.9+ (ou `mvnd`)

## Instalar dependências

```bash
mvn dependency:resolve
```

## Rodar localmente

```bash
mvn spring-boot:run
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
- `400 Bad Request` quando `titulo` está ausente, vazio, só espaços, titulo/descricao acima
  do tamanho máximo (200/1000 caracteres, contados por code point — emoji contam como 1),
  JSON malformado ou body ausente.
- `413 Payload Too Large` quando o body excede 16 KB (`TamanhoMaximoBodyFilter`).

Limites de tamanho usam `codePointCount`, não `length()`, então 200 emojis são aceitos como
título (`length()` seria 400 por serem surrogate pairs). Validação de espaço em branco usa
`isBlank()`/`strip()` (critério `Character.isWhitespace`): NBSP (U+00A0) e zero-width space
(U+200B) não contam como espaço e sobrevivem à normalização — decisão aceita, ver
`docs/CONTEXT.md`.

### `GET /pautas/{id}`

- `200 OK` com o corpo da pauta, se existir.
- `404 Not Found` se não existir.
- `400 Bad Request` se `{id}` não for numérico.

### Formato de erro (400, 404, 413)

```json
{"status": 404, "mensagem": "Pauta nao encontrada"}
```

## Variáveis de ambiente

Nenhuma necessária nesta fatia (H2 em memória, sem segredos).

## Segurança (decisões desta fatia)

- Sem autenticação/autorização em `POST /pautas` e `GET /pautas/{id}`. IDs são sequenciais
  (`IDENTITY`), então `GET /pautas/{id}` permite enumerar todas as pautas. Aceito nesta fatia
  porque pauta não é dado sensível (título/descrição de assembleia são públicos por natureza
  cooperativista) e não há voto/resultado ainda. Reavaliar antes da fatia 3 (voto): se o
  resultado também for exposto por id sequencial, enumeração vira problema real (IDOR).
