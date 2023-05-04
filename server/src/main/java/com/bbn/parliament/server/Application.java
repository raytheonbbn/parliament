package com.bbn.parliament.server;

import javax.servlet.ServletContextListener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot Server for Parliament
 *
 * @author pwilliams
 */
@SpringBootApplication
public class Application {
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@SuppressWarnings("static-method")
	@Bean
	public ServletListenerRegistrationBean<ServletContextListener> servletListener() {
		ServletListenerRegistrationBean<ServletContextListener> srb =
			new ServletListenerRegistrationBean<>();
		srb.setListener(new ParliamentServletContextListener());
		return srb;
	}
}
