API de votação em assembleias de cooperativa. Escopo e requisitos em `docs/SPEC.md`.

## Estado atual

- Fatia 1 (pautas: cadastro e consulta) implementada.
- Fatia 2 (sessão de votação por pauta) implementada.
- Votos, resultado e fechamento de sessão: planejados, ainda não implementados.

## Pautas

- `POST /pautas`: cria pauta. Body `{"titulo": "...", "descricao": "..."}`. `titulo` obrigatório (até 255 caracteres), `descricao` opcional (até 2000). Retorna 201 com `id`.
- `GET /pautas/{id}`: retorna 200 com a pauta, ou 404 se não existir.

## Sessões de votação

- `POST /pautas/{id}/sessao`: abre sessão para a pauta. Body opcional `{"duracaoMinutos": 5}`, inteiro maior que zero, padrão 1. Retorna 201 com `id`, `pautaId`, `abertura` e `fim` (ISO-8601 UTC). `fim = abertura + duracaoMinutos`, calculado pelo relógio do servidor.
- Sessão aberta é a com `fim` posterior ao instante atual. Pauta com sessão aberta responde 409 e a sessão original não muda. Sessão expirada não bloqueia nova abertura.
- Aberturas simultâneas para a mesma pauta são serializadas por lock pessimista na linha da pauta; no máximo uma sessão fica aberta.
- Erros: 400 para `duracaoMinutos` inválido, 404 para pauta inexistente, 409 (`CONFLITO`) para sessão aberta.

## Erros

Todas as respostas de erro usam o corpo `{"status", "erro", "mensagem", "path"}`.

| Status | `erro` | Quando |
|---|---|---|
| 400 | `VALIDACAO_INVALIDA` | campo inválido, JSON malformado, `id` não numérico |
| 404 | `NAO_ENCONTRADO` | pauta ou rota inexistente |
| 409 | `CONFLITO` | pauta já com sessão aberta |
| 405 | `VALIDACAO_INVALIDA` | método não suportado |
| 406 | `VALIDACAO_INVALIDA` | `Accept` não atendível |
| 413 | `PAYLOAD_MUITO_GRANDE` | body acima de 64 KB |
| 415 | `VALIDACAO_INVALIDA` | `Content-Type` não suportado |
| 500 | `ERRO_INTERNO` | erro não tratado; detalhe só no log |

## Critérios de aceite

| AC | Teste |
|---|---|
| AC-1.1 POST válido cria pauta e retorna id | `PautaControllerTest#cadastraPautaComTituloERetornaId` |
| AC-1.2 GET retorna pauta existente | `PautaControllerTest#consultaPautaExistente` |
| AC-1.3 GET inexistente retorna 404 | `PautaControllerTest#consultaPautaInexistenteRetorna404NoFormatoPadrao` |
| AC-1.4 POST sem titulo retorna 400 | `PautaControllerTest#cadastroSemTituloRetorna400`, `#cadastroComTituloEmBrancoRetorna400` |
| AC-1.5 Módulo sobe com H2 e tabela `PAUTA` | `PautasApplicationTests#contextoSobeComH2EmMemoriaEComTabelaDePautas` |
| AC-1.6 Erros 400 e 404 com mesmas chaves | `PautaControllerTest#erros400e404TemExatamenteAsMesmasChaves` |

## Fora de escopo nesta fatia

- Autenticação e identificação do chamador. `POST /pautas` e `GET /pautas/{id}` são abertos. Os endpoints de voto exigirão identificação antes da fatia 2.
- Paginação e listagem de pautas.
- Notificação e persistência durável.

## Execução

```
mvnd test
mvnd spring-boot:run
```

Banco H2 em memória, sem persistência entre reinícios.
