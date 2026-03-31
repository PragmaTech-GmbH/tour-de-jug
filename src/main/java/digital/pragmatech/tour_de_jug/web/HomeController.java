package digital.pragmatech.tour_de_jug.web;

import digital.pragmatech.tour_de_jug.domain.AppUser;
import digital.pragmatech.tour_de_jug.domain.JavaUserGroup;
import digital.pragmatech.tour_de_jug.repository.AppUserRepository;
import digital.pragmatech.tour_de_jug.repository.JavaUserGroupRepository;
import digital.pragmatech.tour_de_jug.repository.TalkEventRepository;
import digital.pragmatech.tour_de_jug.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final JavaUserGroupRepository jugRepository;
    private final AppUserRepository appUserRepository;
    private final TalkEventRepository talkEventRepository;
    private final ProfileService profileService;
    private final JsonMapper jsonMapper;

    public HomeController(JavaUserGroupRepository jugRepository,
                          AppUserRepository appUserRepository,
                          TalkEventRepository talkEventRepository,
                          ProfileService profileService,
                          JsonMapper jsonMapper) {
        this.jugRepository = jugRepository;
        this.appUserRepository = appUserRepository;
        this.talkEventRepository = talkEventRepository;
        this.profileService = profileService;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<JavaUserGroup> activeJugs = jugRepository.findAllActive();
        List<Map<String, Object>> jugData = activeJugs.stream()
                .map(jug -> {
                    var map = new java.util.LinkedHashMap<String, Object>();
                    map.put("slug", jug.getSlug());
                    map.put("name", jug.getName());
                    map.put("city", jug.getCity() != null ? jug.getCity() : "");
                    map.put("country", jug.getCountry() != null ? jug.getCountry() : "");
                    map.put("homepageUrl", jug.getHomepageUrl() != null ? jug.getHomepageUrl() : "");
                    if (jug.getLatitude() != null && jug.getLongitude() != null) {
                        map.put("lat", jug.getLatitude());
                        map.put("lng", jug.getLongitude());
                    }
                    map.put("inactive", jug.getInactiveSince() != null);
                    return map;
                })
                .collect(Collectors.toList());

        List<AppUser> topSpeakers = talkEventRepository.findTopSpeakers();
        List<Map<String, Object>> topSpeakerData = topSpeakers.stream()
                .map(speaker -> Map.<String, Object>of(
                        "username", speaker.getUsername(),
                        "displayName", speaker.getDisplayName() != null ? speaker.getDisplayName() : speaker.getUsername(),
                        "avatarUrl", speaker.getAvatarUrl() != null ? speaker.getAvatarUrl() : "",
                        "talkCount", profileService.countApprovedTalks(speaker)
                ))
                .collect(Collectors.toList());

        model.addAttribute("jugs", activeJugs);
        model.addAttribute("jugsJson", jsonMapper.writeValueAsString(jugData));
        model.addAttribute("topSpeakers", topSpeakerData);
        model.addAttribute("jugCount", activeJugs.size());
        model.addAttribute("speakerCount", appUserRepository.count());
        return "index";
    }
}
