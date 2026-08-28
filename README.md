# Mottainai — Mongo API

Camada REST (Spring MVC + Spring Data MongoDB) de consulta e curadoria do MongoDB usado pela
IA Layer do ecossistema Mottainai. Não é o backend do PostgreSQL nem a IA Layer — é uma API
adicional que expõe via HTTP o que hoje só a IA Layer acessa.

## Restrição de integridade

| Coleção            | Operações permitidas          |
|---------------------|-------------------------------|
| `conversations`     | Somente leitura                |
| `messages`          | Somente leitura                |
| `metrics`           | Somente leitura + agregação    |
| `agent_executions`  | Somente leitura                |
| `rag_documents`     | CRUD completo                  |
| `rag_chunks`        | CRUD completo                  |
| `memories`          | Leitura e exclusão apenas      |

`empresaId` é obrigatório em toda consulta (multi-tenant). Nenhum endpoint retorna dado de
outra empresa — mesmo com um `_id` correto, o resultado é **404**, não 403 (não confirma a
existência do recurso para quem não tem acesso a ele).

## Stack

- Java 17, Spring Boot 3.3.4, Spring MVC (não reativo)
- Spring Data MongoDB
- MapStruct (mapeamento DTO ↔ documento) + Lombok
- springdoc-openapi (Swagger UI)
- JUnit 5 + Mockito (unitários) + Testcontainers MongoDB (integração)
- JaCoCo (cobertura mínima de 80% em `service` e `controller`)

Todas as versões são fixas no `pom.xml` — sem `LATEST`, `RELEASE` ou snapshot.

O projeto inclui o Maven Wrapper (`mvnw` / `mvnw.cmd`), então os comandos abaixo funcionam mesmo
sem Maven instalado na máquina — basta trocar `mvn` por `./mvnw` (Linux/macOS) ou `mvnw.cmd`
(Windows).

## Como subir o MongoDB localmente

```bash
docker run -d --name mottainai-mongo -p 27017:27017 mongo:7.0.14
```

Popule com os dados de exemplo (idempotente):

```bash
mongosh "mongodb://127.0.0.1:27017/mottainai" scripts/mongo-seed.js
```

Se o seu MongoDB exigir autenticação, ajuste a URI:

```bash
mongosh "mongodb://<user>:<senha>@127.0.0.1:27017/mottainai?authSource=admin" scripts/mongo-seed.js
```

## Como rodar a API

A URI do Mongo **nunca** é hardcoded — vem da variável de ambiente `SPRING_DATA_MONGODB_URI`.
Sem a variável, a aplicação usa como padrão `mongodb://localhost:27017/mottainai` (sem
credenciais), útil só para desenvolvimento local.

```bash
# Windows (PowerShell)
$env:SPRING_DATA_MONGODB_URI="mongodb://127.0.0.1:27017/mottainai"
mvn spring-boot:run

# Linux/macOS
export SPRING_DATA_MONGODB_URI="mongodb://127.0.0.1:27017/mottainai"
mvn spring-boot:run
```

Um modelo comentado (com placeholders, nunca credenciais reais) está em
[`src/main/resources/application-example.yml`](src/main/resources/application-example.yml).

Depois de subir, a documentação interativa fica em:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Como rodar os testes

Requer Docker (usado pelo Testcontainers para subir um MongoDB descartável nos testes de
integração — nenhum teste depende de um Mongo já rodando na máquina).

```bash
mvn clean verify
```

Isso roda os testes unitários (Mockito, sem Spring/Mongo) e os de integração (MockMvc +
Testcontainers), e falha o build se a cobertura de `service`/`controller` cair abaixo de 80%
(JaCoCo, configurado no `pom.xml`).

Para rodar só os unitários (rápido, sem Docker):

```bash
mvn test -Dtest=*ServiceTest
```

## Endpoints

Prefixo `/api/v1`. Toda listagem é paginada (`page`, `size`, `sort`) e devolve um `Page`
padrão do Spring (`content`, `totalElements`, `totalPages`, etc.).

### RAG — CRUD completo

```
GET    /rag/documents?empresaId=&source=
GET    /rag/documents/{id}?empresaId=
GET    /rag/documents/slug/{slug}?empresaId=
POST   /rag/documents                          body: {slug, empresaId, title, source}
PUT    /rag/documents/{id}?empresaId=          body: {slug, empresaId, title, source}
DELETE /rag/documents/{id}?empresaId=          apaga os chunks em cascata

GET    /rag/documents/{id}/chunks?empresaId=
POST   /rag/documents/{id}/chunks?empresaId=   body: {chunk?, text}
PUT    /rag/chunks/{id}?empresaId=             body: {chunk?, text}
DELETE /rag/chunks/{id}?empresaId=

GET    /rag/search?empresaId=&q=
```

Notas:
- `slug` é único globalmente (não por empresa) — duplicado retorna **409**.
- Em `POST`/`PUT` de chunk, se `chunk` não vier, é calculado como `max(chunk existente)+1`.
- `embedding` nunca é aceito na entrada (os DTOs de request nem têm o campo) nem devolvido
  na saída (é grande e é responsabilidade exclusiva da IA Layer).
- No `PUT` de documento, o `empresaId` do corpo é ignorado — o dono nunca muda após a criação;
  o `empresaId` de query é quem autoriza a operação.
- `/rag/search` resolve primeiro os documentos da empresa (`rag_documents`) e só então aplica
  a busca textual (`$text`) restrita a esses `documentId`s — o filtro de tenant é a primeira
  barreira, nunca um pós-filtro.

### Conversas e mensagens — somente leitura

```
GET /conversations?empresaId=&usuarioId=&status=
GET /conversations/{sessionId}?empresaId=
GET /conversations/{sessionId}/messages?empresaId=
```

### Memória — leitura e exclusão

```
GET    /memories?empresaId=
GET    /memories/{empresaId}/{usuarioId}
DELETE /memories/{empresaId}/{usuarioId}
```

### Observabilidade — somente leitura + agregação

```
GET /metrics?empresaId=&agent=&from=&to=
GET /metrics/summary?empresaId=&from=&to=
GET /executions?empresaId=&agent=&status=
```

`GET /metrics/summary` roda uma agregação nativa do MongoDB (`$group` por `agent`) e devolve,
por agente: total de execuções, custo total, latência média, média do `judgeScore` (ignorando
execuções sem `judgeScore`, sem penalizar o denominador) e taxa de erro (proporção de execuções
com `status` diferente de `"completed"`).

## Tratamento de erros

Todo erro tem corpo padronizado, sem stack trace nem detalhe interno:

```json
{
  "timestamp": "2026-08-27T18:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Documento não encontrado: 64f...",
  "path": "/api/v1/rag/documents/64f..."
}
```

| Situação                                   | Status |
|---------------------------------------------|--------|
| `empresaId` ausente ou parâmetro inválido    | 400    |
| corpo inválido (Bean Validation)             | 400    |
| recurso não encontrado / de outra empresa    | 404    |
| `slug` duplicado                             | 409    |
| erro inesperado                              | 500    |
