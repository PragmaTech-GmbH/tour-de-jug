package digital.pragmatech.tour_de_jug.repository;

import digital.pragmatech.tour_de_jug.domain.JavaUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JavaUserGroupRepository extends JpaRepository<JavaUserGroup, UUID> {
    Optional<JavaUserGroup> findBySlug(String slug);

    @Query("SELECT j FROM JavaUserGroup j WHERE j.deletedAt IS NULL")
    List<JavaUserGroup> findAllActive();

    @Query("SELECT j FROM JavaUserGroup j WHERE j.deletedAt IS NULL AND (LOWER(j.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(j.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(j.country) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<JavaUserGroup> searchActive(String query);

    List<JavaUserGroup> findTop3ByDeletedAtIsNullOrderByCreatedAtDesc();
}
