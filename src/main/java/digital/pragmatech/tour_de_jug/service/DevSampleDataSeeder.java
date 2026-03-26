package digital.pragmatech.tour_de_jug.service;

import digital.pragmatech.tour_de_jug.domain.AppUser;
import digital.pragmatech.tour_de_jug.domain.JavaUserGroup;
import digital.pragmatech.tour_de_jug.domain.TalkEvent;
import digital.pragmatech.tour_de_jug.domain.TalkStatus;
import digital.pragmatech.tour_de_jug.repository.AppUserRepository;
import digital.pragmatech.tour_de_jug.repository.JavaUserGroupRepository;
import digital.pragmatech.tour_de_jug.repository.TalkEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

/**
 * Seeds sample speakers and talk events for local development.
 * Only active with the "dev" Spring profile.
 * Runs after JugImportService (@Order(1) vs default) so JUG data is available.
 */
@Component
@Profile("dev")
@Order(1)
public class DevSampleDataSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DevSampleDataSeeder.class);

    private final AppUserRepository userRepository;
    private final JavaUserGroupRepository jugRepository;
    private final TalkEventRepository talkEventRepository;

    public DevSampleDataSeeder(AppUserRepository userRepository,
                               JavaUserGroupRepository jugRepository,
                               TalkEventRepository talkEventRepository) {
        this.userRepository = userRepository;
        this.jugRepository = jugRepository;
        this.talkEventRepository = talkEventRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            logger.info("Sample data already present, skipping seeder");
            return;
        }

        logger.info("Seeding sample speaker data for local development...");

        // ── Speakers ────────────────────────────────────────────────────────────
        var anna = createUser(100001L, "anna-dev", "Anna Müller",
                "https://i.pravatar.cc/150?u=anna-dev", "anna@example.com");

        var marco = createUser(100002L, "marco-java", "Marco Rossi",
                "https://i.pravatar.cc/150?u=marco-java", "marco@example.com");

        var sarah = createUser(100003L, "sarahcodes", "Sarah Chen",
                "https://i.pravatar.cc/150?u=sarahcodes", "sarah@example.com");

        var james = createUser(100004L, "jamesjug", "James O'Brien",
                "https://i.pravatar.cc/150?u=jamesjug", "james@example.com");

        var yuki = createUser(100005L, "yuki-spring", "Yuki Tanaka",
                "https://i.pravatar.cc/150?u=yuki-spring", "yuki@example.com");

        // ── JUG references ──────────────────────────────────────────────────────
        Map<String, JavaUserGroup> jugs = Map.of(
                "london", jugRepository.findBySlug("londonjavacommunity").orElseThrow(),
                "hamburg", jugRepository.findBySlug("jug-hamburg").orElseThrow(),
                "zurich", jugRepository.findBySlug("jug-ch").orElseThrow(),
                "amsterdam", jugRepository.findBySlug("nljug").orElseThrow(),
                "stockholm", jugRepository.findBySlug("jfokus").orElseThrow(),
                "paris", jugRepository.findBySlug("paris-jug").orElseThrow(),
                "madrid", jugRepository.findBySlug("madrid-jug").orElseThrow(),
                "berlin", jugRepository.findBySlug("jug-bb").orElseThrow(),
                "prague", jugRepository.findBySlug("czjug").orElseThrow(),
                "sf", jugRepository.findBySlug("sfjug").orElseThrow()
        );

        // ── Anna: prolific European tour speaker (12 talks, 2023–2026) ──────────
        createTalk(anna, jugs.get("berlin"), "Getting Started with Virtual Threads",
                date(2023, 3, 15), TalkStatus.APPROVED,
                "https://speakerdeck.com/anna/virtual-threads", "https://youtube.com/watch?v=example1");
        createTalk(anna, jugs.get("hamburg"), "Spring Boot 3 Migration Guide",
                date(2023, 6, 22), TalkStatus.APPROVED,
                "https://speakerdeck.com/anna/sb3-migration", null);
        createTalk(anna, jugs.get("zurich"), "Testcontainers in Practice",
                date(2023, 10, 10), TalkStatus.APPROVED,
                null, "https://youtube.com/watch?v=example2");
        createTalk(anna, jugs.get("amsterdam"), "GraalVM Native Images for Spring Apps",
                date(2024, 1, 18), TalkStatus.APPROVED,
                "https://speakerdeck.com/anna/graalvm-spring", null);
        createTalk(anna, jugs.get("paris"), "Structured Concurrency in Java 21",
                date(2024, 4, 25), TalkStatus.APPROVED,
                "https://speakerdeck.com/anna/structured-concurrency",
                "https://youtube.com/watch?v=example3");
        createTalk(anna, jugs.get("london"), "Building Resilient Microservices",
                date(2024, 9, 12), TalkStatus.APPROVED,
                "https://speakerdeck.com/anna/resilient-ms", null);
        createTalk(anna, jugs.get("madrid"), "Java Records and Sealed Classes in Production",
                date(2024, 11, 7), TalkStatus.APPROVED,
                null, null);
        createTalk(anna, jugs.get("prague"), "Pattern Matching: Beyond instanceof",
                date(2025, 2, 20), TalkStatus.APPROVED,
                "https://speakerdeck.com/anna/pattern-matching", null);
        createTalk(anna, jugs.get("stockholm"), "Observability with Micrometer and Grafana",
                date(2025, 5, 14), TalkStatus.APPROVED,
                null, "https://youtube.com/watch?v=example4");
        createTalk(anna, jugs.get("berlin"), "Spring Boot 4: What's New",
                date(2025, 11, 6), TalkStatus.APPROVED,
                "https://speakerdeck.com/anna/sb4-new", null);
        createTalk(anna, jugs.get("hamburg"), "Project Loom in Real-World Applications",
                date(2026, 2, 19), TalkStatus.APPROVED,
                null, null);
        createTalk(anna, jugs.get("zurich"), "Building AI-Powered Java Apps with LangChain4j",
                date(2026, 5, 22), TalkStatus.PENDING,
                null, null);

        // ── Marco: Spring Security specialist (7 talks, 2024–2026) ──────────────
        createTalk(marco, jugs.get("paris"), "OAuth2 and OpenID Connect Deep Dive",
                date(2024, 2, 8), TalkStatus.APPROVED,
                "https://speakerdeck.com/marco/oauth2-deep-dive",
                "https://youtube.com/watch?v=example5");
        createTalk(marco, jugs.get("amsterdam"), "Securing Spring Boot APIs",
                date(2024, 5, 16), TalkStatus.APPROVED,
                "https://speakerdeck.com/marco/securing-sb-apis", null);
        createTalk(marco, jugs.get("london"), "JWT vs Session: The Great Debate",
                date(2024, 9, 19), TalkStatus.APPROVED,
                null, "https://youtube.com/watch?v=example6");
        createTalk(marco, jugs.get("madrid"), "CORS, CSP, and Security Headers Explained",
                date(2024, 12, 5), TalkStatus.APPROVED,
                "https://speakerdeck.com/marco/security-headers", null);
        createTalk(marco, jugs.get("berlin"), "Spring Authorization Server in Production",
                date(2025, 3, 13), TalkStatus.APPROVED,
                null, null);
        createTalk(marco, jugs.get("zurich"), "Passkeys and WebAuthn with Spring Security",
                date(2025, 9, 25), TalkStatus.APPROVED,
                "https://speakerdeck.com/marco/passkeys-spring", null);
        createTalk(marco, jugs.get("hamburg"), "Zero Trust Architecture for Java Backends",
                date(2026, 4, 10), TalkStatus.PENDING,
                null, null);

        // ── Sarah: testing & quality advocate (8 talks, 2023–2026) ──────────────
        createTalk(sarah, jugs.get("sf"), "Property-Based Testing in Java",
                date(2023, 5, 11), TalkStatus.APPROVED,
                "https://speakerdeck.com/sarah/pbt-java",
                "https://youtube.com/watch?v=example7");
        createTalk(sarah, jugs.get("london"), "Testcontainers: From Zero to Hero",
                date(2023, 9, 28), TalkStatus.APPROVED,
                "https://speakerdeck.com/sarah/testcontainers-hero", null);
        createTalk(sarah, jugs.get("amsterdam"), "Mutation Testing with Pitest",
                date(2024, 1, 25), TalkStatus.APPROVED,
                null, "https://youtube.com/watch?v=example8");
        createTalk(sarah, jugs.get("berlin"), "ArchUnit: Enforcing Architecture in Tests",
                date(2024, 6, 13), TalkStatus.APPROVED,
                "https://speakerdeck.com/sarah/archunit", null);
        createTalk(sarah, jugs.get("prague"), "Contract Testing with Spring Cloud Contract",
                date(2024, 10, 17), TalkStatus.APPROVED,
                null, null);
        createTalk(sarah, jugs.get("sf"), "Selenium vs Playwright vs Selenide",
                date(2025, 3, 6), TalkStatus.APPROVED,
                "https://speakerdeck.com/sarah/browser-testing",
                "https://youtube.com/watch?v=example9");
        createTalk(sarah, jugs.get("stockholm"), "Wiremock for API Mocking at Scale",
                date(2025, 8, 21), TalkStatus.APPROVED,
                null, null);
        createTalk(sarah, jugs.get("paris"), "AI-Assisted Testing: Hype or Reality?",
                date(2026, 6, 12), TalkStatus.PENDING,
                null, null);

        // ── James: Kotlin & JVM polyglot (5 talks, 2025–2026) ──────────────────
        createTalk(james, jugs.get("london"), "Kotlin Coroutines for Java Developers",
                date(2025, 1, 23), TalkStatus.APPROVED,
                "https://speakerdeck.com/james/kotlin-coroutines",
                "https://youtube.com/watch?v=example10");
        createTalk(james, jugs.get("amsterdam"), "Kotlin Multiplatform: Share Code Everywhere",
                date(2025, 4, 10), TalkStatus.APPROVED,
                "https://speakerdeck.com/james/kmp", null);
        createTalk(james, jugs.get("hamburg"), "Ktor vs Spring Boot: An Honest Comparison",
                date(2025, 7, 17), TalkStatus.APPROVED,
                null, "https://youtube.com/watch?v=example11");
        createTalk(james, jugs.get("madrid"), "Arrow-kt: Functional Programming on the JVM",
                date(2025, 11, 20), TalkStatus.APPROVED,
                null, null);
        createTalk(james, jugs.get("berlin"), "Scala 3, Kotlin, or Java 21? Picking the Right JVM Language",
                date(2026, 3, 12), TalkStatus.PENDING,
                null, null);

        // ── Yuki: cloud-native & Kubernetes (6 talks, 2024–2026) ────────────────
        createTalk(yuki, jugs.get("stockholm"), "Spring Cloud Kubernetes in Production",
                date(2024, 3, 7), TalkStatus.APPROVED,
                "https://speakerdeck.com/yuki/spring-k8s",
                "https://youtube.com/watch?v=example12");
        createTalk(yuki, jugs.get("paris"), "Java on ARM: Performance and Pitfalls",
                date(2024, 7, 18), TalkStatus.APPROVED,
                null, null);
        createTalk(yuki, jugs.get("zurich"), "Quarkus vs Spring Boot: Cloud-Native Showdown",
                date(2024, 11, 14), TalkStatus.APPROVED,
                "https://speakerdeck.com/yuki/quarkus-vs-sb", null);
        createTalk(yuki, jugs.get("sf"), "Building Serverless Java with AWS Lambda SnapStart",
                date(2025, 2, 27), TalkStatus.APPROVED,
                null, "https://youtube.com/watch?v=example13");
        createTalk(yuki, jugs.get("london"), "OpenTelemetry for Java Microservices",
                date(2025, 6, 5), TalkStatus.APPROVED,
                "https://speakerdeck.com/yuki/otel-java", null);
        createTalk(yuki, jugs.get("prague"), "Sustainable Java: Reducing Cloud Carbon Footprint",
                date(2026, 7, 9), TalkStatus.PENDING,
                null, null);

        logger.info("Sample data seeded: 5 speakers, 38 talks across 10 JUGs");
    }

    private AppUser createUser(Long githubId, String username, String displayName,
                               String avatarUrl, String email) {
        var user = new AppUser(githubId, username);
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);
        user.setEmail(email);
        return userRepository.save(user);
    }

    private void createTalk(AppUser speaker, JavaUserGroup jug, String title,
                            Instant eventTime, TalkStatus status,
                            String slidesUrl, String recordingUrl) {
        var talk = new TalkEvent();
        talk.setSpeaker(speaker);
        talk.setJug(jug);
        talk.setTalkTitle(title);
        talk.setEventTime(eventTime);
        talk.setStatus(status);
        talk.setSlidesUrl(slidesUrl);
        talk.setRecordingUrl(recordingUrl);
        talkEventRepository.save(talk);
    }

    private Instant date(int year, int month, int day) {
        return LocalDate.of(year, month, day).atTime(18, 30).toInstant(ZoneOffset.UTC);
    }
}
