## GenAI for Developers #25.11 – Monorepo

This repository contains a small, end‑to‑end GenAI playground used in the **GenAI for Developers** course.  
It is split into three components: a PostgreSQL + Flyway database module, a Spring Boot backend, and a Next.js frontend.  
Together they demonstrate a typical full‑stack GenAI setup you can run locally.

### 1. Components Overview

- **`genai-db`**: Database module with PostgreSQL (via Docker Compose) and Flyway migrations that create and seed the schema.
- **`genai-be`**: Java Spring Boot backend exposing APIs, connecting to the PostgreSQL database, and integrating with an LLM provider.
- **`genai-fe`**: Next.js (React) frontend providing a UI for interacting with GenAI features and the backend APIs.

---

### 2. How to Start Each Component

- **Database (`genai-db`) with Docker Compose**
  - From the repo root:

    ```bash
    cd genai-db
    docker compose up
    ```

  - This will start:
    - A PostgreSQL container named `genai-postgres` on host port **5433**.
    - A Flyway migration container that runs `mvn clean install flyway:migrate` to apply migrations.

- **Backend (`genai-be`) with Maven**
  - From the repo root:

    ```bash
    cd genai-be
    ./mvnw spring-boot:run
    ```

  - The backend runs on port **8080** (configured via `server.port=8080`).

- **Frontend (`genai-fe`) with npm**
  - From the repo root:

    ```bash
    cd genai-fe
    npm install
    npm run dev
    ```

  - The Next.js dev server runs on port **3000** by default.

---

### 3. What’s in Each Component

- **`genai-db`**
  - `docker-compose.yml` defines the PostgreSQL container and a Flyway migration job.
  - `src/main/resources/db/migrations` contains Flyway SQL migration files:
    - `V1__initial_schema.sql` – base schema and tables.
    - `V2__seed_demo_data.sql` – demo seed data for development.

- **`genai-be`**
  - Spring Boot application under `src/main/java/dev/ctrlspace/genai2506/genaibe`.
  - `controllers` expose REST endpoints (e.g. `UserController`).
  - `models/entities` define domain entities like `User`, `Account`, `ChatThread`, `Document`, etc.
  - `services` implement business logic and LLM integration (e.g. `CompletionsApiService`).
  - `src/main/resources/application.properties` configures the server port and DB connection.

- **`genai-fe`**
  - Next.js Pages Router app in `src/pages` (e.g. `index.js` for the main UI).
  - Styling via CSS Modules in `src/styles`.
  - Provides a split‑screen UX for documents and chat‑style GenAI interactions.

---

### 4. How to Access Each Component

- **Frontend UI**
  - Open your browser at: `http://localhost:3000`
  - Requires the frontend dev server running (`npm run dev` in `genai-fe`).

- **Backend API**
  - Base URL: `http://localhost:8080`
  - Example usage:
    - Call APIs from the frontend (configured to talk to port 8080).
    - Or hit endpoints directly from Postman / curl using `http://localhost:8080/...`.

- **Database**
  - Host: `localhost`
  - Port: `5433` (mapped to container’s 5432)
  - Database: `genai`
  - User: `genai`
  - Password: `genai`
  - You can connect using any PostgreSQL client (e.g. pgAdmin, TablePlus, psql) with these credentials.

---

### 5. Environment Notes

- The backend expects the database at `jdbc:postgresql://localhost:5433/genai` with user `genai` and password `genai` (override via `DATABASE_PASSWORD` env var if needed).  
- Some LLM integration keys (e.g. `GROQ_API_KEY`) can be set via environment variables; see `genai-be/src/main/resources/application.properties` for defaults.


