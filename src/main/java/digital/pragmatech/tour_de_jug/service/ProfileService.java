package digital.pragmatech.tour_de_jug.service;

import digital.pragmatech.tour_de_jug.domain.AppUser;
import digital.pragmatech.tour_de_jug.domain.TalkEvent;
import digital.pragmatech.tour_de_jug.domain.TalkStatus;
import digital.pragmatech.tour_de_jug.repository.TalkEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Service
public class ProfileService {

    private final TalkEventRepository talkEventRepository;

    public ProfileService(TalkEventRepository talkEventRepository) {
        this.talkEventRepository = talkEventRepository;
    }

    public List<TalkEvent> getApprovedTalks(AppUser speaker) {
        return talkEventRepository.findBySpeakerAndStatusOrderByEventTimeDesc(speaker, TalkStatus.APPROVED);
    }

    public List<TalkEvent> getAllTalks(AppUser speaker) {
        return talkEventRepository.findBySpeakerOrderByEventTimeDesc(speaker);
    }

    public long countApprovedTalks(AppUser speaker) {
        return getApprovedTalks(speaker).size();
    }

    public long countApprovedTalksThisYear(AppUser speaker) {
        int currentYear = Instant.now().atZone(ZoneId.systemDefault()).getYear();
        return getApprovedTalks(speaker).stream()
                .filter(t -> t.getEventTime().atZone(ZoneId.systemDefault()).getYear() == currentYear)
                .count();
    }

    public Integer getSpeakingSinceYear(AppUser speaker) {
        return getApprovedTalks(speaker).stream()
                .map(t -> t.getEventTime().atZone(ZoneId.systemDefault()).getYear())
                .min(Integer::compareTo)
                .orElse(null);
    }
}
