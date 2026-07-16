-- =============================================================================
-- UKG Green + 1st Blue students only (excludes 1st Green & UKG Blue student rows).
-- Admin: 06935ed4-a36e-49ec-ac6f-e6a4ed91a390
-- Teachers: nuzhatmasooudi325@gmail.com (Nuzhat Masoodi), bhatreyaz1058@gmail.com (RIYAZ AHMAD)
-- Does NOT change teacher password_hash — teachers set/reset password via app or manual UPDATE.
-- Run: psql ... -v ON_ERROR_STOP=1 -f scripts/add_ukg_green_1st_blue_students.sql
-- =============================================================================

\set ON_ERROR_STOP on

\set admin_user_id '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'

-- -----------------------------------------------------------------------------
-- 0) List teachers (passwords are BCrypt — cannot be "decrypted", only verified or reset)
-- -----------------------------------------------------------------------------
-- Run alone if needed:
-- SELECT id, email, name,
--        length(password_hash) AS hash_chars,
--        left(password_hash, 7) AS bcrypt_prefix,
--        password_hash
-- FROM teachers t
-- JOIN users u ON u.school_uuid = t.school_id
-- WHERE u.id = :'admin_user_id'::uuid AND u.user_type = 'school_admin';

BEGIN;

-- -----------------------------------------------------------------------------
-- 1) Update teacher display names only (no password)
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = :'admin_user_id'::uuid AND user_type = 'school_admin'
)
UPDATE teachers t
SET name = v.name,
    email = lower(trim(v.email)),
    updated_at = NOW()
FROM sch
JOIN (VALUES
  ('nuzhatmasooudi325@gmail.com', 'Nuzhat Masoodi'),
  ('bhatreyaz1058@gmail.com', 'RIYAZ AHMAD')
) AS v(email, name) ON lower(trim(t.email)) = lower(trim(v.email))
WHERE t.school_id = sch.school_id;

-- -----------------------------------------------------------------------------
-- 2) Ensure four classes exist (so teacher–class links match your scenario)
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
  ('UKG Green'),
  ('1st Green'),
  ('UKG Blue'),
  ('1st Blue')
) AS v(class_name)
WHERE NOT EXISTS (
  SELECT 1 FROM classes c
  WHERE c.school_id = sch.school_id AND c.name = v.class_name
);

-- -----------------------------------------------------------------------------
-- 3) Reassign teacher_classes: Nuzhat → UKG Green + 1st Green; Reyaz → UKG Blue + 1st Blue
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = :'admin_user_id'::uuid AND user_type = 'school_admin'
),
tid AS (
  SELECT t.id AS teacher_id, lower(t.email) AS em
  FROM teachers t, sch WHERE t.school_id = sch.school_id
),
cls AS (
  SELECT c.id AS class_id, c.name
  FROM classes c, sch WHERE c.school_id = sch.school_id
),
nuzhat AS (SELECT teacher_id FROM tid WHERE em = 'nuzhatmasooudi325@gmail.com'),
reyaz AS (SELECT teacher_id FROM tid WHERE em = 'bhatreyaz1058@gmail.com')
DELETE FROM teacher_classes tc
WHERE tc.teacher_id IN (SELECT teacher_id FROM nuzhat UNION SELECT teacher_id FROM reyaz);

WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = :'admin_user_id'::uuid AND user_type = 'school_admin'
),
tid AS (
  SELECT t.id AS teacher_id, lower(t.email) AS em
  FROM teachers t, sch WHERE t.school_id = sch.school_id
),
cls AS (
  SELECT c.id AS class_id, c.name
  FROM classes c, sch WHERE c.school_id = sch.school_id
),
nuzhat AS (SELECT teacher_id FROM tid WHERE em = 'nuzhatmasooudi325@gmail.com'),
reyaz AS (SELECT teacher_id FROM tid WHERE em = 'bhatreyaz1058@gmail.com')
INSERT INTO teacher_classes (teacher_id, class_id, is_main_teacher)
SELECT n.teacher_id, c.class_id, true
FROM nuzhat n
JOIN cls c ON c.name IN ('UKG Green', '1st Green')
UNION ALL
SELECT r.teacher_id, c.class_id, true
FROM reyaz r
JOIN cls c ON c.name IN ('UKG Blue', '1st Blue');

-- -----------------------------------------------------------------------------
-- 4) Remove bad student row if still present (wrong name = email)
-- -----------------------------------------------------------------------------
DELETE FROM users
WHERE id = '1fe70e31-8ebe-4bfd-9e63-d296c3d41841';

