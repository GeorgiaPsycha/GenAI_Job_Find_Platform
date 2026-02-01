-- 1. Προσθήκη χρήστη Zeta
INSERT INTO app_user (email, display_name)
SELECT 'zeta@gmail.com', 'Zeta'
WHERE NOT EXISTS (
    SELECT 1 FROM app_user WHERE email = 'zeta@gmail.com'
);

-- 2. Προσθήκη χρήστη Pinelopi
INSERT INTO app_user (email, display_name)
SELECT 'pinelopi@gmail.com', 'Pinelopi'
WHERE NOT EXISTS (
    SELECT 1 FROM app_user WHERE email = 'pinelopi@gmail.com'
);

-- 3. Σύνδεση των νέων χρηστών με το Account "GenAI for Developers"
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
  AND u.email IN ('zeta@gmail.com', 'pinelopi@gmail.com')
  AND NOT EXISTS (
    SELECT 1
    FROM account_user au
    WHERE au.account_id = a.id
      AND au.user_id = u.id
);