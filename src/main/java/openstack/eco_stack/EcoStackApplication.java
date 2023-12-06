package openstack.eco_stack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EcoStackApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcoStackApplication.class, args);
	}

}
