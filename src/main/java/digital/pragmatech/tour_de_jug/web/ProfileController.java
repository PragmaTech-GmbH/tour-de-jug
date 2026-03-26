package digital.pragmatech.tour_de_jug.web;

import digital.pragmatech.tour_de_jug.domain.AppUser;
import digital.pragmatech.tour_de_jug.domain.JavaUserGroup;
import digital.pragmatech.tour_de_jug.domain.TalkEvent;
import digital.pragmatech.tour_de_jug.repository.AppUserRepository;
import digital.pragmatech.tour_de_jug.service.ProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/profiles")
public class ProfileController {

    private final AppUserRepository appUserRepository;
    private final ProfileService profileService;
    private final JsonMapper jsonMapper;

    public ProfileController(AppUserRepository appUserRepository,
                             ProfileService profileService,
                             JsonMapper jsonMapper) {
        this.appUserRepository = appUserRepository;
        this.profileService = profileService;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping("/{username}")
    public String publicProfile(@PathVariable String username,
                                @AuthenticationPrincipal OAuth2User currentUser,
                                Model model) {
        AppUser profileUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        boolean isOwnProfile = currentUser != null &&
                profileUser.getUsername().equals(currentUser.getAttribute("login"));

        List<TalkEvent> talks = isOwnProfile
                ? profileService.getAllTalks(profileUser)
                : profileService.getApprovedTalks(profileUser);

        // Build unique JUG locations for the map (only approved talks with coordinates)
        Map<String, Map<String, Object>> jugLocations = new LinkedHashMap<>();
        for (TalkEvent talk : profileService.getApprovedTalks(profileUser)) {
            JavaUserGroup jug = talk.getJug();
            if (jug.getLatitude() != null && jug.getLongitude() != null) {
                jugLocations.computeIfAbsent(jug.getSlug(), slug -> {
                    Map<String, Object> loc = new LinkedHashMap<>();
                    loc.put("slug", jug.getSlug());
                    loc.put("name", jug.getName());
                    loc.put("city", jug.getCity() != null ? jug.getCity() : "");
                    loc.put("country", jug.getCountry() != null ? jug.getCountry() : "");
                    loc.put("lat", jug.getLatitude());
                    loc.put("lng", jug.getLongitude());
                    loc.put("talkCount", 0L);
                    return loc;
                });
                Map<String, Object> loc = jugLocations.get(jug.getSlug());
                loc.put("talkCount", (Long) loc.get("talkCount") + 1);
            }
        }

        model.addAttribute("profileUser", profileUser);
        model.addAttribute("talks", talks);
        model.addAttribute("totalTalks", profileService.countApprovedTalks(profileUser));
        model.addAttribute("talksThisYear", profileService.countApprovedTalksThisYear(profileUser));
        model.addAttribute("speakingSince", profileService.getSpeakingSinceYear(profileUser));
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("speakerJugsJson", jsonMapper.writeValueAsString(jugLocations.values()));
        return "profile";
    }

    @GetMapping("/{username}/edit")
    public String editProfile(@PathVariable String username,
                              @AuthenticationPrincipal OAuth2User currentUser,
                              Model model) {
        AppUser profileUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (currentUser == null || !profileUser.getUsername().equals(currentUser.getAttribute("login"))) {
            return "redirect:/profiles/" + username;
        }

        model.addAttribute("profileUser", profileUser);
        return "profile-edit";
    }

    @PostMapping("/{username}/edit")
    public String saveProfile(@PathVariable String username,
                              @RequestParam String displayName,
                              @AuthenticationPrincipal OAuth2User currentUser) {
        AppUser profileUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (currentUser == null || !profileUser.getUsername().equals(currentUser.getAttribute("login"))) {
            return "redirect:/profiles/" + username;
        }

        profileUser.setDisplayName(displayName);
        appUserRepository.save(profileUser);
        return "redirect:/profiles/" + username;
    }
}
