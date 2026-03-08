package digital.pragmatech.tour_de_jug;

import digital.pragmatech.tour_de_jug.repository.JavaUserGroupRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JugImportServiceIT {

    @Autowired
    private JavaUserGroupRepository jugRepository;

    @Test
    void importsJugsFromYaml() {
        var jugs = jugRepository.findAllActive();
        assertThat(jugs).isNotEmpty();
        assertThat(jugs).anyMatch(j -> j.getSlug().equals("londonjavacommunity"));
    }
}
