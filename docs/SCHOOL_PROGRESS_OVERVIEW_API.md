# School Progress Overview — API for Frontend

**Purpose**: Single endpoint to load the full School Progress view (`/admin/progress`) — admin's progress + all students' progress by class — in **one request** instead of N+1.

---

## Endpoint

**GET** `/schools/{schoolId}/progress/overview`

**Auth**: `Authorization: Bearer <school_admin JWT>`

**Query param** (optional): `totalActivities` — Total activities count from your static data (e.g. from `getAllUnitsData()`). Used to compute `percent`. If omitted, `percent` = 0.

**Example**:
```
GET /schools/33721d99-2091-43bf-a7ec-a437317206ce/progress/overview?totalActivities=45
Authorization: Bearer eyJraWQ...
```

---

## Response (200 OK)

```json
{
  "adminProgress": {
    "completedCount": 12,
    "totalCount": 45,
    "percent": 27,
    "units": [
      { "unitId": "english-unit-1", "completedCount": 5, "totalCount": 0 },
      { "unitId": "maths-unit-1", "completedCount": 7, "totalCount": 0 }
    ]
  },
  "classes": [
    {
      "classId": "uuid",
      "className": "1A",
      "students": [
        {
          "studentId": "uuid",
          "rollNumber": "1",
          "name": "Rahul Kumar",
          "completedCount": 8,
          "totalCount": 45,
          "percent": 18
        }
      ]
    }
  ]
}
```

---

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `adminProgress` | object | School admin's own progress |
| `adminProgress.completedCount` | int | Total completed activities |
| `adminProgress.totalCount` | int | From `totalActivities` query param (or 0) |
| `adminProgress.percent` | int | `completedCount / totalCount * 100` (0 if totalCount=0) |
| `adminProgress.units` | array | Per-unit breakdown (unitId, completedCount) |
| `classes` | array | Classes with students and their progress |
| `classes[].classId` | string | Class UUID |
| `classes[].className` | string | Class name (e.g. "1A") |
| `classes[].students` | array | Students in this class |
| `students[].studentId` | string | Student user UUID |
| `students[].rollNumber` | string | Roll number |
| `students[].name` | string | Display name |
| `students[].completedCount` | int | Completed activities |
| `students[].totalCount` | int | From query param |
| `students[].percent` | int | Completion percent |

---

## Frontend Usage

1. **On `/admin/progress` load**: Call `GET /schools/{schoolUuid}/progress/overview?totalActivities=45` (use your `getAllUnitsData()` total).
2. **Render**: Use `adminProgress` for "You first", then `classes` for "Students by class".
3. **No N+1**: One request replaces 1 + classes + (students per class) requests.

---

## Fallback (Current Approach)

If you prefer to keep the per-student approach:
- `GET /progress/units` with `X-Roll-Number` and `X-Class-Id` for each student
- Works for small schools; use overview endpoint for scale.
