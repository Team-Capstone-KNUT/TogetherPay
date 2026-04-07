package com.devcrew.togetherpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing
@SpringBootApplication
public class TogetherpayApplication {

	public static void main(String[] args) {
		SpringApplication.run(TogetherpayApplication.class, args);
	}

}
