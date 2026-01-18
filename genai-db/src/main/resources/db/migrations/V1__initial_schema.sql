-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- Accounts anchor every other record
CREATE TABLE IF NOT EXISTS account (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    plan_tier TEXT NOT NULL DEFAULT 'free',
    status TEXT NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Users exist globally; memberships bind them to accounts
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS account_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'member',
    status TEXT NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (account_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_account_user_account ON account_user(account_id);
CREATE INDEX IF NOT EXISTS idx_account_user_user ON account_user(user_id);

-- Agents configured per account (mirrors FE agent settings UI)
CREATE TABLE IF NOT EXISTS agent (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    llm_model TEXT NOT NULL,
    embeddings_model TEXT NOT NULL,
    reranking_model TEXT,
    max_tokens INTEGER NOT NULL DEFAULT 1024,
    temperature NUMERIC(3,2) NOT NULL DEFAULT 0.20,
    behavior TEXT,
    created_by UUID REFERENCES app_user(id),
    updated_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_account ON agent(account_id);

-- Chat threads per account (optional agent association)
CREATE TABLE IF NOT EXISTS chat_thread (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    agent_id UUID REFERENCES agent(id) ON DELETE SET NULL,
    title TEXT,
    status TEXT NOT NULL DEFAULT 'active',
    created_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_thread_account ON chat_thread(account_id);

-- Messages inside each chat thread
CREATE TABLE IF NOT EXISTS chat_message (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    thread_id UUID NOT NULL REFERENCES chat_thread(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    agent_id UUID REFERENCES agent(id) ON DELETE SET NULL,
    user_id UUID REFERENCES app_user(id),
    sender_type TEXT NOT NULL CHECK (sender_type IN ('user', 'agent', 'system')),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_message_thread ON chat_message(thread_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_account ON chat_message(account_id);

-- Documents are curated per account (aligns with FE document list)
CREATE TABLE IF NOT EXISTS document (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    summary TEXT,
    body TEXT NOT NULL,
    created_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status TEXT NOT NULL DEFAULT 'active'
);

CREATE INDEX IF NOT EXISTS idx_document_account ON document(account_id);

-- Documents can be broken into sections, each with its own embedding
CREATE TABLE IF NOT EXISTS document_section (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    section_index INTEGER NOT NULL,
    heading TEXT,
    content TEXT NOT NULL,
    embedding VECTOR,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, section_index)
);

CREATE INDEX IF NOT EXISTS idx_document_section_document ON document_section(document_id);

