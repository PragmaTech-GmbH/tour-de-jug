package digital.pragmatech.tour_de_jug.e2e;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Configures WireMock stubs that replicate the three GitHub OAuth2 endpoints
 * the Spring Security client talks to during a login flow:
 *
 *  1. GET  /login/oauth/authorize      — browser redirect (authorization endpoint)
 *  2. POST /login/oauth/access_token   — server-side token exchange
 *  3. GET  /user                       — user-info fetch with Bearer token
 *
 * The authorization stub uses WireMock response templating to echo back the
 * {@code state} and {@code redirect_uri} query parameters that Spring Security
 * injects into the request, so the CSRF protection inside Spring Security
 * stays satisfied even with a fake provider.
 */
class GitHubMockServer {

    static final long   FAKE_GITHUB_ID    = 99_999L;
    static final String FAKE_USERNAME     = "testuser";
    static final String FAKE_DISPLAY_NAME = "Test User";
    static final String FAKE_EMAIL        = "testuser@example.com";
    static final String FAKE_AVATAR_URL   = "https://avatars.githubusercontent.com/u/99999";

    private static final String FAKE_AUTH_CODE    = "fake-auth-code-xyz";
    private static final String FAKE_ACCESS_TOKEN = "ghs_fakeTokenForLocalTesting";

    static void setupStubs(WireMockServer server) {
        server.resetAll();
        stubAuthorizeEndpoint(server);
        stubTokenEndpoint(server);
        stubUserInfoEndpoint(server);
    }

    /**
     * Mimics https://github.com/login/oauth/authorize
     *
     * Spring Security redirects the browser here with:
     *   ?response_type=code&client_id=...&redirect_uri=...&scope=...&state=RANDOM
     *
     * A real GitHub page would show a consent screen; we immediately redirect
     * back to the app's callback URL with the auth code and the original state
     * (required by the PKCE / state verification in Spring Security).
     *
     * The Location header is built via WireMock response templating:
     *   {{request.query.redirect_uri}} — the callback URL our app registered
     *   {{request.query.state}}        — the CSRF state token Spring generated
     */
    private static void stubAuthorizeEndpoint(WireMockServer server) {
        server.stubFor(
            get(urlPathEqualTo("/login/oauth/authorize"))
                .willReturn(aResponse()
                    .withStatus(302)
                    .withHeader("Location",
                        "{{request.query.redirect_uri}}" +
                        "?code=" + FAKE_AUTH_CODE +
                        "&state={{request.query.state}}")
                    .withTransformers("response-template"))
        );
    }

    /**
     * Mimics https://github.com/login/oauth/access_token
     *
     * Spring Security POSTs the auth code here and expects a JSON token response.
     * We don't validate the code value — any request hits this stub.
     */
    private static void stubTokenEndpoint(WireMockServer server) {
        server.stubFor(
            post(urlPathEqualTo("/login/oauth/access_token"))
                .withHeader("Accept", containing("application/json"))
                .willReturn(okJson("""
                    {
                      "access_token": "%s",
                      "token_type":   "bearer",
                      "scope":        "read:user,user:email"
                    }
                    """.formatted(FAKE_ACCESS_TOKEN)))
        );
    }

    /**
     * Mimics https://api.github.com/user
     *
     * Spring Security fetches the authenticated user's profile with the
     * Bearer token it obtained in the previous step.
     *
     * The numeric {@code id} field is the stable GitHub user ID stored as
     * {@code github_id} in our {@code app_user} table.
     */
    private static void stubUserInfoEndpoint(WireMockServer server) {
        server.stubFor(
            get(urlPathEqualTo("/user"))
                .withHeader("Authorization", equalTo("Bearer " + FAKE_ACCESS_TOKEN))
                .willReturn(okJson("""
                    {
                      "id":         %d,
                      "login":      "%s",
                      "name":       "%s",
                      "email":      "%s",
                      "avatar_url": "%s"
                    }
                    """.formatted(
                        FAKE_GITHUB_ID,
                        FAKE_USERNAME,
                        FAKE_DISPLAY_NAME,
                        FAKE_EMAIL,
                        FAKE_AVATAR_URL)))
        );
    }
}
