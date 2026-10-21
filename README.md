# votacao-api

API de gestão de pautas e sessões de votação — Slice 1 (CRUD básico de pauta + abertura de sessão).

## Pré-requisitos

- JDK 21
- Maven (`mvnd` ou `mvn`)

## Rodar localmente

```bash
mvnd spring-boot:run
```

API sobe em `http://localhost:8080/api`.

## Rodar os testes

```bash
mvnd test
```

## Banco de dados

H2 em memória, schema recriado a cada subida (`ddl-auto=create-drop`). Sem persistência entre reinícios.

## Endpoints

- `POST /api/pautas` — cria pauta. Body: `{"titulo": "...", "descricao": "..."}`.
- `GET /api/pautas/{id}` — consulta pauta por ID.
- `POST /api/pautas/{id}/sessao` — abre sessão de votação. Body opcional: `{"duracao_minutos": N}` (padrão 1 minuto).

Erros seguem o formato `{"error": "...", "timestamp": "...", "path": "..."}`.
