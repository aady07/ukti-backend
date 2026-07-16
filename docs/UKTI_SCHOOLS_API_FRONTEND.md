# UKTI in Schools — API Changes for Frontend

**Base URL:** `http://localhost:8080` (dev) / your deployed URL (prod)

This document describes the backend changes for UKTI in Schools. Use it to update the frontend for signup type selection, school admin dashboard, and roll-number-based progress tracking.

---

## Summary of Changes

| Area | Change |
|------|--------|
| **Signup** | `POST /users` supports `signupType: "organization"` + `organizationName` for school admin |
| **Current user** | `GET /users/me` returns `userType`, `schoolUuid`, `schoolName` |
| **School admin** | New endpoints: list/create classes, add/list students |
| **Progress** | All progress APIs accept `X-Roll-Number` and `X-Class-Id` headers for school mode |

---

## 1. Signup Flow

### 1.1 Individual Signup (unchanged)

```
POST /users
Authorization: Bearer <cognito_id_token>
Content-Type: application/json

{
  "displayName": "John Doe"
}
```

If no body or `signupType` omitted → individual user.

### 1.2 School (Organization) Signup

**When:** User selects "School" signup and enters school name.

**Request:**
```
POST /users
Authorization: Bearer <cognito_id_token>
Content-Type: application/json

{
  "displayName": "School Admin Name",
  "signupType": "organization",
  "organizationName": "ABC School"
}
```

**Response (201 Created):**
```json
{
  "id": "uuid",
  "cognitoSub": "...",
  "email": "admin@school.edu",
  "phone": "+919876543210",
  "username": "admin@school.edu",
  "displayName": "School Admin Name",
  "schoolId": null,
  "userType": "school_admin",
  "schoolUuid": "uuid-of-school",
  "schoolName": "ABC School",
  "createdAt": "2026-03-12T...",
  "updatedAt": "2026-03-12T..."
}
```

**Frontend:** After success, redirect to School Admin Dashboard. Use `schoolUuid` for all school APIs.

---

## 2. Get Current User — Extended Response

**GET** `/users/me`

**Headers:** `Authorization: Bearer <cognito_id_token>` (or `X-Cognito-Sub` for dev)

**Response (200 OK):**
```json
{
  "id": "uuid",
  "cognitoSub": "...",
  "email": "user@example.com",
  "phone": "+919876543210",
  "username": "johndoe",
  "displayName": "John Doe",
  "schoolId": null,
  "userType": "individual",
  "schoolUuid": null,
  "schoolName": null,
  "createdAt": "...",
  "updatedAt": "..."
}
```

**New fields:**

| Field | Type | Description |
|-------|------|-------------|
| `userType` | string | `"individual"` \| `"school_admin"` \| `"student"` |
| `schoolUuid` | string \| null | School UUID for `school_admin` and `student` |
| `schoolName` | string \| null | School name for `school_admin` |

**Frontend logic:**
- If `userType === "school_admin"` and `schoolUuid` → show School Admin Dashboard
- Use `schoolUuid` for school API calls

---

## 3. School Admin API

All school endpoints require `Authorization: Bearer <cognito_id_token>` and the caller must be a `school_admin` of that school.

Base path: `/schools/{schoolId}` — use `schoolUuid` from `/users/me`.

### 3.1 List Classes

**GET** `/schools/{schoolId}/classes`

**Response (200 OK):**
```json
[
  { "id": "uuid", "name": "Class 1", "studentCount": 25 },
  { "id": "uuid", "name": "Class 2", "studentCount": 30 }
]
```

### 3.2 Create Class

**POST** `/schools/{schoolId}/classes`

**Request:**
```json
{
  "name": "Class 1"
}
```

**Response (201 Created):**
```json
{
  "id": "uuid",
  "name": "Class 1",
  "studentCount": 0
}
```

**Error (400):** Class name already exists.

### 3.3 Add Students (Bulk)

**POST** `/schools/{schoolId}/classes/{classId}/students`

**Request:**
```json
{
  "students": [
    { "rollNumber": "1", "name": "Student One" },
    { "rollNumber": "2", "name": "Student Two" }
  ]
}
```

**Response (201 Created):**
```json
{
  "created": [
    { "id": "uuid", "rollNumber": "1", "name": "Student One" },
    { "id": "uuid", "rollNumber": "2", "name": "Student Two" }
  ]
}
```

**Notes:**
- Roll numbers must be unique within a class
- Duplicate roll numbers in the same class are skipped (no error)

### 3.4 List Students in Class

**GET** `/schools/{schoolId}/classes/{classId}/students`

