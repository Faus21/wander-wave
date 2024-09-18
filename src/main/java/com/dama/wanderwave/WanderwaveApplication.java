package com.dama.wanderwave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class WanderwaveApplication {

	public static void main( String[] args ) {
		SpringApplication.run(WanderwaveApplication.class, args);
	}

}
