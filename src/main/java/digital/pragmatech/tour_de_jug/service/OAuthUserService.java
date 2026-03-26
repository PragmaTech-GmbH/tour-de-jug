package digital.pragmatech.tour_de_jug.service;

import digital.pragmatech.tour_de_jug.domain.AppUser;
import digital.pragmatech.tour_de_jug.repository.AppUserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthUserService extends DefaultOAuth2UserService {

    private final AppUserRepository appUserRepository;

    public OAuthUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Object rawId = oAuth2User.getAttribute("id");
        Long githubId = rawId instanceof Number ? ((Number) rawId).longValue() : Long.parseLong(rawId.toString());
        String username = oAuth2User.getAttribute("login");
        String avatarUrl = oAuth2User.getAttribute("avatar_url");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        AppUser appUser = appUserRepository.findByGithubId(githubId)
                .orElse(new AppUser(githubId, username));

        appUser.setUsername(username);
        appUser.setAvatarUrl(avatarUrl);
        appUser.setEmail(email);
        if (name != null && !name.isBlank()) {
            appUser.setDisplayName(name);
        }

        appUserRepository.save(appUser);

        return oAuth2User;
    }
}
