package com.bnn.parliament.spring.boot;

import javax.servlet.ServletContextListener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot Server for Parliament
 *
 * @author pwilliams
 */
@SpringBootApplication
@ComponentScan({
	"com.bbn.parliament.spring.boot.controller",
	"com.bbn.parliament.spring.boot.service",
	"com.bbn.parliament.jena.bridge"})
public class Application {
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@SuppressWarnings("static-method")
	@Bean
	ServletListenerRegistrationBean<ServletContextListener> servletListener() {
		ServletListenerRegistrationBean<ServletContextListener> srb =
			new ServletListenerRegistrationBean<>();
		srb.setListener(new ParliamentServletContextListener());
		return srb;
	}
}
