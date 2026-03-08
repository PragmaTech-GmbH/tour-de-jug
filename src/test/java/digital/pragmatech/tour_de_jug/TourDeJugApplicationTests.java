package digital.pragmatech.tour_de_jug;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TourDeJugApplicationTests {

	@Test
	void contextLoads() {
	}

}
