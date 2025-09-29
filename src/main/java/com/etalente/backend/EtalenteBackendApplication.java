package com.etalente.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EtalenteBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(EtalenteBackendApplication.class, args);
	}

}
