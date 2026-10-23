# CONTEXT

## Módulos

- `pautas` (pacote único, estrutura por feature):
  - `PautasApplication`: bootstrap Spring Boot.
  - `Pauta`: entidade JPA, tabela `pauta`, id `Long` via sequência `pauta_seq`.
  - `PautaRepository`: `JpaRepository<Pauta, Long>`. Reutilizável nas fatias 2 e 3.
  - `PautaService`: `cadastrar(CriarPautaRequest) -> Long`, `buscar(Long) -> PautaResponse`.
  - `PautaController`: `POST /pautas` (201 + `Location` + `{"id"}`), `GET /pautas/{id}` (200).
  - `CriarPautaRequest` (record, Bean Validation), `PautaResponse`, `PautaCriadaResponse`, `ErroResponse` (com `CampoErro`).
  - `PautaNaoEncontradaException`: lançada pelo service, mapeada para 404.
  - `GlobalExceptionHandler` (`@RestControllerAdvice`): 400 validação (com `fieldErrors`), 400 corpo ilegível, 400 parâmetro com tipo errado, 404 pauta, 405 método não permitido, 415 mídia não suportada.

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Maven. Dependências: web, data-jpa, validation, H2 (runtime), starter-test.
- H2 em memória: `jdbc:h2:mem:pautas`, `ddl-auto: create-drop`, `open-in-view: false`.
- Testes: `mvnd test` (suite inteira, sem `clean`). Classe única: `mvnd test -Dtest=PautaControllerTest`.
- Sem Docker e sem internet: dependências resolvem pelo mirror Maven (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`, configurado em `/usr/share/maven/conf/settings.xml`).
- Versões fixadas conforme cache local `/orqaliz/cache/m2`: Boot 3.3.5, H2 2.2.224, JUnit 5.10.x.

## Convenções

- Pacote base `pautas`. Mensagens de erro de validação em português, no `@NotBlank`/`@Size` do request.
- Corpo de erro padronizado: `timestamp, status, error, message, path, fieldErrors`. Vale para erros tratados pelo `GlobalExceptionHandler`. Rota inexistente (404 de `/error`) fica fora: o Spring só lança exceção para rota sem handler com `spring.mvc.throw-exception-if-no-handler-found=true` e `spring.web.resources.add-mappings=false`, que não estão configurados. Não habilitado nesta fatia para não afetar outros recursos estáticos.
- Campo `id` do POST é ignorado (não existe no record de request).
- Campo desconhecido no JSON é ignorado (padrão Jackson).
- Testes: `@SpringBootTest` + `@AutoConfigureMockMvc`, cenários de aceite AC-1.1 a AC-1.5 em `PautaControllerTest`.

## Decisões

- `descricao` opcional, máximo 2000 caracteres. `titulo` obrigatório, sem vazio após trim, máximo 200.
- Pauta duplicada (mesmo título) permitida. Sem regra de unicidade.
- Persistência em H2 em memória, premissa até decisão antes de produção.
- Contrato de API é premissa até confirmação com PO (ver `task.md` seção 3).

## Entregas

- Fatia 1/3 (cadastro e consulta de pautas): endpoints `POST /pautas` e `GET /pautas/{id}`, handler global, entidade e repositório, 22 testes MockMvc (limites 200/2000 aceitos, duplicata permitida, erros de forma/método/mídia com corpo padronizado).

## Armadilhas

- `@ValueSource` não aceita expressões concatenadas (`"a".repeat(n)`). Usar `@MethodSource` para casos gerados.
- Saída do `mvnd` vem truncada pelos avisos do jline. Redirecionar para arquivo (`> log 2>&1`) e filtrar `BUILD|Tests run|ERROR`.
- `curl` não existe na imagem.
- `open-in-view: false`: DTOs são montados dentro do service (`@Transactional(readOnly = true)`), não no controller.

## Pendências

- Confirmar contrato com PO (campos, tamanhos, formato de erro).
- Decidir persistência durável antes de produção (H2 em memória perde dados no restart).
- Fatias 2 (sessão, votos) e 3 (resultado) reutilizam `Pauta` e `PautaRepository` sem alteração de assinatura.
- Atualizar/remover pauta e listagem fora desta fatia.
