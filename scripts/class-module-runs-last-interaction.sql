-- Last interaction hint for class module runs (GET /status).
-- Safe to re-run.

ALTER TABLE class_module_runs ADD COLUMN IF NOT EXISTS last_roll_number VARCHAR(100);
ALTER TABLE class_module_runs ADD COLUMN IF NOT EXISTS last_activity_id VARCHAR(150);
ALTER TABLE class_module_runs ADD COLUMN IF NOT EXISTS last_section_id VARCHAR(150);
ALTER TABLE class_module_runs ADD COLUMN IF NOT EXISTS last_interaction_at TIMESTAMPTZ;
