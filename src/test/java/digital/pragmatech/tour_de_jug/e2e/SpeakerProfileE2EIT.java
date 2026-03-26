package digital.pragmatech.tour_de_jug.e2e;

import com.codeborne.selenide.CollectionCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;

/**
 * E2E tests for the speaker profile and the full talk-submission flow.
 *
 * WireMock simulates GitHub's three OAuth2 endpoints so the login redirect
 * chain completes locally without any network calls to github.com.
 *
 * Each test starts with a clean anonymous browser session (cookies cleared
 * in {@link BaseE2EIT#prepareForTest()}).
 *
 * Flow under test:
 *   anonymous → login (WireMock OAuth) → public profile → add talk → profile shows talk
 */
@DisplayName("Speaker profile and talk submission")
class SpeakerProfileE2EIT extends BaseE2EIT {

    @Test
    @DisplayName("GitHub OAuth login completes and shows username in navbar")
    void loginWithGitHubShowsUsernameInNavbar() {
        performLogin();

        // After the OAuth redirect chain the navbar must show the authenticated username.
        $("nav").shouldHave(text(GitHubMockServer.FAKE_USERNAME));

        // The login link must be gone; a logout link should be present.
        $("a[href='/oauth2/authorization/github']").shouldNotBe(visible);
    }

    @Test
    @DisplayName("authenticated user can see their own profile link in the navbar")
    void authenticatedUserSeesProfileLinkInNavbar() {
        performLogin();

        // The navbar renders a link like /profiles/testuser for authenticated users.
        $("nav a[href*='/profiles/" + GitHubMockServer.FAKE_USERNAME + "']")
            .shouldBe(visible);
    }

    @Test
    @DisplayName("public profile page shows avatar, display name, and stats grid")
    void publicProfilePageRendersCorrectly() {
        // Log in so that OAuthUserService persists the user to the DB.
        performLogin();

        open("/profiles/" + GitHubMockServer.FAKE_USERNAME);

        // Display name injected by the WireMock /user stub
        $("h1").shouldHave(text(GitHubMockServer.FAKE_DISPLAY_NAME));

        // GitHub link shows the @handle
        $("a[href*='github.com/" + GitHubMockServer.FAKE_USERNAME + "']")
            .shouldBe(visible)
            .shouldHave(text("@" + GitHubMockServer.FAKE_USERNAME));

        // Avatar image rendered (src comes from the fake avatar URL)
        $("img[src*='" + GitHubMockServer.FAKE_GITHUB_ID + "']").shouldBe(visible);

        // Three stat boxes: total talks, this year, speaking since
        $(".grid .text-2xl").shouldBe(visible);
    }

    @Test
    @DisplayName("own profile shows edit and add-talk buttons; other profiles do not")
    void ownProfileShowsEditButtons() {
        performLogin();

        open("/profiles/" + GitHubMockServer.FAKE_USERNAME);

        // Edit Profile and Add Talk buttons are visible on own profile
        $("a[href*='/edit']").shouldBe(visible);
        $("a[href='/speaking-events/new']").shouldBe(visible);
    }

    @Test
    @DisplayName("unauthenticated visit to /speaking-events/new does not show the form")
    void unauthenticatedNewTalkRedirectsToLogin() {
        // Remove all WireMock stubs so the GitHub OAuth flow cannot complete.
        // Spring Security still redirects the browser to the WireMock authorization
        // URL, but WireMock returns a 404 (no matching stub), so the callback with
        // the auth-code never reaches the app and the user stays unauthenticated.
        GITHUB_MOCK.resetAll();

        open("/speaking-events/new");

        // The talk form must not be accessible — the browser is stuck on
        // WireMock's 404 page, not on the Spring app's form page.
        $("input[name='talkTitle']").shouldNotBe(visible);
    }

    @Test
    @DisplayName("authenticated user can submit a new talk and it appears on their profile")
    void submitNewTalkAppearsOnProfile() {
        performLogin();

        open("/speaking-events/new");

        // ── Fill in the form ────────────────────────────────────────────────
        $("input[name='talkTitle']").setValue("Spring Boot 4 Deep Dive");

        // JUG autocomplete: type a partial name, wait for suggestions, pick first.
        $("#jugSearch").setValue("London");
        $$("#jugSuggestions div").shouldHave(CollectionCondition.sizeGreaterThan(0));
        $$("#jugSuggestions div").first().click();

        // Set event date/time via JavaScript — more reliable than sendKeys
        // for datetime-local inputs across browser versions.
        executeJavaScript(
            "document.querySelector('input[name=\"eventTime\"]').value = '2026-06-15T14:00'"
        );

        $("form button[type='submit']").click();

        // ── Verify the talk appears on the profile ──────────────────────────
        // The controller redirects to /profiles/{username} after a successful POST.
        $("h1").shouldHave(text(GitHubMockServer.FAKE_DISPLAY_NAME));

        // The talk title and status badge must appear in the talk list.
        $$(".talk-item").findBy(text("Spring Boot 4 Deep Dive")).shouldBe(visible);
        $$(".talk-item").findBy(text("PENDING")).shouldBe(visible);
    }

    @Test
    @DisplayName("speaker can update the display name via the edit form")
    void editProfileUpdatesDisplayName() {
        performLogin();

        open("/profiles/" + GitHubMockServer.FAKE_USERNAME + "/edit");

        $("input[name='displayName']").setValue("Updated Name");
        $("button[type='submit']").click();

        // Redirected back to public profile
        $("h1").shouldHave(text("Updated Name"));
    }

    @Test
    @DisplayName("talk with slides URL shows a slides link on the profile")
    void talkWithSlidesUrlShowsLink() {
        performLogin();

        open("/speaking-events/new");

        $("input[name='talkTitle']").setValue("Talk With Slides");

        $("#jugSearch").setValue("Hamburg");
        $$("#jugSuggestions div").shouldHave(CollectionCondition.sizeGreaterThan(0));
        $$("#jugSuggestions div").first().click();

        executeJavaScript(
            "document.querySelector('input[name=\"eventTime\"]').value = '2026-09-10T18:00'"
        );

        $("input[name='slidesUrl']").setValue("https://speakerdeck.com/testuser/slides");

        $("form button[type='submit']").click();

        // Profile shows the slides link for this talk
        $("a[href='https://speakerdeck.com/testuser/slides']")
            .shouldBe(visible)
            .shouldHave(text("Slides"));
    }
}
