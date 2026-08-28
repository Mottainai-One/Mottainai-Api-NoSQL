## O que mudou e por quê

<!-- Descreva a mudança em 1-3 frases. Se corrigir um bug, descreva o cenário que falhava. -->

## Checklist

**Build e testes**
- [ ] `mvn clean verify` (ou `./mvnw clean verify`) passa localmente
- [ ] Testes novos cobrem o caso feliz **e** pelo menos um caso negativo (400/404/409)
- [ ] Cobertura de `service`/`controller` continua ≥80% (gate do JaCoCo)

**Contrato de integridade com a IA Layer**
- [ ] Nenhum endpoint novo escreve em `conversations`, `messages`, `metrics` ou `agent_executions` (somente leitura)
- [ ] `empresaId` é obrigatório em toda consulta nova; acesso a dado de outra empresa retorna 404, não 403 nem os dados
- [ ] `embedding` continua nunca aceito na entrada nem devolvido em nenhuma resposta
- [ ] Nenhum índice existente foi criado/alterado pela aplicação (`auto-index-creation` continua `false`; índices só mudam via `scripts/mongo-seed.js`)

**RAG (se a mudança tocou em `rag_documents`/`rag_chunks`/busca)**
- [ ] `/rag/search` continua retornando os chunks esperados para uma busca de teste (índice de texto existe no ambiente usado)
- [ ] Cascata de exclusão de documento → chunks continua funcionando

**Segurança e documentação**
- [ ] Nenhuma credencial, URI com senha/token ou segredo foi commitado (URI do Mongo só via `SPRING_DATA_MONGODB_URI`)
- [ ] Endpoints novos/alterados têm anotação `@Operation` (Swagger) e, se relevante, entrada no README
- [ ] Mudança de comportamento visível para o time está descrita nesta PR (não só no código)

## Como testar manualmente

<!-- Passos rápidos pra quem for revisar reproduzir localmente (endpoint, payload, resultado esperado). -->
