# Contexto do projeto

## Módulos

Projeto único Maven `com.acme:pautas`, pacote base `com.acme.pautas`.

- `domain`: entidade `Pauta` (fábrica `Pauta.criar`, valida título não vazio), `PautaRepository` (Spring Data JPA), `PautaService` (transacional), `PautaNaoEncontradaException`.
- `api`: `PautaController` (`POST /pautas`, `GET /pautas/{id}`), `CriarPautaRequest` (record com `@NotBlank titulo`, `descricao` opcional), `PautaResponse` (record `id`, `titulo`, `descricao`), `PautaExceptionHandler` (mapeia not-found para 404).

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Maven. Spring Web MVC, Spring Data JPA, Bean Validation.
- Banco: H2 em memória (`jdbc:h2:mem:pautas`), schema por `spring.jpa.hibernate.ddl-auto=create-drop`. Sem Flyway/Liquibase.
- Testes: JUnit 5, MockMvc (`@SpringBootTest` + `@AutoConfigureMockMvc`), AssertJ.
- Build e testes: `mvnd test` (suite inteira, sem `clean`). Teste único: `mvnd test -Dtest='Classe#metodo'`.
- Sem Docker, sem rede externa. Dependências resolvem pelo proxy interno.

## Convenções

- Id `Long` autoincremento (`GenerationType.IDENTITY`).
- Títulos duplicados permitidos.
- Erro de validação de corpo: 400 pelo handler padrão do Spring.
- 201 com corpo `{id, titulo, descricao}` e header `Location` apontando para `/pautas/{id}`.
- `java.time.Clock` previsto na stack, ainda não injetado (sem regra de prazo neste escopo).

## Decisões

- Dados em memória, perdidos ao reiniciar. Aceito pelo escopo.
- Validação de título em duas camadas: `@NotBlank` no request e guarda em `Pauta.criar`.
- Descrição nula é persistida como nula e omitida do JSON de resposta quando ausente.

## Entregas

- Commit WIP: esqueleto Maven, criação e consulta de pautas, validação do título, 404, testes MockMvc e de domínio. `mvnd test`: 13 testes, todos verdes.

## Armadilhas

- `spring.jpa.open-in-view` está ativo por padrão e gera WARN na subida. Não é erro.
- Nenhum limite de tamanho de título/descrição implementado ainda (aguarda P1).
- Divergências de código HTTP para votação (409, 422, 410, zeros vs 409) pertencem a W1 e não se aplicam aqui.

## Pendências

- P1 (bloqueante para S2): limite de tamanho. Ticket diz título 255 e descrição 2000; `task.md` anterior diz 200 e 1000. Decisão do PO. Depois, virar constante configurável com validação.
- P3 confirmar: 201 com id no corpo e header `Location` (assumido).
- P5: prioridade a confirmar.
- W1 a W5 fora do escopo: votação, autenticação, listagem, atualização, remoção.
