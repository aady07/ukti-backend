# Backend Contract v2: Curriculum Catalog

## Goal

PostgreSQL is the source of truth for curriculum **definitions**. React activity components remain frontend-owned. One JSON catalog shape serves web, mobile (future), class runs, and progress totals.

## API

Base path: `/v1/curriculum` (public read, no auth)

### GET /catalog

Returns full catalog or one class level.

Query params:
- `classLevel` (optional): e.g. `grade4`

Response shape (matches `ukti/scripts/export-mobile-catalog.ts` output):

```json
{
  "version": 1,
  "generatedAt": "2026-07-13T…",
  "cdnOrigin": "https://d1194rs9ausm91.cloudfront.net",
  "classLevels": [
    {
      "id": "grade4",
      "label": "Grade 4",
      "modules": [
        {
          "id": "grade4-module-2",
          "weekLabel": "Week 2",
          "title": "…",
          "summary": "…",
          "sections": [
            {
              "id": "…",
              "title": "…",
              "activities": [
                {
                  "id": "…",
                  "title": "…",
                  "engine": "fill_blank_drag",
                  "questions": []
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

Cache: `Cache-Control: public, max-age=300`

### GET /modules/{moduleId}

Returns full `curriculum_modules.payload` including orchestration metadata and per-module pool rules:

```json
{
  "moduleId": "grade4-module-2",
  "classLevel": "grade4",
  "catalog": { "…module slice…" },
  "orchestration": {
    "sectionIds": ["section-1", "section-2"],
    "sectionActivities": {
      "section-1": ["g4-m2-activity-slug", "…"]
    }
  },
  "trackable": {
    "activityIds": ["…"],
    "pairingByActivityId": { "playgroup-letter-1": "paired_show_say" }
  },
  "poolRules": { "g4-m2-sec-letters": { "id": "…", "order": ["…"] } },
  "poolOrder": ["g4-m2-sec-letters", "…"],
  "businessSectionGroups": [{ "id": "section-1", "serviceSectionIds": ["…"] }]
}
```

## Database

Tables (see `scripts/curriculum-catalog-schema.sql`):

- `curriculum_modules` — one row per module, agent upsert target
- `curriculum_releases` — published snapshot per class level (read API source)

Seed: `npx tsx scripts/seed-curriculum-from-ts.ts` (from current TS until agents write JSON)

## Run orchestration (server authority)

`POST /v1/class-modules/runs/start`:

- `sectionIds` and `sectionActivities` are **optional** when module exists in catalog
- Server derives both from `curriculum_modules.payload.orchestration`
- Client-provided values kept only when catalog row missing; mismatch logs warning and catalog wins

Existing runs with missing `section_activities_json` are backfilled from catalog on reuse or resolve-next.

## Progress totals

`GET /schools/…/modules/progress` and student variant:

- `totalCount` prefers catalog `trackable.activityIds.length` per module
- Falls back to v1 inferred totals when catalog row missing

Pairing rules unchanged from v1 contract (`paired_show_say`, `say_only`, `single`).

## Module ID contract (unchanged from v1)

- Format: `grade{N}-module-{M}`, `nursery-module-1`, etc.
- IDs immutable after release
- Breaking changes: new id suffix `-v2`

## Agent pipeline (Phase 4)

Assembler writes `docs/curriculum-intake/exports/{moduleId}.json`, then:

```bash
npx tsx scripts/seed-curriculum-module.ts --module grade4-module-2
npx tsx scripts/verify-catalog-parity.ts --classLevel grade4
```

## Deferred

- Admin write API / CMS UI
- Mobile live API swap
- Deleting frontend `CLASS_MODULES_DATA` monolith
