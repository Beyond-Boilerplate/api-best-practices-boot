package com.github.sardul3.io.api_best_practices_boot;

import io.temporal.worker.WorkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
public class ApiBestPracticesBootApplication  implements CommandLineRunner {

	private final WorkerFactory workerFactory;

    public ApiBestPracticesBootApplication(WorkerFactory workerFactory) {
        this.workerFactory = workerFactory;
    }

    public static void main(String[] args) {
		SpringApplication.run(ApiBestPracticesBootApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		workerFactory.start();
	}
}
