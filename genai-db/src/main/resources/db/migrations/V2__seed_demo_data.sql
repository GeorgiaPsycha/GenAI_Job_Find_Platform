-- =========================================================
-- Ensure base account and user exist
-- =========================================================

-- Account
INSERT INTO account (name, plan_tier, status)
SELECT 'GenAI for Developers', 'demo', 'active'
WHERE NOT EXISTS (
    SELECT 1 FROM account WHERE name = 'GenAI for Developers'
);

-- User
INSERT INTO app_user (email, display_name)
SELECT 'chris@mailinator.com', 'Chris'
WHERE NOT EXISTS (
    SELECT 1 FROM app_user WHERE email = 'chris@mailinator.com'
);

-- =========================================================
-- Seed account + user membership
-- =========================================================

INSERT INTO account_user (id, account_id, user_id, role, status)
SELECT
    uuid_generate_v4(),
    a.id,
    u.id,
    'owner',
    'active'
FROM account a
         CROSS JOIN app_user u
WHERE a.name = 'GenAI for Developers'
  AND u.email = 'chris@mailinator.com'
  AND NOT EXISTS (
    SELECT 1
    FROM account_user au
    WHERE au.account_id = a.id
      AND au.user_id   = u.id
);

-- =========================================================
-- Seed agents
-- =========================================================

INSERT INTO agent (
    id, account_id, name, llm_model, embeddings_model, reranking_model,
    max_tokens, temperature, behavior, created_by, updated_by
)
SELECT
    uuid_generate_v4(),
    a.id,
    v.name,
    v.llm,
    v.emb,
    v.rerank,
    v.max_tokens,
    v.temperature,
    v.behavior,
    u.id,
    u.id
FROM account a
         CROSS JOIN app_user u
         CROSS JOIN (
    VALUES
        ('my-gpt-5.1-mini'::text,
         'gpt-5.1-mini'::text,
         'text-embedding-3-small'::text,
         'colbert-latest'::text,
         1024::integer,
         0.30::numeric(3,2),
         'Respond concisely with actionable next steps.'::text),
        ('support-pro',
         'gpt-4.1',
         'text-embedding-3-large',
         'bge-large',
         2048,
         0.20,
         'Sound professional and detail every diagnostic step.')
) AS v(name, llm, emb, rerank, max_tokens, temperature, behavior)
WHERE a.name = 'GenAI for Developers'
  AND u.email = 'chris@mailinator.com'
  AND NOT EXISTS (
    SELECT 1
    FROM agent ag
    WHERE ag.account_id = a.id
      AND ag.name       = v.name
);

-- =========================================================
-- Seed documents
-- =========================================================

INSERT INTO document (id, account_id, title, summary, body, created_by, status)
SELECT
    uuid_generate_v4(),
    a.id,
    v.title,
    v.summary,
    v.body,
    u.id,
    'active'
FROM account a
         CROSS JOIN app_user u
         CROSS JOIN (
    VALUES
        ('Senior Java Developer'::text,
         'Own enterprise-scale Spring microservices modernization.'::text,
         '- Lead large-scale modernization projects
- Mentor engineers and ensure reliability KPIs
- Coordinate with product on roadmap delivery'::text),
        ('Junior Backend Developer',
         'Build internal APIs and automation tooling.',
         '- Maintain Node.js services and pipelines
- Ship integrations with finance tooling
- Collaborate with DevOps for smoother releases'),
        ('Frontend Developer',
         'Deliver Next.js experiences for AI document flows.',
         '- Build accessible UI for document comparisons
- Implement live previews for GenAI output
- Partner with design on rapid experiments'),
        ('DevOps Engineer',
         'Scale hybrid-cloud infra with IaC and observability.',
         '- Own IaC modules for AWS/GCP rollout
- Improve telemetry and incident response
- Champion developer productivity workflows')
) AS v(title, summary, body)
WHERE a.name = 'GenAI for Developers'
  AND u.email = 'chris@mailinator.com'
  AND NOT EXISTS (
    SELECT 1
    FROM document d
    WHERE d.account_id = a.id
      AND d.title      = v.title
);

-- =========================================================
-- Document sections
-- =========================================================

INSERT INTO document_section (id, document_id, section_index, heading, content, embedding)
SELECT
    uuid_generate_v4(),
    d.id,
    v.section_index,
    v.heading,
    v.content,
    NULL
FROM account a
         JOIN document d
              ON d.account_id = a.id
         JOIN (
    VALUES
        ('Senior Java Developer'::text, 1::integer, 'Role overview'::text,
         'Lead backend modernization with Spring Boot.'::text),
        ('Junior Backend Developer', 1, 'Responsibilities',
         'Assist with API development and automation.'),
        ('Frontend Developer', 1, 'Role overview',
         'Craft responsive Next.js UI for GenAI flows.'),
        ('DevOps Engineer', 1, 'Responsibilities',
         'Manage IaC, telemetry, and releases.')
) AS v(title, section_index, heading, content)
              ON v.title = d.title
WHERE a.name = 'GenAI for Developers'
  AND NOT EXISTS (
    SELECT 1
    FROM document_section ds
    WHERE ds.document_id   = d.id
      AND ds.section_index = v.section_index
);

-- =========================================================
-- Threads
-- =========================================================

-- Threads
INSERT INTO chat_thread (id, account_id, agent_id, title, status, created_by)
SELECT
    uuid_generate_v4(),
    a.id,
    ag.id,
    v.title,
    'active',
    u.id
FROM account a
         JOIN app_user u
              ON u.email = 'chris@mailinator.com'
         CROSS JOIN (
    VALUES
        ('Senior Java discovery'::text, 'my-gpt-5.1-mini'::text),
        ('DevOps hiring Q&A',           'support-pro')
) AS v(title, agent_name)
         JOIN agent ag
              ON ag.account_id = a.id
                  AND ag.name       = v.agent_name
WHERE a.name = 'GenAI for Developers'
  AND NOT EXISTS (
    SELECT 1
    FROM chat_thread ct
    WHERE ct.account_id = a.id
      AND ct.title      = v.title
);


-- =========================================================
-- Messages
-- =========================================================

INSERT INTO chat_message (id, thread_id, account_id, agent_id, user_id, sender_type, content)
SELECT
    uuid_generate_v4(),
    t.id,
    a.id,
    t.agent_id,
    CASE WHEN v.is_user THEN u.id ELSE NULL END,
    v.sender_type,
    v.content
FROM account a
         JOIN app_user u
              ON u.email = 'chris@mailinator.com'
         JOIN chat_thread t
              ON t.account_id = a.id
         JOIN (
    VALUES
        ('Senior Java discovery'::text, 'user'::text,
         'Can you summarize the senior Java posting for stakeholder updates?'::text,
         TRUE),
        ('Senior Java discovery', 'agent',
         'The role focuses on modernizing Spring-based services and mentoring engineers.',
         FALSE),
        ('DevOps hiring Q&A', 'user',
         'What are the key outcomes for the DevOps engineer?',
         TRUE),
        ('DevOps hiring Q&A', 'agent',
         'They will own IaC modules, telemetry improvements, and productivity tooling.',
         FALSE)
) AS v(thread_title, sender_type, content, is_user)
              ON v.thread_title = t.title
WHERE a.name = 'GenAI for Developers'
  AND NOT EXISTS (
    SELECT 1
    FROM chat_message cm
    WHERE cm.thread_id   = t.id
      AND cm.sender_type = v.sender_type
      AND cm.content     = v.content
);
