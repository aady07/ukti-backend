# Admin, school, teacher, and class reference — full data snapshot

This file is a **point-in-time copy** of what was exported from `ukti_db` (inventory run). Use it as a human-readable directory of IDs, emails, and class names. When the database changes, re-run the queries in the appendix to refresh.

---

## 1. School admins (7 rows)

| admin_user_id | cognito_sub | cognito_login_email | display_name | school_uuid |
|---------------|-------------|---------------------|--------------|-------------|
| 9a135a28-60b9-49c9-9391-1471f5b32fd3 | 01237d8a-3051-70d8-d9b7-6cb635d4d7a5 | 01237d8a-3051-70d8-d9b7-6cb635d4d7a5@cognito.local | Rachit Srivastava | 14b0c7e0-157b-4803-b135-df987d89c76c |
| 1411f6e4-7d61-4898-8807-9cf00a1b0b2f | 41b32d7a-3091-70fe-4327-53c3a88c1579 | 41b32d7a-3091-70fe-4327-53c3a88c1579@cognito.local | Def | 1571db47-8e67-4cc5-9ff3-bf58ba8f846b |
| 9f90c0e0-8b26-42e6-99f5-9f8e79ee533d | 61b3ddea-2081-7031-68c0-3a5f34ed4952 | 61b3ddea-2081-7031-68c0-3a5f34ed4952@cognito.local | Taruna | 660af6d1-e7e1-41ab-81eb-708103e957a6 |
| 06935ed4-a36e-49ec-ac6f-e6a4ed91a390 | 71632d5a-80d1-709f-f097-35f3058a25e0 | 71632d5a-80d1-709f-f097-35f3058a25e0@cognito.local | Masroor Ahmad Dar | e6aaf3b8-825d-42c2-82fa-c5d87e718971 |
| df8c265e-3a93-4f3c-922d-f89157c840f7 | 71e3edca-40e1-70c1-33ef-7bb0e82563a7 | 71e3edca-40e1-70c1-33ef-7bb0e82563a7@cognito.local | Srilekha | 0350665a-d28e-4257-98dc-a0a8feeff2af |
| 15a00fc4-c7eb-4fbf-be8f-acd7e2ebf7a2 | d1c3bd9a-6081-7029-3a88-10c0cba60cde | d1c3bd9a-6081-7029-3a88-10c0cba60cde@cognito.local | Sunny | 3666e336-4ddd-411b-b991-34b1fcce787c |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | adarsh | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 |

Admin login email in the DB is the Cognito-style `…@cognito.local` value above (same pattern as `cognito_sub` + suffix).

---

## 2. Every admin → school → class (all rows)

