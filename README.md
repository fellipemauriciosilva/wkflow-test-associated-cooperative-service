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
mvn test
```

## Endpoints

### `POST /pautas`

Request:
```json
{"titulo": "Eleicao de diretoria", "descricao": "Votacao anual"}
```

- `201 Created` com `id`, `titulo`, `descricao` quando `titulo` é válido.
- `400 Bad Request` quando `titulo` está ausente, vazio, só espaços, ou titulo/descricao acima do tamanho máximo (200/1000 caracteres).

### `GET /pautas/{id}`

- `200 OK` com o corpo da pauta, se existir.
- `404 Not Found` se não existir.

### Formato de erro (400 e 404)

```json
{"status": 404, "mensagem": "Pauta nao encontrada"}
```

## Variáveis de ambiente

Nenhuma necessária nesta fatia (H2 em memória, sem segredos).
