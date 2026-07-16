-- Display name for teacher sadarsh51000@gmail.com → Adarsh
-- Run: psql -d ukti_db -f scripts/rename-teacher-sadarsh-to-adarsh.sql

UPDATE teachers
SET name = 'Adarsh',
    updated_at = now()
WHERE lower(trim(email)) = lower(trim('sadarsh51000@gmail.com'));
