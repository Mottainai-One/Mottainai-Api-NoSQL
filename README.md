# 🍃 Mottainai — Mongo API

Bem-vindo ao repositório da **Mongo API** do ecossistema **Mottainai**! 

Esta é a camada REST (Spring MVC + Spring Data MongoDB) responsável pela consulta e curadoria do banco de dados MongoDB. O seu objetivo principal é servir como ponte de ligação entre o MongoDB do Mottainai e o nosso aplicativo mobile, armazenando e expondo as informações vitais do funcionamento do chatbot, tais como **prompts, respostas, execuções de agentes e memórias**.

> ⚠️ **Nota Arquitetural:** Este não é o backend relacional (PostgreSQL) nem a própria *IA Layer*. Trata-se de uma API adicional que expõe via HTTP os dados que, historicamente, apenas a IA Layer acessava de forma direta.

---

## 🔒 Restrição de Integridade (Multi-tenant)

A nossa API foi desenhada com segurança e isolamento de dados em mente:
- O campo `empresaId` é **obrigatório** em toda consulta.
- Nenhum endpoint retorna dados de outra empresa. 
- Mesmo que um `_id` seja válido, se não pertencer à empresa da requisição, o resultado retornado é **404 (Not Found)** e não 403. Isso garante que o sistema sequer confirme a existência de um recurso para quem não tem acesso a ele.

---

## 🗂️ Coleções e Operações Permitidas

Para manter a consistência dos dados geridos pela Inteligência Artificial, aplicamos restrições rigorosas de operações por coleção:

| Coleção | Operações Permitidas |
| :--- | :--- |
| 💬 `conversations` | Somente leitura |
| ✉️ `messages` | Somente leitura |
| 📊 `metrics` | Somente leitura + agregação |
| 🤖 `agent_executions` | Somente leitura |
| 📄 `rag_documents` | CRUD completo |
| 🧩 `rag_chunks` | CRUD completo |
| 🧠 `memories` | Leitura e exclusão apenas |

O Mongo da IA também contém coleções internas de configuração, cache e observabilidade
(`tool_runs`, `prompts`, `feedbacks` e outras). Esta API expõe apenas as coleções
da tabela acima. O schema completo e seus validadores são mantidos pela IA Layer
em `scripts/mongo/schema.json` daquele projeto.

Para adicionar conteúdo que deva aparecer na busca semântica da IA, use o upload
`POST /rag/documents` da IA Layer: ele cria os chunks e seus embeddings. O CRUD
RAG desta API Java grava chunks sem embeddings; a IA não os inclui na recuperação
semântica até que sejam processados pela IA Layer.

---

## 🛠️ Stack Tecnológica

O projeto foi construído com ferramentas robustas, visando alta cobertura de testes e padronização:
* **Linguagem & Framework:** Java 17, Spring Boot 3.3.4, Spring MVC (não reativo)
* **Persistência:** Spring Data MongoDB
* **Mapeamento:** MapStruct (mapeamento DTO ↔ documento) e Lombok
* **Documentação da API:** springdoc-openapi (Swagger UI)
* **Qualidade e Testes:** 
  * JUnit 5 + Mockito (Testes unitários)
  * Testcontainers MongoDB (Testes de integração)
  * JaCoCo (Cobertura mínima exigida de 80% em `service` e `controller`)
* **Gerenciamento de Dependências:** Todas as versões são rigorosamente fixas no `pom.xml` (é proibido o uso de `LATEST`, `RELEASE` ou snapshots).

---

## 🎓 Quem fez este repositório?

Este sistema faz parte do **Projeto Interdisciplinar – 2026** desenvolvido por alunos do **Instituto J&F Germinare TECH**.

**🚀 Integrantes do Grupo Desenvolvedor:**
* Arthur do Vale Silva