| admin_user_id | admin_email | school_id | school_name | class_id | class_name |
|---------------|-------------|-----------|-------------|----------|------------|
| 9a135a28-60b9-49c9-9391-1471f5b32fd3 | 01237d8a-3051-70d8-d9b7-6cb635d4d7a5@cognito.local | 14b0c7e0-157b-4803-b135-df987d89c76c | ABC School | a6047334-c0cb-4d8a-abc6-be7ee53692cc | 1A |
| 9a135a28-60b9-49c9-9391-1471f5b32fd3 | 01237d8a-3051-70d8-d9b7-6cb635d4d7a5@cognito.local | 14b0c7e0-157b-4803-b135-df987d89c76c | ABC School | af8223c8-e096-4a1e-bb29-afc81c16687d | UKGA |
| 1411f6e4-7d61-4898-8807-9cf00a1b0b2f | 41b32d7a-3091-70fe-4327-53c3a88c1579@cognito.local | 1571db47-8e67-4cc5-9ff3-bf58ba8f846b | Abc | d3c7ee17-a24b-4c26-bf6d-90a290575d55 | 1 |
| 9f90c0e0-8b26-42e6-99f5-9f8e79ee533d | 61b3ddea-2081-7031-68c0-3a5f34ed4952@cognito.local | 660af6d1-e7e1-41ab-81eb-708103e957a6 | Test3 | c021db13-1c61-49b3-91ab-f7dbe0ad92e5 | 1 A |
| 9f90c0e0-8b26-42e6-99f5-9f8e79ee533d | 61b3ddea-2081-7031-68c0-3a5f34ed4952@cognito.local | 660af6d1-e7e1-41ab-81eb-708103e957a6 | Test3 | 37a27af9-d26c-4c93-bac2-ed01c062a52d | UKG Red |
| 06935ed4-a36e-49ec-ac6f-e6a4ed91a390 | 71632d5a-80d1-709f-f097-35f3058a25e0@cognito.local | e6aaf3b8-825d-42c2-82fa-c5d87e718971 | Harshan Army Goodwill School Krusan | fb62f502-e46a-4215-b859-561742640f33 | 1st Blue |
| 06935ed4-a36e-49ec-ac6f-e6a4ed91a390 | 71632d5a-80d1-709f-f097-35f3058a25e0@cognito.local | e6aaf3b8-825d-42c2-82fa-c5d87e718971 | Harshan Army Goodwill School Krusan | 532dad7f-ed3f-4b3b-b62c-ed5b54af7c2e | 1st Green |
| 06935ed4-a36e-49ec-ac6f-e6a4ed91a390 | 71632d5a-80d1-709f-f097-35f3058a25e0@cognito.local | e6aaf3b8-825d-42c2-82fa-c5d87e718971 | Harshan Army Goodwill School Krusan | 910bbf3f-f281-474f-93a4-328174d8acda | UKG Blue |
| 06935ed4-a36e-49ec-ac6f-e6a4ed91a390 | 71632d5a-80d1-709f-f097-35f3058a25e0@cognito.local | e6aaf3b8-825d-42c2-82fa-c5d87e718971 | Harshan Army Goodwill School Krusan | f694caba-18f2-4880-824b-d31aae9bc238 | UKG Green |
| df8c265e-3a93-4f3c-922d-f89157c840f7 | 71e3edca-40e1-70c1-33ef-7bb0e82563a7@cognito.local | 0350665a-d28e-4257-98dc-a0a8feeff2af | abc | db8842d8-842f-4e30-8b21-fe3798c40158 | 1 |
| 15a00fc4-c7eb-4fbf-be8f-acd7e2ebf7a2 | d1c3bd9a-6081-7029-3a88-10c0cba60cde@cognito.local | 3666e336-4ddd-411b-b991-34b1fcce787c | ABC | 45d8f076-d1db-4416-a040-93d94840d2a9 | 1 |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | ac3eaf87-95d8-4261-95f6-1f2d286ed5dd | 1a |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 51463270-7c29-491e-b793-cc92d12acd70 | 1B |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 7342f795-2255-4e68-b125-31eac700677e | 1C |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | f45514dc-03c3-4ddf-9d07-4db2cb9be31f | 1D |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 764ac738-703a-47db-a700-40354fd50cbd | 2C |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | cb1fc0fd-795e-46a7-9637-d3e887d09963 | 2D |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 6f9dc85e-2006-454f-b1ba-ed89ca9d4301 | 2nd |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 64c4a1a8-3f98-4b3a-ae0a-64cc46725029 | 3C |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 3cc87ea9-2e24-48b4-8352-19facfd14228 | 3D |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 9841d88f-d16a-4136-89ca-b2de77e67542 | 3rd |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | a4839024-0ebb-48fc-8267-790116f93c13 | UkG 1 |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | c1290dec-6a56-4f2e-98cb-19943f788b64 | UKG 2 |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | a58e2a24-9c43-4c33-aaa3-acb0aee4f30b | UKG 3 |
| 01da2698-6310-44e6-ab5c-65ebd3888d42 | e1937daa-d001-7068-7bf7-8ec75b85f3a3@cognito.local | 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | c32d5578-6059-45fb-8f31-07cfbad2795c | UKG Green |

**Row counts:** 2 + 1 + 2 + 4 + 1 + 1 + 14 = **25** admin–class rows.

---

## 3. Teachers by school (15 rows)

