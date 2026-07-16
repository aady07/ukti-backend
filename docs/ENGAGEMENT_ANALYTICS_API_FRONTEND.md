# Activity engagement analytics — frontend integration

This document describes the **implemented** backend for per-student, per-activity engagement ingest (counters + optional event timeline). Aligns with the product spec: batch requests, low traffic, non-blocking on the client.

---

## 1. Endpoint

| Method | Path | Success |
|--------|------|--------|
| `POST` | `/v1/engagement/sessions/batch` | `200 OK` |

Base URL is your API host (same origin as other app APIs).

---

## 2. Authentication and headers

Same pattern as **`/progress/**`**:

| Header | Required | Purpose |
|--------|----------|---------|
| `Authorization: Bearer <JWT>` | Yes (for real users) | Cognito JWT (learner / school admin) **or** teacher JWT. |
| `X-Cognito-Sub` | No | Legacy/dev override when not using Bearer (same as progress). |
| `X-Roll-Number` | No | Station / class flow: whose engagement to record (student roll). |
| `X-Class-Id` | No | UUID string; disambiguates roll when needed (same as progress). |
| `Idempotency-Key` | No | If the **same** key was already processed successfully, server returns **200** with the **same** `sessionId` and `receivedBatchId` as the first call. Max length **200** characters. |

**Who can call**

- **Learner** (Cognito, no roll): records for **that** user.
- **Teacher / admin with `X-Roll-Number`**: records for the **resolved student** (same rules as progress; teacher must be allowed for that class).

**Blocked cases**

- Teacher JWT **without** roll → `401` with `error: "STUDENT_CONTEXT_REQUIRED"`.
- Roll provided but student not found → `400` with `error: "STUDENT_NOT_FOUND"`.
- Invalid / missing auth (and not a recognized dev header) → `401` with `error: "UNAUTHORIZED"`.

---

## 3. Request body (JSON)

Top-level property names are **camelCase** (default Jackson).

### 3.1 Example

```json
{
  "batchId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "clientSessionId": "f0e1d2c3-b4a5-6789-0123-456789abcdef",
  "unitSlug": "english-unit-1",
  "activitySlug": "body-parts-1",
  "runId": null,
  "sectionId": null,
  "startedAt": "2026-05-10T10:00:00.000Z",
  "endedAt": "2026-05-10T10:04:30.000Z",
  "sessionClosed": true,
  "counters": {
    "video_complete": 1,
    "video_skip": 0,
    "video_replay": 1,
    "video_error": 0,
    "vision_attempts": 3,
    "vision_passes": 1,
    "vision_failures": 2,
    "stt_listen_starts": 4,
    "pron_pass": 1,
    "pron_fail": 2,
    "skip_audio": 0,
    "stt_empty_cycles": 2,
    "client_error": 0
  },
  "events": [
    {
      "id": "11111111-2222-3333-4444-555555555555",
      "ts": "2026-05-10T10:00:05.000Z",
      "type": "video_skip",
      "phase": "station-video",
      "challengeIndex": null,
      "payload": {}
    }
  ],
  "payloadVersion": 1
}
```

### 3.2 Fields

| Field | Required | Type | Notes |
|-------|----------|------|--------|
| `batchId` | Yes | UUID | **New** UUID per HTTP request. Used for idempotent retries **per visit** (see §5). |
| `clientSessionId` | Yes | UUID | **Stable** for the whole activity visit; ties all batches for that visit. **Globally unique** on the server. |
| `unitSlug` | Yes | string | Must match progress/content slugs; cannot change mid-visit for the same `clientSessionId`. |
| `activitySlug` | Yes | string | Same as above. |
| `runId` | No | UUID | Class-module run, if applicable. Backfilled on the session if first sent non-null. |
| `sectionId` | No | string | Business section id; backfilled if session has none yet. |
| `startedAt` | No | ISO-8601 instant | Recommended on first batch; used when **creating** the session row. |
| `endedAt` | No | ISO-8601 instant | Used only when closing the session (see `sessionClosed`). |
| `sessionClosed` | No | boolean | If **`true`**, sets `ended_at` on the session (latest non-null `endedAt`, else server now). |
| `counters` | Yes | object | **Deltas** (see §4). Inner keys are **snake_case** (see table below). |
| `events` | No | array | Optional timeline; capped server-side (see §6). |
| `payloadVersion` | Yes | integer | Start with **`1`**. Server keeps `max(stored, request)`. |

