API de votação em assembleias de cooperativa. Escopo e requisitos em `docs/SPEC.md`.

## Estado atual

- Fatia 1 (pautas: cadastro e consulta) implementada.
- Sessão de votação e votos/resultado: planejados, ainda não implementados.

## Pautas

- `POST /pautas`: cria pauta. Body `{"titulo": "...", "descricao": "..."}`. `titulo` obrigatório. Retorna 201 com `id`.
- `GET /pautas/{id}`: retorna 200 com a pauta, ou 404 se não existir.
- Erros 400 e 404 usam o formato `{"status", "erro", "mensagem", "path"}`.

## Execução

```
mvnd test
mvnd spring-boot:run
```

Banco H2 em memória, sem persistência entre reinícios.
