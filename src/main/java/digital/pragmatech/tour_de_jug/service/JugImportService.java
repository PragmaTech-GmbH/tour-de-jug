package digital.pragmatech.tour_de_jug.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import digital.pragmatech.tour_de_jug.domain.JavaUserGroup;
import digital.pragmatech.tour_de_jug.repository.JavaUserGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JugImportService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(JugImportService.class);

    private final JavaUserGroupRepository jugRepository;

    public JugImportService(JavaUserGroupRepository jugRepository) {
        this.jugRepository = jugRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        List<Map<String, Object>> yamlJugs = loadJugsFromYaml();
        Set<String> yamlSlugs = yamlJugs.stream()
                .map(j -> (String) j.get("slug"))
                .collect(Collectors.toSet());

        int added = 0, updated = 0, deactivated = 0;

        for (Map<String, Object> jugData : yamlJugs) {
            String slug = (String) jugData.get("slug");
            var existing = jugRepository.findBySlug(slug);
            if (existing.isPresent()) {
                updateJug(existing.get(), jugData);
                updated++;
            } else {
                jugRepository.save(createJug(jugData));
                added++;
            }
        }

        List<JavaUserGroup> allActive = jugRepository.findAllActive();
        for (JavaUserGroup jug : allActive) {
            if (!yamlSlugs.contains(jug.getSlug())) {
                jug.setInactiveSince(Instant.now());
                jugRepository.save(jug);
                deactivated++;
            }
        }

        logger.info("JUG import complete: {} added, {} updated, {} deactivated", added, updated, deactivated);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadJugsFromYaml() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        ClassPathResource resource = new ClassPathResource("jugs.yaml");
        Map<String, Object> root = mapper.readValue(resource.getInputStream(), Map.class);
        return (List<Map<String, Object>>) root.get("jugs");
    }

    private JavaUserGroup createJug(Map<String, Object> data) {
        JavaUserGroup jug = new JavaUserGroup();
        applyData(jug, data);
        return jug;
    }

    private void updateJug(JavaUserGroup jug, Map<String, Object> data) {
        applyData(jug, data);
        jug.setUpdatedAt(Instant.now());
        jug.setInactiveSince(null);
        jugRepository.save(jug);
    }

    private void applyData(JavaUserGroup jug, Map<String, Object> data) {
        jug.setSlug((String) data.get("slug"));
        jug.setName((String) data.get("name"));
        jug.setCity((String) data.get("city"));
        jug.setCountry((String) data.get("country"));
        jug.setHomepageUrl((String) data.get("homepage_url"));
        if (data.get("social_url") != null) jug.setSocialUrl((String) data.get("social_url"));
        if (data.get("meetup_url") != null) jug.setMeetupUrl((String) data.get("meetup_url"));
        if (data.get("description") != null) jug.setDescription((String) data.get("description"));
        if (data.get("latitude") != null) jug.setLatitude(((Number) data.get("latitude")).doubleValue());
        if (data.get("longitude") != null) jug.setLongitude(((Number) data.get("longitude")).doubleValue());
        if (data.get("established_year") != null) jug.setEstablishedYear((Integer) data.get("established_year"));
    }
}
