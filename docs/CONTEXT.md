# CONTEXT

## Módulos

- `br.coop.pautas.PautasApplication`: entrypoint Spring Boot.
- `br.coop.pautas.pauta`: fatia 1, cadastro e consulta de pautas.
  - `Pauta` (entidade JPA, tabela `pauta`, ID `Long` IDENTITY, `titulo` obrigatório, `descricao` opcional).
  - `PautaRepository` (Spring Data JPA).
  - `PautaRequest` (record de entrada, `titulo` com `@NotBlank` e `@Size(max = 255)`, `descricao` com `@Size(max = 255)`).
  - `PautaResponse` (record de saída: `id`, `titulo`, `descricao`).
  - `PautaController`: `POST /pautas` e `GET /pautas/{id}`.

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Maven. Starters: web, data-jpa, validation, test. H2 runtime.
- Build e testes: `mvnd test` (sem `clean`). Teste único: `mvnd test -Dtest='PautaControllerTest#metodo'`.
- Testes exigem profile `test` (`@ActiveProfiles("test")`), que carrega `application-test.yml` com H2 e `create-drop`.
- Dependências resolvem pelo proxy interno (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`). Sem `settings.xml` no ambiente; o Maven resolveu sem configuração extra.
- Sem `curl` nem `python3` no ambiente.

## Convenções

- Pacote por feature (`pauta`), não por camada.
- DTO como record; entidade com construtor protegido para JPA.
- Erros via `ProblemDetail` (`spring.mvc.problemdetails.enabled=true`).
- Testes: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`, sem servidor real, H2 in-memory. Isolamento por `repository.deleteAll()` em `@BeforeEach`.

## Decisões

- Campos da pauta: `titulo` obrigatório, `descricao` opcional (proposta do ticket, decisão 1 em aberto).
- `POST /pautas` retorna 201 com `Location: /pautas/{id}` (proposta, decisão 2 em aberto).
- ID `Long` IDENTITY (proposta, decisão 3 em aberto).
- Persistência JPA/Hibernate (proposta, decisão 4 em aberto).
- Validação: campo ausente ou vazio retorna 400 em `ProblemDetail` (proposta, decisão 5 em aberto).
- Tamanho: `titulo` e `descricao` limitados a 255 caracteres por `@Size(max = 255)` no `PautaRequest`, batendo com a coluna default `VARCHAR(255)`. Excedente retorna 400, não 500.
- Configuração de banco: base `application.yml` sem datasource e com `ddl-auto: none`. H2 in-memory e `create-drop` ficam só em `application-test.yml`.
- GET com ID inexistente lança `ResponseStatusException(NOT_FOUND)`, respondido como `ProblemDetail`.

## Mapeamento de critérios de aceitação (fatia 1)

- AC-1.1 (POST válido cria e retorna ID não nulo): `PautaControllerTest.criaPautaERetornaIdNaoNulo`.
- AC-1.2 (GET existente retorna campos): `PautaControllerTest.consultaPautaExistenteRetornaCamposEnviados`.
- AC-1.3 (GET inexistente retorna 404 ProblemDetail): `PautaControllerTest.consultaPautaInexistenteRetorna404ComProblemDetail`.
- AC-1.4 (POST sem `titulo` ou vazio retorna 400 e não persiste): `PautaControllerTest.criaPautaSemTituloRetorna400ENaoPersiste`.
- AC-1.5 (pauta persistida em H2 e recuperável pelo ID): `PautaControllerTest.pautaCriadaEstaPersistidaComCamposCorretos`.
- Extras: `descricao` ausente retorna `null` no GET (`criaPautaSoComTituloERetornaDescricaoNula`); `titulo` e `descricao` com 256 caracteres retornam 400 e não persistem.

## Entregas

- Fatia 1 (cadastro e consulta de pautas): AC-1.1 a AC-1.5 cobertos por `PautaControllerTest` (8 testes).
- Correções do review: teste de `descricao` opcional, limite de 255 caracteres, config de teste separada, isolamento determinístico, `Location` com asserção exata.

## Armadilhas

- `ResponseStatusException` precisa de `spring.mvc.problemdetails.enabled` para sair como `ProblemDetail`.
- Com H2 e `create-drop`, usar `DB_CLOSE_DELAY=-1` para não perder dados entre conexões.
- Sem o profile `test`, a aplicação não sobe com datasource: a base não define banco. Garantir `@ActiveProfiles("test")` em todo teste de integração.
- `mvnd test` sem `clean` é o fluxo esperado neste ambiente.
- `maxPostSize` do Tomcat cobre form, não corpo JSON. Limite de tamanho do body depende de `@Size` e, se necessário, de gateway.

## Pendências

- Decisões 1 a 4 do `task.md` (seção 6) não confirmadas pelo time; implementado com as propostas.
- Decisão 5 (validação 400) implementada conforme proposta; confirmar com o time.
- **Risco de segurança (alto):** `POST /pautas` e `GET /pautas/{id}` sem autenticação nem autorização. Qualquer cliente cria e lê pauta. IDs IDENTITY sequenciais permitem enumeração trivial. Decisão pendente antes da fatia 2: fatias 2 (votos) e 3 (resultado) herdam o problema, pois voto sem identidade do associado é fraude trivial. Fora de escopo nesta fatia (W1 do epic).
- Normalização de entrada: `titulo` com espaços nas bordas é persistido como enviado. Sem unicidade: pautas duplicadas são aceitas. Decisão de negócio pendente.
- Saída de `descricao` é JSON sem escape HTML. Frontend que renderizar como HTML fica exposto a XSS armazenado. Documentar para o consumidor.
- Dependências sem verificação de CVE (sem ferramenta SCA no ambiente). Sugerido OWASP dependency-check no build.
- Fatias 2 (sessão e votos) e 3 (resultado) dependem do contrato de `{id}` (tipo `Long`).
- C1 (Springdoc) fica no epic.
