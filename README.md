# AI RAG Fullstack — HR Policy Assistant

A zero-cost, self-hosted AI chatbot for HR policy Q&A. Employees ask natural-language questions; the system retrieves relevant chunks from uploaded PDF policy documents and generates answers using a free LLM.

---

## Projects

| Directory | Role | Tech |
|---|---|---|
| [`rag-hr-spring/`](rag-hr-spring/) | REST API + RAG pipeline | Java 17, Spring Boot 3.5, Spring AI 1.1 |
| [`rag-hr-angular/`](rag-hr-angular/) | Chat UI | Angular 18, Tailwind CSS |

---

## Architecture

```
Browser (localhost:4200)
  │
  │  /api/**  (Angular dev proxy)
  ▼
Spring Boot Backend (localhost:8282)
  │
  ├── POST /api/documents/ingest or /api/documents/upload
  │     → PDF → chunk → embed (ONNX, local JVM, no API cost)
  │     → 384-dim vectors → PostgreSQL + pgvector (port 5433)
  │
  └── POST /api/v1/rag/chat
        → embed query (ONNX, local)
        → cosine similarity search (top-4) in pgvector
        → build context from matched chunks
        → Groq API free-tier LLM call
        → return { answer, sources[] }
```

**Key design choices:**
- Embeddings run **fully locally** via `all-MiniLM-L6-v2` ONNX model (384-dim) — no external embedding API
- LLM inference via **Groq free tier** (`openai/gpt-oss-20b`), OpenAI-compatible client
- Only one document is active at a time; ingestion clears and replaces all vectors

---

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| Docker | any recent | PostgreSQL + pgvector |
| Java | 17+ | Spring Boot backend |
| Node.js | 18+ | Angular frontend |
| Angular CLI | 18 | `ng serve` / `ng build` |

Install Angular CLI globally if needed:
```bash
npm install -g @angular/cli
```

---

## Running Locally

### 1. Start PostgreSQL

```bash
docker compose -f rag-hr-spring/compose.yml up -d
```

| Service | URL | Credentials |
|---|---|---|
| PostgreSQL 16 + pgvector | `localhost:5433` | `postgres` / `postgres` / db: `ragdb` |
| pgAdmin 4 | http://localhost:5050 | `admin@admin.com` / `admin` |

### 2. Start the backend

```powershell
cd rag-hr-spring
.\mvnw.cmd spring-boot:run
```

> **First run:** downloads ~90 MB ONNX model + ~300 MB DJL native libs from HuggingFace. Cached on subsequent runs. Backend listens on **port 8282**.

### 3. Ingest the default HR policy PDF

Run this once after the backend starts (or whenever you want to reset the knowledge base to the bundled document):

```bash
curl -X POST http://localhost:8282/api/documents/ingest
```

### 4. Start the frontend

```powershell
cd rag-hr-angular
npm install
ng serve
```

Open **http://localhost:4200**. All `/api/**` requests are proxied to `localhost:8282` automatically.

---

## API Reference

| Method | Endpoint | Body | Response |
|---|---|---|---|
| `POST` | `/api/documents/ingest` | — | `string` (status message) |
| `POST` | `/api/documents/upload` | `multipart/form-data`, field `file` (PDF, max 20 MB) | `string` (status message) |
| `POST` | `/api/v1/rag/chat` | `{ "question": "..." }` | `{ "answer": "...", "sources": [{ "source", "page", "content" }] }` |

A Postman collection is available at [`rag-hr-spring/rag-hr-spring.postman_collection.json`](rag-hr-spring/rag-hr-spring.postman_collection.json).

---

## Configuration

All backend configuration lives in [`rag-hr-spring/src/main/resources/application.properties`](rag-hr-spring/src/main/resources/application.properties).

Key settings:

| Property | Default | Notes |
|---|---|---|
| `server.port` | `8282` | Must match Angular proxy target |
| `spring.ai.openai.api-key` | *(Groq key)* | **Move to env var in production** |
| `spring.ai.openai.base-url` | `https://api.groq.com/openai` | Groq's OpenAI-compatible endpoint |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/ragdb` | |
| `app.rag.top-k` | `4` | Number of chunks retrieved per query |
| `app.rag.similarity-threshold` | `0.10` | Minimum cosine similarity to include a chunk |

> **Security note:** The Groq API key is currently committed in plaintext. Before sharing this repo publicly, move it to an environment variable or secret manager.

---

## Production Build

**Backend:** Package as a JAR and run with environment variables for secrets.
```bash
cd rag-hr-spring
.\mvnw.cmd package -DskipTests
java -jar target/assistance-*.jar
```

**Frontend:** Build static assets and serve via a web server or CDN.
```bash
cd rag-hr-angular
ng build --configuration production
# Output: dist/rag-hr-angular/
```

Update [`rag-hr-angular/src/environments/environment.prod.ts`](rag-hr-angular/src/environments/environment.prod.ts) to point `apiUrl` at your deployed backend.

---

## Testing

**Backend** (JUnit 5 + Mockito, JaCoCo enforces 80% line coverage):
```powershell
cd rag-hr-spring
.\mvnw.cmd test
```

**Frontend** (Karma + Jasmine):
```bash
cd rag-hr-angular
ng test
```
