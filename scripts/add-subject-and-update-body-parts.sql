-- =============================================================================
-- Migration: Add subject column to units + update Body Parts prompts
-- Run: psql -h <host> -U myappuser -d ukti_db -f scripts/add-subject-and-update-body-parts.sql
-- =============================================================================

-- 1. Add subject column (if not exists)
ALTER TABLE units ADD COLUMN IF NOT EXISTS subject VARCHAR(50);

-- 2. Set default for existing rows
UPDATE units SET subject = 'english' WHERE subject IS NULL;

-- 3. Make subject NOT NULL
ALTER TABLE units ALTER COLUMN subject SET NOT NULL;

-- 4. Update Body Parts activity prompts to require finger pointing
--    (Only updates activities in Body Parts task of english-unit-1)
UPDATE activities a
SET config = jsonb_set(
  jsonb_set(
    COALESCE(config, '{}'::jsonb),
    '{prompts}',
    '["Point your finger at the doll''s head", "Point your finger at the doll''s shoulders", "Point your finger at the doll''s knees", "Point your finger at the doll''s toes", "Point your finger at the doll''s eyes", "Point your finger at the doll''s ears", "Point your finger at the doll''s nose", "Point your finger at the doll''s mouth"]'::jsonb
  ),
  '{requireFingerPointing}',
  'true'::jsonb
)
WHERE a.task_id IN (
  SELECT t.id FROM tasks t
  JOIN units u ON t.unit_id = u.id
  WHERE u.slug = 'english-unit-1' AND t.slug = 'body-parts'
)
AND a.name = 'Activity 1: Face & Core';

UPDATE activities a
SET config = jsonb_set(
  jsonb_set(
    COALESCE(config, '{}'::jsonb),
    '{prompts}',
    '["Point your finger at the doll''s arms", "Point your finger at the doll''s left arm", "Point your finger at the doll''s right arm", "Point your finger at the doll''s hands", "Point your finger at the doll''s fingers"]'::jsonb
  ),
  '{requireFingerPointing}',
  'true'::jsonb
)
WHERE a.task_id IN (
  SELECT t.id FROM tasks t
  JOIN units u ON t.unit_id = u.id
  WHERE u.slug = 'english-unit-1' AND t.slug = 'body-parts'
)
AND a.name = 'Activity 2: Arms';

UPDATE activities a
SET config = jsonb_set(
  jsonb_set(
    COALESCE(config, '{}'::jsonb),
    '{prompts}',
    '["Point your finger at the doll''s legs", "Point your finger at the doll''s left leg", "Point your finger at the doll''s right leg", "Point your finger at the doll''s feet", "Point your finger at the doll''s toes"]'::jsonb
  ),
  '{requireFingerPointing}',
  'true'::jsonb
)
WHERE a.task_id IN (
  SELECT t.id FROM tasks t
  JOIN units u ON t.unit_id = u.id
  WHERE u.slug = 'english-unit-1' AND t.slug = 'body-parts'
)
AND a.name = 'Activity 3: Legs';

UPDATE activities a
SET config = jsonb_set(
  jsonb_set(
    COALESCE(config, '{}'::jsonb),
    '{prompts}',
    '["Point your finger at this body part and say what it is.", "What body part is this? Point at it with your finger."]'::jsonb
  ),
  '{requireFingerPointing}',
  'true'::jsonb
)
WHERE a.task_id IN (
  SELECT t.id FROM tasks t
  JOIN units u ON t.unit_id = u.id
  WHERE u.slug = 'english-unit-1' AND t.slug = 'body-parts'
)
AND a.name = 'Activity 4: Assessment';
