-- Remove Task 4 (Numbers Training) from English Unit 1
-- Run: psql -d ukti_db -f scripts/remove-task4-numbers-training.sql
DELETE FROM tasks WHERE slug = 'numbers-training' AND unit_id IN (SELECT id FROM units WHERE slug = 'english-unit-1');
