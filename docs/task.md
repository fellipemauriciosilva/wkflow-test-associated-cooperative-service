# task.md: Fatia 1 - Cadastro e consulta de pautas

Épico: run_2057fd08c4e4 (Votação Pautas). Fatia 1 de 3. Sem dependência.

## 1. Objetivo

CRUD mínimo de pauta: criar e consultar por id. Validação de payload. Persistência H2 em memória. Testes de controller (MockMvc) e serviço (JUnit 5).

## 2. Escopo

Dentro:
- `POST /pautas`: cria pauta, 201 com id.
- `GET /pautas/{id}`: 200 com dados ou 404.
- Validação de payload: 400.
- Persistência H2 em memória.

Fora:
- Sessão, votos, resultado (fatias 2 e 3).
- Listagem paginada (RF-13, Could).
- Erro padronizado RF-12 (Should, fora desta fatia salvo se sair sem custo extra).
- Auth, notificações.

## 3. Requisitos (MoSCoW)

**Must**
- RF-01: `POST /pautas` cria pauta, retorna 201 com id.
- RF-02: `GET /pautas/{id}` retorna 200 com dados ou 404.
- RF-03: payload inválido retorna 400.

**Should**
- Nenhum nesta fatia.

**Could**
- Nenhum nesta fatia.

**Won't (esta fatia)**
- RF-12 (corpo padronizado de erro), RF-13 (paginação).

## 4. Contrato

Request `POST /pautas`:
```json
{"titulo": "string obrigatório, não vazio", "descricao": "string opcional"}
```

Response 201:
```json
{"id": 1}
```

Response `GET /pautas/{id}` 200:
```json
{"id": 1, "titulo": "...", "descricao": "..."}
```

Regras:
- `titulo` ausente, nulo ou só espaços: 400.
- `descricao` ausente ou nula: aceita.
- `id` gerado pelo banco (sequence/identity).

## 5. Premissas

Herdadas do épico (seção 4 do master). Nenhuma nova obrigatória. Propostas, confirmar com PO:
- Tamanho máximo de `titulo` (sugestão: 255). Não coberto por AC, gera 400 se excedido. Se PO não quiser limite, remover.

## 6. Requisitos técnicos

- Java 21, Spring Boot 3, Spring MVC, Maven.
- H2 in-memory. Schema via JPA (`ddl-auto`) ou `schema.sql`. Sem migração persistente.
- Entidade `Pauta` com `id`, `titulo`, `descricao`.
- Validação com Bean Validation (`@NotBlank` em `titulo`).
- Sem alteração em código existente (greenfield).
- Testes:
  - JUnit 5: `PautaService` (criação, busca, id inexistente).
  - MockMvc: `PautaController` cobrindo 201, 200, 404, 400.
- Estimativa de arquivos: 9 (épico). Lista prevista: `Pauta`, `PautaRepository`, `PautaRequest`, `PautaResponse`, `PautaService`, `PautaController`, `PautaNaoEncontradaException`, `GlobalExceptionHandler`, `application.yml`, mais 2 testes. Total 10 sem `pom.xml` e classe main gerada. Divergência de 1, confirmar ou fundir `PautaNaoEncontradaException` no handler.

## 7. Ordem de execução

1. `pom.xml` e `application.yml` (H2 + JPA).
2. `Pauta` + `PautaRepository`.
3. `PautaRequest` (validação) + `PautaResponse`.
4. `PautaService` + teste JUnit.
5. `PautaController` + `GlobalExceptionHandler` + teste MockMvc.
6. Rodar `mvn test`. Todos os ACs verdes.

## 8. Riscos

- Baixo. Escopo pequeno, stack conhecida.
- Contrato de `POST /pautas` e `GET` vira base para fatias 2 e 3. Fechar campos antes de implementar.

```json
{"acceptance_criteria": [
  {"id": "AC-1.1", "description": "Dado pauta válida, quando POST /pautas, então 201 com id.", "how_to_verify": "MockMvc: POST /pautas com titulo válido; status 201; corpo contém id não nulo."},
  {"id": "AC-1.2", "description": "Dado id existente, quando GET /pautas/{id}, então 200 com dados da pauta.", "how_to_verify": "MockMvc: criar pauta via POST, GET /pautas/{id}; status 200; titulo e descricao conferem com o payload enviado."},
  {"id": "AC-1.3", "description": "Dado id inexistente, quando GET /pautas/{id}, então 404.", "how_to_verify": "MockMvc: GET /pautas/{id} com id inexistente; status 404."},
  {"id": "AC-1.4", "description": "Dado payload inválido (titulo ausente, nulo ou em branco), quando POST /pautas, então 400.", "how_to_verify": "MockMvc: POST /pautas sem titulo, com titulo null e com titulo em branco; status 400 nos três. JUnit: validação de bean em PautaRequest retorna violação para titulo em branco."}
]}
```
