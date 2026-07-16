-- =============================================================================
-- Masroor school: add "4th Blue" and "4th Green", clone students from
-- "1st Blue" / "1st Green" (display_name + roll_number + school only; no
-- progress/activity tables touched). Assign both classes to Nuzhat Masoodi.
--
-- Admin: 06935ed4-a36e-49ec-ac6f-e6a4ed91a390
-- Teacher: nuzhatmasooudi325@gmail.com
--
-- Re-run safe: clears students in 4th Blue / 4th Green only, then refills from
-- current 1st Blue / 1st Green rosters; teacher links use ON CONFLICT DO NOTHING.
--
-- Run: psql ... -v ON_ERROR_STOP=1 -f scripts/add_masroor_4th_blue_green_classes.sql
-- =============================================================================

\set ON_ERROR_STOP on

\set admin_user_id '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'

BEGIN;

-- -----------------------------------------------------------------------------
-- 1) Ensure 4th Blue / 4th Green exist
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = :'admin_user_id'::uuid AND user_type = 'school_admin'
)
INSERT INTO classes (id, school_id, name)
SELECT uuid_generate_v4(), sch.school_id, v.class_name
FROM sch
CROSS JOIN (VALUES
  ('4th Blue'),
  ('4th Green')
) AS v(class_name)
WHERE NOT EXISTS (
  SELECT 1 FROM classes c
  WHERE c.school_id = sch.school_id AND c.name = v.class_name
);

-- -----------------------------------------------------------------------------
-- 2) Drop existing students in 4th Blue / 4th Green (idempotent refill)
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = :'admin_user_id'::uuid AND user_type = 'school_admin'
),
target AS (
  SELECT c.id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name IN ('4th Blue', '4th Green')
)
DELETE FROM users u
WHERE u.user_type = 'student'
  AND u.school_uuid = (SELECT school_id FROM sch)
  AND u.class_id IN (SELECT id FROM target);

-- -----------------------------------------------------------------------------
-- 3) Clone 1st Blue → 4th Blue
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = :'admin_user_id'::uuid AND user_type = 'school_admin'
),
src AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '1st Blue'
),
dst AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '4th Blue'
)
INSERT INTO users (
  id, cognito_sub, email, phone, username, display_name,
  school_id, school_uuid, user_type, class_id, roll_number,
  password_hash, created_at, updated_at
)
SELECT
  uuid_generate_v4(),
  NULL,
  NULL,
  NULL,
  NULL,
  trim(u.display_name),
  NULL,
  u.school_uuid,
  'student',
  (SELECT class_id FROM dst),
  u.roll_number,
  u.password_hash,
  NOW(),
  NOW()
FROM users u
WHERE u.user_type = 'student'
  AND u.class_id = (SELECT class_id FROM src);

-- -----------------------------------------------------------------------------
-- 4) Clone 1st Green → 4th Green
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = :'admin_user_id'::uuid AND user_type = 'school_admin'
),
src AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '1st Green'
),
dst AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '4th Green'
)
INSERT INTO users (
  id, cognito_sub, email, phone, username, display_name,
  school_id, school_uuid, user_type, class_id, roll_number,
  password_hash, created_at, updated_at
)
SELECT
  uuid_generate_v4(),
  NULL,
  NULL,
  NULL,
  NULL,
  trim(u.display_name),
  NULL,
  u.school_uuid,
  'student',
  (SELECT class_id FROM dst),
  u.roll_number,
  u.password_hash,
  NOW(),
  NOW()
FROM users u
WHERE u.user_type = 'student'
  AND u.class_id = (SELECT class_id FROM src);

-- -----------------------------------------------------------------------------
-- 5) Assign Nuzhat as main teacher on 4th Blue + 4th Green
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = :'admin_user_id'::uuid AND user_type = 'school_admin'
),
tid AS (
  SELECT t.id AS teacher_id
  FROM teachers t
  JOIN sch ON t.school_id = sch.school_id
  WHERE lower(trim(t.email)) = 'nuzhatmasooudi325@gmail.com'
),
cls AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name IN ('4th Blue', '4th Green')
)
INSERT INTO teacher_classes (teacher_id, class_id, is_main_teacher)
SELECT tid.teacher_id, cls.class_id, true
FROM tid
CROSS JOIN cls
ON CONFLICT (teacher_id, class_id) DO NOTHING;

COMMIT;
