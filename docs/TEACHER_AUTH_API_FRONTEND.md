# Teacher Auth & School-Only Flow — API for Frontend

**Purpose**: Backend supports teacher auth (email/password), school-only signup, and teacher token for school/class/student APIs.

---

## 1. Database Migration

Run after `ukti-schools-migration.sql`:

```bash
psql -h <host> -U postgres -d ukti_db -f scripts/ukti-teachers-migration.sql
```

---

## 2. Add Teacher (Admin Only)

**POST** `/schools/{schoolId}/teachers`

**Auth**: `Authorization: Bearer <admin Cognito JWT>`

**Request**:
```json
{
  "email": "teacher@school.edu",
  "password": "initial-password",
  "name": "Teacher Name"
}
```

**Response (201)**:
```json
{
  "id": "uuid",
  "email": "teacher@school.edu",
  "name": "Teacher Name"
}
```

**Error (400)**: Email already exists in school.

---

## 3. List Teachers

**GET** `/schools/{schoolId}/teachers`

**Auth**: `Authorization: Bearer <admin or teacher JWT>`

**Response (200)**:
```json
[
  { "id": "uuid", "email": "teacher@school.edu", "name": "Teacher Name" }
]
```

---

## 4. Teacher Login

**POST** `/auth/teacher/login`

**Request**:
```json
{
  "email": "teacher@school.edu",
  "password": "teacher-password",
  "schoolId": "school-uuid"
}
```

- `schoolId` is optional; required if same email exists in multiple schools.

**Response (200)**:
```json
{
  "token": "jwt-token-for-teacher",
  "teacher": {
    "id": "uuid",
    "email": "teacher@school.edu",
    "name": "Teacher Name",
    "schoolId": "school-uuid",
    "schoolName": "ABC School",
    "classes": [
      { "id": "class-uuid", "name": "Class 1A" }
    ]
  }
}
```

**Response (401)**: "Teacher login failed" — email not found or password invalid.

**Frontend**: Store `token` and use as `Authorization: Bearer <token>` for all school/class/student/progress APIs.

---

## 5. Create Class with Teacher

**POST** `/schools/{schoolId}/classes`

**Request** (extended):
```json
{
  "name": "Class 1A",
  "teacherId": "teacher-uuid"
}
```

- `teacherId` optional; assigns existing teacher to the new class.

---

## 6. Auth: Admin vs Teacher

| Action | Admin (Cognito) | Teacher (token) |
|--------|-----------------|-----------------|
| Add teacher | ✓ | ✗ |
| List teachers | ✓ | ✓ |
| Create class | ✓ | ✗ |
| Add students | ✓ | ✗ |
| List classes | ✓ | ✓ |
| List students | ✓ | ✓ |
| Progress overview | ✓ | ✓ |
| Progress APIs (with X-Roll-Number) | ✓ | ✓ |

---

## 7. Removed

- **`GET /progress/user`** — Removed. Frontend no longer uses individual progress.

---

## 8. School-Only Signup

- `POST /users` — Frontend sends `signupType: "organization"` and `organizationName` only.
- Individual signup is deprecated; backend still accepts it for backward compatibility.
