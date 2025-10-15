package com.etalente.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EtalenteBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(EtalenteBackendApplication.class, args);
	}

}