| school_id | school_name | teacher_id | teacher_email | teacher_name |
|-----------|-------------|------------|---------------|----------------|
| 0350665a-d28e-4257-98dc-a0a8feeff2af | abc | 1596aa17-2300-4fc8-8a1e-902cab03671e | srilekhashaw04@gmail.com | Srilekha |
| 0350665a-d28e-4257-98dc-a0a8feeff2af | abc | ef4a3c44-4986-4625-a14f-c6afc4c7e47d | srilekhashaw@gmail.com | Srilrkha |
| 14b0c7e0-157b-4803-b135-df987d89c76c | ABC School | e7ef7ace-f706-4d78-9b10-e8a30ec8f080 | ceo@miraista.com | Anita Kumar Singh |
| 14b0c7e0-157b-4803-b135-df987d89c76c | ABC School | 9069b40a-07dd-43c8-b888-b4624f45a080 | rachit@miraista.com | Anita Kumar |
| 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 950b5c62-9370-4cee-ab0e-bf0559be76c8 | info@miraista.com | Demo |
| 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | ef0b1e05-dbf5-4c7e-a12d-7f7af79c2f15 | new.unique.dummy@abcd.com | Nuzhat Masoodi |
| 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 3dcbbb30-9a93-4b1b-babf-749220233db7 | p.bharat0808@gmail.com | Bharat |
| 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 0ee0f390-3c9b-4f3b-ae93-030eb1541e9e | sadarsh51000@gmail.com | *(empty)* |
| 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 04949928-fc1c-4d4f-9880-8092cd36764b | uktidemo1@miraista.com | Teacher |
| 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 1c341f4a-f5d2-448d-9223-93bb8a1f407e | uktidemo2@miraista.com | Teacher |
| 0200c035-c326-4f0d-b09e-ac5daf3d5a05 | DPS | 1b62e68f-d121-4945-a297-d9f936dc2783 | uktidemo3@miraista.com | Teacher |
| e6aaf3b8-825d-42c2-82fa-c5d87e718971 | Harshan Army Goodwill School Krusan | 566bea5e-cd69-495c-a47e-dd355eba47c7 | bhatreyaz1058@gmail.com | RIYAZ AHMAD |
| e6aaf3b8-825d-42c2-82fa-c5d87e718971 | Harshan Army Goodwill School Krusan | 3b77d296-6e2f-4567-8617-dca84cad717d | mirrayees545@gmail.com | Rayees Ahmad Mir |
| e6aaf3b8-825d-42c2-82fa-c5d87e718971 | Harshan Army Goodwill School Krusan | 338a46fb-0218-4f81-aa77-5be8af4343da | nuzhatmasooudi325@gmail.com | Nuzhat Masoodi |
| 660af6d1-e7e1-41ab-81eb-708103e957a6 | Test3 | bbfb19d2-6110-4c87-b888-ce378de37414 | taruna@gmail.com | Taruna |

**Schools with an admin but no teachers in this export:** Abc (`1571db47-8e67-4cc5-9ff3-bf58ba8f846b`), ABC (`3666e336-4ddd-411b-b991-34b1fcce787c`) — no row above for those `school_id` values.

---

## 4. Teacher ↔ class (`teacher_classes`, 22 rows)