-- -----------------------------------------------------------------------------
-- 5) Delete existing students in UKG Green & 1st Blue only (fresh reload for these classes)
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = :'admin_user_id'::uuid AND user_type = 'school_admin'
),
target_classes AS (
  SELECT c.id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name IN ('UKG Green', '1st Blue')
)
DELETE FROM users u
WHERE u.user_type = 'student'
  AND u.school_uuid = (SELECT school_id FROM sch)
  AND u.class_id IN (SELECT id FROM target_classes);

-- -----------------------------------------------------------------------------
-- 6) Insert UKG Green students
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = :'admin_user_id'::uuid AND user_type = 'school_admin'
),
cg AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = 'UKG Green'
)
INSERT INTO users (id, cognito_sub, email, phone, username, display_name, school_id, school_uuid, user_type, class_id, roll_number, password_hash, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, NULL, NULL, NULL, trim(v.nm), NULL, sch.school_id, 'student', cg.class_id, v.roll, NULL, NOW(), NOW()
FROM sch
CROSS JOIN cg
CROSS JOIN (VALUES
  ('SHAH MOHD ROHAAN', '1'),
  ('SHARAFAT ALI MIR', '2'),
  ('AHIL IQBAL', '3'),
  ('SUFIYAN NISAR', '4'),
  ('MOHD NIHAAN BHAT', '5'),
  ('MOHD HAMAAD KHAN', '6'),
  ('LAIBA JAAN', '7'),
  ('ZIYAAN MAQBOOL', '8'),
  ('WASIQ WASEEM WAR', '9'),
  ('ARHAM KHAN', '10'),
  ('MANAN SHOWKAT', '11'),
  ('IBRA AZAD', '12'),
  ('MOHD SAAD MIR', '13'),
  ('MOHD SAFAAN', '14'),
  ('MIR LAREEB', '15'),
  ('AKSA ZAHOOR', '16'),
  ('TOIBA JAAN', '17'),
  ('SAAD ALTAF', '18'),
  ('MOHD YAMIN', '19'),
  ('MOHD YAHYA', '20'),
  ('YASIR MAJEED', '21'),
  ('MOHD SAAD', '22'),
  ('AHIL BILAL GANIE', '23'),
  ('UMI HABIBA', '24'),
  ('MOHD ZAIDAN', '25'),
  ('AHMED MAQBOOL', '26'),
  ('AISHA MUMTAZ', '27'),
  ('MOHD ABRAHIM', '28'),
  ('MOHD USMAAN KHAN', '29'),
  ('SAFAN SHAH', '30')
) AS v(nm, roll);

-- -----------------------------------------------------------------------------
-- 7) Insert 1st Blue students (roll 16 duplicated in source → second uses '16b')
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = :'admin_user_id'::uuid AND user_type = 'school_admin'
),
cb AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '1st Blue'
)
INSERT INTO users (id, cognito_sub, email, phone, username, display_name, school_id, school_uuid, user_type, class_id, roll_number, password_hash, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, NULL, NULL, NULL, trim(v.nm), NULL, sch.school_id, 'student', cb.class_id, v.roll, NULL, NOW(), NOW()
FROM sch
CROSS JOIN cb
CROSS JOIN (VALUES
  ('Batool fatima', '1'),
  ('Fouzan Irshad', '2'),
  ('Hadiya Altaf', '3'),
  ('Humna khan', '4'),
  ('Huzaif rasool', '5'),
  ('Immad Tahir', '6'),
  ('Laiba binte javid', '7'),
  ('Mahira khan', '8'),
  ('Mahraan ahmad khatari', '9'),
  ('Mir Midhat', '10'),
  ('Mohd Haider', '11'),
  ('Mohd Aatif', '12'),
  ('Mohd Hazik', '13'),
  ('Mohd musaib lone', '14'),
  ('Omiya hilal', '15'),
  ('Pakeeza wani', '16'),
  ('Rahat mashooq', '16b'),
  ('Rizwan Rafiq', '18'),
  ('Salim majaz', '19'),
  ('Shadam qayoom', '20'),
  ('Shafiya mohd', '21'),
  ('Shahzaib shabir dar', '22'),
  ('Sidratul Muntaha', '23'),
  ('Suzain shafi', '24'),
  ('Syed Hasan', '25'),
  ('Umi aymaan', '26'),
  ('Urooj arif', '27'),
  ('Zainab ishfaq', '28'),
  ('Taha Mushtaq', '29'),
  ('Areeba Irshad', '30'),
  ('Mohammad Haris', '31'),
  ('Mir Manan', '32'),
  ('Aayan sajad', '33'),
  ('Mohammad zain wani', '34'),
  ('Momin manzoor', '35')
) AS v(nm, roll);

COMMIT;
