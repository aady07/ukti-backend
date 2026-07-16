-- =============================================================================
-- Nursery Yellow (prod test): 30 students + user_activity_progress + engagement rows 1–3.
-- Class / teacher link already exists — this only touches students in that class.
--
-- Frontend handoff (your DB):
--   classId  = f20f985d-b281-4753-be45-27ca94920926  (Nursery Yellow)
--   schoolId = run: SELECT school_id FROM classes WHERE id = 'f20f985d-b281-4753-be45-27ca94920926'::uuid;
--              (expected: d3334c08-1005-4a7b-9c7f-70142a866437 for sadarsh51000@gmail.com’s school)
--
-- Aligns with teacher dashboard doc (frontend):
--   • Weighted overall %: ~0.5 per Show or Speak half, ~1.0 per full pair; denominator ≈ TEACHER_DASHBOARD_TOTAL_ACTIVITIES (~102).
--   • Module rings (Nursery): use playgroup-unit-1 slugs (e.g. playgroup-letter-N-show/say, playgroup-colour-N-show/say).
--   • English table: body-parts-* under english-unit-1 (pairs).
-- Status: Good ≥60%, Slow 30–59%, Needs help <30%; Needs attention = weighted <60% (everyone not Good).
--
-- This seed (approx. full steps = paired bases, counted by backend logical rules too):
--   rolls 1–12  → 33 letter + 33 colour + 6 body-parts pairs = 72 → ~71% Good (non-zero donut “on track”)
--   rolls 13–22 → 20 letter + 20 colour pairs = 40 → ~39% Slow
--   rolls 23–27 → 7 letter + 7 colour pairs = 14 → ~14% Needs help
--   rolls 28–30 → no rows → 0% Not started
--
-- GET /schools/{schoolId}/progress/overview?totalActivities=102 — coarse % close to weighted story for QA.
-- Per-student detail: GET /progress/units with X-Roll-Number + X-Class-Id.
--
-- Run: psql -d ukti_db -v ON_ERROR_STOP=1 -f scripts/seed-class-students-progress-engagement.sql
-- =============================================================================

DO $$
DECLARE
  v_class_id  uuid := 'f20f985d-b281-4753-be45-27ca94920926'; -- Nursery Yellow
  v_school_id uuid;
  v_dummy_pw  text := '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi'; -- NOT NULL on users; not used for station login