**Response (200 OK):**
```json
[
  { "id": "uuid", "rollNumber": "1", "name": "Student One" },
  { "id": "uuid", "rollNumber": "2", "name": "Student Two" }
]
```

### 3.5 Progress Overview (Bulk — Recommended)

**GET** `/schools/{schoolId}/progress/overview?totalActivities=45`

Returns admin's progress + all students' progress by class in **one call**. Use for the School Progress view (`/admin/progress`) to avoid N+1 requests.

**Response (200 OK):**
```json
{
  "adminProgress": {
    "completedCount": 12,
    "totalCount": 45,
    "percent": 27,
    "units": [{ "unitId": "english-unit-1", "completedCount": 5, "totalCount": 0 }]
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

See `docs/SCHOOL_PROGRESS_OVERVIEW_API.md` for full details.

---

## 4. Progress API — Roll Number Context (School Mode)

When a **school admin** is logged in on a tablet and a **student** is doing an activity, the frontend must pass the student's roll number so progress is stored for that student.

### 4.1 Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes | `Bearer <cognito_id_token>` (teacher's token) |
| `X-Roll-Number` | Yes* | Student's roll number in school mode |
| `X-Class-Id` | No | Class UUID; use when roll numbers repeat across classes |

*Required only when in school mode (teacher logged in, student doing activity).

### 4.2 Affected Endpoints

All progress endpoints support `X-Roll-Number` and `X-Class-Id`:

| Method | Endpoint |
|--------|----------|
| GET | `/progress/units` |
| GET | `/progress/units/{unitSlug}` |
| POST | `/progress/units/{unitSlug}/activities/{activitySlug}/complete` |
| GET | `/progress/module/{moduleId}` |
| POST | `/progress/module/{moduleId}/complete` |
| GET | `/progress/user` |
| POST | `/progress/task` |
| PUT | `/progress/experiential/{moduleId}` |

### 4.3 Behavior

1. **Without `X-Roll-Number`:** Progress is stored for the authenticated user (current behavior).
2. **With `X-Roll-Number`:** Backend resolves the student by `(schoolId, classId, rollNumber)` and stores progress for that student. The authenticated user must be a `school_admin` of that school.

### 4.4 Example (School Mode)

```typescript
// Teacher logged in; student with roll number "5" in class "class-uuid" does an activity
await fetch(`http://localhost:8080/progress/units/unit-1/activities/activity-1/complete`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${teacherIdToken}`,
    'X-Roll-Number': '5',
    'X-Class-Id': 'class-uuid',  // optional if only one class has roll 5
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ metadata: {} })
});
```

### 4.5 Error: Student Not Found

When `X-Roll-Number` is sent but the student is not found:

**Response (400 Bad Request):**
```json
{
  "error": "STUDENT_NOT_FOUND",
  "message": "Student not found for roll number"
}
```

---

## 5. Auth Flow (Unchanged)

- Cognito handles signup/login
- Frontend sends Cognito JWT in `Authorization: Bearer <idToken>`
- `X-Cognito-Sub` still supported for dev

---

## 6. Error Responses

Format (unchanged):
```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable message"
}
```

| HTTP | Error | When |
|------|-------|------|
| 400 | BAD_REQUEST | Validation, duplicate class name |
| 400 | STUDENT_NOT_FOUND | X-Roll-Number sent but student not found |
| 401 | UNAUTHORIZED | Missing/invalid JWT |
| 403 | FORBIDDEN | School admin accessing another school |
| 404 | NOT_FOUND | User/resource not found |
| 500 | - | Server error |

---

## 7. Frontend Implementation Checklist

- [ ] Signup: Add "User" vs "School" option; for School, collect school name and send `signupType`, `organizationName`
- [ ] After school signup: Redirect to School Admin Dashboard
- [ ] Dashboard: Use `schoolUuid` from `/users/me` for school API calls
- [ ] Classes: List, create; use class `id` when adding students
- [ ] Students: Bulk add (roll number + name), list by class
- [ ] Activity screens (school mode): Show roll number input; pass `X-Roll-Number` and `X-Class-Id` on all progress API calls
- [ ] Route logic: If `userType === "school_admin"` → show admin dashboard; else individual flow

---

## 8. CORS

Allowed origins: `http://localhost:3000`, `http://localhost:5173`, `http://localhost:5172`, `https://miraista.com`, `http://miraista.com`, `https://www.miraista.com`, `http://www.miraista.com`, `https://education.miraista.com`, `http://education.miraista.com`, `https://educationuat.miraista.com`, `http://educationuat.miraista.com`, `https://ukti.example.com`
