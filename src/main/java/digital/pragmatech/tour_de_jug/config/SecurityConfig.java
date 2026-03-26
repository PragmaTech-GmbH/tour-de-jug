package digital.pragmatech.tour_de_jug.config;

import digital.pragmatech.tour_de_jug.service.OAuthUserService;
import digital.pragmatech.tour_de_jug.service.OidcUserSyncService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuthUserService oAuthUserService;
    private final OidcUserSyncService oidcUserSyncService;

    public SecurityConfig(OAuthUserService oAuthUserService, OidcUserSyncService oidcUserSyncService) {
        this.oAuthUserService = oAuthUserService;
        this.oidcUserSyncService = oidcUserSyncService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/profiles/**", "/jugs/**", "/css/**", "/js/**", "/api/**", "/actuator/**").permitAll()
                .requestMatchers("/speaking-events/new", "/profiles/*/edit").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuthUserService)
                    .oidcUserService(oidcUserSyncService)
                )
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
            );
        return http.build();
    }
}
