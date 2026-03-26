package digital.pragmatech.tour_de_jug.repository;

import digital.pragmatech.tour_de_jug.domain.AppUser;
import digital.pragmatech.tour_de_jug.domain.JavaUserGroup;
import digital.pragmatech.tour_de_jug.domain.TalkEvent;
import digital.pragmatech.tour_de_jug.domain.TalkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface TalkEventRepository extends JpaRepository<TalkEvent, UUID> {
    List<TalkEvent> findBySpeakerOrderByEventTimeDesc(AppUser speaker);
    List<TalkEvent> findByJug(JavaUserGroup jug);
    List<TalkEvent> findBySpeakerAndStatusOrderByEventTimeDesc(AppUser speaker, TalkStatus status);
    List<TalkEvent> findByJugAndStatus(JavaUserGroup jug, TalkStatus status);

    @Query("SELECT t.speaker FROM TalkEvent t WHERE t.status = 'APPROVED' GROUP BY t.speaker ORDER BY COUNT(t) DESC LIMIT 5")
    List<AppUser> findTopSpeakers();
}
