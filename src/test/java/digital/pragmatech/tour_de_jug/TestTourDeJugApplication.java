package digital.pragmatech.tour_de_jug;

import org.springframework.boot.SpringApplication;

public class TestTourDeJugApplication {

	public static void main(String[] args) {
		SpringApplication.from(TourDeJugApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
