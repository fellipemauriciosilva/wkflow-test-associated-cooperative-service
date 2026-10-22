# votacao-pautas

Cadastro e consulta de pautas (Fatia 1 do épico Votação Pautas). CRUD mínimo: criar pauta e buscar por id.

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

- `POST /pautas` — cria pauta. Body: `{"titulo": "string obrigatória", "descricao": "string opcional"}`. Retorna `201` com `{"id": 1}`. Payload inválido (titulo ausente/nulo/em branco ou acima de 255 caracteres) retorna `400`.
- `GET /pautas/{id}` — retorna `200` com `{"id", "titulo", "descricao"}` ou `404` se não existir.

## Rodar os testes

```bash
mvnd test
```

## Banco de dados

H2 em memória (`jdbc:h2:mem:votacao`), schema gerado via JPA (`ddl-auto: update`). Sem persistência entre execuções. Console H2 disponível em `/h2-console` quando a aplicação está rodando.

## Variáveis de ambiente

Nenhuma obrigatória nesta fatia.
