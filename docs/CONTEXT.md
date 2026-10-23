# Contexto do projeto

## Módulos
- `br.coop.assembleia.pauta`: slice 1 da pauta de assembleia.
  - `PautaController`: `POST /pautas` (201 + Location), `GET /pautas/{id}` (200 / 404).
  - `Pauta`: entidade JPA (`id`, `titulo` obrigatório, `descricao` opcional).
  - `PautaRepository`: Spring Data JPA.
  - `PautaRequest`: record de entrada com Bean Validation.
  - `PautaResponse`: record de saída (`id`, `titulo`, `descricao`).

## Stack e comandos
- Java 21, Spring Boot 3.3.5, Maven, H2 em memória, JUnit 5, MockMvc.
- Rodar teste: `mvnd test -Dtest=PautaControllerTest`.
- Suite completa: `mvnd test` (sem `clean`).

## Convenções
- Erros 400 de validação e 404 seguem o padrão do Spring Boot.
- Limites: `titulo` 200, `descricao` 1000 (defaults propostos, pendentes de confirmação do PO).

## Decisões
- Sem Flyway/Liquibase; schema gerado pelo Hibernate (`ddl-auto` padrão para H2 em memória).
- `Clock` não introduzido: a slice não tem campo temporal.
- 404 via `ResponseStatusException`.

## Entregas
- Slice 1: criação e consulta de pauta, validação de `titulo` e `descricao` (limites 200/1000), testes MockMvc para 201, 400, 200 e 404.
- Ciclo de correção do review: teste de `descricao` acima do limite; `Location` validado como `/pautas/{id}` exato.

## Armadilhas
- Teste de tab em JSON: o caractere `\t` cru quebra o parse e gera 400 de sintaxe, não de validação. Usar `\\t` no literal Java.

## Pendências
- Confirmar com PO os limites 200/1000 (ticket diz "sem limite").
- Confirmar prioridade (sugerido média).
- Cenário 409 citado no ticket, sem caso definido: removido do escopo.
