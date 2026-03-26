package digital.pragmatech.tour_de_jug.service;

import digital.pragmatech.tour_de_jug.domain.AppUser;
import digital.pragmatech.tour_de_jug.repository.AppUserRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the OIDC login flow (used when the openid scope is present, e.g. Keycloak).
 * Syncs user attributes to the DB using the same claim names as the OAuth2 flow.
 */
@Service
public class OidcUserSyncService extends OidcUserService {

    private final AppUserRepository appUserRepository;

    public OidcUserSyncService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        Object rawId = oidcUser.getClaim("id");
        if (rawId == null) {
            rawId = oidcUser.getAttribute("id");
        }
        if (rawId == null) {
            return oidcUser;
        }

        Long githubId = rawId instanceof Number ? ((Number) rawId).longValue() : Long.parseLong(rawId.toString());
        String username = oidcUser.getClaim("login");
        String avatarUrl = oidcUser.getClaim("avatar_url");
        String email = oidcUser.getEmail();
        String name = oidcUser.getClaim("name");

        AppUser appUser = appUserRepository.findByGithubId(githubId)
                .orElse(new AppUser(githubId, username));

        appUser.setUsername(username);
        appUser.setAvatarUrl(avatarUrl);
        appUser.setEmail(email);
        if (name != null && !name.isBlank()) {
            appUser.setDisplayName(name);
        }

        appUserRepository.save(appUser);

        return oidcUser;
    }
}
