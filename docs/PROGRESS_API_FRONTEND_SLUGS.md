# Progress API — Frontend (Slug-Based)

**Backend holds only progress.** All unit/activity definitions live on the frontend.

**Base URL:** `http://localhost:8080` (local)

**Auth:** `Authorization: Bearer <cognito_id_token>`

---

## Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/progress/units` | Get all progress (by unit_slug) |
| GET | `/progress/units/{unitSlug}` | Get progress for one unit |
| POST | `/progress/units/{unitSlug}/activities/{activitySlug}/complete` | Mark activity complete |

---

## 1. Get All Progress

**GET** `/progress/units`

**Response (200):**
```json
[
  {
    "unitId": "english-unit-1",
    "completedActivities": [
      {
        "activityId": "body-parts-1",
        "taskId": null,
        "completedAt": "2026-03-05T10:30:00Z",
        "metadata": null
      },
      {
        "activityId": "body-parts-2",
        "taskId": null,
        "completedAt": "2026-03-05T10:32:00Z",
        "metadata": null
      }
    ],
    "completedCount": 2
  },
  {
    "unitId": "maths-unit-1",
    "completedActivities": [],
    "completedCount": 0
  }
]
```

**Activity slug format:** `{task_slug}-{order}` e.g. `body-parts-1`, `round-long-2`

---

## 2. Get Unit Progress

**GET** `/progress/units/{unitSlug}`

**Example:** `GET /progress/units/english-unit-1`

**Response (200):**
```json
{
  "unitId": "english-unit-1",
  "completedActivities": [
    {
      "activityId": "body-parts-1",
      "taskId": null,
      "completedAt": "2026-03-05T10:30:00Z",
      "metadata": null
    }
  ],
  "completedCount": 1
}
```

---

## 3. Complete Activity

**POST** `/progress/units/{unitSlug}/activities/{activitySlug}/complete`

**Example:** `POST /progress/units/english-unit-1/activities/body-parts-1/complete`

**Body (optional):**
```json
{
  "metadata": { "correct": true }
}
```

**Response (201):**
```json
{
  "activityId": "body-parts-1",
  "completedAt": "2026-03-05T10:30:00Z",
  "metadata": { "correct": true }
}
```

---

## Frontend Usage

1. **Units data** — Store as static JSON (from export). Each activity has `slug: task_slug + "-" + order`.
2. **Progress** — Call `GET /progress/units` on load. Merge with your static units to show completed state.
3. **On activity complete** — Call `POST /progress/units/{unitSlug}/activities/{activitySlug}/complete`.
4. **Progress %** — `completedCount / totalActivities` (total from your static data).