### 3.3 `counters` object (snake_case keys)

Each value is an integer **delta** for this batch only. **Omitted or `null` fields count as 0.**

| JSON key | Meaning |
|----------|---------|
| `video_complete` | Natural end of video. |
| `video_skip` | User skipped video. |
| `video_replay` | User replayed video. |
| `video_error` | Media/load error. |
| `vision_attempts` | Vision/camera evaluation attempts (pass + fail). |
| `vision_passes` | Vision passes. |
| `vision_failures` | Vision failures. |
| `stt_listen_starts` | Listen / STT started (incl. restarts). |
| `pron_pass` | Pronunciation pass. |
| `pron_fail` | Pronunciation fail (non-skip). |
| `skip_audio` | Skip-audio path. |
| `stt_empty_cycles` | Listen ended with no usable transcript. |
| `client_error` | Client-reported non-fatal errors. |

### 3.4 `events[]` items (optional)

| Field | Required | Type | Notes |
|-------|----------|------|--------|
| `id` | No | UUID | Informational; DB primary key for events is **server-generated**. |
| `ts` | Recommended | ISO-8601 instant | Defaults to server time if missing. |
| `type` | Yes | string | Rows without `type` are **skipped**. |
| `phase` | No | string | e.g. UI phase label. |
| `challengeIndex` | No | integer | camelCase in JSON. |
| `payload` | No | object | Arbitrary JSON object (e.g. `vision_result`, `pron_result`). |

**Suggested `type` values** (contract with product; not enforced as an enum on the server):

| `type` | Typical `payload` |
|--------|-------------------|
| `video_complete` | `{}` |
| `video_skip` | `{}` |
| `video_replay` | `{}` |
| `video_error` | `{}` |
| `vision_result` | `{ "passed": boolean, "attemptNumber": number }` |
| `pron_listen_start` | `{}` |
| `pron_result` | `{ "outcome": "pass" \| "fail" \| "skip_audio" }` |
| `stt_empty_cycle` | `{}` |
| `client_error` | `{ "code": string, "step": string }` |

---

## 4. Counter merge semantics (important)

- The server stores **running totals** per session.
- Each batch sends **deltas**; the server **adds** them to the stored counters.
- After each add, each total is **clamped to ≥ 0** (large negative deltas cannot drive totals below zero).
- **Do not** send full cumulative totals each time unless you intend them to be **additional** increments (that would double-count). For a single flush per visit, send the **increments since session start** (or one batch with all deltas).

`payloadVersion` is updated with **`max(existing, request.payloadVersion)`**.

---

## 5. Idempotency

1. **`batchId` (per session)**  
   If the same `(session server id, batchId)` was already applied, the server returns **200** with the same `sessionId` and `receivedBatchId` **without** applying counters or events again.

2. **`Idempotency-Key` (HTTP header)**  
   Optional. Same key → **200** with the **first successful** response’s `sessionId` and `receivedBatchId`. Use the same key when **retrying the same logical HTTP request** (network failure after success, etc.).

`clientSessionId` must be **one stable UUID per visit**; do not rotate it mid-activity.

---

## 6. Event storage limits

- At most **50** events are stored **per session** in `activity_engagement_event`. Additional events in later batches are **ignored** once the cap is reached.
- The session row’s **`raw_events`** JSONB is overwritten with the **last batch’s** `events` array (audit snapshot of the latest payload), not a full merge of all batches.

---

## 7. Success response

`200 OK`, body:

```json
{
  "ok": true,
  "sessionId": "server-generated-uuid",
  "receivedBatchId": "same-as-request-batchId"
}
```

