package digital.pragmatech.tour_de_jug.repository;

import digital.pragmatech.tour_de_jug.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByGithubId(Long githubId);
    Optional<AppUser> findByUsername(String username);
}
