# GenAI Job Finder Platform 🚀
### GenAI for Developers #25.11 – Monorepo

> A next-generation job search and recruitment platform powered by Generative AI (LLMs, RAG, Reranking).

This repository contains the end-to-end implementation for the **GenAI for Developers** course. It demonstrates a full-stack application where **Candidates** can use an AI Agent to find jobs based on their CV, and **Recruiters** can use AI to automatically rank applicants based on relevance using Vector Search and Reranking models.

---

## 📸 Screenshots

### 1. User Chat & Semantic Search
The AI Agent understands natural language, analyzes the user's CV context, and recommends jobs.
![User Chat Interface](./screenshots/chat_interface.png)
*(Place screenshot here: The chat interface showing "Find me jobs based on my CV")*

### 2. Admin Dashboard & AI Ranking
Recruiters can see a ranked list of applicants. The system uses **Voyage AI** to rerank candidates based on how well their profile matches the job description.
![Admin Dashboard](./screenshots/admin_dashboard.png)
*(Place screenshot here: The Admin panel showing the "AI Candidate Review" list with scores)*

---

## ✨ Key Features

### 👤 For Candidates (User Role)
* **Semantic Job Search:** Search for jobs using natural language (e.g., "Remote Java jobs for juniors") backed by Vector Search.
* **AI Career Agent:** A conversational assistant (powered by Llama 3.2) that can:
  * **Analyze CVs:** Automatically extracts text from uploaded PDFs.
  * **Context-Aware Search:** Uses the user's CV details to refine job search queries.
  * **Apply via Tools:** Can execute actions like `apply_to_job` or `get_my_applications` directly from the chat.

### 🛡️ For Recruiters (Admin Role)
* **Job Management:** Post and manage job listings.
* **AI Candidate Ranking:** Uses **Voyage AI Reranking** models to compare applicant CVs against the Job Description and rank them by a relevance score (0-100%).
* **Automated Screening:** Saves time by highlighting the most relevant candidates first.

---

## 🏗️ Architecture & Components

This project is split into three components: a PostgreSQL + Flyway database module, a Spring Boot backend, and a Next.js frontend.

### 1. Components Overview

- **`genai-db`**: Database module with PostgreSQL (via Docker Compose) and Flyway migrations that create and seed the schema.
- **`genai-be`**: Java Spring Boot backend exposing APIs, connecting to the PostgreSQL database, and integrating with LLM providers (Ollama & Voyage AI).
- **`genai-fe`**: Next.js (React) frontend providing a UI for interacting with GenAI features and the backend APIs.

---

## 🚀 How to Start

### Prerequisites
* **Docker Desktop**
* **Java 21+** & Maven
* **Node.js** (v18+) & npm
* **Ollama** (running locally with `llama3.2` model pulled)

### Step 1: Database (`genai-db`)
From the repo root:
```bash
cd genai-db
docker compose up