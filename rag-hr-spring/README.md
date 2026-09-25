# RAG HR Spring — HR Policy Assistant

A Spring Boot RAG (Retrieval-Augmented Generation) chatbot that answers employee questions using only the content of company HR policy documents.

> **Original source**: [rajkumarsingh0907/ragimplementation](https://github.com/rajkumarsingh0907/ragimplementation/)
> This project is a fork of that repo, modified to run with **zero cost** — replacing OpenAI (paid) with Spring AI Transformers (local ONNX embeddings) + Groq free cloud API for chat.

---

## Architecture Overview

### Original Architecture (OpenAI only)

```
PDF → OpenAI text-embedding-3-small (cloud) → PGVector (1536 dims)
                                                      ↑
User question → OpenAI text-embedding-3-small ────────┘
                → similarity search
                → context chunks
                → OpenAI gpt-4o-mini (cloud) → answer
```

**Dependency**: 100% on OpenAI API — both embeddings and chat completions require credits.

---

### Current Architecture (Hybrid: local embeddings + free cloud chat)

```
PDF → Spring AI Transformers / all-MiniLM-L6-v2 (local JVM) → PGVector (384 dims)
                                                                       ↑
User question → Spring AI Transformers / all-MiniLM-L6-v2 ────────────┘
                → similarity search
                → context chunks
                → Groq API / openai/gpt-oss-20b (free cloud) → answer
```

**Dependency**: Embeddings are free and local. Chat uses Groq's free-tier API (no credits needed).

---

## What Changed

### 1. `pom.xml` — Added local embedding dependency

```xml
<!-- ADDED -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-transformers</artifactId>
</dependency>
```

`spring-ai-transformers` provides `TransformersEmbeddingModel` — an ONNX-based embedding runner that executes models directly inside the JVM using Microsoft ONNX Runtime and DJL (Deep Java Library). No separate server required.

The original `spring-ai-starter-model-openai` was **kept** because Groq exposes an OpenAI-compatible REST API, so the same Spring AI OpenAI client works by just changing the base URL.

---

### 2. `application.properties` — Full config overhaul

| Property | Before | After | Reason |
|---|---|---|---|
| `spring.docker.compose.enabled` | *(not set, defaults true)* | `false` | Docker is in WSL, not on Windows PATH |
| `server.port` | *(not set, defaults 8080)* | `8282` | Port 8080 was occupied by another app |
| `spring.ai.openai.api-key` | OpenAI key (`sk-proj-...`) | Groq key (`gsk_...`) | Switched provider |
| `spring.ai.openai.base-url` | *(not set, defaults to OpenAI)* | `https://api.groq.com/openai` | Route chat to Groq |
| `spring.ai.openai.chat.options.model` | `gpt-4o-mini` | `openai/gpt-oss-20b` | Available model on Groq |
| `spring.ai.openai.embedding.enabled` | *(not set, defaults true)* | `false` | Prevent OpenAI embedding client from conflicting with Transformers |
| `spring.ai.embedding.transformer.onnx.modelUri` | *(not set)* | HuggingFace ONNX URL | Points to `all-MiniLM-L6-v2` ONNX model |
| `spring.ai.embedding.transformer.tokenizer.uri` | *(not set)* | HuggingFace tokenizer URL | Points to `all-MiniLM-L6-v2` tokenizer |
| `spring.ai.vectorstore.pgvector.dimensions` | `1536` | `384` | `all-MiniLM-L6-v2` outputs 384-dim vectors vs OpenAI's 1536 |

**Important note on base URL**: Groq's API path is `https://api.groq.com/openai/v1/...`. Spring AI appends `/v1/...` automatically, so `base-url` must be set to `https://api.groq.com/openai` (without the trailing `/v1`) to avoid a double `/v1/v1/` in the URL.

Full current `application.properties`:

```properties
spring.application.name=rag-hr-spring
spring.docker.compose.enabled=false
server.port=8282

# Groq via OpenAI-compatible API (chat only)
spring.ai.openai.api-key=YOUR_GROQ_API_KEY
spring.ai.openai.base-url=https://api.groq.com/openai
spring.ai.openai.chat.options.model=openai/gpt-oss-20b
spring.ai.openai.embedding.enabled=false

# Local embeddings - all-MiniLM-L6-v2 (384 dims, downloaded on first run)
spring.ai.embedding.transformer.onnx.modelUri=https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx
spring.ai.embedding.transformer.tokenizer.uri=https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/tokenizer.json

# PostgreSQL datasource
spring.datasource.url=jdbc:postgresql://localhost:5433/ragdb
spring.datasource.username=postgres
spring.datasource.password=postgres

# PGVector - 384 dims to match all-MiniLM-L6-v2
spring.ai.vectorstore.pgvector.initialize-schema=true
spring.ai.vectorstore.pgvector.dimensions=384
spring.ai.vectorstore.pgvector.distance-type=cosine_distance

# RAG configuration
app.rag.document-path=documents/company-leave-policy.pdf
app.rag.top-k=4
app.rag.similarity-threshold=0.10
```

---

### 3. `src/main/java/com/hr/assistance/config/EmbeddingConfig.java` — New file

**Why this file was needed**: Spring AI's `spring-ai-starter-model-openai` auto-configures an `OpenAiEmbeddingModel` bean. When both OpenAI and Transformers are on the classpath, Spring injects `OpenAiEmbeddingModel` into `PgVectorStore` by default (it was registered first). Setting `spring.ai.openai.embedding.enabled=false` alone is not sufficient in Spring AI 1.1.8 to prevent this.

The fix: explicitly declare `TransformersEmbeddingModel` as a `@Primary` bean so Spring always prefers it for `EmbeddingModel` injection points.

```java
@Configuration
public class EmbeddingConfig {

    @Value("${spring.ai.embedding.transformer.onnx.modelUri}")
    private String modelUri;

    @Value("${spring.ai.embedding.transformer.tokenizer.uri}")
    private String tokenizerUri;

    @Bean
    @Primary
    public TransformersEmbeddingModel embeddingModel() throws Exception {
        var model = new TransformersEmbeddingModel();
        model.setModelResource(new UrlResource(modelUri));
        model.setTokenizerResource(new UrlResource(tokenizerUri));
        return model;
    }
}
```

No changes were made to any service or controller Java files — Spring AI's abstraction layer (`EmbeddingModel`, `ChatModel`) means the business logic is provider-agnostic.

---

## Embedding Model Comparison

| | `text-embedding-3-small` (original) | `all-MiniLM-L6-v2` (current) |
|---|---|---|
| Provider | OpenAI (cloud, paid) | HuggingFace / ONNX (local, free) |
| Dimensions | 1536 | 384 |
| Context window | 8191 tokens | 256 tokens |
| Cost | ~$0.02 / 1M tokens | Free |
| Runs on | OpenAI servers | Your JVM (CPU) |
| First-run download | None | ~90 MB ONNX model |
| Quality (MTEB avg) | ~62 | ~56 |

---

## Running the App

### Prerequisites

- Java 17+ on Windows (or WSL)
- Docker installed in WSL
- A free Groq API key from [console.groq.com](https://console.groq.com)

### Step 1 — Start PostgreSQL container from WSL

```bash
docker compose -f compose.yml up -d
```

PostgreSQL runs on port `5433`. WSL 2 automatically forwards this port to Windows `localhost`.

### Step 2 — Set your Groq API key

Edit `src/main/resources/application.properties` and set your key:

```properties
spring.ai.openai.api-key=gsk_YOUR_KEY_HERE
```

### Step 3 — Run the app (Windows PowerShell)

```powershell
.\mvnw.cmd spring-boot:run
```

**First run only**: the app will download the ONNX model (~90 MB) and PyTorch native libraries (~300 MB) from HuggingFace and DJL. Subsequent startups are fast.

App starts on **port 8282**.

### Step 4 — Ingest the PDF (run once)

```bash
# From WSL
curl -X POST http://localhost:8282/api/documents/ingest
```

> Do not call this endpoint multiple times — there is no deduplication. Duplicate ingestion adds redundant vectors and degrades retrieval quality.

### Step 5 — Ask questions

```bash
curl -s -X POST http://localhost:8282/api/v1/rag/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "How many days of annual leave do I get?"}' \
  | python3 -m json.tool
```

---

## API Endpoints

| Method | Path | Body | Description |
|---|---|---|---|
| `POST` | `/api/documents/ingest` | none | Ingest the company leave policy PDF |
| `POST` | `/api/v1/rag/chat` | `{"question": "..."}` | Ask a question, get AI answer |

### Chat response format

```json
{
  "answer": "You are entitled to 24 earned annual leave days per calendar year.",
  "sources": [
    {
      "source": "company-leave-policy.pdf",
      "page": "1",
      "content": "..."
    }
  ]
}
```

---

## Infrastructure

`compose.yml` defines two services (unchanged from original):

| Service | Image | Port | Purpose |
|---|---|---|---|
| `spring-ai-rag-postgres` | `pgvector/pgvector:pg16` | `5433` | Vector store (PostgreSQL + pgvector extension) |
| `spring-ai-rag-pgadmin` | `dpage/pgadmin4` | `5050` | Database admin UI |

pgAdmin: `http://localhost:5050` — login `admin@admin.com` / `admin`

---

## Cost Comparison

| Component | Original | Current |
|---|---|---|
| Embeddings | Paid (OpenAI) | **Free** (local ONNX) |
| Chat | Paid (OpenAI) | **Free** (Groq free tier) |
| Infrastructure | Free (Docker local) | Free (Docker local) |
| **Total** | **Paid** | **$0** |
