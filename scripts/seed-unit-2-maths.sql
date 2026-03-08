-- =============================================================================
-- Maths Unit 1: Round / Long
-- Run: psql -d ukti_db -f scripts/seed-unit-2-maths.sql
-- Video played by frontend. Webcam + Gemini checks if object is round or long.
-- =============================================================================

DO $$
DECLARE
  v_unit_id UUID;
  v_task1_id UUID;
BEGIN
  -- ===========================================================================
  -- UNIT 2: Maths - Round and Long
  -- ===========================================================================
  INSERT INTO units (name, slug, description, module_type, subject, "order") 
  VALUES (
    'Maths Unit 1: Round / Long',
    'maths-unit-1',
    'Learn to identify round and long objects',
    'assistive',
    'maths',
    2
  )
  RETURNING id INTO v_unit_id;

  -- ===========================================================================
  -- TASK 1: Round and Long
  -- ===========================================================================
  INSERT INTO tasks (unit_id, name, slug, "order") 
  VALUES (v_unit_id, 'Round and Long', 'round-long', 1)
  RETURNING id INTO v_task1_id;

  -- Activity 1: Round objects - Video intro, then find round objects via webcam
  INSERT INTO activities (task_id, name, type, "order", prop_name, video_url, config) 
  VALUES (
    v_task1_id, 'Activity 1: Round Objects', 'activity', 1, 'Round and long objects', '/maths-round-long-guide.mp4',
    '{
      "prompts": ["Find a round object. Show it to the camera and say ''round''.", "Pick up something round. Show it and say ''round''."],
      "targetWords": ["round"],
      "pronunciationPrompts": ["This is round", "It is round"],
      "propName": "Round and long objects",
      "videoUrl": "/maths-round-long-guide.mp4",
      "questionType": "round_or_long",
      "expectedAnswer": "round",
      "geminiPrompt": "Check if the object shown is round. Student should say ''round''."
    }'::jsonb
  );

  -- Activity 2: Long objects - Video intro, then find long objects via webcam
  INSERT INTO activities (task_id, name, type, "order", prop_name, video_url, config) 
  VALUES (
    v_task1_id, 'Activity 2: Long Objects', 'activity', 2, 'Round and long objects', '/maths-round-long-guide.mp4',
    '{
      "prompts": ["Find a long object. Show it to the camera and say ''long''.", "Pick up something long. Show it and say ''long''."],
      "targetWords": ["long"],
      "pronunciationPrompts": ["This is long", "It is long"],
      "propName": "Round and long objects",
      "videoUrl": "/maths-round-long-guide.mp4",
      "questionType": "round_or_long",
      "expectedAnswer": "long",
      "geminiPrompt": "Check if the object shown is long. Student should say ''long''."
    }'::jsonb
  );

  -- Activity 3: Round or Long? - Mixed assessment via webcam
  INSERT INTO activities (task_id, name, type, "order", prop_name, video_url, config) 
  VALUES (
    v_task1_id, 'Activity 3: Round or Long?', 'assessment', 3, 'Round and long objects', '/maths-round-long-guide.mp4',
    '{
      "prompts": ["Find an object. Show it and tell me - is it round or long?", "Pick any object. Show it to the camera and say if it is round or long."],
      "targetWords": ["round", "long"],
      "pronunciationPrompts": ["It is round", "It is long"],
      "propName": "Round and long objects",
      "videoUrl": "/maths-round-long-guide.mp4",
      "questionType": "round_or_long",
      "expectedAnswer": "round_or_long",
      "geminiPrompt": "Check if the object shown is round or long. Student should say ''round'' or ''long'' correctly.",
      "randomOrder": true,
      "retryOnWrong": true
    }'::jsonb
  );

  RAISE NOTICE 'Maths Unit 1 (Round/Long) seeded successfully. Unit ID: %', v_unit_id;
END $$;
