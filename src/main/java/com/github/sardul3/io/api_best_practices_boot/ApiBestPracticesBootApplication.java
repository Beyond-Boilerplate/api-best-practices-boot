package com.github.sardul3.io.api_best_practices_boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
public class ApiBestPracticesBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiBestPracticesBootApplication.class, args);
	}

}
