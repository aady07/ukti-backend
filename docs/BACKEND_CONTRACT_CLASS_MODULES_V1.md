# Backend Contract v1: Class Modules Progress

## Goal

Ship module-level progress with minimal backend changes and no breaking impact:

- Keep existing activity completion APIs.
- Support module-first frontend UI.
- Avoid large schema redesign in v1.
- Keep a clear migration path to backend-owned curriculum in v2.

## Frontend Handoff (Required Before Backend Rollout)

Frontend must publish and freeze:

1. **Module ID contract**
   - `moduleId` format (example: `nursery-module-1`)
   - IDs are immutable after release
   - Versioning rule for breaking curriculum updates (example: `-v2`)

2. **Class level enum**
   - `nursery`
   - `lkg`
   - `ukg`
   - `grade1`
   - `grade2`

3. **Module composition map**
   - `classLevel -> moduleId -> moduleItemId -> activityId[]`

4. **Pairing type map**
   - which activity keys are `paired_show_say`
   - which are `say_only`
   - which are `single`

5. **Metadata send policy**
   - whether metadata is sent for all new completions
   - or only module-flow completions

6. **Legacy data policy**
   - how to handle older progress rows without module metadata

## Backend MVP Scope

1. Completion write continues to work as-is.
2. Completion metadata accepts module fields (optional):
   - `metadata.moduleId`
   - `metadata.classLevel`
   - `metadata.moduleItemId`
3. Add student module progress endpoint.
4. Add class aggregate module progress endpoint.
5. Module definitions remain frontend-owned for v1.

## Data Model Strategy

### Option A (recommended for v1): metadata JSON only

Reuse `user_activity_progress.metadata` JSONB:

- no migration required for MVP
- no impact to existing clients

### Option B (later): dedicated columns

If needed, add nullable columns:

- `module_id`
- `class_level`
- `module_item_id`

## API Contract

### A) Existing completion write (extended, backward-compatible)

Request sample:

```json
{
  "activityId": "playgroup-letter-1-show",
  "metadata": {
    "moduleId": "ukg-module-1",
    "classLevel": "ukg",
    "moduleItemId": "letters-a-f"
  }
}
```

### B) Student module progress

`GET /schools/:schoolId/classes/:classId/students/:rollNumber/modules/progress`

Response sample:

```json
{
  "classId": "cls_123",
  "classLevel": "nursery",
  "rollNumber": "1",
  "modules": [
    {
      "moduleId": "nursery-module-1",
      "completedCount": 8,
      "totalCount": 20,
      "percent": 40
    }
  ]
}
```

### C) Class module aggregate progress

`GET /schools/:schoolId/classes/:classId/modules/progress`

Response sample:

```json
{
  "classId": "cls_123",
  "classLevel": "nursery",
  "studentsTracked": 22,
  "modules": [
    {
      "moduleId": "nursery-module-1",
      "avgPercent": 54,
      "studentsWithData": 18
    }
  ]
}
```

## Completion Evaluation Rules

- `paired_show_say`: complete only when both `-show` and `-say` are present
- `say_only`: complete when base row or `-say` is present
- `single`: complete when base row is present
- dedupe by logical activity key (`moduleItemId` if provided, else base activity slug)

## Backend Decisions (Explicit Defaults for v1)

- **Total count source**: inferred from class-level observed logical items per module
- **Duplicate policy**: dedupe by logical activity key
- **Pairing window**: no same-day restriction
- **Percent rounding**: integer `Math.round`
- **Empty states**:
  - `studentsTracked = 0` when class has no students
  - numeric fields return `0`, not `null`

## Legacy Rows (No Metadata)

- Requests do not fail.
- Only mappable rows contribute to module progress.
- If a module cannot be mapped from rows, it remains excluded in v1 response.

## Risks Avoided by Contract

- frontend/backend module mismatch causing percent drift
- inconsistent `show/say` interpretation
- regressions in non-module flows

## Rollout Sequence

1. Frontend publishes frozen v1 module + pairing maps.
2. Backend deploys metadata-compatible completion write (already backward-compatible).
3. Backend deploys student module progress endpoint.
4. Frontend validates with one class + one student roll.
5. Backend deploys class aggregate endpoint.
6. Optional v2: backend-owned `module_activity_map` source of truth.
