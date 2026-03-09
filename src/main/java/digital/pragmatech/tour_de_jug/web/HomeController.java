package digital.pragmatech.tour_de_jug.web;

import digital.pragmatech.tour_de_jug.domain.JavaUserGroup;
import digital.pragmatech.tour_de_jug.repository.JavaUserGroupRepository;
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
    private final JsonMapper jsonMapper;

    public HomeController(JavaUserGroupRepository jugRepository, JsonMapper jsonMapper) {
        this.jugRepository = jugRepository;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<JavaUserGroup> activeJugs = jugRepository.findAllActive();
        List<Map<String, Object>> jugData = activeJugs.stream()
                .filter(jug -> jug.getLatitude() != null && jug.getLongitude() != null)
                .map(jug -> Map.<String, Object>of(
                        "slug", jug.getSlug(),
                        "name", jug.getName(),
                        "city", jug.getCity() != null ? jug.getCity() : "",
                        "country", jug.getCountry() != null ? jug.getCountry() : "",
                        "lat", jug.getLatitude(),
                        "lng", jug.getLongitude(),
                        "homepageUrl", jug.getHomepageUrl() != null ? jug.getHomepageUrl() : ""
                ))
                .collect(Collectors.toList());

        model.addAttribute("jugs", activeJugs);
        model.addAttribute("jugsJson", jsonMapper.writeValueAsString(jugData));
        return "index";
    }
}
