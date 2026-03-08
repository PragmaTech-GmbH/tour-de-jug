package digital.pragmatech.tour_de_jug.web;

import digital.pragmatech.tour_de_jug.domain.JavaUserGroup;
import digital.pragmatech.tour_de_jug.repository.JavaUserGroupRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class JugController {

    private final JavaUserGroupRepository jugRepository;

    public JugController(JavaUserGroupRepository jugRepository) {
        this.jugRepository = jugRepository;
    }

    @GetMapping("/jugs/{slug}")
    public String jugDetail(@PathVariable String slug, Model model) {
        JavaUserGroup jug = jugRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("JUG not found: " + slug));
        model.addAttribute("jug", jug);
        return "jug-detail";
    }

    @GetMapping("/api/jugs/search")
    @ResponseBody
    public List<Map<String, Object>> searchJugs(@RequestParam String q) {
        return jugRepository.searchActive(q).stream()
                .map(jug -> Map.<String, Object>of(
                        "id", jug.getId().toString(),
                        "name", jug.getName(),
                        "city", jug.getCity() != null ? jug.getCity() : "",
                        "country", jug.getCountry() != null ? jug.getCountry() : ""
                ))
                .collect(Collectors.toList());
    }
}
