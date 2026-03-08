package digital.pragmatech.tour_de_jug.e2e;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import digital.pragmatech.tour_de_jug.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.codeborne.selenide.Selenide.open;

/**
 * Base class for all Selenide E2E tests.
 *
 * Architecture
 * ────────────
 *  ┌──────────────────────────────────────────────────────────┐
 *  │  Browser (headless Chrome via Selenide)                  │
 *  │     │ navigates to localhost:{randomPort}                │
 *  │     ▼                                                    │
 *  │  Spring Boot app (RANDOM_PORT + PostgreSQL Testcontainer)│
 *  │     │ OAuth2 redirects to WireMock instead of GitHub     │
 *  │     ▼                                                    │
 *  │  WireMock (dynamic port, stubs GitHub OAuth endpoints)   │
 *  └──────────────────────────────────────────────────────────┘
 *
 * WireMock is started once per JVM (static initialiser) so that all
 * E2E test classes share the same Spring application context — avoiding
 * a costly restart between EntryPageE2EIT and SpeakerProfileE2EIT.
 *
 * The browser is also kept open across tests within a single class and
 * torn down in {@link #closeBrowser()}. Cookies are cleared before each
 * test to guarantee a clean anonymous session.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseE2EIT {

    // ── WireMock — started once for the entire test run ───────────────────────

    static final WireMockServer GITHUB_MOCK;

    static {
        GITHUB_MOCK = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        GITHUB_MOCK.start();
    }

    /**
     * Override Spring Security's OAuth2 provider URLs to point to WireMock.
     * Because GITHUB_MOCK is already started (static block above) the port is
     * stable, so Spring's test-context cache can reuse the same context across
     * all subclasses.
     */
    @DynamicPropertySource
    static void overrideOAuth2Endpoints(DynamicPropertyRegistry registry) {
        String base = GITHUB_MOCK.baseUrl();   // e.g. http://localhost:56432

        // Use harmless placeholder credentials — WireMock doesn't validate them.
        registry.add("spring.security.oauth2.client.registration.github.client-id",
            () -> "wiremock-client-id");
        registry.add("spring.security.oauth2.client.registration.github.client-secret",
            () -> "wiremock-client-secret");

        // Redirect the three GitHub OAuth endpoints to our local WireMock.
        registry.add("spring.security.oauth2.client.provider.github.authorization-uri",
            () -> base + "/login/oauth/authorize");
        registry.add("spring.security.oauth2.client.provider.github.token-uri",
            () -> base + "/login/oauth/access_token");
        registry.add("spring.security.oauth2.client.provider.github.user-info-uri",
            () -> base + "/user");

        // Tell Spring Security which field in the user-info JSON to use as the principal name.
        registry.add("spring.security.oauth2.client.provider.github.user-name-attribute",
            () -> "login");
    }

    // ── Selenide — browser kept alive for the whole test class ────────────────

    @LocalServerPort
    private int serverPort;

    @BeforeAll
    static void configureBrowser() {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments(
            "--headless=new",          // new headless mode (Chrome 112+)
            "--no-sandbox",            // required in CI / Docker
            "--disable-dev-shm-usage", // prevents OOM in containers
            "--disable-gpu",
            "--window-size=1920,1080"
        );
        Configuration.browserCapabilities = chromeOptions;
        Configuration.browser     = "chrome";
        Configuration.timeout     = 10_000;
        Configuration.browserSize = "1920x1080";
        // Store screenshots under target/ so the CI artifact upload step finds them.
        Configuration.reportsFolder = "target/selenide-screenshots";
        // Selenide auto-downloads the matching ChromeDriver via WebDriverManager.
    }

    @BeforeEach
    void prepareForTest() {
        // Point Selenide at the random port Spring chose for this test run.
        Configuration.baseUrl = "http://localhost:" + serverPort;

        // Reset WireMock stubs so each test starts with a clean slate,
        // then install the standard GitHub stubs.
        GitHubMockServer.setupStubs(GITHUB_MOCK);

        // Clear browser cookies → ensures an anonymous (logged-out) session.
        // open() first so the WebDriver has a valid origin to clear cookies for.
        open("/");
        WebDriverRunner.getWebDriver().manage().deleteAllCookies();
    }

    @AfterEach
    void captureFailureScreenshot() {
        // Selenide takes a screenshot automatically on assertion failure;
        // they land in build/reports/tests/ by default.
    }

    @AfterAll
    static void closeBrowser() {
        if (WebDriverRunner.hasWebDriverStarted()) {
            WebDriverRunner.closeWebDriver();
        }
    }

    // ── Helper — perform a full GitHub OAuth login via WireMock ───────────────

    /**
     * Navigates to the home page, clicks "Login with GitHub", follows the
     * WireMock-backed OAuth redirect chain, and waits until the navbar shows
     * the authenticated username.
     *
     * Call this at the start of any test that requires an authenticated session.
     */
    void performLogin() {
        open("/");
        // The login link is only visible when the user is not authenticated.
        com.codeborne.selenide.Selenide.$("a[href='/oauth2/authorization/github']").click();
        // After the redirect chain (app → WireMock → app callback → home), the
        // navbar should show the username injected by GitHubMockServer.
        com.codeborne.selenide.Selenide.$("nav")
            .shouldHave(com.codeborne.selenide.Condition.text(GitHubMockServer.FAKE_USERNAME),
                java.time.Duration.ofSeconds(15));
    }
}
