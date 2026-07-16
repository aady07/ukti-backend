-- Client-sync blob for in-class station flow (multi-device resume). Safe to re-run.

ALTER TABLE class_module_runs
    ADD COLUMN IF NOT EXISTS class_runtime_state_json JSONB NULL;

COMMENT ON COLUMN class_module_runs.class_runtime_state_json IS
    'Optional JSON: { stationFlowV1: { v, rowIndex, challengeIndex, rollsDoneForStep, updatedAt, classId?, moduleId? }, ... }';
