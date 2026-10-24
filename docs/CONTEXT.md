# Contexto do projeto

## Módulos

- `README.md`: descrição da API de votação, marcada como planejada.
- `docs/SPEC.md`: escopo, modelo de ameaças e critérios de aceite (rascunho).
- Sem código executável ainda.

## Stack e comandos

- Stack declarada: Java 21, Spring Boot 3, Maven, H2 em memória, JUnit 5, MockMvc.
- Nenhum projeto Maven criado. Sem comandos de build disponíveis.

## Convenções

- Documentação em português.
- Código novo entra sempre com teste no mesmo commit.

## Decisões

- Entrega de 24/10 tratada como README inicial com spec, sem código (cenário A do ticket).
- README não afirma que a API existe; declara estado planejado.
- Endpoints e regras de quórum/pauta ficam em pendência até o solicitante enviar lista.

## Entregas

- `ba5b192` README inicial com descrição da API.
- Ciclo de correção do review: README ajustado para estado planejado e `docs/SPEC.md` criado com modelo de ameaças e critérios de aceite.

## Armadilhas

- Ticket (cenário A) prevê só `README.md` no diff; o review pediu spec. Spec foi adicionado em `docs/` por exigência do review.
- Critérios AC-1.3 do ticket dizem "só README.md"; este diff adiciona também `docs/SPEC.md` e `docs/CONTEXT.md`.

## Pendências

- Confirmar cenário (A ou B) e prioridade com o solicitante.
- Lista de endpoints e regras de votação (quórum, voto único, pauta).
- Após receber lista: criar projeto Maven, testes JUnit 5 com MockMvc, H2 em memória.
