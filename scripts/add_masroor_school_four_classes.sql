-- =============================================================================
-- Masroor admin: UKG Green, 1st Green, UKG Blue, 1st Blue — teachers + all students.
-- Admin: 06935ed4-a36e-49ec-ac6f-e6a4ed91a390
-- Teachers (existing rows updated, not duplicated):
--   nuzhatmasooudi325@gmail.com → Nuzhat Masoodi → UKG Green + 1st Green
--   bhatreyaz1058@gmail.com     → RIYAZ AHMAD    → UKG Blue + 1st Blue
-- Password for both: Admin@123 (BCrypt below)
--
-- Duplicate rolls in source Excel are stored as 25 / 25b, 18 / 18b, 22 / 22b / 22c, 26 / 26b / 26c (same class must have unique roll_number).
--
-- Run: psql ... -v ON_ERROR_STOP=1 -f scripts/add_masroor_school_four_classes.sql
-- =============================================================================

\set ON_ERROR_STOP on

\set admin_user_id '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'

BEGIN;

-- -----------------------------------------------------------------------------
-- 1) Update teacher names + password (same teacher ids / emails)
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = :'admin_user_id'::uuid AND user_type = 'school_admin'
)
UPDATE teachers t
SET name = v.name,
    email = lower(trim(v.email)),
    password_hash = '$2a$10$ORLsYPVCeaF2igvqDoagPej685fQPKzWoMvoCUl6btkAGe6PE0FMm',
    updated_at = NOW()
FROM sch
CROSS JOIN (VALUES
  ('nuzhatmasooudi325@gmail.com', 'Nuzhat Masoodi'),
  ('bhatreyaz1058@gmail.com', 'RIYAZ AHMAD')
) AS v(email, name)
WHERE t.school_id = sch.school_id
  AND lower(trim(t.email)) = lower(trim(v.email));

-- -----------------------------------------------------------------------------
-- 2) Ensure four classes exist
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
-- 3) Teacher ↔ class links
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
-- 4) Remove bad student (name was wrongly set to email)
-- -----------------------------------------------------------------------------
DELETE FROM users
WHERE id = '1fe70e31-8ebe-4bfd-9e63-d296c3d41841';

-- -----------------------------------------------------------------------------
-- 5) Remove all students in these four classes (fresh reload)
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
  WHERE c.name IN ('UKG Green', '1st Green', 'UKG Blue', '1st Blue')
)
DELETE FROM users u
WHERE u.user_type = 'student'
  AND u.school_uuid = (SELECT school_id FROM sch)
  AND u.class_id IN (SELECT id FROM target_classes);

-- -----------------------------------------------------------------------------
-- 6) UKG Green
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
-- 7) 1st Green (roll 25 duplicated in source → 25b for Nadima javeed)
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
  WHERE c.name = '1st Green'
)
INSERT INTO users (id, cognito_sub, email, phone, username, display_name, school_id, school_uuid, user_type, class_id, roll_number, password_hash, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, NULL, NULL, NULL, trim(v.nm), NULL, sch.school_id, 'student', cg.class_id, v.roll, NULL, NOW(), NOW()
FROM sch
CROSS JOIN cg
CROSS JOIN (VALUES
  ('Aaqib Hussain', '01'),
  ('Aayat jan', '02'),
  ('Abdul Hanan', '03'),
  ('Abu Anan', '04'),
  ('Abu bakar Ganie', '05'),
  ('Aadil Ahmad lone', '06'),
  ('Adnan irshad', '07'),
  ('Aleeza Qaiser', '08'),
  ('Aqsa jan', '09'),
  ('Areeba mushtaq', '10'),
  ('Arshafa iqbal', '11'),
  ('Aayat shabir', '12'),
  ('Basit Shabir', '13'),
  ('Hadim Bashir', '14'),
  ('Fazil sajad', '15'),
  ('Hamaad Qaiser', '16'),
  ('Haris Ahmad bhat', '17'),
  ('Imad Rayaz', '18'),
  ('Mariya jan', '19'),
  ('Mohsin zahoor', '20'),
  ('Mohd Musaib bhat', '21'),
  ('Mohd musavir', '22'),
  ('Mohd Rizwan shah', '23'),
  ('Munaza wali', '25'),
  ('Nadima javeed', '25b'),
  ('Sabiha Zakir', '26'),
  ('Zayeem Zahoor', '27'),
  ('Zayaan maqbool', '28'),
  ('Hurmat zahoor', '29'),
  ('Kifayat nissar', '30'),
  ('Zeenab mudasir', '31'),
  ('Mohammad Mehran khan', '32'),
  ('Mohammad ibrahim', '33'),
  ('Huraib javid', '34'),
  ('Zeeshan tahir', '35'),
  ('Mohd Asrar', '36')
) AS v(nm, roll);

-- -----------------------------------------------------------------------------
-- 8) 1st Blue
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
  ('Batool fatima', '01'),
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
  ('Rahat mashooq', '17'),
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

-- -----------------------------------------------------------------------------
-- 9) UKG Blue (duplicate rolls in source → suffixes b/c)
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
  WHERE c.name = 'UKG Blue'
)
INSERT INTO users (id, cognito_sub, email, phone, username, display_name, school_id, school_uuid, user_type, class_id, roll_number, password_hash, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, NULL, NULL, NULL, trim(v.nm), NULL, sch.school_id, 'student', cg.class_id, v.roll, NULL, NOW(), NOW()
FROM sch
CROSS JOIN cg
CROSS JOIN (VALUES
  ('Zayeemul Islam', '01'),
  ('Muntasha Yaseen', '2'),
  ('Talha Tariq', '3'),
  ('Aqsa Binte Shahid', '4'),
  ('Sufiyan Shabir', '5'),
  ('Mohd.Hamaad Wani', '6'),
  ('Simran Jan', '7'),
  ('Aatif Aslam', '8'),
  ('Saad Bin Majeed', '10'),
  ('Mir Zuha Fatima', '11'),
  ('Daniyal Bilal', '12'),
  ('Mohd Numaan', '13'),
  ('Owais Ahmad Khan', '15'),
  ('Syed Zuhaib Maqsood', '17'),
  ('Toiba Hussain', '18'),
  ('Barzish Tanveer', '18b'),
  ('Mehnoor Shabir', '20'),
  ('Zaidul Islam', '22'),
  ('Mohd.Zaid Wani', '22b'),
  ('Shaikh Azimullah Haq', '22c'),
  ('Zoya Mansoor', '23'),
  ('Mohd Ibrahim Lone', '24'),
  ('Rahil Reyaz', '25'),
  ('Aazim Rafiq', '26'),
  ('Mohd Arkam Khan', '26b'),
  ('Mahira Nazir', '26c'),
  ('Hamsha Mashooq', '28'),
  ('Mehrun. Nissa', '29'),
  ('Mir Ayesha', '30'),
  ('Musaib Ah Khan', '31'),
  ('Sana Sadam', '32'),
  ('Sana Sadam', '33'),
  ('Hurain Jan', '34'),
  ('Arwa Imtiyaz', '35'),
  ('Aleeza Quyoom', '36')
) AS v(nm, roll);

COMMIT;
