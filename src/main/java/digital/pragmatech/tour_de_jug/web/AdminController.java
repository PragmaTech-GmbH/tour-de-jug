package digital.pragmatech.tour_de_jug.web;

import digital.pragmatech.tour_de_jug.domain.TalkEvent;
import digital.pragmatech.tour_de_jug.domain.TalkStatus;
import digital.pragmatech.tour_de_jug.repository.JavaUserGroupRepository;
import digital.pragmatech.tour_de_jug.repository.TalkEventRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final JavaUserGroupRepository jugRepository;
    private final TalkEventRepository talkEventRepository;

    public AdminController(JavaUserGroupRepository jugRepository, TalkEventRepository talkEventRepository) {
        this.jugRepository = jugRepository;
        this.talkEventRepository = talkEventRepository;
    }

    @GetMapping("/jugs/{jugSlug}/events")
    public String listPendingEvents(@PathVariable String jugSlug, Model model) {
        var jug = jugRepository.findBySlug(jugSlug)
                .orElseThrow(() -> new RuntimeException("JUG not found"));
        var pendingEvents = talkEventRepository.findByJugAndStatus(jug, TalkStatus.PENDING);
        model.addAttribute("jug", jug);
        model.addAttribute("events", pendingEvents);
        return "admin-jug-events";
    }

    @PostMapping("/jugs/{jugSlug}/events/{eventId}/approve")
    public String approveEvent(@PathVariable String jugSlug, @PathVariable UUID eventId) {
        TalkEvent event = talkEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.setStatus(TalkStatus.APPROVED);
        talkEventRepository.save(event);
        return "redirect:/admin/jugs/" + jugSlug + "/events";
    }

    @PostMapping("/jugs/{jugSlug}/events/{eventId}/reject")
    public String rejectEvent(@PathVariable String jugSlug, @PathVariable UUID eventId) {
        TalkEvent event = talkEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.setStatus(TalkStatus.REJECTED);
        talkEventRepository.save(event);
        return "redirect:/admin/jugs/" + jugSlug + "/events";
    }
}
