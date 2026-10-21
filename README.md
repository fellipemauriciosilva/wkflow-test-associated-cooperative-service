# votacao-api

API de gestão de pautas e sessões de votação — Slice 1 (CRUD básico de pauta + abertura de sessão) e Slice 2 (registro de voto com validação de duplicata e sessão aberta).

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
- `POST /api/pautas/{id}/votos` — registra voto. Body: `{"id_associado": N, "voto": "SIM"|"NAO"}`.
  Requer sessão aberta (422 se não iniciada/fechada/expirada), rejeita segundo voto do mesmo
  associado na mesma pauta (409) e pauta inexistente (404).

Erros seguem o formato `{"error": "...", "timestamp": "...", "path": "..."}` — inclusive para
JSON malformado (400), parâmetro de caminho com tipo inválido (400), rota inexistente (404),
método HTTP não suportado (405) e erro interno não previsto (500).
