# Units Data Export & Progress-Only Backend

## Step 1: Export all units/activities data (MacBook local)

```bash
psql -h localhost -U aady -d ukti_db
```

**Single query — copy result to frontend JSON file:**

```sql
SELECT json_build_object(
  'units', (
    SELECT json_agg(
      json_build_object(
        'slug', u.slug,
        'name', u.name,
        'description', u.description,
        'moduleType', u.module_type,
        'subject', u.subject,
        'order', u."order",
        'videoUrl', u.video_url,
        'tasks', (
          SELECT json_agg(
            json_build_object(
              'slug', t.slug,
              'name', t.name,
              'order', t."order",
              'activities', (
                SELECT json_agg(
                  json_build_object(
                    'slug', t.slug || '-' || a."order",
                    'name', a.name,
                    'type', a.type,
                    'order', a."order",
                    'propName', a.prop_name,
                    'videoUrl', a.video_url,
                    'config', a.config
                  ) ORDER BY a."order"
                )
                FROM activities a WHERE a.task_id = t.id
              )
            ) ORDER BY t."order"
          )
          FROM tasks t WHERE t.unit_id = u.id
        )
      ) ORDER BY u."order"
    )
    FROM units u
  )
) AS units_json;
```

**Save to file:**
```bash
psql -h localhost -U aady -d ukti_db -t -A -c "SELECT json_build_object(...)" > units-data.json
```

---

## Step 2: Export existing progress (for migration)

```sql
-- Map unit_progress to (user_id, unit_slug, activity_slug) for migration
SELECT 
  up.user_id,
  u.slug AS unit_slug,
  t.slug || '-' || a."order" AS activity_slug,
  up.completed_at,
  up.metadata
FROM unit_progress up
JOIN units u ON up.unit_id = u.id
JOIN tasks t ON up.task_id = t.id
JOIN activities a ON up.activity_id = a.id
ORDER BY up.completed_at DESC;
```

---

## Step 3: MacBook local - Run migration (remove units data, keep progress only)

```bash
psql -h localhost -U aady -d ukti_db -f scripts/migrate-to-progress-only.sql
```

---

## Step 4: New API for frontend

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/progress/units` | Get all progress (unit_slug, completed activity_slugs, counts) |
| GET | `/progress/units/{unitSlug}` | Get progress for one unit |
| POST | `/progress/units/{unitSlug}/activities/{activitySlug}/complete` | Mark activity complete |

**Progress uses slugs now** (unit_slug, activity_slug) - no UUIDs. Frontend holds all unit/activity definitions.