- `sessionId`: server primary key for `activity_engagement_session` (not the same as `clientSessionId`).

---

## 8. Error responses

| HTTP | When | Body shape |
|------|------|------------|
| `400` | Validation / bad slug mismatch / idempotency key too long | `{ "error": "BAD_REQUEST" \| "STUDENT_NOT_FOUND", "message": "..." }` |
| `401` | Missing auth, or teacher without student context | `{ "error": "UNAUTHORIZED" \| "STUDENT_CONTEXT_REQUIRED", "message": "..." }` |
| `403` | `clientSessionId` already owned by **another** user | `{ "error": "FORBIDDEN", "message": "..." }` |
| `500` | Unexpected failure | `{ "error": "INTERNAL_ERROR", "message": "Failed to record engagement" }` |

Slug mismatch for an existing `clientSessionId`: **`400`** with `BAD_REQUEST` (`unitSlug` / `activitySlug` must match the existing session).

---

## 9. Database schema (reference)

Used for reporting and ops; **frontend does not call these directly**. DDL for manual installs: `scripts/activity-engagement-analytics.sql` (if present in repo). Hibernate `ddl-auto=update` can also create/update tables in dev.

### 9.1 `activity_engagement_session`

One row per **visit** (unique `client_session_id`).

| Column | Type | Notes |
|--------|------|--------|
| `id` | UUID | PK; returned as `sessionId`. |
| `client_session_id` | UUID | UNIQUE; from client. |
| `user_id` | UUID | Resolved learner (`users.id`). |
| `school_id` | UUID | Nullable (at-home). |
| `class_id` | UUID | Nullable. |
| `roll_number` | varchar | Nullable. |
| `run_id` | UUID | Nullable. |
| `section_id` | varchar | Nullable. |
| `unit_slug` | varchar | Required. |
| `activity_slug` | varchar | Required. |
| `started_at` | timestamptz | Required. |
| `ended_at` | timestamptz | Nullable; set when `sessionClosed`. |
| `video_complete` … `client_error` | int | Running totals (defaults 0). |
| `payload_version` | int | Schema / interpretation version. |
| `raw_events` | JSONB | Last batch’s `events` array. |
| `created_at` / `updated_at` | timestamptz | Maintained by server. |

Indexes (conceptual): `(school_id, class_id, roll_number, started_at)`, `(unit_slug, activity_slug, started_at)`.

### 9.2 `activity_engagement_event`

Optional timeline rows.

| Column | Type |
|--------|------|
| `id` | UUID PK (server-generated) |
| `session_id` | UUID FK → session |
| `ts` | timestamptz |
| `type` | varchar |
| `phase` | varchar (nullable) |
| `challenge_index` | int (nullable) |
| `payload` | JSONB (nullable) |

### 9.3 `activity_engagement_processed_batch`

Internal idempotency for **`batchId`** per session.

| Column | Type |
|--------|------|
| `id` | UUID PK |
| `session_id` | UUID |
| `batch_id` | UUID |
| `created_at` | timestamptz |

UNIQUE `(session_id, batch_id)`.

### 9.4 `activity_engagement_idempotency`

HTTP **`Idempotency-Key`** replay cache.

| Column | Type |
|--------|------|
| `idempotency_key` | varchar(200) PK |
| `session_id` | UUID |
| `received_batch_id` | UUID |
| `created_at` | timestamptz |

---

## 10. Client checklist

1. Generate **`clientSessionId`** once per activity open; reuse for all batches until visit ends.
2. Generate a **new `batchId`** per HTTP request.
3. Send **`counters` as deltas** per batch.
4. On navigate away / completion, send final batch with **`sessionClosed: true`** and optional `endedAt`.
5. Optionally attach **`events`** for debugging/analytics; keep list small if you care about the 50-row cap.
6. On retry of the **same** POST, send the same **`Idempotency-Key`** header.

---

**Document version:** 1.0 (matches backend implementation as of authoring.)
