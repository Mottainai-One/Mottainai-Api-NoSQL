// Mottainai — seed do MongoDB para a API Spring (DS2).
// Idempotente: não duplica documentos já existentes.
// Uso: mongosh "mongodb://<user>:<senha>@127.0.0.1:27017/mottainai?authSource=admin" scripts/mongo-seed.js
//
// Bootstrap demonstrativo das coleções expostas por esta API.
// O schema completo (validadores e demais coleções) é mantido pela IA Layer
// em scripts/mongo/schema.json; este seed não o substitui.

const EMPRESA_ID = 1;
const now = () => new Date();

// ── Índices das coleções usadas pela API ──────────────────────────
db.conversations.createIndex({ sessionId: 1 }, { name: "ux_conversations_sessionId", unique: true });
db.conversations.createIndex({ empresaId: 1, usuarioId: 1, startedAt: -1 }, { name: "ix_conversations_empresa_usuario_startedAt" });
db.conversations.createIndex({ agent: 1, status: 1 }, { name: "ix_conversations_agent_status" });
db.conversations.createIndex({ lastInteraction: -1 }, { name: "ix_conversations_lastInteraction" });

db.messages.createIndex({ conversationId: 1, createdAt: 1 }, { name: "ix_messages_conversation_createdAt" });
db.messages.createIndex({ agent: 1, skill: 1, createdAt: -1 }, { name: "ix_messages_agent_skill_createdAt" });

db.metrics.createIndex({ conversationId: 1, createdAt: -1 }, { name: "ix_metrics_conversation_createdAt" });
db.metrics.createIndex({ agent: 1, skill: 1, createdAt: -1 }, { name: "ix_metrics_agent_skill_createdAt" });

db.memories.createIndex({ empresaId: 1, usuarioId: 1 }, { name: "ux_memories_empresa_usuario", unique: true });
db.memories.createIndex({ updatedAt: -1 }, { name: "ix_memories_updatedAt" });

db.agent_executions.createIndex({ conversationId: 1, startedAt: -1 }, { name: "ix_agent_executions_conversation_startedAt" });
db.agent_executions.createIndex({ agent: 1, status: 1, startedAt: -1 }, { name: "ix_agent_executions_agent_status_startedAt" });

// O Mongo atual mantém duas chaves únicas por empresa: slug e (title, version).
db.rag_documents.createIndex({ empresaId: 1, category: 1, createdAt: -1 }, { name: "ix_rag_documents_empresa_category_createdAt" });
db.rag_documents.createIndex({ empresaId: 1, slug: 1 }, { name: "ux_rag_documents_empresa_slug", unique: true });
db.rag_documents.createIndex({ empresaId: 1, title: 1, version: 1 }, { name: "ux_rag_documents_empresa_title_version", unique: true });
// O índice de texto vive em rag_documents.title (não mais em rag_chunks.text).
db.rag_documents.createIndex({ title: "text" }, { name: "tx_rag_documents_title", default_language: "english" });

db.rag_chunks.createIndex({ documentId: 1, chunk: 1 }, { name: "ux_rag_chunks_document_chunk", unique: true });

// ── RAG: documentos e chunks ─────────────────────────────────────
const documentos = [
  {
    slug: "politica-estoque-mottainai",
    title: "Política de Gestão de Estoque — Mottainai",
    source: "manual_operacional",
    category: "MANUAL",
    version: "1.0",
    metadata: { audience: "funcionario" },
    chunks: [
      "O sistema Mottainai utiliza o método FEFO (First Expired, First Out) para controle de estoque. Produtos com data de vencimento mais próxima devem ser vendidos prioritariamente.",
      "O nível mínimo de estoque (ponto de pedido) é calculado com base na demanda média dos últimos 30 dias mais o estoque de segurança de 20%.",
      "Produtos com menos de 7 dias para vencimento devem receber alerta de prioridade CRITICAL automaticamente. Com menos de 14 dias, prioridade HIGH.",
      "Transferências entre lojas devem ser aprovadas pelo gerente e registradas no sistema antes da movimentação física.",
      "O inventário físico deve ser realizado mensalmente. Divergências superiores a 2% do valor total exigem apuração imediata."
    ]
  },
  {
    slug: "guia-vencimento-pereciveis",
    title: "Guia de Controle de Perecíveis e Validade",
    source: "manual_operacional",
    category: "MANUAL",
    version: "1.0",
    metadata: { audience: "funcionario" },
    chunks: [
      "Leite UHT pode ser armazenado em temperatura ambiente até 6 meses. Após aberto, refrigerar e consumir em até 5 dias.",
      "Iogurtes e produtos lácteos refrigerados devem ser mantidos entre 1°C e 10°C. Alerta de vencimento deve ser emitido com 5 dias de antecedência.",
      "Pães e produtos de panificação têm vida útil curta (2-5 dias). Acionar promoção relâmpago quando restar 48h para o vencimento.",
      "Refrigerantes e bebidas não alcoólicas seladas têm validade de 6 a 12 meses. Verificar integridade da embalagem no recebimento.",
      "O método FIFO deve ser aplicado a produtos não perecíveis. Para perecíveis, sempre usar FEFO independentemente."
    ]
  },
  {
    slug: "procedimento-descarte-doacao",
    title: "Procedimentos de Descarte e Doação",
    source: "manual_operacional",
    category: "MANUAL",
    version: "1.0",
    metadata: { audience: "funcionario" },
    chunks: [
      "Produtos vencidos ou com embalagem violada devem ser descartados conforme normas sanitárias. Registrar no sistema com motivo e quantidade.",
      "Doações de alimentos próximos ao vencimento (>2 dias de validade) podem ser realizadas para entidades cadastradas no sistema.",
      "O custo de descarte deve ser monitorado mensalmente. Meta: redução de 15% ao trimestre através de ações preditivas.",
      "Promoções relâmpago de até 40% de desconto são recomendadas para produtos com 2-5 dias de vencimento, antes de acionar descarte.",
      "Registro de descarte requer aprovação do supervisor e deve incluir: produto, lote, quantidade, motivo e foto (quando possível)."
    ]
  },
  {
    slug: "faq-cliente-mottainai",
    title: "FAQ — Perguntas Frequentes de Clientes",
    source: "faq",
    category: "FAQ",
    version: "1.0",
    metadata: { audience: "cliente" },
    chunks: [
      "Posso devolver um produto comprado? Sim, em até 7 dias após a compra, mediante apresentação do cupom fiscal.",
      "Como funciona o programa de fidelidade? Cada compra acumula pontos que podem ser trocados por descontos nas próximas compras.",
      "Qual o horário de funcionamento das lojas Mottainai? As lojas funcionam de segunda a sábado das 8h às 22h e domingos das 9h às 20h.",
      "Como solicitar nota fiscal de empresa? Informe o CNPJ no momento do pagamento. A nota é emitida automaticamente.",
      "Como entrar em contato com o suporte? Pelo WhatsApp (11) 99999-0000 ou email contato@mottainai.com, disponível 24h."
    ]
  }
];

