# Campaign read-along API

Base: `{BACKEND}/v1/campaign`

## Public

| Method | Path | Body |
|--------|------|------|
| POST | `/sessions` | `{ email?, utmSource?, utmMedium?, utmCampaign?, packId? }` |
| POST | `/sessions/{id}/events` | `{ events: [{ eventType, path?, payloadJson? }] }` |
| POST | `/sessions/{id}/parts` | part attempt metrics + transcript |
| POST | `/sessions/{id}/parts/{partIndex}/score` | `{ geminiScoreJson, status? }` |
| GET | `/sessions/{id}/parts` | list parts |
| GET | `/sessions/{id}/parts/{partIndex}` | one part |
| POST | `/auth/link` | Bearer **D2C** Cognito JWT (id token preferred); `{ sessionId?, displayName? }` |

## Super admin (D2C Cognito group `super_admin`, or `ukti.campaign.admin-emails`)

School `school_admin` users do **not** get these endpoints.

| Method | Path |
|--------|------|
| GET | `/admin/summary` |
| GET | `/admin/leads` |
| GET | `/admin/sessions` |
| GET | `/admin/sessions/{id}` |
| GET | `/admin/schools` | High-level school / class / student counts |

Backend config:

- `ukti.cognito.jwks-url` — school pool (unchanged)
- `ukti.cognito.d2c.jwks-url` — D2C pool JWKS (falls back to school JWKS if empty)
- `ukti.campaign.admin-emails` — email allowlist backup for super admin

Frontend routes: `/try`, `/try/read/[packId]`, `/try/next/[sessionId]`, `/try/report/[sessionId]`, `/try/login`, `/try/library`, `/admin/super`.
