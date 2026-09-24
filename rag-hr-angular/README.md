# HR Policy Assistant — Angular Frontend

A modern, mobile-responsive chat interface for the **RAG HR Spring** backend. Employees can ask natural-language questions about company HR policies and receive AI-generated answers backed by real document sources.

---

## Table of Contents

- [Overview](#overview)
- [Business Process](#business-process)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Integration](#api-integration)
- [Mobile Responsiveness](#mobile-responsiveness)

---

## Overview

This Angular frontend connects to a Spring Boot RAG (Retrieval-Augmented Generation) backend. It allows HR staff or employees to:

1. **Ask questions** about company HR policies in plain language
2. **Upload new PDF policy documents** to replace the knowledge base
3. **Ingest the default** bundled policy document
4. **View cited sources** — every answer shows which page and document it came from

---

## Business Process

### 1. Document Ingestion Flow

```
Employee/HR Admin
      │
      ▼
┌─────────────────┐     multipart/form-data      ┌──────────────────────────┐
│  Angular UI     │ ─── POST /api/documents/upload ──► Spring Boot Backend   │
│  (file upload   │                               │                          │
│   or ingest     │ ─── POST /api/documents/ingest ──► Reads classpath PDF   │
│   default PDF)  │                               │                          │
└─────────────────┘                               │  1. Clear vector store   │
                                                  │     (DELETE FROM         │
                                                  │      vector_store)       │
                                                  │  2. Save PDF to uploads/ │
                                                  │  3. Read & split into    │
                                                  │     chunks (tokens)      │
                                                  │  4. Embed each chunk     │
                                                  │     (all-MiniLM-L6-v2)   │
                                                  │  5. Store in PGVector    │
                                                  └──────────────────────────┘
```

> **Important:** Every new ingestion **replaces** all previous document chunks. Only one document is active in the knowledge base at a time.

---

### 2. Question & Answer Flow

```
Employee types question
         │
         ▼
┌─────────────────┐    POST /api/v1/rag/chat     ┌──────────────────────────┐
│  Angular UI     │ ─── { question: "..." } ─────► Spring Boot Backend      │
│                 │                               │                          │
│  Displays:      │                               │  1. Lowercase query      │
│  • AI answer    │                               │  2. Embed query text     │
│  • Source cards │                               │     (all-MiniLM-L6-v2)   │
│    (filename,   │                               │  3. Cosine similarity    │
│     page, text) │                               │     search in PGVector   │
│  • Loading dots │                               │     (top-4, threshold    │
│    while waiting│                               │      0.10)               │
└─────────────────┘                               │  4. Build context from   │
         ▲                                        │     matched chunks       │
         │   { answer, sources[] }                │  5. Call Groq LLM with   │
         └────────────────────────────────────────│     context + question   │
                                                  │  6. Return structured    │
                                                  │     JSON response        │
                                                  └──────────────────────────┘
```

---

## Architecture

### Frontend Architecture

```
src/app/
│
├── models/                         # Pure TypeScript interfaces (no logic)
│   ├── chat-message.model.ts       # UI message: id, role, text, sources, loading
│   ├── chat-response.model.ts      # API response: { answer, sources[] }
│   └── source-response.model.ts   # Source citation: { source, page, content }
│
├── services/
│   └── chat.service.ts             # HTTP client — 3 methods:
│                                   #   sendMessage(question) → ChatApiResponse
│                                   #   ingestDefault()       → string
│                                   #   uploadDocument(file)  → string
│
└── components/
    ├── app.component               # Root layout — sidebar + main area
    │   ├── sidebarOpen signal      # Controls mobile drawer open/close
    │   └── darkMode signal         # Toggles dark class on <html>
    │
    ├── chat-window/                # Main chat area
    │   ├── messages signal         # List of ChatMessage (with welcome message)
    │   ├── isLoading signal        # Shows spinner in send button
    │   ├── Auto-scroll             # afterNextRender → scrollToBottom()
    │   └── Enter to send           # Shift+Enter = new line
    │
    ├── message-bubble/             # Single message (user or bot)
    │   ├── User bubble             # Right-aligned, indigo background
    │   ├── Bot bubble              # Left-aligned, white/dark background
    │   ├── Loading dots            # Animated bounce while waiting
    │   └── Sources accordion       # Collapsible source citation cards
    │
    └── knowledge-sidebar/          # Document management panel
        ├── Ingest Default PDF      # Calls POST /api/documents/ingest
        ├── Drag-and-drop zone      # Handles dragover/drop events
        └── File browse             # Hidden <input type="file"> triggered by label
```

### State Management

All reactive state uses **Angular 18 signals** — no RxJS `BehaviorSubject`, no NgRx store:

| Signal | Type | Location | Purpose |
|---|---|---|---|
| `messages` | `signal<ChatMessage[]>` | `ChatWindowComponent` | Full chat history |
| `isLoading` | `signal<boolean>` | `ChatWindowComponent` | Disables input while waiting |
| `ingestStatus` | `signal<IngestStatus>` | `KnowledgeSidebarComponent` | idle / loading / success / error |
| `uploadStatus` | `signal<IngestStatus>` | `KnowledgeSidebarComponent` | idle / loading / success / error |
| `sidebarOpen` | `signal<boolean>` | `AppComponent` | Mobile drawer visibility |
| `darkMode` | `signal<boolean>` | `AppComponent` | Theme toggle |

### Dev Proxy

In development (`ng serve`), all `/api/**` requests are proxied to the backend — no CORS configuration needed:

```
Browser (localhost:4200)
    │
    │  /api/v1/rag/chat  (same origin — no CORS)
    ▼
Angular Dev Server (proxy)
    │
    │  forwards to http://localhost:8282/api/v1/rag/chat
    ▼
Spring Boot Backend (localhost:8282)
```

Proxy config: [`proxy.conf.json`](proxy.conf.json)

---

## Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| Framework | Angular 18.2 (standalone) | Component architecture, signals |
| Styling | Tailwind CSS 3 | Utility-first responsive design |
| HTTP | Angular `HttpClient` | REST calls to Spring Boot backend |
| Icons | emoji (📄 📚 ✅ ❌) | No extra icon library dependency |
| Build | Angular CLI + webpack | Development and production builds |
| Proxy | `proxy.conf.json` | CORS bypass in development |

---

## Project Structure

```
rag-hr-angular/
├── src/
│   ├── app/
│   │   ├── models/
│   │   │   ├── chat-message.model.ts
│   │   │   ├── chat-response.model.ts
│   │   │   └── source-response.model.ts
│   │   ├── services/
│   │   │   └── chat.service.ts
│   │   ├── components/
│   │   │   ├── chat-window/
│   │   │   │   ├── chat-window.component.ts
│   │   │   │   └── chat-window.component.html
│   │   │   ├── message-bubble/
│   │   │   │   ├── message-bubble.component.ts
│   │   │   │   └── message-bubble.component.html
│   │   │   └── knowledge-sidebar/
│   │   │       ├── knowledge-sidebar.component.ts
│   │   │       └── knowledge-sidebar.component.html
│   │   ├── app.component.ts
│   │   ├── app.component.html
│   │   └── app.config.ts           # provideHttpClient, provideAnimations
│   ├── environments/
│   │   ├── environment.ts          # dev: apiUrl = '' (proxy)
│   │   └── environment.prod.ts     # prod: apiUrl = 'http://localhost:8282'
│   ├── styles.scss                 # Tailwind directives + custom scrollbar
│   └── index.html
├── proxy.conf.json                 # /api → http://localhost:8282
├── tailwind.config.js
└── angular.json
```

---

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Angular CLI 18: `npm install -g @angular/cli`
- Spring Boot backend running on port 8282 (see `../README.md`)

### Install & Run

```bash
# From the rag-hr-angular directory
npm install

# Start development server with proxy
ng serve

# Open in browser
# http://localhost:4200
```

### Production Build

```bash
ng build --configuration production
# Output: dist/rag-hr-angular/
```

---

## Configuration

### Dev Backend URL

Edit [`proxy.conf.json`](proxy.conf.json):

```json
{
  "/api": {
    "target": "http://localhost:8282",
    "secure": false,
    "changeOrigin": true
  }
}
```

### Production Backend URL

Edit [`src/environments/environment.prod.ts`](src/environments/environment.prod.ts):

```typescript
export const environment = {
  production: true,
  apiUrl: 'http://localhost:8282'
};
```

---

## API Integration

| Method | Endpoint | Payload | Response | Used by |
|---|---|---|---|---|
| `POST` | `/api/v1/rag/chat` | `{ question: string }` | `{ answer, sources[] }` | `ChatWindowComponent` |
| `POST` | `/api/documents/ingest` | _(none)_ | `string` | `KnowledgeSidebarComponent` |
| `POST` | `/api/documents/upload` | `multipart/form-data` (field: `file`) | `string` | `KnowledgeSidebarComponent` |

All three are called through `ChatService` (`src/app/services/chat.service.ts`).

---

## Mobile Responsiveness

The layout adapts across screen sizes using Tailwind CSS breakpoints:

| Screen | Layout |
|---|---|
| **Mobile** (`< md`) | Full-width chat, sidebar hidden behind `📚` button in header |
| **Desktop** (`≥ md`) | Fixed 288px sidebar on left + chat fills remaining width |

On mobile, tapping `📚` opens the sidebar as a full-height slide-in overlay with a dark backdrop. Tapping outside or the `✕` button closes it.

The chat input bar is always sticky at the bottom of the screen regardless of screen size or message count.

---

## User Manual

### Screen Layout (Desktop)

```
┌──────────────────────────────────────────────────────────────────┐
│  📚  HR Policy Assistant                              🌙         │
│ (1)                                                  (2)         │
├──────────────────┬───────────────────────────────────────────────┤
│                  │                                               │
│  Knowledge Base  │   🤖 Hello! I'm your HR Policy Assistant.    │
│ ─────────────── (3)    Ask me anything about company policies.   │
│  Default Document│                                               │
│  📄 company-     │          How many leave days do I get?  👤   │
│    leave-        │                                               │
│    policy.pdf    │   🤖 You are entitled to 24 annual leave     │
│                  │      days per calendar year.                  │
│ [▶ Ingest PDF](4)│      ▶ Sources (1)                 (7)       │
│                  │                                               │
│ ─────────────────│                                               │
│  Upload Document │                                               │
│                  │                                               │
│  ┌────────────┐  │                                               │
│  │  Drop PDF  │  │                                               │
│  │  here or   │(5)                                               │
│  │ [📎 Browse]│  │                                               │
│  └────────────┘  │  ┌───────────────────────────────────────┐   │
│                  │  │ Ask about HR policies...      [→] (6) │   │
│  Status: ✅ Done │  └───────────────────────────────────────┘   │
└──────────────────┴───────────────────────────────────────────────┘
```

| # | Element | Description |
|---|---|---|
| 1 | `📚` button | Opens the Knowledge Base sidebar (on all screen sizes) |
| 2 | `🌙 / ☀️` button | Toggles dark / light mode |
| 3 | Sidebar | Document management panel |
| 4 | Ingest Default PDF | Loads the built-in policy document into the knowledge base |
| 5 | Upload zone | Drag-and-drop or browse to upload a new PDF |
| 6 | Chat input + Send | Type a question and press Enter or click → |
| 7 | Sources accordion | Click `▶ Sources` to expand cited document passages |

---

### Step-by-Step Guide

#### Step 1 — Load a Document into the Knowledge Base

The chatbot can only answer questions from documents that have been ingested. Do this first before asking any questions.

**Option A — Use the built-in default document:**

1. Click `📚` to open the sidebar (if not already open)
2. Click **▶ Ingest Default PDF**
3. Wait for the spinner to finish — you will see `✅ Successfully ingested N chunks from company-leave-policy.pdf`

**Option B — Upload your own PDF:**

1. Click `📚` to open the sidebar
2. In the **Upload New Document** section, either:
   - **Drag and drop** a PDF file onto the dashed box, or
   - Click **📎 Browse** and select a PDF from your computer
3. The upload starts automatically — wait for `✅ Successfully ingested N chunks from <filename>`

> **Note:** Every upload **replaces** the previous document. Only one document is active in the knowledge base at a time. Maximum file size: **20 MB**, PDF format only.

---

#### Step 2 — Ask a Question

1. Click the text box at the bottom of the chat area
2. Type your question in plain language, for example:
   - `How many annual leave days do I get?`
   - `What is the maternity leave policy?`
   - `Can I carry over unused leave to next year?`
3. Press **Enter** to send (or click the `→` button)
   - Use **Shift + Enter** to add a new line without sending
4. Wait for the bot to respond — animated dots `● ● ●` appear while it is thinking

---

#### Step 3 — View Source Citations

Every bot answer is backed by passages from the ingested document.

1. Look for the `▶ Sources (N)` link below the bot's answer
2. Click it to expand the source panel
3. Each source card shows:
   - `📄 filename.pdf` — the document name
   - `p.X` — the page number the passage came from
   - A short excerpt of the relevant text

Click `▶ Sources` again to collapse the panel.

---

#### Step 4 — Toggle Dark / Light Mode

Click the `🌙` icon in the top-right corner of the header to switch to dark mode.  
Click `☀️` to switch back to light mode.

---

### Mobile Usage

On a phone or narrow screen:

1. The sidebar is **hidden by default** — tap `📚` in the header to open it
2. A dark overlay covers the chat area while the sidebar is open — tap the overlay or `✕` to close the sidebar
3. The chat input bar stays **pinned to the bottom** of the screen at all times
4. Messages are **full width** for easier reading on small screens

---

### Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| Bot says "I could not find this information" | No document ingested yet, or question not covered | Ingest a document first (Step 1), then ask again |
| Upload shows ❌ error | Backend not running, or file is not a valid PDF | Start the Spring Boot backend; ensure the file is a PDF under 20 MB |
| Ingest shows ❌ error | Backend not reachable | Check that Spring Boot is running on port 8282 |
| Chat shows ❌ error | Groq API key missing or expired | Set a valid key in `application.properties` and restart the backend |
| Page loads but nothing happens | Angular dev server not proxying | Confirm `ng serve` is running and `proxy.conf.json` points to the correct backend URL |
