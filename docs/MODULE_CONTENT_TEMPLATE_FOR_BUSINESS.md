# Module Content Template (Business -> JSON Ready)

Use this template to define module content for each grade/class.
It is designed so engineering can directly convert it to JSON and AI can generate:

- question text
- hints/prompts
- TTS script
- STT expected phrases

---

## 1) Module Header (one row per module)

Fill once per module/week.

| Field | Required | Example | Meaning |
|---|---|---|---|
| classLevel | Yes | `ukg` | Grade bucket (`nursery`, `lkg`, `ukg`, `grade1`, `grade2`) |
| moduleId | Yes | `ukg-module-1` | Stable module identifier (do not change after release) |
| moduleTitle | Yes | `Letters A-F` | Display title |
| weekLabel | Yes | `Week 1` | Week label shown in UI |
| summary | Yes | `Intro to letters A-F with sounds and tracing.` | Short module summary |
| learningObjective | Yes | `Identify, speak, and trace letters A-F.` | What student should learn |
| difficulty | Optional | `easy` | `easy` / `medium` / `hard` |
| estimatedMinutes | Optional | `35` | Approx module duration |
| language | Yes | `en-IN` | Primary language for TTS/STT |
| active | Yes | `true` | Whether module is active |
| notes | Optional | `Start with audio-heavy activities.` | Internal business note |

---

## 2) Module Item Template (one row per activity/item)

Use one row for each teachable item (letter/word/shape/color/etc).

| Field | Required | Example | Meaning |
|---|---|---|---|
| classLevel | Yes | `ukg` | Same as header |
| moduleId | Yes | `ukg-module-1` | Parent module |
| moduleItemId | Yes | `letters-a` | Stable item id (unique inside module) |
| sequenceNo | Yes | `1` | Display/order number |
| domain | Yes | `letter` | `letter` / `word` / `shape` / `color` / `number` / `phonics` / `body-movement` / etc |
| topicLabel | Yes | `Letter A` | Human-readable item title |
| conceptValue | Yes | `A` | Core concept token (e.g., `A`, `Red`, `Circle`) |
| conceptMeaning | Yes | `Capital letter A sound and recognition` | Business meaning of the concept |
| whatItDoes | Yes | `Builds letter recognition and phoneme awareness` | Why this item exists / learning role |
| activityType | Yes | `paired_show_say` | `paired_show_say` / `say_only` / `single` |
| interactionMode | Yes | `listen_repeat_trace` | How student interacts (`listen_repeat`, `tap_match`, `trace`, etc) |
| instructionText | Yes | `This is letter A. Say A.` | On-screen instruction |
| promptText | Yes | `Can you say letter A?` | AI prompt/question |
| expectedAnswer | Optional | `A` | Canonical expected answer |
| acceptedAnswers | Optional | `["a","letter a","eh"]` | Valid answer variants |
| hintText | Optional | `It is the first letter of alphabet.` | Hint if child is stuck |
| reinforcementText | Optional | `Great! You said A correctly.` | Positive feedback |
| correctionText | Optional | `Let's try again. This is A.` | Retry feedback |
| ttsText | Optional | `This is letter A. Repeat after me: A.` | Exact text for TTS |
| ttsVoice | Optional | `female_child_friendly_en_in` | Preferred TTS voice |
| sttExpectedPhrases | Optional | `["a","letter a"]` | STT phrase list |
| sttStrictness | Optional | `medium` | `low` / `medium` / `high` |
| mediaType | Optional | `image` | `image` / `audio` / `video` / `animation` |
| mediaRef | Optional | `letters/a_card.png` | Asset path/key |
| successCriteria | Yes | `Student says A clearly once` | Completion rule for business |
| retryLimit | Optional | `2` | Allowed retries before hint/help |
| scoringWeight | Optional | `1` | Relative weight in scoring/progress |
| tags | Optional | `["phonics","alphabet","foundation"]` | Search/filter tags |
| isAssessment | Optional | `false` | Assessment item flag |
| active | Yes | `true` | Item active/inactive |
| businessNotes | Optional | `Use slower voice for first attempt.` | Additional context for content ops |

---

## 3) CSV Header (copy/paste for sheets)

```csv
classLevel,moduleId,moduleItemId,sequenceNo,domain,topicLabel,conceptValue,conceptMeaning,whatItDoes,activityType,interactionMode,instructionText,promptText,expectedAnswer,acceptedAnswers,hintText,reinforcementText,correctionText,ttsText,ttsVoice,sttExpectedPhrases,sttStrictness,mediaType,mediaRef,successCriteria,retryLimit,scoringWeight,tags,isAssessment,active,businessNotes
```

---

## 4) Example Rows

```csv
ukg,ukg-module-1,letters-a,1,letter,Letter A,A,Capital letter A sound and recognition,Builds letter recognition and phoneme awareness,paired_show_say,listen_repeat_trace,"This is letter A. Say A.","Can you say letter A?",A,"[""a"",""letter a"",""eh""]","It is the first letter of alphabet.","Great! You said A correctly.","Let's try again. This is A.","This is letter A. Repeat after me: A.",female_child_friendly_en_in,"[""a"",""letter a""]",medium,image,letters/a_card.png,"Student says A clearly once",2,1,"[""phonics"",""alphabet""]",false,true,"Use slower voice for first attempt."
ukg,ukg-module-1,color-red,2,color,Color Red,Red,Primary color recognition,Builds visual identification and vocabulary,single,tap_match,"Tap the red color.","Which one is red?",Red,"[""red""]","Think of apple color.","Nice! That's red.","Not this one, try the red color.","Find the red color.",female_child_friendly_en_in,"[""red""]",low,image,colors/red_card.png,"Student selects red correctly",2,1,"[""colors"",""visual""]",false,true,"Pair with real-world object examples."
```

---

## 5) Business Rules (important for clean conversion)

- Keep `moduleId` and `moduleItemId` stable after go-live.
- Use lowercase kebab-case for IDs (`letters-a`, `color-red`).
- Do not leave required fields blank.
- Keep `activityType` only from allowed values.
- Put JSON-like lists in `acceptedAnswers`, `sttExpectedPhrases`, `tags`.
- If no TTS/STT needed, keep those fields empty (do not invent values).

---

## 6) Suggested Submission Process

1. Business fills one sheet/tab per class level.
2. Content lead validates required fields and IDs.
3. Engineering converts CSV to JSON.
4. AI layer uses `promptText`, `ttsText`, `sttExpectedPhrases`, `successCriteria` to generate runtime question/prompt flow.

---

## 7) Optional JSON Shape (for engineering reference)

```json
{
  "classLevel": "ukg",
  "moduleId": "ukg-module-1",
  "moduleTitle": "Letters A-F",
  "weekLabel": "Week 1",
  "summary": "Intro to letters A-F with sounds and tracing.",
  "items": [
    {
      "moduleItemId": "letters-a",
      "sequenceNo": 1,
      "domain": "letter",
      "topicLabel": "Letter A",
      "conceptValue": "A",
      "conceptMeaning": "Capital letter A sound and recognition",
      "whatItDoes": "Builds letter recognition and phoneme awareness",
      "activityType": "paired_show_say",
      "interactionMode": "listen_repeat_trace",
      "instructionText": "This is letter A. Say A.",
      "promptText": "Can you say letter A?",
      "expectedAnswer": "A",
      "acceptedAnswers": ["a", "letter a", "eh"],
      "ttsText": "This is letter A. Repeat after me: A.",
      "sttExpectedPhrases": ["a", "letter a"],
      "successCriteria": "Student says A clearly once"
    }
  ]
}
```
