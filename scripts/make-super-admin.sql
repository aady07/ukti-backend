-- Optional: inspect the DB super admin created on backend startup.
-- Credentials come from application.properties:
--   ukti.super-admin.email
--   ukti.super-admin.password
-- Login UI: /admin/super/login (no Cognito)

SELECT id, email, user_type, left(password_hash, 20) AS hash_prefix
FROM users
WHERE user_type = 'super_admin';
