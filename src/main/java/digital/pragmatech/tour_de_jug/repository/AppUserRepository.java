package digital.pragmatech.tour_de_jug.repository;

import digital.pragmatech.tour_de_jug.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByGithubId(Long githubId);
    Optional<AppUser> findByUsername(String username);

    @Query("SELECT u FROM AppUser u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<AppUser> search(String query);
}
