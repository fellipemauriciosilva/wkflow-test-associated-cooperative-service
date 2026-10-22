# votacao-pautas

Cadastro e consulta de pautas (Fatia 1 do épico Votação Pautas). CRUD mínimo: criar pauta e buscar por id.

## Pré-requisitos

- JDK 21
- Maven (ou `mvnd`)

## Instalar dependências

```bash
mvnd dependency:resolve
```

## Rodar localmente

```bash
mvnd spring-boot:run
```

API disponível em `http://localhost:8080`.

## Endpoints

- `POST /pautas` — cria pauta. Body: `{"titulo": "string obrigatória", "descricao": "string opcional"}`. Retorna `201` com `{"id": 1}`. Payload inválido (titulo ausente/nulo/em branco, titulo ou descricao acima de 255 caracteres, JSON malformado) retorna `400`.
- `GET /pautas/{id}` — retorna `200` com `{"id", "titulo", "descricao"}`, `404` se não existir, ou `400` se `{id}` não for numérico.

### Limite de tamanho de titulo/descricao

Ambos aceitam no máximo 255 **code units UTF-16** (`String.length()` em Java), não bytes nem code points. Isso é o que `@Size(max = 255)` do Bean Validation mede e o que o H2 `VARCHAR(255)` também conta (caracteres, não bytes) — para a maioria dos alfabetos (incluindo acentuados) os dois coincidem. A única divergência é para caracteres fora do plano básico (ex. muitos emojis), representados por par substituto (2 code units): `"😀".repeat(128)` tem 256 code units e é rejeitado com `400`, mesmo "parecendo" 128 caracteres visuais. Essa é a regra efetivamente aplicada nesta fatia; se o PO quiser contagem por code point (grafemas), trocar `@Size` por um validador customizado com `codePointCount`.

## Rodar os testes

```bash
mvnd test
```

## Banco de dados

H2 em memória (`jdbc:h2:mem:votacao`), schema gerado via JPA (`ddl-auto: update`). Sem persistência entre execuções. Console H2 **desabilitado por padrão** (risco de execução remota de código via `CREATE ALIAS`); disponível em `/h2-console` só com o profile `dev` ativo (`--spring.profiles.active=dev`).

## Variáveis de ambiente

Nenhuma obrigatória nesta fatia.
