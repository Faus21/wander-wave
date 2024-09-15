package com.dama.wanderwave;

import com.dama.wanderwave.email.EmailServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WanderwaveApplication implements CommandLineRunner {

	@Autowired
	private EmailServiceImpl emailService;

	public static void main( String[] args ) {
		SpringApplication.run(WanderwaveApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		emailService.sendRecoveryEmail("daniil.pavlovskyi@gmail.com");
	}
}