let novosDocs = 0, novosChunks = 0;
documentos.forEach((doc) => {
  // Idempotência pela chave única atual da coleção: (empresaId, title, version).
  if (db.rag_documents.findOne({ empresaId: EMPRESA_ID, title: doc.title, version: doc.version })) { return; }
  const res = db.rag_documents.insertOne({
    slug: doc.slug, empresaId: EMPRESA_ID, title: doc.title,
    source: doc.source, category: doc.category, version: doc.version,
    metadata: doc.metadata, createdAt: now()
  });
  novosDocs++;
  doc.chunks.forEach((texto, i) => {
    db.rag_chunks.insertOne({
      documentId: res.insertedId, chunk: i, text: texto,
      // embedding é calculado exclusivamente pela IA Layer; o seed nunca o preenche.
      embedding: null, metadata: { section: doc.category.toLowerCase() }, createdAt: now()
    });
    novosChunks++;
  });
});

// ── Conversa de exemplo (leitura pela API) ───────────────────────
const SESSION_ID = "seed-ds2-demo-001";
if (!db.conversations.findOne({ sessionId: SESSION_ID })) {
  const conv = db.conversations.insertOne({
    sessionId: SESSION_ID, empresaId: EMPRESA_ID, usuarioId: 1,
    agent: "funcionario", skill: null, status: "active",
    title: "Consulta de validade", startedAt: now(),
    lastInteraction: now(), endedAt: null
  });

  db.messages.insertMany([
    {
      empresaId: EMPRESA_ID, conversationId: conv.insertedId, role: "user",
      content: "Quais produtos estão próximos do vencimento?",
      agent: null, skill: null, model: null, sources: [],
      inputTokens: null, outputTokens: null, createdAt: now()
    },
    {
      empresaId: EMPRESA_ID, conversationId: conv.insertedId, role: "assistant",
      content: "Há 3 lotes com vencimento em até 7 dias na loja Centro.",
      agent: "funcionario", skill: null, model: "qwen2.5:7b-instruct",
      sources: [{ type: "rag", ref: "slug:guia-vencimento-pereciveis" }],
      inputTokens: 120, outputTokens: 45, createdAt: now()
    }
  ]);

  db.metrics.insertOne({
    empresaId: EMPRESA_ID, sessionId: SESSION_ID,
    conversationId: conv.insertedId, agent: "funcionario", skill: null,
    model: "qwen2.5:7b-instruct", inputTokens: 120, outputTokens: 45,
    latency: 1.842, estimatedCost: Double(0),
    costReference: { inputPerMillionUsd: Double(0), outputPerMillionUsd: Double(0) },
    judgeScore: 0.91, status: "completed",
    nodeLatenciesMs: { supervisor: 120.5, funcionario: 1500.2, juiz: 221.3 },
    createdAt: now()
  });

  db.agent_executions.insertOne({
    empresaId: EMPRESA_ID, sessionId: SESSION_ID,
    conversationId: conv.insertedId, agent: "funcionario", skill: null,
    status: "completed", latency: 1.842,
    nodeLatenciesMs: { supervisor: 120.5, funcionario: 1500.2, juiz: 221.3 },
    tools: ["rag_search"], executionFlow: ["Supervisor", "Funcionário", "Resposta"],
    // error é documento (ex.: { message: "APIStatusError" }), não string.
    error: null, startedAt: now(), finishedAt: now(), createdAt: now()
  });
}

// ── Memória de longo prazo (sem PII) ─────────────────────────────
db.memories.updateOne(
  { empresaId: EMPRESA_ID, usuarioId: 1 },
  {
    $setOnInsert: {
      empresaId: EMPRESA_ID, usuarioId: 1,
      preferences: ["relatórios compactos", "alertas por prioridade"],
      facts: ["Gerente da loja Paulista", "Responsável por perecíveis"],
      lastAgent: null, lastSkill: null, updatedAt: now()
    }
  },
  { upsert: true }
);

print("=== Seed concluído ===");
print("  rag_documents novos: " + novosDocs);
print("  rag_chunks novos:    " + novosChunks);
print("  conversations: " + db.conversations.countDocuments());
print("  messages:      " + db.messages.countDocuments());
print("  metrics:       " + db.metrics.countDocuments());
print("  memories:      " + db.memories.countDocuments());
