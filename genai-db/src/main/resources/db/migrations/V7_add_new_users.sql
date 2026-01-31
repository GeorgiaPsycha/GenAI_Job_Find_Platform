
 INSERT INTO app_user (email, display_name, role)
 SELECT 'maria@example.com', 'Maria', 'USER'
 WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'maria@example.com');

 INSERT INTO app_user (email, display_name, role)
 SELECT 'jimmy@example.com', 'Jimmy', 'USER'
 WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'jimmy@example.com');

 INSERT INTO app_user (email, display_name, role)
 SELECT 'natalia@example.com', 'Natalia', 'USER'
 WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'natalia@example.com');

 INSERT INTO app_user (email, display_name, role)
 SELECT 'niki@example.com', 'Niki', 'USER'
 WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'niki@example.com');

 INSERT INTO app_user (email, display_name, role)
 SELECT 'ilias@example.com', 'Ilias', 'USER'
 WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'ilias@example.com');

 INSERT INTO app_user (email, display_name, role)
 SELECT 'peter@example.com', 'Peter', 'USER'
 WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'peter@example.com');

 INSERT INTO app_user (email, display_name, role)
 SELECT 'george@example.com', 'George', 'USER'
 WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'george@example.com');

 INSERT INTO app_user (email, display_name, role)
 SELECT 'stella@example.com', 'Stella', 'USER'
 WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'stella@example.com');

 INSERT INTO app_user (email, display_name, role)
 SELECT 'nick@example.com', 'Nick', 'USER'
 WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'nick@example.com');

 INSERT INTO app_user (email, display_name, role)
 SELECT 'panos@example.com', 'Panos', 'USER'
 WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'panos@example.com');