| teacher_id | teacher_email | teacher_name | class_id | class_name | is_main_teacher |
|------------|---------------|--------------|----------|------------|-----------------|
| 566bea5e-cd69-495c-a47e-dd355eba47c7 | bhatreyaz1058@gmail.com | RIYAZ AHMAD | fb62f502-e46a-4215-b859-561742640f33 | 1st Blue | true |
| 566bea5e-cd69-495c-a47e-dd355eba47c7 | bhatreyaz1058@gmail.com | RIYAZ AHMAD | 910bbf3f-f281-474f-93a4-328174d8acda | UKG Blue | true |
| e7ef7ace-f706-4d78-9b10-e8a30ec8f080 | ceo@miraista.com | Anita Kumar Singh | a6047334-c0cb-4d8a-abc6-be7ee53692cc | 1A | true |
| e7ef7ace-f706-4d78-9b10-e8a30ec8f080 | ceo@miraista.com | Anita Kumar Singh | af8223c8-e096-4a1e-bb29-afc81c16687d | UKGA | true |
| 950b5c62-9370-4cee-ab0e-bf0559be76c8 | info@miraista.com | Demo | 51463270-7c29-491e-b793-cc92d12acd70 | 1B | true |
| ef0b1e05-dbf5-4c7e-a12d-7f7af79c2f15 | new.unique.dummy@abcd.com | Nuzhat Masoodi | c32d5578-6059-45fb-8f31-07cfbad2795c | UKG Green | true |
| 338a46fb-0218-4f81-aa77-5be8af4343da | nuzhatmasooudi325@gmail.com | Nuzhat Masoodi | 532dad7f-ed3f-4b3b-b62c-ed5b54af7c2e | 1st Green | true |
| 338a46fb-0218-4f81-aa77-5be8af4343da | nuzhatmasooudi325@gmail.com | Nuzhat Masoodi | f694caba-18f2-4880-824b-d31aae9bc238 | UKG Green | true |
| 3dcbbb30-9a93-4b1b-babf-749220233db7 | p.bharat0808@gmail.com | Bharat | ac3eaf87-95d8-4261-95f6-1f2d286ed5dd | 1a | true |
| 3dcbbb30-9a93-4b1b-babf-749220233db7 | p.bharat0808@gmail.com | Bharat | 6f9dc85e-2006-454f-b1ba-ed89ca9d4301 | 2nd | true |
| 0ee0f390-3c9b-4f3b-ae93-030eb1541e9e | sadarsh51000@gmail.com | *(empty)* | ac3eaf87-95d8-4261-95f6-1f2d286ed5dd | 1a | true |
| 0ee0f390-3c9b-4f3b-ae93-030eb1541e9e | sadarsh51000@gmail.com | *(empty)* | 6f9dc85e-2006-454f-b1ba-ed89ca9d4301 | 2nd | true |
| 0ee0f390-3c9b-4f3b-ae93-030eb1541e9e | sadarsh51000@gmail.com | *(empty)* | 9841d88f-d16a-4136-89ca-b2de77e67542 | 3rd | true |
| ef4a3c44-4986-4625-a14f-c6afc4c7e47d | srilekhashaw@gmail.com | Srilrkha | db8842d8-842f-4e30-8b21-fe3798c40158 | 1 | true |
| bbfb19d2-6110-4c87-b888-ce378de37414 | taruna@gmail.com | Taruna | c021db13-1c61-49b3-91ab-f7dbe0ad92e5 | 1 A | true |
| bbfb19d2-6110-4c87-b888-ce378de37414 | taruna@gmail.com | Taruna | 37a27af9-d26c-4c93-bac2-ed01c062a52d | UKG Red | true |
| 04949928-fc1c-4d4f-9880-8092cd36764b | uktidemo1@miraista.com | Teacher | a4839024-0ebb-48fc-8267-790116f93c13 | UkG 1 | true |
| 1c341f4a-f5d2-448d-9223-93bb8a1f407e | uktidemo2@miraista.com | Teacher | 7342f795-2255-4e68-b125-31eac700677e | 1C | true |
| 1c341f4a-f5d2-448d-9223-93bb8a1f407e | uktidemo2@miraista.com | Teacher | 764ac738-703a-47db-a700-40354fd50cbd | 2C | true |
| 1c341f4a-f5d2-448d-9223-93bb8a1f407e | uktidemo2@miraista.com | Teacher | c1290dec-6a56-4f2e-98cb-19943f788b64 | UKG 2 | true |
| 1b62e68f-d121-4945-a297-d9f936dc2783 | uktidemo3@miraista.com | Teacher | 64c4a1a8-3f98-4b3a-ae0a-64cc46725029 | 3C | true |
| 1b62e68f-d121-4945-a297-d9f936dc2783 | uktidemo3@miraista.com | Teacher | a58e2a24-9c43-4c33-aaa3-acb0aee4f30b | UKG 3 | true |

**Note:** `1596aa17-2300-4fc8-8a1e-902cab03671e` (srilekhashaw04@gmail.com) and `9069b40a-07dd-43c8-b888-b4624f45a080` (rachit@miraista.com) appear in §3 but have **no** row here — no `teacher_classes` assignment in this snapshot. `3b77d296-6e2f-4567-8617-dca84cad717d` (Rayees) likewise has no class row in §4.

---

## Appendix — SQL only to re-export (optional)

Use these in `psql` when you need to refresh this document from the live DB.

**Admins**

```sql
SELECT u.id, u.cognito_sub, u.email, u.display_name, u.school_uuid
FROM users u
WHERE u.user_type = 'school_admin'
ORDER BY u.email NULLS LAST, u.id;
```

**Admin → all classes**

```sql
SELECT u.id, u.email, s.id, s.name, c.id, c.name
FROM users u
JOIN schools s ON s.id = COALESCE(u.school_uuid, u.school_id::uuid)
JOIN classes c ON c.school_id = s.id
WHERE u.user_type = 'school_admin'
ORDER BY u.email, c.name;
```

**Teachers per school**

```sql
SELECT s.id, s.name, t.id, t.email, t.name
FROM schools s
JOIN teachers t ON t.school_id = s.id
ORDER BY s.name, t.email;
```

**Teacher → classes**

```sql
SELECT t.id, t.email, t.name, c.id, c.name, tc.is_main_teacher
FROM teachers t
JOIN teacher_classes tc ON tc.teacher_id = t.id
JOIN classes c ON c.id = tc.class_id
ORDER BY t.email, c.name;
```

**Gaps:** teachers with no class; classes with no teacher; schools with admin but no teachers — see earlier version of this doc or ask for those three queries.
