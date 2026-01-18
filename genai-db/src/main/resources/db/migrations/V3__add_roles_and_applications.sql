-- 1. Προσθήκη Role στον User (Admin vs Candidate)
ALTER TABLE app_user
    ADD COLUMN role TEXT NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN'));

-- 2. Μετατροπή του πίνακα 'document' σε πλήρες Job Post
-- Προσθέτουμε τα πεδία που λείπουν σύμφωνα με την εκφώνηση (Company, Location, Seniority)
ALTER TABLE document
    ADD COLUMN company   TEXT,
    ADD COLUMN location  TEXT,
    ADD COLUMN seniority TEXT,
    ADD COLUMN tags      TEXT;
-- Μπορούμε να το αποθηκεύσουμε ως comma-separated text ή JSON

-- Προαιρετικά: Ενημέρωση των υπαρχόντων demo data (από το V2) με dummy τιμές
UPDATE document
SET company   = 'Ctrl+Space Labs',
    location  = 'Remote',
    seniority = 'Senior'
WHERE title LIKE '%Senior%';
UPDATE document
SET company   = 'Ctrl+Space Labs',
    location  = 'Athens',
    seniority = 'Junior'
WHERE title LIKE '%Junior%';

-- 3. Δημιουργία πίνακα Application (Αιτήσεις)
-- Συνδέει τον User με το Document (το οποίο είναι το Job Post)
CREATE TABLE IF NOT EXISTS application
(
    id              UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    user_id         UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,

    -- ΕΔΩ Η ΑΛΛΑΓΗ: Συνδέουμε την αίτηση με το document_id
    job_id          UUID        NOT NULL REFERENCES document (id) ON DELETE CASCADE,

    -- Πεδία για την αίτηση
    cv_file_url     TEXT,
    cv_content_text TEXT, -- Το κείμενο του CV για χρήση από το GenAI
    motivation_text TEXT,

    status          TEXT        NOT NULL DEFAULT 'APPLIED' CHECK (status IN ('APPLIED', 'REVIEWED', 'REJECTED', 'HIRED')),

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Ένας χρήστης μπορεί να κάνει μόνο μία αίτηση ανά αγγελία (document)
    UNIQUE (user_id, job_id)
);

-- Indexes για γρήγορη αναζήτηση αιτήσεων
CREATE INDEX IF NOT EXISTS idx_application_user ON application (user_id);
CREATE INDEX IF NOT EXISTS idx_application_job ON application (job_id);