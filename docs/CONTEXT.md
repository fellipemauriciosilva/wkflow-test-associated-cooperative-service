# Contexto do projeto

## Módulos

- `com.acme.votacao` — classe main (`VotacaoPautasApplication`).
- `com.acme.votacao.pauta` — módulo único desta fatia: `Pauta` (entidade JPA), `PautaRepository` (Spring Data JPA), `PautaRequest`/`PautaResponse`/`PautaIdResponse` (DTOs), `PautaService` (regra de criação/busca), `PautaController` (`POST /pautas`, `GET /pautas/{id}`), `PautaNaoEncontradaException`, `GlobalExceptionHandler` (mapeia validação → 400, pauta não encontrada → 404).

## Stack e comandos

- Java 21, Spring Boot 3.3.4 (web, data-jpa, validation), H2 em memória, Maven.
- Build/teste: `mvnd test` (sem `clean`). Build isolado: `mvnd compile`.
- Rodar local: `mvnd spring-boot:run`.
- H2: `jdbc:h2:mem:votacao`, `ddl-auto: update`, sem migração (Flyway não usado — fora de escopo para greenfield pequeno com H2 em memória).

## Convenções

- DTOs como `record` (Java 21), sem Lombok (não adicionado ao pom).
- Validação via Bean Validation (`@NotBlank`, `@Size(max=255)`) em `PautaRequest`; `GlobalExceptionHandler` captura `MethodArgumentNotValidException` → 400 e `PautaNaoEncontradaException` → 404. Corpo de erro vazio (RF-12, erro padronizado, está fora desta fatia).
- Pacote único `pauta` (sem camadas `api/application/domain/infrastructure` da skill genérica — escopo pequeno não justificou a separação).

## Decisões

- Testcontainers e Flyway (sugeridos pela skill `backend-java-springboot`) **não usados**: ambiente sem Docker/rede externa, e task.md pede explicitamente H2 em memória + MockMvc. Testes de controller usam `@SpringBootTest` + `@AutoConfigureMockMvc` com o H2 real (sem mocks de banco).
- Tamanho máximo de `titulo` fixado em 255 (premissa do task.md, seção 5), via `@Size(max=255)`. Gera 400 se excedido. Ainda não confirmado com PO — revisar se divergir.
- `PautaNaoEncontradaException` mantida como classe própria (não fundida no handler), compatível com a lista de 10 arquivos prevista no épico.

## Entregas

- Fatia 1 completa: `POST /pautas` (201/400), `GET /pautas/{id}` (200/404). AC-1.1 a AC-1.4 cobertos por `PautaControllerTest` (MockMvc, 6 testes). `PautaService` coberto por `PautaServiceTest` (JUnit 5 + Mockito, 3 testes). Suite completa: 9 testes, `mvnd test` → BUILD SUCCESS.

## Armadilhas

- `mvnd`/`mvn` neste ambiente resolvem dependências via proxy interno (`orqaliz-artifact-proxy`); primeira execução de `mvnd test` é lenta (download de plugins/deps). Sem internet real — se um artefato não existir no proxy, não há como obtê-lo de outra forma.
- Warnings de `jlinenative`/`jansi` ao rodar `mvnd` no console são ruído do daemon (terminal "dumb"), não indicam falha de build.

## Pendências

- Fatias 2 (sessão de votação) e 3 (votos/resultado) do épico `run_2057fd08c4e4`, fora do escopo desta entrega.
- RF-12 (corpo padronizado de erro) e RF-13 (listagem paginada) marcados Won't/Could nesta fatia — não implementados.
- Confirmar com PO o limite de 255 caracteres em `titulo` (ver Decisões).
