# Group Activity API — Frontend Guide

**Auth:** `Authorization: Bearer <admin or teacher JWT>`

---

## Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/progress/group-activity/complete` | Store session result |
| GET | `/progress/group-activity/history?schoolId=&classId=` | List past sessions |
| GET | `/progress/group-activity/leaderboard?schoolId=&classId=` | Which groups won most often |

---

## 1. Complete Group Activity

**POST** `/progress/group-activity/complete`

**Body:**
```json
{
  "activityId": "gactivity1",
  "schoolId": "<uuid>",
  "classId": "<uuid>",
  "groups": { "1": ["1","2","3","4"], "2": ["5","6","7","8"] },
  "scores": { "1": 30, "2": 20 },
  "winnerGroup": 1,
  "sessionId": "<optional-uuid>"
}
```

**Response (201):**
```json
{
  "id": "<uuid>",
  "activityId": "gactivity1",
  "schoolId": "<uuid>",
  "classId": "<uuid>",
  "winnerGroup": 1,
  "createdAt": "2026-03-13T10:30:00Z"
}
```

---

## 2. History

**GET** `/progress/group-activity/history?schoolId=<uuid>&classId=<uuid>`

**Response (200):**
```json
{
  "sessions": [
    {
      "id": "<uuid>",
      "activityId": "gactivity1",
      "groups": { "1": ["1","2"], "2": ["3","4"] },
      "scores": { "1": 30, "2": 20 },
      "winnerGroup": 1,
      "createdAt": "2026-03-13T10:30:00Z"
    }
  ]
}
```

---

## 3. Leaderboard

**GET** `/progress/group-activity/leaderboard?schoolId=<uuid>&classId=<uuid>`

**Response (200):**
```json
{
  "leaderboard": [
    { "groupNumber": "1", "winCount": 5 },
    { "groupNumber": "2", "winCount": 2 }
  ]
}
```

---

## Notes

- Teacher: only sees history/leaderboard for classes they're assigned to.
- Admin: sees all classes in their school.
- `schoolId` and `classId` must be UUIDs and must match the auth token's school.
