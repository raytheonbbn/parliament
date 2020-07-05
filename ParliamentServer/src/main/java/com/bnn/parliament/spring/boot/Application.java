package com.bnn.parliament.spring.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Spring Boot Server for Parliament
 *
 * @author pwilliams
 */
@SpringBootApplication
public class Application {
	public static void main(String[] args) {
		try (ConfigurableApplicationContext context = SpringApplication.run(Application.class, args)) {
			// Do nothing
		}
	}
}
