-- students from official roster (rolls 1..n per class by source SNO order; SNO 99 omitted).
--
--



BEGIN;

-- -----------------------------------------------------------------------------
-- 1) Ensure eight classes exist
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
)
INSERT INTO classes (id, school_id, name)
SELECT uuid_generate_v4(), sch.school_id, v.class_name
FROM sch
CROSS JOIN (VALUES
  ('2nd Green'),
  ('2nd Blue'),
  ('3rd Green'),
  ('3rd Blue'),
  ('4th Green'),
  ('4th Blue'),
  ('5th Green'),
  ('5th Blue')
) AS v(class_name)
WHERE NOT EXISTS (
  SELECT 1 FROM classes c
  WHERE c.school_id = sch.school_id AND c.name = v.class_name
);

-- -----------------------------------------------------------------------------
-- 2) Teacher ↔ class links (do not remove UKG/1st links)
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
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
JOIN cls c ON c.name IN ('2nd Green', '3rd Green', '4th Green', '5th Green')
UNION ALL
SELECT r.teacher_id, c.class_id, true
FROM reyaz r
JOIN cls c ON c.name IN ('2nd Blue', '3rd Blue', '4th Blue', '5th Blue')
ON CONFLICT (teacher_id, class_id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 3) Remove engagement + progress for students in these classes only
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
target_classes AS (
  SELECT c.id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name IN (
    '2nd Green', '2nd Blue', '3rd Green', '3rd Blue',
    '4th Green', '4th Blue', '5th Green', '5th Blue'
  )
),
target_students AS (
  SELECT u.id
  FROM users u
  WHERE u.user_type = 'student'
    AND u.school_uuid = (SELECT school_id FROM sch)
    AND u.class_id IN (SELECT id FROM target_classes)
),
target_sessions AS (
  SELECT s.id
  FROM activity_engagement_session s
  WHERE s.class_id IN (SELECT id FROM target_classes)
     OR s.user_id IN (SELECT id FROM target_students)
)
DELETE FROM activity_engagement_event e
WHERE e.session_id IN (SELECT id FROM target_sessions);

WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
target_classes AS (
  SELECT c.id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name IN (
    '2nd Green', '2nd Blue', '3rd Green', '3rd Blue',
    '4th Green', '4th Blue', '5th Green', '5th Blue'
  )
),
target_students AS (
  SELECT u.id
  FROM users u
  WHERE u.user_type = 'student'
    AND u.school_uuid = (SELECT school_id FROM sch)
    AND u.class_id IN (SELECT id FROM target_classes)
),
target_sessions AS (
  SELECT s.id
  FROM activity_engagement_session s
  WHERE s.class_id IN (SELECT id FROM target_classes)
     OR s.user_id IN (SELECT id FROM target_students)
)
DELETE FROM activity_engagement_processed_batch b
WHERE b.session_id IN (SELECT id FROM target_sessions);

WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
target_classes AS (
  SELECT c.id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name IN (
    '2nd Green', '2nd Blue', '3rd Green', '3rd Blue',
    '4th Green', '4th Blue', '5th Green', '5th Blue'
  )
),
target_students AS (
  SELECT u.id
  FROM users u
  WHERE u.user_type = 'student'
    AND u.school_uuid = (SELECT school_id FROM sch)
    AND u.class_id IN (SELECT id FROM target_classes)
),
target_sessions AS (
  SELECT s.id
  FROM activity_engagement_session s
  WHERE s.class_id IN (SELECT id FROM target_classes)
     OR s.user_id IN (SELECT id FROM target_students)
)
DELETE FROM activity_engagement_idempotency i
WHERE i.session_id IN (SELECT id FROM target_sessions);

WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
target_classes AS (
  SELECT c.id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name IN (
    '2nd Green', '2nd Blue', '3rd Green', '3rd Blue',
    '4th Green', '4th Blue', '5th Green', '5th Blue'
  )
),
target_students AS (
  SELECT u.id
  FROM users u
  WHERE u.user_type = 'student'
    AND u.school_uuid = (SELECT school_id FROM sch)
    AND u.class_id IN (SELECT id FROM target_classes)
)
DELETE FROM activity_engagement_session s
WHERE s.class_id IN (SELECT id FROM target_classes)
   OR s.user_id IN (SELECT id FROM target_students);

WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
target_classes AS (
  SELECT c.id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name IN (
    '2nd Green', '2nd Blue', '3rd Green', '3rd Blue',
    '4th Green', '4th Blue', '5th Green', '5th Blue'
  )
),
target_students AS (
  SELECT u.id
  FROM users u
  WHERE u.user_type = 'student'
    AND u.school_uuid = (SELECT school_id FROM sch)
    AND u.class_id IN (SELECT id FROM target_classes)
)
DELETE FROM user_activity_progress p
WHERE p.user_id IN (SELECT id FROM target_students);

-- -----------------------------------------------------------------------------
-- 4) Delete students in these eight classes only
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
target_classes AS (
  SELECT c.id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name IN (
    '2nd Green', '2nd Blue', '3rd Green', '3rd Blue',
    '4th Green', '4th Blue', '5th Green', '5th Blue'
  )
)
DELETE FROM users u
WHERE u.user_type = 'student'
  AND u.school_uuid = (SELECT school_id FROM sch)
  AND u.class_id IN (SELECT id FROM target_classes);

-- -----------------------------------------------------------------------------
-- 5) 5th Green
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
cg AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '5th Green'
)
INSERT INTO users (id, cognito_sub, email, phone, username, display_name, school_id, school_uuid, user_type, class_id, roll_number, password_hash, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, NULL, NULL, NULL, trim(v.nm), NULL, sch.school_id, 'student', cg.class_id, v.roll, NULL, NOW(), NOW()
FROM sch
CROSS JOIN cg
CROSS JOIN (VALUES
  ('Furqan Ahmad Shah', '1'),
  ('Hafsa Jan', '2'),
  ('AIKA JAN', '3'),
  ('NAZIM AHMAD KHAN', '4'),
  ('Zainul Aabideen Shah', '5'),
  ('SAMEER AHMAD BAJARD', '6'),
  ('SUHAIB PARVAIZ', '7'),
  ('MUNTAZIR HAMID LONE', '8'),
  ('KAMIL IRSHAD', '9'),
  ('MIR IMADUL ISLAM', '10'),
  ('MUSTAFA GUL', '11'),
  ('Ifra Asif', '12'),
  ('ZUHA RAFIQ', '13'),
  ('Mohd Imraan Bhat', '14'),
  ('Aayat Jaan', '15'),
  ('Abu Azim Khan', '16'),
  ('Sufaya Ahad', '17'),
  ('Aatif Riyaz KHAN', '18'),
  ('Noman Ahmad Lone', '19'),
  ('Sulifa Aijaz', '20'),
  ('Aayan Abas', '21'),
  ('Onesa Rashid', '22'),
  ('Adeeba Khan', '23'),
  ('Aliza altaf', '24'),
  ('Sadiya Tufail', '25'),
  ('AKSA JAN', '26'),
  ('AATIF REYAZ MIR', '27'),
  ('Salma Riyaz', '28'),
  ('WAHID NAZIR', '29'),
  ('MOHD AKIL KHAN', '30'),
  ('ATHAR WALI', '31'),
  ('LONE ABU ZAR', '32'),
  ('IFFAT ABAS', '33'),
  ('SEERAT ABAS', '34'),
  ('Mohd ZUhaib -Ul Islam', '35'),
  ('SYED MOHAMMAD IBRAHIM', '36'),
  ('MOHSIN MUSHTAQ', '37'),
  ('AABID MAJEED', '38'),
  ('AAHIL QUYOOM KHAN', '39'),
  ('MARIYA MUSHTAQ', '40'),
  ('QURATUN NISA', '41'),
  ('AYAN KHURSHEED', '42'),
  ('WAQAR AHMAD PIRZADA', '43'),
  ('UMER JAMSHEED', '44'),
  ('AARIZ HAFEEZ', '45'),
  ('MANAN FAYAZ', '46'),
  ('SADIYA JAVED', '47'),
  ('ANFA JAN', '48'),
  ('Ayat Aslam Mir', '49')
) AS v(nm, roll);


-- -----------------------------------------------------------------------------
-- 6) 5th Blue
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
cg AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '5th Blue'
)
INSERT INTO users (id, cognito_sub, email, phone, username, display_name, school_id, school_uuid, user_type, class_id, roll_number, password_hash, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, NULL, NULL, NULL, trim(v.nm), NULL, sch.school_id, 'student', cg.class_id, v.roll, NULL, NOW(), NOW()
FROM sch
CROSS JOIN cg
CROSS JOIN (VALUES
  ('NOMAN Naseer Khan', '1'),
  ('Zainab muhammad', '2'),
  ('AYMAN WASEEM', '3'),
  ('AYMAN FAROOQ', '4'),
  ('IMAD AASHIQ TANTRE', '5'),
  ('SHEIKH USMAN', '6'),
  ('MOHAMMAD MOOMIN', '7'),
  ('FARHAT JAVID', '8'),
  ('MOHAMMAD AFFAN', '9'),
  ('PZ ZUHAIB HUSSAIN', '10'),
  ('ZAHID AHMAD MIR', '11'),
  ('ZEESHAN BASHIR', '12'),
  ('Sehrish Akther', '13'),
  ('UZAIR AH KHAN', '14'),
  ('TABINDA BILAL', '15'),
  ('SABIT IQBAL', '16'),
  ('MIR ZARA', '17'),
  ('KHAN MUNTAHA', '18'),
  ('SYEEDA SHAH PARA JAWDET', '19'),
  ('Mudasir Hilal', '20'),
  ('MEHREEN BASHIR', '21'),
  ('Ikhlaq Zahoor', '22'),
  ('Majid Zahoor', '23'),
  ('BASIT AHMAD WANI', '24'),
  ('SADIYA JAN', '25'),
  ('NAILAH MUDASIR', '26'),
  ('FAWAD KHURSHEED', '27'),
  ('SHARIQ IQBAL NAJAR', '28'),
  ('MUNEEB MYSER', '29'),
  ('ARBEENA AHAD', '30'),
  ('HAMAD AIJAZ', '31'),
  ('BASIT FAROOQ SHAH', '32'),
  ('RAKSHANDA SAYEED', '33'),
  ('MAVIYA MANZOOR', '34'),
  ('SANID MUSHTAQ', '35'),
  ('Beigh Yawar Showket', '36'),
  ('SADIYA', '37'),
  ('JIBRAN RAFIQ BHAT', '38'),
  ('ADEEBA QAISAR', '39'),
  ('BABAR AHMAD LONE', '40'),
  ('MOHAMMAD FAISAL PASWAL', '41'),
  ('HUMA MANSOOR', '42'),
  ('ITRAT FAROOQ', '43'),
  ('ANAYAT NISSAR', '44'),
  ('IFAAM WASEEM', '45'),
  ('UMAIS MAJEED', '46'),
  ('MOHSIN RASHID', '47'),
  ('AREEBA JAVID', '48'),
  ('ASIM JAVID', '49')
) AS v(nm, roll);


-- -----------------------------------------------------------------------------
-- 7) 4th Green
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
cg AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '4th Green'
)
INSERT INTO users (id, cognito_sub, email, phone, username, display_name, school_id, school_uuid, user_type, class_id, roll_number, password_hash, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, NULL, NULL, NULL, trim(v.nm), NULL, sch.school_id, 'student', cg.class_id, v.roll, NULL, NOW(), NOW()
FROM sch
CROSS JOIN cg
CROSS JOIN (VALUES
  ('AFFAN BIN HILAL', '1'),
  ('SOBIYA JAAN', '2'),
  ('ZEYAN JAVEED', '3'),
  ('JUNAID AHMAD BHAT', '4'),
  ('TUBA MUSHTAQ', '5'),
  ('ATHER MOHIUDIN', '6'),
  ('Haya Wani', '7'),
  ('Momin Rashid', '8'),
  ('Mohd. Muneeb Mir', '9'),
  ('Uzma Javid Mir', '10'),
  ('Azhar-Ul Islam', '11'),
  ('Farhan Ashiq Khan', '12'),
  ('Zaidul Islam', '13'),
  ('Sheikh SHAFKAT', '14'),
  ('Asmat Rashid', '15'),
  ('MIRHA HILAL', '16'),
  ('Tyba Jan', '17'),
  ('ifrat Ashraf', '18'),
  ('Mir Ubaid', '19'),
  ('ATIFA TARIQ', '20'),
  ('MUNAZAH JAVID', '21'),
  ('ALIZA ADIL SHAH', '22'),
  ('ADNAN HAMEED', '23'),
  ('OWAIS AH LONE', '24'),
  ('RAFIA  ZAKIR', '25'),
  ('SHAH FAKIRA ASHIQ', '26'),
  ('ZUHAIB SHABIR', '27'),
  ('Aayan Nazir Bhat', '28'),
  ('Aanisa Irshad', '29'),
  ('SAHAR JAN', '30'),
  ('ZAINAB AASIF', '31'),
  ('ARBAAZ BILAL BHAT', '32'),
  ('JUNAID AHMAD SHAH', '33'),
  ('HAFSA SHOWKAT', '34'),
  ('Faika Javeed', '35'),
  ('SYEDA ASHIYA', '36'),
  ('RUMAYA TARIQ', '37'),
  ('BUSHRAH WALI', '38'),
  ('AFLA NAZIR', '39'),
  ('ZAINAB NASEER', '40'),
  ('MOHAMMAD AANIS BHAT', '41')
) AS v(nm, roll);


-- -----------------------------------------------------------------------------
-- 8) 4th Blue
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
cg AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '4th Blue'
)
INSERT INTO users (id, cognito_sub, email, phone, username, display_name, school_id, school_uuid, user_type, class_id, roll_number, password_hash, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, NULL, NULL, NULL, trim(v.nm), NULL, sch.school_id, 'student', cg.class_id, v.roll, NULL, NOW(), NOW()
FROM sch
CROSS JOIN cg
CROSS JOIN (VALUES
  ('FAIZ FIRDOUS LONE', '1'),
  ('Sumaya Nabi', '2'),
  ('Hamid Bilal', '3'),
  ('TAMANA JAN', '4'),
  ('Usaman Aijaz', '5'),
  ('Mohd UZAIR Peer', '6'),
  ('Hibsa Assif', '7'),
  ('Zarah Iftikar', '8'),
  ('Zarnain Zahoor', '9'),
  ('Furqan Shahzad', '10'),
  ('Darkhshan Jan', '11'),
  ('Hisham Ellahi Muneer', '12'),
  ('SHEEBA JAN', '13'),
  ('KAMRAN MAJEED', '14'),
  ('SEHRISH KHALID KHAN', '15'),
  ('MUZAMIL ZAHOOR', '16'),
  ('FAIZAN FAROOQ', '17'),
  ('SALMAN BASHIR', '18'),
  ('AAMINA JAN', '19'),
  ('IRTIQA JAN', '20'),
  ('Rutba Jan', '21'),
  ('UZMA NABI', '22'),
  ('MEHRAN WAHEED KHAN', '23'),
  ('AHTISHAM UL HAQ', '24'),
  ('FARHAN REYAZ', '25'),
  ('HADIYA JAN', '26'),
  ('MOOMIN RASOOL', '27'),
  ('FAIZAN ALTAF', '28'),
  ('SIDRA SHOWKAT', '29'),
  ('ANIYA MUSHTAQ', '30'),
  ('MEHRAAN MUSHTAQ', '31'),
  ('SHAH NOOR UL HUDA', '32'),
  ('TAFHEEM UL ANSAR', '33'),
  ('AHSAN SAYAR', '34'),
  ('REHAN MANZOOR', '35'),
  ('MIR FURAAT', '36')
) AS v(nm, roll);


-- -----------------------------------------------------------------------------
-- 9) 3rd Green
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
cg AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '3rd Green'
)
INSERT INTO users (id, cognito_sub, email, phone, username, display_name, school_id, school_uuid, user_type, class_id, roll_number, password_hash, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, NULL, NULL, NULL, trim(v.nm), NULL, sch.school_id, 'student', cg.class_id, v.roll, NULL, NOW(), NOW()
FROM sch
CROSS JOIN cg
CROSS JOIN (VALUES
  ('SHAH SHAIMA', '1'),
  ('REHAN AHMAD KHAN', '2'),
  ('ANUM RAFIQ', '3'),
  ('SHARIK AZIZ', '4'),
  ('ZOHA ZAHOOR', '5'),
  ('MOHAMMAD SANAN KHAN', '6'),
  ('mir salim Aktar', '7'),
  ('Fazil Nazir', '8'),
  ('Arbiya Jan', '9'),
  ('Mohd Awab Bhat', '10'),
  ('Momin Katari', '11'),
  ('MOHD MOHSIN MIR', '12'),
  ('AFIYA FAYAZ', '13'),
  ('Aleebah Irshad', '14'),
  ('sadiya Ashraf', '15'),
  ('Takwa jaan', '16'),
  ('Lone haziq Mushtaq', '17'),
  ('AB MANAN  MIR', '18'),
  ('Sudais Tanveer Bhat', '19'),
  ('Syed Mohammad Haziq', '20'),
  ('ZUHAIB PARVAIZ', '21'),
  ('ZAHEER AHMAD GOJAR', '22'),
  ('syed Laraib Nayeem', '23'),
  ('Syed Abu bakar', '24'),
  ('Umar Majeed', '25'),
  ('MOHAMMAD WAJID', '26'),
  ('Arleena Fyaz', '27'),
  ('Mohammad Azam', '28'),
  ('Aafiya Muhammad', '29'),
  ('MOHAMMAD ABRAR LONE', '30'),
  ('ABU BAKAR SHAH', '31'),
  ('UMAR  BIN MUDASIR', '32'),
  ('ABU ZAR ASHRAF', '33'),
  ('SHAHID LATEEF', '34'),
  ('Zainab Manzoor', '35'),
  ('Iqra Akther', '36'),
  ('OWAIS AHMAD RAINA', '37'),
  ('ABASS AHMAD RAINA', '38'),
  ('MIR DAYIM MUDASIR', '39'),
  ('MOHSIN QUYOOM', '40'),
  ('ZUHA JAN', '41'),
  ('ASRAR JAVEED', '42'),
  ('AAROOFA JAN', '43'),
  ('AAYAN ASHARAF  LONE', '44'),
  ('USMAN FAROOQ', '45'),
  ('IMAD WASEEM', '46'),
  ('SADAT NASEER', '47'),
  ('SHAHVEER SHOWKAT', '48'),
  ('AAHIL IRSHAD', '49'),
  ('HOORAIN MUDASIR', '50')
) AS v(nm, roll);


-- -----------------------------------------------------------------------------
-- 10) 3rd Blue
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
cg AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '3rd Blue'
)
INSERT INTO users (id, cognito_sub, email, phone, username, display_name, school_id, school_uuid, user_type, class_id, roll_number, password_hash, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, NULL, NULL, NULL, trim(v.nm), NULL, sch.school_id, 'student', cg.class_id, v.roll, NULL, NOW(), NOW()
FROM sch
CROSS JOIN cg
CROSS JOIN (VALUES
  ('ZOHAN QAISAR', '1'),
  ('DANISH AHMADBHAT', '2'),
  ('AKSA KHURSHEED', '3'),
  ('Anaya Shabhir', '4'),
  ('Neera Naseer', '5'),
  ('Khalid AH Khan', '6'),
  ('TOIBA JAAN', '7'),
  ('SAHIL IQBAL KHAN', '8'),
  ('SALIM SHOWKAT', '9'),
  ('ZUHAIB ZAHOOR', '10'),
  ('USMAN IMRAN', '11'),
  ('AKSA ASLAM', '12'),
  ('MOHAMMAD EMAD', '13'),
  ('MOHAMMAD AAHIL MIR', '14'),
  ('AZRA BASHARAT', '15'),
  ('SAAD FAROOQ', '16'),
  ('ABU REYAN MIR', '17'),
  ('SAHIL AHMAD MIR', '18'),
  ('PZ MUSAIB HUSSAIN SHAH', '19'),
  ('FURQAN SHAFI', '20'),
  ('AREEBA ZAKIR', '21'),
  ('AYAT SHABIR ZARGAR', '22'),
  ('ALIZA JAN', '23'),
  ('SUNAIN KHAN', '24'),
  ('UROOJ UL ISLAM', '25'),
  ('ZUBAIR YOUSF', '26'),
  ('SADIYA RAFIQ', '27'),
  ('AALIYA MUMTAZ', '28'),
  ('AAHIL RASOOL TANTRAY', '29'),
  ('HAYA MUDASIR', '30'),
  ('ZAHIKA WANI', '31'),
  ('SAYEEDA NOOR PARA AKRAB', '32'),
  ('SOLIHA IRSHAD', '33'),
  ('KHAN HANZAL', '34'),
  ('SYED MOHD JAMI', '35'),
  ('ARIZOO SHAFI', '36'),
  ('ADEEB TARIQ', '37'),
  ('ALEENA SAJAD', '38'),
  ('SYED HASHAAM HIDAYAT', '39'),
  ('SYED MUZAIF MASOOD', '40'),
  ('HAMMAD AHMAD MIR', '41'),
  ('ZAINAB FAROOQ', '42'),
  ('SALIK TARIQ', '43'),
  ('JAFFAR NAZIR WANI', '44'),
  ('FAWAD HAMID TANTRAY', '45'),
  ('AZAN AIJAZ', '46'),
  ('HAFSA RAYEES', '47'),
  ('ARHAAN AASHIQ', '48'),
  ('BAREERA JAVID', '49'),
  ('FARHAT JAN', '50'),
  ('UMAISA IQBAL', '51')
) AS v(nm, roll);


-- -----------------------------------------------------------------------------
-- 11) 2nd Green
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
cg AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '2nd Green'
)
INSERT INTO users (id, cognito_sub, email, phone, username, display_name, school_id, school_uuid, user_type, class_id, roll_number, password_hash, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, NULL, NULL, NULL, trim(v.nm), NULL, sch.school_id, 'student', cg.class_id, v.roll, NULL, NOW(), NOW()
FROM sch
CROSS JOIN cg
CROSS JOIN (VALUES
  ('IFRA JEHANGIR', '1'),
  ('SAOODA', '2'),
  ('ANABIYA SHAYAN SHAH', '3'),
  ('ARKAM AHMAD LONE', '4'),
  ('ZAIRA WASEEM', '5'),
  ('ABU ZAR', '6'),
  ('NADIA FAROOQ', '7'),
  ('ASMA JAVEED', '8'),
  ('RAHIL AHMAD WANI', '9'),
  ('ZAIKA FAYAZ', '10'),
  ('QARIYA ASHRAF', '11'),
  ('AREEBA IQBAL', '12'),
  ('ANZAR AHMAD DAR', '13'),
  ('SHAHID SHABIR DAR', '14'),
  ('TANISHA WANI', '15'),
  ('MOHAMMAD AYAN', '16'),
  ('ZAINAB BINT KHURSHEED', '17'),
  ('MIR ABU ZAR', '18'),
  ('MAHEENA RASHID', '19'),
  ('DUA JAN', '20'),
  ('SHAH SUHAIB BILAL', '21'),
  ('AAYAT MYSER', '22'),
  ('SOMAN AAMIR', '23'),
  ('MOHAMMAD MURSLEEN WANI', '24'),
  ('ROMAN RAYEES', '25'),
  ('RAHAT JAN', '26'),
  ('AFHAM AIJAZ', '27'),
  ('EHSAN MOHAMMAD SHEIKH', '28'),
  ('SAJAD AHMAD BHAT', '29'),
  ('FATIMA JAN', '30'),
  ('AATIFA ASHIQ', '31'),
  ('AZAN REYAZ', '32'),
  ('IMRAN YASEEN', '33'),
  ('AHQAM AHMAD KHAN', '34'),
  ('MOHD ABRAR CHECHI', '35'),
  ('MOHD  REYAN SHAH', '36'),
  ('MOHAMMAD EZHAAN', '37'),
  ('MOHAMMAD ADNAN', '38'),
  ('MOHAMMAD ABU BAKAR', '39'),
  ('AISHA JAHAN', '40'),
  ('JUNAID UL ISLAM', '41'),
  ('AJWA SAJAD', '42'),
  ('FAHEEM JAFFER', '43'),
  ('HORAIB JAVID', '44'),
  ('SUWAID SHOAIB', '45'),
  ('MIR AZHAR', '46'),
  ('MUSHARAF MASOOD', '47')
) AS v(nm, roll);


-- -----------------------------------------------------------------------------
-- 12) 2nd Blue
-- -----------------------------------------------------------------------------
WITH sch AS (
  SELECT school_uuid AS school_id
  FROM users
  WHERE id = '06935ed4-a36e-49ec-ac6f-e6a4ed91a390'::uuid AND user_type = 'school_admin'
),
cg AS (
  SELECT c.id AS class_id
  FROM classes c
  JOIN sch ON c.school_id = sch.school_id
  WHERE c.name = '2nd Blue'
)
INSERT INTO users (id, cognito_sub, email, phone, username, display_name, school_id, school_uuid, user_type, class_id, roll_number, password_hash, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, NULL, NULL, NULL, trim(v.nm), NULL, sch.school_id, 'student', cg.class_id, v.roll, NULL, NOW(), NOW()
FROM sch
CROSS JOIN cg
CROSS JOIN (VALUES
  ('AISHA NASEER', '1'),
  ('AYESHA MANZOOR', '2'),
  ('AATIF KHALID', '3'),
  ('AAYAT IRSHAD', '4'),
  ('IMAD IQBAL', '5'),
  ('MUNEEB MUSHTAQ', '6'),
  ('HAFSA FAROOQ', '7'),
  ('RIZWAN IRSHAD', '8'),
  ('ZENAB N NISSA', '9'),
  ('MOHAMMAD HAMAD', '10'),
  ('MUHAMMAD AMAD BHAT', '11'),
  ('MOHAMMAD MURAD SHEIKH', '12'),
  ('MOHMAD ARHAN', '13'),
  ('MEHAK BASHIR', '14'),
  ('MAJID KHAN', '15'),
  ('SHAHID MANZOOR', '16'),
  ('IMAAD SHOWKAT', '17'),
  ('NEHAAN AHMAD KHAN', '18'),
  ('AROOSH AIJAZ', '19'),
  ('MUZAMIL MUDASIR SHAH', '20'),
  ('MOHD WASIQ SHAH', '21'),
  ('IMAAD RASOOL RATHER', '22'),
  ('MOHAMMAD YAMIN SHAH', '23'),
  ('MEHRISH JAN', '24'),
  ('MEHREN JAN', '25'),
  ('FARHAN FIRDOUS LONE', '26'),
  ('WASIF WASEEM', '27'),
  ('DAVOOD AHMAD MIR', '28'),
  ('WAQAS NABI DAR', '29'),
  ('FAREHA JAVID', '30'),
  ('SAFANA TARIQ', '31'),
  ('AASIMA TARIQ', '32'),
  ('HAFSA MUSHTAQ', '33'),
  ('HASHMAT JAN', '34'),
  ('HURAIN NOOR', '35'),
  ('TOIBA IRSHAD', '36'),
  ('BURHAN NASEER', '37'),
  ('AKEEL MANZOOR', '38'),
  ('SIDRATUN MUNTAHA', '39'),
  ('URBA MANSOOR', '40'),
  ('MAHAN QUYOOM', '41'),
  ('QURATUL AIN', '42'),
  ('SOLIYA JAVEED', '43'),
  ('HUDA MUNEER', '44'),
  ('FATIMA MEHJOOR', '45'),
  ('MOHAMMAD ZAID', '46'),
  ('MOHIT BHAT', '47')
) AS v(nm, roll);


COMMIT;

