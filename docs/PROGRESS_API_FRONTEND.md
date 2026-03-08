# Ukti Progress API — Frontend Integration Guide

**Base URL:** `http://localhost:8080` (dev) / your deployed URL (prod)

**Auth:** All progress endpoints require `Authorization: Bearer <cognito_id_token>`. User must exist in our DB (call POST /users first).

---

## Endpoints Overview

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/progress/module/{moduleId}/complete` | Mark a module as complete |
| GET | `/progress/module/{moduleId}` | Get progress for a specific module |
| GET | `/progress/user` | Get all progress for the current user |
| POST | `/progress/task` | Record task completion (word, challenge, video, etc.) |
| PUT | `/progress/experiential/{moduleId}` | Update experiential stats (e.g. questions attempted) |

---

## 1. Mark Module Complete

**POST** `/progress/module/{moduleId}/complete`

**Headers:** `Authorization: Bearer <cognito_id_token>`

**Path param:** `moduleId` — e.g. `"1"`, `"primary-learning-1"`, `"ecam-practice"`

**Response (201 Created):**
```json
{
  "moduleId": "1",
  "completed": true,
  "completedAt": "2026-02-21T10:30:00Z",
  "tasks": [
    {
      "taskId": "word-a",
      "taskType": "word",
      "completedAt": "2026-02-21T10:25:00Z",
      "metadata": null
    }
  ]
}
```

**Notes:** Idempotent — if already complete, returns existing data.

---

## 2. Get Module Progress

**GET** `/progress/module/{moduleId}`

**Headers:** `Authorization: Bearer <cognito_id_token>`

**Response (200 OK):**
```json
{
  "moduleId": "1",
  "completed": false,
  "completedAt": null,
  "tasks": [
    {
      "taskId": "word-a",
      "taskType": "word",
      "completedAt": "2026-02-21T10:25:00Z",
      "metadata": null
    },
    {
      "taskId": "challenge-1",
      "taskType": "challenge",
      "completedAt": "2026-02-21T10:28:00Z",
      "metadata": {"score": 85}
    }
  ]
}
```

**Response (404):** Module not found (rare — we return empty progress for new modules).

---

## 3. Get All User Progress

**GET** `/progress/user`

**Headers:** `Authorization: Bearer <cognito_id_token>`

**Response (200 OK):**
```json
{
  "modules": [
    {
      "moduleId": "1",
      "completedAt": "2026-02-21T10:30:00Z"
    },
    {
      "moduleId": "2",
      "completedAt": "2026-02-21T11:00:00Z"
    }
  ],
  "tasks": [
    {
      "moduleId": "1",
      "taskId": "word-a",
      "taskType": "word",
      "completedAt": "2026-02-21T10:25:00Z",
      "metadata": null
    },
    {
      "moduleId": "1",
      "taskId": "challenge-1",
      "taskType": "challenge",
      "completedAt": "2026-02-21T10:28:00Z",
      "metadata": {"score": 85}
    }
  ],
  "experientialStats": [
    {
      "moduleId": "ecam-practice",
      "statType": "questions_attempted",
      "count": 42
    },
    {
      "moduleId": "ecam-practice",
      "statType": "challenges_completed",
      "count": 5
    }
  ]
}
```

**Use case:** Dashboard, progress overview, sync on app load.

---

## 4. Record Task Completion

**POST** `/progress/task`

**Headers:** `Authorization: Bearer <cognito_id_token>`, `Content-Type: application/json`

**Request Body:**
```json
{
  "moduleId": "1",
  "taskId": "word-a",
  "taskType": "word",
  "metadata": null
}
```

| Field | Required | Description |
|-------|----------|-------------|
| moduleId | Yes | Module ID (e.g. `"1"`, `"primary-learning-1"`) |
| taskId | Yes | Task ID (e.g. `"word-a"`, `"challenge-1"`, `"video-chapter-1"`) |
| taskType | No | `"word"`, `"challenge"`, `"exercise"`, `"video_chapter"` |
| metadata | No | Extra data (e.g. `{"score": 85}`) |

**Response (201 Created):**
```json
{
  "taskId": "word-a",
  "completedAt": "2026-02-21T10:25:00Z"
}
```

**Notes:** Idempotent — if task already completed, updates metadata and returns.

---

## 5. Update Experiential Stats

**PUT** `/progress/experiential/{moduleId}`

**Headers:** `Authorization: Bearer <cognito_id_token>`, `Content-Type: application/json`

**Path param:** `moduleId` — e.g. `"ecam-practice"`, `"physical-training-1"`

**Request Body:**
```json
{
  "statType": "questions_attempted",
  "increment": 1
}
```

| Field | Required | Description |
|-------|----------|-------------|
| statType | Yes | e.g. `"questions_attempted"`, `"challenges_completed"` |
| increment | No | Amount to add (default: 1) |

**Response (200 OK):**
```json
{
  "statType": "questions_attempted",
  "count": 43
}
```

**Use case:** Ecam Practice (questions attempted), challenges completed, etc.

---

## ID Conventions (Frontend-Defined)

| Type | Example IDs |
|------|-------------|
| moduleId | `"1"`, `"2"`, `"primary-learning-1"`, `"ecam-practice"`, `"physical-training-1"` |
| taskId | `"word-a"`, `"challenge-1"`, `"video-chapter-1"`, `"exercise-open-posture"` |
| taskType | `"word"`, `"challenge"`, `"exercise"`, `"video_chapter"` |
| statType | `"questions_attempted"`, `"challenges_completed"` |

---

## Error Responses

| HTTP | When |
|------|------|
| 400 | Bad request (validation) |
| 401 | Missing or invalid Cognito JWT; user not in DB |
| 404 | Resource not found |
| 500 | Server error |

Format:
```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable message"
}
```

---

## Frontend Flow Examples

### On module completion
```typescript
// User finishes last step of module
await fetch(`http://localhost:8080/progress/module/1/complete`, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${idToken}` }
});
```

### On task/step completion
```typescript
// User completes a word, challenge, or video chapter
await fetch('http://localhost:8080/progress/task', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${idToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    moduleId: '1',
    taskId: 'word-a',
    taskType: 'word'
  })
});
```

### On Ecam Practice question attempt
```typescript
// User attempts a question
await fetch(`http://localhost:8080/progress/experiential/ecam-practice`, {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${idToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    statType: 'questions_attempted',
    increment: 1
  })
});
```

### On app load (dashboard)
```typescript
const res = await fetch('http://localhost:8080/progress/user', {
  headers: { 'Authorization': `Bearer ${idToken}` }
});
const { modules, tasks, experientialStats } = await res.json();
```

---

## CORS

Allowed origins: `http://localhost:3000`, `http://localhost:5173`, `https://ukti.example.com`
