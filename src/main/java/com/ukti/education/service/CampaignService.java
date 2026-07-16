package com.ukti.education.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukti.education.dto.CampaignDtos;
import com.ukti.education.dto.CognitoUserClaims;
import com.ukti.education.entity.CampaignEvent;
import com.ukti.education.entity.CampaignLead;
import com.ukti.education.entity.CampaignPartAttempt;
import com.ukti.education.entity.CampaignSession;
import com.ukti.education.entity.School;
import com.ukti.education.entity.User;
import com.ukti.education.repository.CampaignEventRepository;
import com.ukti.education.repository.CampaignLeadRepository;
import com.ukti.education.repository.CampaignPartAttemptRepository;
import com.ukti.education.repository.CampaignSessionRepository;
import com.ukti.education.entity.SchoolClass;
import com.ukti.education.entity.Teacher;
import com.ukti.education.entity.TeacherClass;
import com.ukti.education.repository.SchoolClassRepository;
import com.ukti.education.repository.SchoolRepository;
import com.ukti.education.repository.TeacherClassRepository;
import com.ukti.education.repository.TeacherRepository;
import com.ukti.education.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignSessionRepository sessionRepository;
    private final CampaignPartAttemptRepository partRepository;
    private final CampaignEventRepository eventRepository;
    private final CampaignLeadRepository leadRepository;
    private final CognitoJwtService cognitoJwtService;
    private final CognitoD2cJwtService cognitoD2cJwtService;
    private final SuperAdminJwtService superAdminJwtService;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ukti.campaign.admin-emails:}")
    private String adminEmailsCsv;

    public CampaignDtos.SessionResponse startSession(CampaignDtos.StartSessionRequest req) {
        String email = normalizeEmail(req.getEmail());
        CampaignSession session = CampaignSession.builder()
                .email(email)
                .utmSource(trimToNull(req.getUtmSource()))
                .utmMedium(trimToNull(req.getUtmMedium()))
                .utmCampaign(trimToNull(req.getUtmCampaign()))
                .packId(trimToNull(req.getPackId()) != null ? trimToNull(req.getPackId()) : "rainy-free")
                .startedAt(Instant.now())
                .build();
        session = sessionRepository.save(session);

        if (email != null) {
            CampaignLead lead = leadRepository.findByEmailIgnoreCase(email).orElseGet(() ->
                    CampaignLead.builder().email(email).status("anon").build());
            lead.setSessionId(session.getId());
            if (lead.getStatus() == null) lead.setStatus("anon");
            leadRepository.save(lead);
        }

        eventRepository.save(CampaignEvent.builder()
                .sessionId(session.getId())
                .eventType("page_open")
                .path("/try")
                .payloadJson("{\"source\":\"start_session\"}")
                .build());

        return toSessionResponse(session);
    }

    @Transactional
    public void ingestEvents(UUID sessionId, CampaignDtos.EventsBatchRequest req) {
        requireSession(sessionId);
        if (req.getEvents() == null || req.getEvents().isEmpty()) return;
        List<CampaignEvent> rows = new ArrayList<>();
        for (CampaignDtos.EventItem item : req.getEvents()) {
            if (item.getEventType() == null || item.getEventType().isBlank()) continue;
            rows.add(CampaignEvent.builder()
                    .sessionId(sessionId)
                    .eventType(item.getEventType().trim())
                    .path(trimToNull(item.getPath()))
                    .payloadJson(item.getPayloadJson())
                    .createdAt(item.getClientTs() != null ? item.getClientTs() : Instant.now())
                    .build());
        }
        if (!rows.isEmpty()) eventRepository.saveAll(rows);
    }

    @Transactional
    public CampaignDtos.PartAttemptResponse upsertPart(UUID sessionId, CampaignDtos.PartAttemptRequest req) {
        CampaignSession session = requireSession(sessionId);
        // Always insert a new attempt row (history). Score job updates the latest for partIndex.
        CampaignPartAttempt part = CampaignPartAttempt.builder()
                .sessionId(sessionId)
                .packId(trimToNull(req.getPackId()))
                .passageId(req.getPassageId())
                .partIndex(req.getPartIndex())
                .expectedText(req.getExpectedText())
                .spokenTranscript(req.getSpokenTranscript())
                .durationMs(req.getDurationMs())
                .pauseCount(req.getPauseCount() != null ? req.getPauseCount() : 0)
                .wrongCount(req.getWrongCount() != null ? req.getWrongCount() : 0)
                .skipCount(req.getSkipCount() != null ? req.getSkipCount() : 0)
                .wordsTotal(req.getWordsTotal())
                .wordsMatched(req.getWordsMatched())
                .accuracyPct(req.getAccuracyPct())
                .status("pending_score")
                .build();
        part = partRepository.save(part);

        eventRepository.save(CampaignEvent.builder()
                .sessionId(sessionId)
                .eventType("part_complete")
                .path("/try/read")
                .payloadJson("{\"passageId\":\"" + req.getPassageId() + "\",\"partIndex\":" + req.getPartIndex() + "}")
                .build());

        if (Boolean.TRUE.equals(req.getFreePackComplete()) && session.getFreeCompletedAt() == null) {
            session.setFreeCompletedAt(Instant.now());
            sessionRepository.save(session);
            eventRepository.save(CampaignEvent.builder()
                    .sessionId(sessionId)
                    .eventType("free_completed")
                    .path("/try/results")
                    .build());
        }

        return toPartResponse(part);
    }

    @Transactional
    public CampaignDtos.PartAttemptResponse savePartScore(
            UUID sessionId, int partIndex, CampaignDtos.PartScoreRequest req) {
        requireSession(sessionId);
        List<CampaignPartAttempt> parts =
                partRepository.findBySessionIdAndPartIndexOrderByCreatedAtDesc(sessionId, partIndex);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Part attempt not found for index " + partIndex);
        }
        // Prefer the newest pending attempt; else newest overall.
        CampaignPartAttempt part = parts.stream()
                .filter(p -> p.getGeminiScoreJson() == null || "pending_score".equals(p.getStatus()))
                .findFirst()
                .orElse(parts.get(0));
        part.setGeminiScoreJson(req.getGeminiScoreJson());
        part.setStatus(req.getStatus() != null && !req.getStatus().isBlank() ? req.getStatus() : "scored");
        part = partRepository.save(part);
        return toPartResponse(part);
    }

    public Optional<CampaignDtos.PartAttemptResponse> getPart(UUID sessionId, int partIndex) {
        requireSession(sessionId);
        return partRepository.findBySessionIdAndPartIndexOrderByCreatedAtDesc(sessionId, partIndex).stream()
                .findFirst()
                .map(this::toPartResponse);
    }

    public CampaignDtos.MeAttemptsResponse listMyAttempts(String authorization) {
        CognitoUserClaims claims = resolveCampaignAuthClaims(authorization)
                .orElseThrow(() -> new SecurityException("Valid D2C Cognito JWT required"));

        List<CampaignSession> sessions =
                sessionRepository.findByCognitoSubOrderByCreatedAtDesc(claims.getSub());
        if (sessions.isEmpty()) {
            return CampaignDtos.MeAttemptsResponse.builder()
                    .attempts(List.of())
                    .packs(List.of())
                    .build();
        }

        List<UUID> sessionIds = sessions.stream().map(CampaignSession::getId).collect(Collectors.toList());
        Map<UUID, String> sessionPack = sessions.stream()
                .collect(Collectors.toMap(CampaignSession::getId, s -> s.getPackId() != null ? s.getPackId() : "", (a, b) -> a));

        List<CampaignPartAttempt> parts =
                partRepository.findBySessionIdInOrderByCreatedAtDesc(sessionIds);

        List<CampaignDtos.MeAttemptItem> attempts = parts.stream()
                .map(p -> {
                    String packId = p.getPackId() != null && !p.getPackId().isBlank()
                            ? p.getPackId()
                            : sessionPack.getOrDefault(p.getSessionId(), null);
                    return CampaignDtos.MeAttemptItem.builder()
                            .attemptId(p.getId())
                            .sessionId(p.getSessionId())
                            .packId(packId)
                            .passageId(p.getPassageId())
                            .partIndex(p.getPartIndex())
                            .accuracyPct(p.getAccuracyPct())
                            .durationMs(p.getDurationMs())
                            .status(p.getStatus())
                            .geminiScoreJson(p.getGeminiScoreJson())
                            .createdAt(p.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        // One "attempt" = one session for a pack. Latest item = newest part row for that pack.
        Map<String, List<CampaignDtos.MeAttemptItem>> byPack = attempts.stream()
                .filter(a -> a.getPackId() != null && !a.getPackId().isBlank())
                .collect(Collectors.groupingBy(CampaignDtos.MeAttemptItem::getPackId, LinkedHashMap::new, Collectors.toList()));

        List<CampaignDtos.MePackSummary> packSummaries = new ArrayList<>();
        for (Map.Entry<String, List<CampaignDtos.MeAttemptItem>> e : byPack.entrySet()) {
            List<CampaignDtos.MeAttemptItem> items = e.getValue();
            long sessionCount = items.stream().map(CampaignDtos.MeAttemptItem::getSessionId).distinct().count();
            packSummaries.add(CampaignDtos.MePackSummary.builder()
                    .packId(e.getKey())
                    .attemptCount(sessionCount)
                    .latest(items.get(0))
                    .build());
        }

        return CampaignDtos.MeAttemptsResponse.builder()
                .attempts(attempts)
                .packs(packSummaries)
                .build();
    }

    public List<CampaignDtos.PartAttemptResponse> listParts(UUID sessionId) {
        requireSession(sessionId);
        return partRepository.findBySessionIdOrderByPartIndexAsc(sessionId).stream()
                .map(this::toPartResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CampaignDtos.LeadResponse linkAuth(String authorization, CampaignDtos.AuthLinkRequest req) {
        CognitoUserClaims claims = resolveCampaignAuthClaims(authorization)
                .orElseThrow(() -> new SecurityException("Valid D2C Cognito JWT required"));

        String email = normalizeEmail(claims.getEmail());
        CampaignLead lead = leadRepository.findByCognitoSub(claims.getSub())
                .or(() -> email != null ? leadRepository.findByEmailIgnoreCase(email) : Optional.empty())
                .orElseGet(() -> CampaignLead.builder().status("anon").build());

        lead.setCognitoSub(claims.getSub());
        if (email != null) lead.setEmail(email);
        if (req.getDisplayName() != null && !req.getDisplayName().isBlank()) {
            lead.setDisplayName(req.getDisplayName().trim());
        } else if (lead.getDisplayName() == null && claims.getUsername() != null) {
            lead.setDisplayName(claims.getUsername());
        }
        lead.setStatus("verified");
        if (req.getSessionId() != null) {
            lead.setSessionId(req.getSessionId());
            sessionRepository.findById(req.getSessionId()).ifPresent(session -> {
                session.setCognitoSub(claims.getSub());
                if (session.getEmail() == null && email != null) session.setEmail(email);
                sessionRepository.save(session);
            });
            eventRepository.save(CampaignEvent.builder()
                    .sessionId(req.getSessionId())
                    .eventType("login")
                    .path("/try/login")
                    .payloadJson("{\"cognitoSub\":\"" + claims.getSub() + "\"}")
                    .build());
        }
        lead = leadRepository.save(lead);
        upsertD2cUserRow(claims, lead.getDisplayName());
        return toLeadResponse(lead);
    }

    /**
     * Super admin: DB JWT from POST /auth/super-admin/login (preferred),
     * or legacy Cognito / email allowlist.
     */
    public void assertCampaignAdmin(String authorization) {
        if (superAdminJwtService.validateAndExtract(authorization).isPresent()) {
            return;
        }

        CognitoUserClaims claims = cognitoD2cJwtService.validateAndExtract(authorization)
                .or(() -> cognitoJwtService.validateAndExtract(authorization))
                .orElse(null);
        if (claims == null) {
            throw new SecurityException("Super admin login required");
        }

        if (isDbSuperAdmin(claims)) {
            return;
        }
        if (claims.hasGroup("super_admin")) {
            return;
        }

        String email = normalizeEmail(claims.getEmail());
        Set<String> allow = parseAdminEmails();
        if (email != null && allow.contains(email)) {
            return;
        }
        throw new SecurityException("Super admin access required");
    }

    private boolean isDbSuperAdmin(CognitoUserClaims claims) {
        Optional<User> bySub = userRepository.findByCognitoSub(claims.getSub());
        if (bySub.isPresent() && "super_admin".equalsIgnoreCase(bySub.get().getUserType())) {
            return true;
        }
        String email = normalizeEmail(claims.getEmail());
        if (email == null) return false;
        return userRepository.findByEmail(email)
                .map(u -> "super_admin".equalsIgnoreCase(u.getUserType()))
                .orElse(false);
    }

    /** Ensure a users row exists for D2C sign-in so super_admin can be set in DB. */
    private void upsertD2cUserRow(CognitoUserClaims claims, String displayName) {
        Optional<User> existing = userRepository.findByCognitoSub(claims.getSub());
        if (existing.isPresent()) {
            User u = existing.get();
            if (claims.getEmail() != null && !claims.getEmail().isBlank()) {
                u.setEmail(claims.getEmail());
            }
            if (claims.getPhone() != null) u.setPhone(claims.getPhone());
            if (displayName != null && !displayName.isBlank() && u.getDisplayName() == null) {
                u.setDisplayName(displayName);
            }
            // Never downgrade super_admin
            if (u.getUserType() == null) u.setUserType("individual");
            userRepository.save(u);
            return;
        }
        String email = normalizeEmail(claims.getEmail());
        Optional<User> byEmail = email != null ? userRepository.findByEmail(email) : Optional.empty();
        if (byEmail.isPresent()) {
            User u = byEmail.get();
            if (u.getCognitoSub() == null) u.setCognitoSub(claims.getSub());
            if (u.getUserType() == null) u.setUserType("individual");
            userRepository.save(u);
            return;
        }
        userRepository.save(User.builder()
                .cognitoSub(claims.getSub())
                .email(email != null ? email : "")
                .phone(claims.getPhone())
                .username(claims.getUsername() != null ? claims.getUsername() : email)
                .displayName(displayName != null ? displayName : claims.getUsername())
                .userType("individual")
                .passwordHash("COGNITO")
                .build());
    }

    /** Prefer D2C pool; fall back to school pool while D2C JWKS is not configured. */
    private Optional<CognitoUserClaims> resolveCampaignAuthClaims(String authorization) {
        Optional<CognitoUserClaims> d2c = cognitoD2cJwtService.validateAndExtract(authorization);
        if (d2c.isPresent()) return d2c;
        return cognitoJwtService.validateAndExtract(authorization);
    }

    public List<CampaignDtos.SchoolOverviewItem> adminSchoolsOverview() {
        List<School> schools = schoolRepository.findAll();
        List<CampaignDtos.SchoolOverviewItem> out = new ArrayList<>();
        for (School school : schools) {
            UUID id = school.getId();
            long classCount = schoolClassRepository.countBySchoolId(id);
            long studentCount = userRepository.countBySchoolUuidAndUserType(id, "student");
            long adminCount = userRepository.countBySchoolUuidAndUserType(id, "school_admin");
            long teacherCount = teacherRepository.findBySchoolIdOrderByEmail(id).size();
            out.add(CampaignDtos.SchoolOverviewItem.builder()
                    .id(id)
                    .name(school.getName())
                    .classCount(classCount)
                    .studentCount(studentCount)
                    .teacherCount(teacherCount)
                    .adminCount(adminCount)
                    .createdAt(school.getCreatedAt())
                    .build());
        }
        out.sort(Comparator.comparing(CampaignDtos.SchoolOverviewItem::getName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return out;
    }

    public CampaignDtos.SchoolDetailResponse adminSchoolDetail(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found"));

        List<Teacher> teachers = teacherRepository.findBySchoolIdOrderByEmail(schoolId);
        Map<UUID, Teacher> teacherById = teachers.stream()
                .collect(Collectors.toMap(Teacher::getId, t -> t, (a, b) -> a));

        List<CampaignDtos.SchoolPersonItem> teacherItems = teachers.stream()
                .map(t -> CampaignDtos.SchoolPersonItem.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .email(t.getEmail())
                        .userType("teacher")
                        .build())
                .collect(Collectors.toList());

        List<CampaignDtos.SchoolPersonItem> adminItems = userRepository
                .findBySchoolUuidAndUserTypeOrderByEmail(schoolId, "school_admin")
                .stream()
                .map(u -> CampaignDtos.SchoolPersonItem.builder()
                        .id(u.getId())
                        .name(u.getDisplayName())
                        .email(u.getEmail())
                        .userType("school_admin")
                        .build())
                .collect(Collectors.toList());

        List<SchoolClass> classes = schoolClassRepository.findBySchoolIdOrderByName(schoolId);
        List<CampaignDtos.SchoolClassDetail> classDetails = new ArrayList<>();
        long totalStudents = 0;
        for (SchoolClass c : classes) {
            List<User> students = userRepository.findBySchoolUuidAndClassIdAndUserTypeOrderByRollNumber(
                    schoolId, c.getId(), "student");
            totalStudents += students.size();

            List<CampaignDtos.SchoolPersonItem> studentItems = students.stream()
                    .map(s -> CampaignDtos.SchoolPersonItem.builder()
                            .id(s.getId())
                            .name(s.getDisplayName())
                            .email(s.getEmail())
                            .rollNumber(s.getRollNumber())
                            .userType("student")
                            .build())
                    .collect(Collectors.toList());

            List<CampaignDtos.SchoolPersonItem> classTeachers = new ArrayList<>();
            for (TeacherClass tc : teacherClassRepository.findByClassId(c.getId())) {
                Teacher t = teacherById.get(tc.getTeacherId());
                if (t != null) {
                    classTeachers.add(CampaignDtos.SchoolPersonItem.builder()
                            .id(t.getId())
                            .name(t.getName())
                            .email(t.getEmail())
                            .userType("teacher")
                            .build());
                }
            }

            classDetails.add(CampaignDtos.SchoolClassDetail.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .studentCount(students.size())
                    .students(studentItems)
                    .teachers(classTeachers)
                    .build());
        }

        return CampaignDtos.SchoolDetailResponse.builder()
                .id(school.getId())
                .name(school.getName())
                .createdAt(school.getCreatedAt())
                .admins(adminItems)
                .teachers(teacherItems)
                .classes(classDetails)
                .studentCount(totalStudents)
                .teacherCount(teachers.size())
                .classCount(classes.size())
                .adminCount(adminItems.size())
                .build();
    }

    public CampaignDtos.AdminSummaryResponse adminSummary() {
        Map<String, Long> eventCounts = new LinkedHashMap<>();
        for (Object[] row : eventRepository.countGroupedByEventType()) {
            eventCounts.put(String.valueOf(row[0]), (Long) row[1]);
        }

        // Funnel KPIs = distinct sessions per stage (never raw event spam).
        long totalSessions = sessionRepository.count();
        long opens = Math.max(
                eventRepository.countDistinctSessionsByEventType("page_open"),
                totalSessions
        );
        long startedRead = eventRepository.countDistinctSessionsByEventType("read_start");
        long spoke = eventRepository.countDistinctSessionsByEventTypes(
                List.of("mic_start", "speech_final")
        );
        long freeCompleted = Math.max(
                sessionRepository.countByFreeCompletedAtIsNotNull(),
                eventRepository.countDistinctSessionsByEventType("free_completed")
        );
        long signedUp = Math.max(
                sessionRepository.countByCognitoSubIsNotNull(),
                eventRepository.countDistinctSessionsByEventType("signup")
        );
        long loggedIn = eventRepository.countDistinctSessionsByEventType("login");

        // Reading funnel is monotonic (unique sessions). Auth steps are capped only by opens
        // (someone can sign up from landing without finishing the free try).
        startedRead = Math.min(startedRead, opens);
        spoke = Math.min(spoke, startedRead > 0 ? startedRead : opens);
        freeCompleted = Math.min(
                freeCompleted,
                spoke > 0 ? spoke : (startedRead > 0 ? startedRead : opens)
        );
        signedUp = Math.min(signedUp, opens);
        loggedIn = Math.min(loggedIn, opens);

        List<CampaignPartAttempt> allParts = partRepository.findAll();
        Double avg = null;
        List<Double> scores = new ArrayList<>();
        for (CampaignPartAttempt p : allParts) {
            Double s = extractScore(p.getGeminiScoreJson());
            if (s != null) scores.add(s);
        }
        if (!scores.isEmpty()) {
            avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }

        return CampaignDtos.AdminSummaryResponse.builder()
                .opens(opens)
                .startedRead(startedRead)
                .spoke(spoke)
                .freeCompleted(freeCompleted)
                .signedUp(signedUp)
                .loggedIn(loggedIn)
                .avgGeminiScore(avg)
                .eventCounts(eventCounts)
                .totalSessions(totalSessions)
                .totalLeads(leadRepository.count())
                .build();
    }

    public List<CampaignDtos.LeadResponse> adminLeads() {
        return leadRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toLeadResponse)
                .collect(Collectors.toList());
    }

    public CampaignDtos.SessionDetailResponse adminSessionDetail(UUID sessionId) {
        CampaignSession session = requireSession(sessionId);
        List<CampaignDtos.PartAttemptResponse> parts = listParts(sessionId);
        List<CampaignDtos.EventItem> events = eventRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(e -> {
                    CampaignDtos.EventItem item = new CampaignDtos.EventItem();
                    item.setEventType(e.getEventType());
                    item.setPath(e.getPath());
                    item.setPayloadJson(e.getPayloadJson());
                    item.setClientTs(e.getCreatedAt());
                    return item;
                })
                .collect(Collectors.toList());
        CampaignDtos.LeadResponse lead = null;
        if (session.getCognitoSub() != null) {
            lead = leadRepository.findByCognitoSub(session.getCognitoSub()).map(this::toLeadResponse).orElse(null);
        } else if (session.getEmail() != null) {
            lead = leadRepository.findByEmailIgnoreCase(session.getEmail()).map(this::toLeadResponse).orElse(null);
        }
        return CampaignDtos.SessionDetailResponse.builder()
                .session(toSessionResponse(session))
                .parts(parts)
                .events(events)
                .lead(lead)
                .build();
    }

    public List<CampaignDtos.SessionResponse> adminSessions() {
        return sessionRepository.findAll().stream()
                .sorted(Comparator.comparing(CampaignSession::getCreatedAt).reversed())
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    private CampaignSession requireSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign session not found"));
    }

    private CampaignDtos.SessionResponse toSessionResponse(CampaignSession s) {
        return CampaignDtos.SessionResponse.builder()
                .id(s.getId())
                .email(s.getEmail())
                .packId(s.getPackId())
                .startedAt(s.getStartedAt())
                .freeCompletedAt(s.getFreeCompletedAt())
                .cognitoSub(s.getCognitoSub())
                .build();
    }

    private CampaignDtos.PartAttemptResponse toPartResponse(CampaignPartAttempt p) {
        return CampaignDtos.PartAttemptResponse.builder()
                .id(p.getId())
                .sessionId(p.getSessionId())
                .packId(p.getPackId())
                .passageId(p.getPassageId())
                .partIndex(p.getPartIndex())
                .status(p.getStatus())
                .geminiScoreJson(p.getGeminiScoreJson())
                .accuracyPct(p.getAccuracyPct())
                .durationMs(p.getDurationMs())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private CampaignDtos.LeadResponse toLeadResponse(CampaignLead lead) {
        return CampaignDtos.LeadResponse.builder()
                .id(lead.getId())
                .email(lead.getEmail())
                .cognitoSub(lead.getCognitoSub())
                .displayName(lead.getDisplayName())
                .status(lead.getStatus())
                .sessionId(lead.getSessionId())
                .createdAt(lead.getCreatedAt())
                .build();
    }

    private Double extractScore(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("score0to100")) return node.get("score0to100").asDouble();
            if (node.has("score")) return node.get("score").asDouble();
        } catch (Exception e) {
            log.debug("Could not parse gemini score json: {}", e.getMessage());
        }
        return null;
    }

    private Set<String> parseAdminEmails() {
        if (adminEmailsCsv == null || adminEmailsCsv.isBlank()) return Set.of();
        return Arrays.stream(adminEmailsCsv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return email.trim().toLowerCase();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
