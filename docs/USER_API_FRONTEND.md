# Ukti User API — Frontend Integration Guide

**Base URL:** `http://localhost:8080` (dev) / your deployed URL (prod)

**Auth:** Cognito handles signup/login. Frontend sends the Cognito JWT in `Authorization: Bearer <token>`.

---

## Endpoints

### 1. Create / Update User (after Cognito signup/login)

**POST** `/users`

**Required:** `Authorization: Bearer <cognito_id_token>`

Call this after the user signs up or signs in with Cognito. User info (sub, email, phone, username) is extracted from the JWT. Optional body for displayName, schoolId.

**Headers:**
| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes | `Bearer <cognito_id_token>` |

**Request Body (optional):**
```json
{
  "displayName": "John Doe",
  "schoolId": "school-123"
}
```

If no body, user is created/updated from JWT claims only.

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "cognitoSub": "abc123-xyz-...",
  "email": "user@example.com",
  "phone": "+919876543210",
  "username": "johndoe",
  "displayName": "John Doe",
  "schoolId": "school-123",
  "createdAt": "2026-02-21T09:00:00Z",
  "updatedAt": "2026-02-21T09:00:00Z"
}
```

**Notes:**
- If user with same `cognitoSub` (from JWT `sub`) exists, we **update** the record (upsert).
- JWT claims used: `sub`, `email`, `phone_number`, `cognito:username`.

---

### 2. Get Current User

**GET** `/users/me`

**Headers (one required):**
| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes* | `Bearer <cognito_id_token>` |
| `X-Cognito-Sub` | Yes* | Cognito `sub` (legacy/dev only) |

*Use `Authorization` in production. `X-Cognito-Sub` supported for dev.

**Example:**
```
GET /users/me
Authorization: Bearer eyJraWQ...
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "cognitoSub": "abc123-xyz-...",
  "email": "user@example.com",
  "phone": "+919876543210",
  "username": "johndoe",
  "displayName": "John Doe",
  "schoolId": "school-123",
  "createdAt": "2026-02-21T09:00:00Z",
  "updatedAt": "2026-02-21T09:00:00Z"
}
```

**Response (404):** User not found in our DB (call POST `/users` first).

**Response (400):** Missing or empty `X-Cognito-Sub` header.

---

### 3. Get User by ID

**GET** `/users/{id}`

**Example:**
```
GET /users/550e8400-e29b-41d4-a716-446655440000
```

**Response (200 OK):** Same user object as above.

**Response (404):** User not found.

---

## Frontend Flow (Cognito + Backend)

1. **Signup/Login:** User signs up or signs in via Cognito.
2. **Get tokens:** Cognito returns `idToken` (JWT) and optionally `accessToken`, `refreshToken`.
3. **Sync to backend:** Call `POST /users` with header `Authorization: Bearer <idToken>`.
4. **On app load:** Call `GET /users/me` with header `Authorization: Bearer <idToken>` to fetch user from our DB.

**Cognito JWT claims we use:** `sub`, `email`, `phone_number`, `cognito:username`

---

## Error Responses

All errors use this format:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable message"
}
```

| HTTP | When                    |
|------|-------------------------|
| 400  | Bad request (validation) |
| 404  | User not found          |
| 500  | Server error            |

---

## CORS

Allowed origins: `http://localhost:3000`, `http://localhost:5173`, `https://ukti.example.com`

---

## Database Migration (if you created tables from original spec)

If your `users` table has `password_hash NOT NULL`, run this to allow Cognito users (we use placeholder "COGNITO"):

```sql
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
```

To add Cognito columns if missing:

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS cognito_sub VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(50);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_cognito_sub ON users(cognito_sub);
```

## Cognito Configuration

Backend validates JWTs using JWKS from:
`https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_XYqySdLwI/.well-known/jwks.json`

Override in `application.properties`:
```properties
ukti.cognito.jwks-url=https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_XYqySdLwI/.well-known/jwks.json
```
