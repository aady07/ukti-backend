-- Ordered activity slugs per business section (curriculum) for resolve-next.
ALTER TABLE class_module_runs ADD COLUMN IF NOT EXISTS section_activities_json JSONB;
