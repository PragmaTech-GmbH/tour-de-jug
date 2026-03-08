package digital.pragmatech.tour_de_jug.web;

import digital.pragmatech.tour_de_jug.domain.AppUser;
import digital.pragmatech.tour_de_jug.domain.JavaUserGroup;
import digital.pragmatech.tour_de_jug.domain.TalkEvent;
import digital.pragmatech.tour_de_jug.domain.TalkStatus;
import digital.pragmatech.tour_de_jug.repository.AppUserRepository;
import digital.pragmatech.tour_de_jug.repository.JavaUserGroupRepository;
import digital.pragmatech.tour_de_jug.repository.TalkEventRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Controller
@RequestMapping("/speaking-events")
public class SpeakingEventController {

    private final TalkEventRepository talkEventRepository;
    private final AppUserRepository appUserRepository;
    private final JavaUserGroupRepository jugRepository;

    public SpeakingEventController(TalkEventRepository talkEventRepository,
                                   AppUserRepository appUserRepository,
                                   JavaUserGroupRepository jugRepository) {
        this.talkEventRepository = talkEventRepository;
        this.appUserRepository = appUserRepository;
        this.jugRepository = jugRepository;
    }

    @GetMapping("/new")
    public String newEventForm(Model model) {
        model.addAttribute("jugs", jugRepository.findAllActive());
        return "speaking-event-form";
    }

    @PostMapping
    public String createEvent(@RequestParam String talkTitle,
                              @RequestParam UUID jugId,
                              @RequestParam String eventTime,
                              @RequestParam(required = false) String slidesUrl,
                              @RequestParam(required = false) String recordingUrl,
                              @AuthenticationPrincipal OAuth2User currentUser) {
        String username = currentUser.getAttribute("login");
        AppUser speaker = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        JavaUserGroup jug = jugRepository.findById(jugId)
                .orElseThrow(() -> new RuntimeException("JUG not found"));

        TalkEvent event = new TalkEvent();
        event.setSpeaker(speaker);
        event.setJug(jug);
        event.setTalkTitle(talkTitle);
        event.setEventTime(Instant.parse(eventTime + ":00Z"));
        if (slidesUrl != null && !slidesUrl.isBlank()) event.setSlidesUrl(slidesUrl);
        if (recordingUrl != null && !recordingUrl.isBlank()) event.setRecordingUrl(recordingUrl);
        event.setStatus(TalkStatus.PENDING);

        talkEventRepository.save(event);
        return "redirect:/profiles/" + username;
    }

    @GetMapping("/{id}/edit")
    public String editEventForm(@PathVariable UUID id,
                                @AuthenticationPrincipal OAuth2User currentUser,
                                Model model) {
        TalkEvent event = talkEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        String username = currentUser.getAttribute("login");
        if (!event.getSpeaker().getUsername().equals(username)) {
            return "redirect:/";
        }
        model.addAttribute("event", event);
        model.addAttribute("jugs", jugRepository.findAllActive());
        return "speaking-event-form";
    }

    @PostMapping("/{id}")
    public String updateEvent(@PathVariable UUID id,
                              @RequestParam String talkTitle,
                              @RequestParam UUID jugId,
                              @RequestParam String eventTime,
                              @RequestParam(required = false) String slidesUrl,
                              @RequestParam(required = false) String recordingUrl,
                              @RequestParam(required = false) String _method,
                              @AuthenticationPrincipal OAuth2User currentUser) {
        TalkEvent event = talkEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        String username = currentUser.getAttribute("login");
        if (!event.getSpeaker().getUsername().equals(username)) {
            return "redirect:/";
        }

        if ("DELETE".equalsIgnoreCase(_method)) {
            talkEventRepository.delete(event);
            return "redirect:/profiles/" + username;
        }

        JavaUserGroup jug = jugRepository.findById(jugId)
                .orElseThrow(() -> new RuntimeException("JUG not found"));
        event.setTalkTitle(talkTitle);
        event.setJug(jug);
        event.setEventTime(Instant.parse(eventTime + ":00Z"));
        event.setSlidesUrl(slidesUrl != null && !slidesUrl.isBlank() ? slidesUrl : null);
        event.setRecordingUrl(recordingUrl != null && !recordingUrl.isBlank() ? recordingUrl : null);
        talkEventRepository.save(event);
        return "redirect:/profiles/" + username;
    }
}