BEGIN
  SELECT school_id INTO v_school_id FROM classes WHERE id = v_class_id;
  IF v_school_id IS NULL THEN
    RAISE EXCEPTION 'Class not found: %', v_class_id;
  END IF;

  -- Remove prior seed data for this class only (students + their progress + engagement)
  DELETE FROM activity_engagement_event e
  USING activity_engagement_session s
  WHERE e.session_id = s.id
    AND s.user_id IN (SELECT id FROM users WHERE class_id = v_class_id AND user_type = 'student');

  DELETE FROM activity_engagement_processed_batch b
  USING activity_engagement_session s
  WHERE b.session_id = s.id
    AND s.user_id IN (SELECT id FROM users WHERE class_id = v_class_id AND user_type = 'student');

  DELETE FROM activity_engagement_session
  WHERE user_id IN (SELECT id FROM users WHERE class_id = v_class_id AND user_type = 'student');

  DELETE FROM user_activity_progress
  WHERE user_id IN (SELECT id FROM users WHERE class_id = v_class_id AND user_type = 'student');

  DELETE FROM users
  WHERE class_id = v_class_id AND user_type = 'student';

  -- 30 students (rolls 1–30)
  INSERT INTO users (
    id, cognito_sub, email, phone, username, display_name,
    school_id, school_uuid, user_type, class_id, roll_number,
    password_hash, created_at, updated_at
  )
  SELECT
    gen_random_uuid(),
    NULL, NULL, NULL, NULL,
    trim(v.nm),
    NULL,
    v_school_id,
    'student',
    v_class_id,
    v.roll,
    v_dummy_pw,
    now(),
    now()
  FROM (VALUES
    ('1',  'Aarav S.'),
    ('2',  'Ananya P.'),
    ('3',  'Arjun R.'),
    ('4',  'Ishita N.'),
    ('5',  'Rohan D.'),
    ('6',  'Neha B.'),
    ('7',  'Kavya M.'),
    ('8',  'Manav K.'),
    ('9',  'Sia R.'),
    ('10', 'Om L.'),
    ('11', 'Kiara V.'),
    ('12', 'Reyansh J.'),
    ('13', 'Myra T.'),
    ('14', 'Advik G.'),
    ('15', 'Sara F.'),
    ('16', 'Ayaan Z.'),
    ('17', 'Tanvi E.'),
    ('18', 'Ishaan W.'),
    ('19', 'Veer O.'),
    ('20', 'Mira Y.'),
    ('21', 'Vivaan K.'),
    ('22', 'Diya M.'),
    ('23', 'Kabir L.'),
    ('24', 'Pari H.'),
    ('25', 'Yash C.'),
    ('26', 'Riya A.'),
    ('27', 'Dev P.'),
    ('28', 'Anika S.'),
    ('29', 'Harsh Q.'),
    ('30', 'Tara X.')
  ) AS v(roll, nm);

  -- ---------------------------------------------------------------------------
  -- Good (rolls 1–12): playgroup module rings + english body-parts
  -- ---------------------------------------------------------------------------
  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, 'playgroup-unit-1', format('playgroup-letter-%s-show', n),
         now() - (n * interval '95 minutes') - ((n % 13) * interval '1 day') - (u.r * interval '7 minutes'),
         CASE WHEN n <= 3 THEN jsonb_build_object('moduleId', 'nursery-playgroup', 'moduleItemId', format('playgroup-letter-%s', n)) ELSE NULL END
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 1 AND 12) u
  CROSS JOIN generate_series(1, 33) AS n;

  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, 'playgroup-unit-1', format('playgroup-letter-%s-say', n),
         now() - (n * interval '95 minutes') - ((n % 13) * interval '1 day') - (u.r * interval '7 minutes') + interval '6 minutes',
         CASE WHEN n <= 3 THEN jsonb_build_object('moduleId', 'nursery-playgroup', 'moduleItemId', format('playgroup-letter-%s', n)) ELSE NULL END
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 1 AND 12) u
  CROSS JOIN generate_series(1, 33) AS n;

  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, 'playgroup-unit-1', format('playgroup-colour-%s-show', n),
         now() - (n * interval '88 minutes') - ((n % 11) * interval '1 day') - (u.r * interval '5 minutes'),
         CASE WHEN n <= 3 THEN jsonb_build_object('moduleId', 'nursery-playgroup', 'moduleItemId', format('playgroup-colour-%s', n)) ELSE NULL END
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 1 AND 12) u
  CROSS JOIN generate_series(1, 33) AS n;

  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, 'playgroup-unit-1', format('playgroup-colour-%s-say', n),
         now() - (n * interval '88 minutes') - ((n % 11) * interval '1 day') - (u.r * interval '5 minutes') + interval '6 minutes',
         CASE WHEN n <= 3 THEN jsonb_build_object('moduleId', 'nursery-playgroup', 'moduleItemId', format('playgroup-colour-%s', n)) ELSE NULL END
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 1 AND 12) u
  CROSS JOIN generate_series(1, 33) AS n;

  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, p.unit_slug, p.activity_slug,
         now() - (p.d || ' days')::interval - make_interval(mins => u.r * 3 + p.seq),
         jsonb_build_object('moduleId', 'english-u1', 'moduleItemId', p.activity_slug)
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 1 AND 12) u
  CROSS JOIN (
    VALUES
      ('english-unit-1','body-parts-1-show', 0, 1), ('english-unit-1','body-parts-1-say', 0, 2),
      ('english-unit-1','body-parts-2-show', 1, 3), ('english-unit-1','body-parts-2-say', 1, 4),
      ('english-unit-1','body-parts-3-show', 2, 5), ('english-unit-1','body-parts-3-say', 2, 6),
      ('english-unit-1','body-parts-4-show', 3, 7), ('english-unit-1','body-parts-4-say', 3, 8),
      ('english-unit-1','body-parts-5-show', 4, 9), ('english-unit-1','body-parts-5-say', 4,10),
      ('english-unit-1','body-parts-6-show', 5,11), ('english-unit-1','body-parts-6-say', 5,12)
  ) AS p(unit_slug, activity_slug, d, seq);

  -- ---------------------------------------------------------------------------
  -- Slow (rolls 13–22): playgroup only (~40/102)
  -- ---------------------------------------------------------------------------
  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, 'playgroup-unit-1', format('playgroup-letter-%s-show', n),
         now() - (n * interval '100 minutes') - ((n % 10) * interval '1 day') - (u.r * interval '4 minutes'),
         NULL
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 13 AND 22) u
  CROSS JOIN generate_series(1, 20) AS n;

  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, 'playgroup-unit-1', format('playgroup-letter-%s-say', n),
         now() - (n * interval '100 minutes') - ((n % 10) * interval '1 day') - (u.r * interval '4 minutes') + interval '5 minutes',
         NULL
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 13 AND 22) u
  CROSS JOIN generate_series(1, 20) AS n;

  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, 'playgroup-unit-1', format('playgroup-colour-%s-show', n),
         now() - (n * interval '92 minutes') - ((n % 9) * interval '1 day') - (u.r * interval '6 minutes'),
         NULL
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 13 AND 22) u
  CROSS JOIN generate_series(1, 20) AS n;

  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, 'playgroup-unit-1', format('playgroup-colour-%s-say', n),
         now() - (n * interval '92 minutes') - ((n % 9) * interval '1 day') - (u.r * interval '6 minutes') + interval '5 minutes',
         NULL
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 13 AND 22) u
  CROSS JOIN generate_series(1, 20) AS n;

  -- ---------------------------------------------------------------------------
  -- Needs help (rolls 23–27): light playgroup (~14/102)
  -- ---------------------------------------------------------------------------
  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, 'playgroup-unit-1', format('playgroup-letter-%s-show', n),
         now() - (n * interval '110 minutes') - ((n % 7) * interval '1 day') - (u.r * interval '3 minutes'),
         NULL
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 23 AND 27) u
  CROSS JOIN generate_series(1, 7) AS n;

  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, 'playgroup-unit-1', format('playgroup-letter-%s-say', n),
         now() - (n * interval '110 minutes') - ((n % 7) * interval '1 day') - (u.r * interval '3 minutes') + interval '5 minutes',
         NULL
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 23 AND 27) u
  CROSS JOIN generate_series(1, 7) AS n;

  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, 'playgroup-unit-1', format('playgroup-colour-%s-show', n),
         now() - (n * interval '105 minutes') - ((n % 6) * interval '1 day') - (u.r * interval '2 minutes'),
         NULL
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 23 AND 27) u
  CROSS JOIN generate_series(1, 7) AS n;

  INSERT INTO user_activity_progress (id, user_id, unit_slug, activity_slug, completed_at, metadata)
  SELECT gen_random_uuid(), u.id, 'playgroup-unit-1', format('playgroup-colour-%s-say', n),
         now() - (n * interval '105 minutes') - ((n % 6) * interval '1 day') - (u.r * interval '2 minutes') + interval '5 minutes',
         NULL
  FROM (SELECT id, roll_number::int AS r FROM users WHERE class_id = v_class_id AND user_type = 'student' AND roll_number::int BETWEEN 23 AND 27) u
  CROSS JOIN generate_series(1, 7) AS n;

  -- rolls 28–30: intentionally no user_activity_progress

  -- Engagement samples: rolls 1–3
  INSERT INTO activity_engagement_session (
    id, client_session_id, user_id, school_id, class_id, roll_number,
    unit_slug, activity_slug, started_at, ended_at,
    video_skip, video_replay, vision_attempts, vision_failures, vision_passes,
    stt_listen_starts, pron_pass, skip_audio, pron_fail, stt_empty_cycles, client_error,
    video_complete, video_error,
    payload_version, raw_events, created_at, updated_at
  )
  SELECT
    gen_random_uuid(),
    gen_random_uuid(),
    u.id,
    v_school_id,
    v_class_id,
    u.roll_number,
    'english-unit-1',
    'body-parts-1',
    now() - interval '2 days',
    now() - interval '2 days' + interval '12 minutes',
    1, 0, 2, 1, 1, 2, 1, 0, 0, 0, 0,
    0, 0,
    1,
    '[
       {"type":"video_skip","ts":"2026-05-08T10:00:05Z","phase":"station-video","payload":{}},
       {"type":"vision_result","ts":"2026-05-08T10:02:00Z","payload":{"passed":true,"attemptNumber":2}},
       {"type":"pron_result","ts":"2026-05-08T10:05:00Z","payload":{"outcome":"pass"}}
     ]'::jsonb,
    now(), now()
  FROM users u
  WHERE u.class_id = v_class_id AND u.user_type = 'student' AND u.roll_number = '1';

  INSERT INTO activity_engagement_session (
    id, client_session_id, user_id, school_id, class_id, roll_number,
    unit_slug, activity_slug, started_at, ended_at,
    video_complete, vision_attempts, vision_passes, stt_listen_starts, pron_pass,
    video_skip, video_replay, video_error, vision_failures, skip_audio, pron_fail, stt_empty_cycles, client_error,
    payload_version, created_at, updated_at
  )
  SELECT
    gen_random_uuid(),
    gen_random_uuid(),
    u.id,
    v_school_id,
    v_class_id,
    u.roll_number,
    'english-unit-1',
    'body-parts-2',
    now() - interval '1 day',
    now() - interval '1 day' + interval '8 minutes',
    1, 1, 1, 1, 1,
    0, 0, 0, 0, 0, 0, 0, 0,
    1,
    now(), now()
  FROM users u
  WHERE u.class_id = v_class_id AND u.user_type = 'student' AND u.roll_number = '2';

  INSERT INTO activity_engagement_session (
    id, client_session_id, user_id, school_id, class_id, roll_number,
    unit_slug, activity_slug, started_at, ended_at,
    skip_audio, pron_fail, stt_empty_cycles, stt_listen_starts,
    video_complete, video_skip, video_replay, video_error,
    vision_attempts, vision_passes, vision_failures, pron_pass, client_error,
    payload_version, created_at, updated_at
  )
  SELECT
    gen_random_uuid(),
    gen_random_uuid(),
    u.id,
    v_school_id,
    v_class_id,
    u.roll_number,
    'english-unit-1',
    'body-parts-3',
    now() - interval '5 hours',
    now() - interval '4 hours',
    1, 1, 1, 3,
    0, 0, 0, 0,
    0, 0, 0, 0, 0,
    1,
    now(), now()
  FROM users u
  WHERE u.class_id = v_class_id AND u.user_type = 'student' AND u.roll_number = '3';

  RAISE NOTICE 'Seeded class_id=% school_id=% (30 students + progress + engagement rolls 1–3)', v_class_id, v_school_id;
END $$;
