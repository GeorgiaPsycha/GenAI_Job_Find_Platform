## GenAI for Developers - Frontend App

Next.js (Pages Router) + React UI that renders a split-screen experience for working with AI-assisted job documents. Styling is handled with CSS Modules, fonts via `next/font`, and everything runs on the client—no server-side routes or APIs are used in this module.

### Install & Run

```bash
cd genai-fe
npm install
npm run dev
```

Then open [http://localhost:3000](http://localhost:3000).

### Product Overview

The interface is intentionally simple so it can grow alongside the backend and Flyway modules. Today it ships with two primary surfaces:

- **Documents panel** – a fixed-width sidebar listing sample job postings (`Senior Java Developer`, `Junior Backend Developer`, `Frontend Developer`, `DevOps Engineer`). The “Semantic search” textarea debounces user input, shows a short loading overlay, and filters the list client-side to simulate semantic retrieval.

- **Chat panel** – a flexible canvas with a chat transcript, input box, and agent configuration. Messages track sender IDs and timestamps so “my” messages align to the right while agent/system responses align left. A modal lets you switch among mock agents, rename them, edit model/embedding/rerank/temperature/max-token settings, and provide behavior prompts. Saving updates the in-memory store and re-selects the agent, while pressing Escape closes the modal without persisting changes.
