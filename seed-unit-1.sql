-- =============================================================================
-- English Unit 1: My Family and Me - Complete seed data
-- Run: psql -d ukti_db -f scripts/seed-unit-1.sql
-- =============================================================================

DO $$
DECLARE
  v_unit_id UUID;
  v_task1_id UUID;
  v_task2_id UUID;
  v_task3_id UUID;
BEGIN
  -- ===========================================================================
  -- UNIT
  -- ===========================================================================
  INSERT INTO units (name, slug, description, module_type, subject, "order") 
  VALUES (
    'English Unit 1: My Family and Me',
    'english-unit-1',
    'Learn body parts, movements, and letters',
    'assistive',
    'english',
    1
  )
  RETURNING id INTO v_unit_id;

  -- ===========================================================================
  -- TASK 1: Body Parts (stricter prompts: finger pointing required)
  -- ===========================================================================
  INSERT INTO tasks (unit_id, name, slug, "order") 
  VALUES (v_unit_id, 'Body Parts', 'body-parts', 1)
  RETURNING id INTO v_task1_id;

  -- Activity 1: Head, shoulders, knees, toes, eyes, ears, nose, mouth
  INSERT INTO activities (task_id, name, type, "order", prop_name, video_url, config) 
  VALUES (
    v_task1_id, 'Activity 1: Face & Core', 'activity', 1, 'Big Doll', '/guide.mp4',
    '{
      "prompts": ["Point your finger at the doll''s head", "Point your finger at the doll''s shoulders", "Point your finger at the doll''s knees", "Point your finger at the doll''s toes", "Point your finger at the doll''s eyes", "Point your finger at the doll''s ears", "Point your finger at the doll''s nose", "Point your finger at the doll''s mouth"],
      "targetWords": ["head", "shoulders", "knees", "toes", "eyes", "ears", "nose", "mouth"],
      "pronunciationPrompts": ["This is Head", "This is shoulders", "This is knees", "This is toes", "This is eyes", "This is ears", "This is nose", "This is mouth"],
      "propName": "Big Doll",
      "videoUrl": "/guide.mp4",
      "requireFingerPointing": true
    }'::jsonb
  );

  -- Activity 2: Arms
  INSERT INTO activities (task_id, name, type, "order", prop_name, video_url, config) 
  VALUES (
    v_task1_id, 'Activity 2: Arms', 'activity', 2, 'Big Doll', '/guide.mp4',
    '{
      "prompts": ["Point your finger at the doll''s arms", "Point your finger at the doll''s left arm", "Point your finger at the doll''s right arm", "Point your finger at the doll''s hands", "Point your finger at the doll''s fingers"],
      "targetWords": ["arms", "arm", "hands", "hand", "fingers"],
      "pronunciationPrompts": ["This is arms", "This is arm", "This is hands", "This is hand", "This is fingers"],
      "propName": "Big Doll",
      "videoUrl": "/guide.mp4",
      "requireFingerPointing": true
    }'::jsonb
  );

  -- Activity 3: Legs
  INSERT INTO activities (task_id, name, type, "order", prop_name, video_url, config) 
  VALUES (
    v_task1_id, 'Activity 3: Legs', 'activity', 3, 'Big Doll', '/guide.mp4',
    '{
      "prompts": ["Point your finger at the doll''s legs", "Point your finger at the doll''s left leg", "Point your finger at the doll''s right leg", "Point your finger at the doll''s feet", "Point your finger at the doll''s toes"],
      "targetWords": ["legs", "leg", "feet", "foot", "toes"],
      "pronunciationPrompts": ["This is legs", "This is leg", "This is feet", "This is foot", "This is toes"],
      "propName": "Big Doll",
      "videoUrl": "/guide.mp4",
      "requireFingerPointing": true
    }'::jsonb
  );

  -- Activity 4: Assessment - What is this? (body part in image)
  INSERT INTO activities (task_id, name, type, "order", prop_name, config) 
  VALUES (
    v_task1_id, 'Activity 4: Assessment', 'assessment', 4, NULL,
    '{
      "questionType": "body_part_image",
      "bodyParts": ["head", "shoulders", "knees", "toes", "eyes", "ears", "nose", "mouth", "arms", "hands", "legs", "feet"],
      "prompts": ["Point your finger at this body part and say what it is.", "What body part is this? Point at it with your finger."],
      "targetWords": ["head", "shoulders", "knees", "toes", "eyes", "ears", "nose", "mouth", "arms", "hands", "legs", "feet"],
      "randomOrder": true,
      "retryOnWrong": true,
      "callRandomRollNo": true,
      "requireFingerPointing": true
    }'::jsonb
  );

  -- ===========================================================================
  -- TASK 2: Body Movements
  -- ===========================================================================
  INSERT INTO tasks (unit_id, name, slug, "order") 
  VALUES (v_unit_id, 'Body Movements', 'body-movements', 2)
  RETURNING id INTO v_task2_id;

  -- Activity 1: Clapping
  INSERT INTO activities (task_id, name, type, "order", video_url, config) 
  VALUES (
    v_task2_id, 'Activity 1: Clapping', 'activity', 1, '/guide.mp4',
    '{
      "prompts": ["What is she doing?"],
      "targetWords": ["clapping", "she is clapping"],
      "pronunciationPrompts": ["She is clapping"],
      "movement": "clapping",
      "videoUrl": "/guide.mp4"
    }'::jsonb
  );

  -- Activity 2: Eating
  INSERT INTO activities (task_id, name, type, "order", video_url, config) 
  VALUES (
    v_task2_id, 'Activity 2: Eating', 'activity', 2, '/guide.mp4',
    '{
      "prompts": ["What is she doing?"],
      "targetWords": ["eating", "she is eating"],
      "pronunciationPrompts": ["She is eating"],
      "movement": "eating",
      "videoUrl": "/guide.mp4"
    }'::jsonb
  );

  -- Activity 3: Smelling
  INSERT INTO activities (task_id, name, type, "order", video_url, config) 
  VALUES (
    v_task2_id, 'Activity 3: Smelling', 'activity', 3, '/guide.mp4',
    '{
      "prompts": ["What is she doing?"],
      "targetWords": ["smelling", "she is smelling"],
      "pronunciationPrompts": ["She is smelling"],
      "movement": "smelling",
      "videoUrl": "/guide.mp4"
    }'::jsonb
  );

  -- Activity 4: Walking
  INSERT INTO activities (task_id, name, type, "order", video_url, config) 
  VALUES (
    v_task2_id, 'Activity 4: Walking', 'activity', 4, '/guide.mp4',
    '{
      "prompts": ["What is she doing?"],
      "targetWords": ["walking", "she is walking"],
      "pronunciationPrompts": ["She is walking"],
      "movement": "walking",
      "videoUrl": "/guide.mp4"
    }'::jsonb
  );

  -- Activity 5: Legs tapping
  INSERT INTO activities (task_id, name, type, "order", video_url, config) 
  VALUES (
    v_task2_id, 'Activity 5: Legs Tapping', 'activity', 5, '/guide.mp4',
    '{
      "prompts": ["What is she doing?"],
      "targetWords": ["tapping", "legs tapping", "she is tapping her legs"],
      "pronunciationPrompts": ["She is tapping her legs"],
      "movement": "legs tapping",
      "videoUrl": "/guide.mp4"
    }'::jsonb
  );

  -- Activity 6: Eyes movement
  INSERT INTO activities (task_id, name, type, "order", video_url, config) 
  VALUES (
    v_task2_id, 'Activity 6: Eyes Movement', 'activity', 6, '/guide.mp4',
    '{
      "prompts": ["What is she doing?"],
      "targetWords": ["blinking", "moving eyes", "she is moving her eyes"],
      "pronunciationPrompts": ["She is moving her eyes"],
      "movement": "eyes movement",
      "videoUrl": "/guide.mp4"
    }'::jsonb
  );

  -- Activity 7: Head movement
  INSERT INTO activities (task_id, name, type, "order", video_url, config) 
  VALUES (
    v_task2_id, 'Activity 7: Head Movement', 'activity', 7, '/guide.mp4',
    '{
      "prompts": ["What is she doing?"],
      "targetWords": ["nodding", "moving head", "she is moving her head"],
      "pronunciationPrompts": ["She is moving her head"],
      "movement": "head movement",
      "videoUrl": "/guide.mp4"
    }'::jsonb
  );

  -- Activity 8: Hearing
  INSERT INTO activities (task_id, name, type, "order", video_url, config) 
  VALUES (
    v_task2_id, 'Activity 8: Hearing', 'activity', 8, '/guide.mp4',
    '{
      "prompts": ["What is she doing?"],
      "targetWords": ["hearing", "listening", "she is hearing", "she is listening"],
      "pronunciationPrompts": ["She is hearing"],
      "movement": "hearing",
      "videoUrl": "/guide.mp4"
    }'::jsonb
  );

  -- ===========================================================================
  -- TASK 3: Letter Alphabets
  -- ===========================================================================
  INSERT INTO tasks (unit_id, name, slug, "order") 
  VALUES (v_unit_id, 'Letter Alphabets', 'letter-alphabets', 3)
  RETURNING id INTO v_task3_id;

  -- Activity 1: Letter A
  INSERT INTO activities (task_id, name, type, "order", prop_name, config) 
  VALUES (
    v_task3_id, 'Activity 1: Letter A', 'activity', 1, 'Objects with letter A',
    '{
      "letter": "A",
      "prompts": ["Show me an object that starts with letter A", "What is it?"],
      "targetWords": ["apple", "ant", "axe", "angel", "aim"],
      "pronunciationPrompts": ["It is an apple", "It is an ant", "It is an axe", "It is an angel"],
      "propName": "Objects with letter A",
      "videoUrl": "/guide.mp4"
    }'::jsonb
  );

  -- Activity 2: Letter E
  INSERT INTO activities (task_id, name, type, "order", prop_name, config) 
  VALUES (
    v_task3_id, 'Activity 2: Letter E', 'activity', 2, 'Objects with letter E',
    '{
      "letter": "E",
      "prompts": ["Show me an object that starts with letter E", "What is it?"],
      "targetWords": ["elephant", "egg", "ear", "eye", "eraser"],
      "pronunciationPrompts": ["It is an elephant", "It is an egg", "It is an ear", "It is an eye", "It is an eraser"],
      "propName": "Objects with letter E",
      "videoUrl": "/guide.mp4"
    }'::jsonb
  );

  -- Activity 3: Letter B (extra)
  INSERT INTO activities (task_id, name, type, "order", prop_name, config) 
  VALUES (
    v_task3_id, 'Activity 3: Letter B', 'activity', 3, 'Objects with letter B',
    '{
      "letter": "B",
      "prompts": ["Show me an object that starts with letter B", "What is it?"],
      "targetWords": ["ball", "book", "banana", "bat", "box"],
      "pronunciationPrompts": ["It is a ball", "It is a book", "It is a banana", "It is a bat", "It is a box"],
      "propName": "Objects with letter B",
      "videoUrl": "/guide.mp4"
    }'::jsonb
  );

  RAISE NOTICE 'Unit 1 seeded successfully. Unit ID: %', v_unit_id;
END $$;

