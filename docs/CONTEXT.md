# CONTEXT

## Módulos

- `com.acme.pautas.pauta`: cadastro e consulta de pautas (único módulo).
  - `PautaController`: `POST /pautas`, `GET /pautas/{id}`.
  - `PautaService`: regra de cadastro e busca; lança `PautaNaoEncontradaException`.
  - `Pauta`: entidade JPA (`id` Long IDENTITY, `titulo`, `descricao`).
  - `PautaRepository`: Spring Data JPA.
  - `CriarPautaRequest`, `PautaResponse`: DTOs do contrato.
  - `ApiExceptionHandler`: corpo JSON de erro `{status, erro, detalhes}`.

## Stack e comandos

- Java 21, Spring Boot 3.3.5 (`jakarta.*`), Maven.
- Spring Web MVC, Spring Data JPA, Bean Validation, H2 em memória.
- Testes: JUnit 5 + MockMvc (`@SpringBootTest` + `@AutoConfigureMockMvc`).
- Build/teste: `mvnd test` (sem `clean`). Teste único: `mvnd test -Dtest='PautaControllerTest#metodo'`.
- Sem Docker, sem rede externa. Dependências via proxy interno (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`).

## Convenções

- Pacote por feature (`pauta`).
- Id gerado pelo banco (`GenerationType.IDENTITY`).
- Erros de validação e 404 retornam o mesmo corpo JSON.
- Sem comentários de código; sem timestamps (`Clock` fora do escopo).

## Decisões

- Título obrigatório: `@NotBlank` → 400 (assumido, aguarda PO).
- Descrição opcional, sem limite de tamanho (não informado).
- Corpo do POST: `{"id": <Long>}` com 201 e header `Location: /pautas/{id}` (assumido, aguarda PO).
- Sem requisito de latência (W1).
- Escopo restrito a POST e GET; nada de alterar, excluir, listar, votação, autenticação.

## Entregas

- Cadastro de pauta (`POST /pautas`, 201 + id + Location).
- Consulta de pauta (`GET /pautas/{id}`, 200 ou 404).
- Validação de título (400 para ausente, vazio ou só espaços).
- Testes MockMvc: 10 casos cobrindo AC-1.1 a AC-1.4, AC-2.1, AC-2.2 e header Location. Suite verde.

## Armadilhas

- `@Lob` em `titulo` e `descricao` no H2: aceito sem limite de tamanho, conforme decisão atual. Revisar se o PO definir limite.
- `curl` e `wget` não existem na imagem; não usar para testar o proxy Maven.
- Saída do `mvnd` no terminal sai truncada/embaralhada (jline sem native lib); conferir resultado em `target/surefire-reports`.

## Pendências

- Gate da spec: implementação bloqueada até PO confirmar:
  - status de validação (400 assumido);
  - corpo do POST (`{"id"}` assumido);
  - limite de tamanho de título/descrição;
  - requisito de latência (nenhum assumido);
  - prioridade.
- Se o PO mudar o contrato, ajustar `ApiExceptionHandler`, `PautaController` e os testes correspondentes.
