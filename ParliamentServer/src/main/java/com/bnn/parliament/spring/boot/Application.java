package com.bnn.parliament.spring.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;

import com.bbn.parliament.jena.bridge.ParliamentBridge;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Boot Server for Parliament
 *
 * @author pwilliams
 */
@SpringBootApplication
@ComponentScan("com.bbn.parliament.spring.boot.controller")

public class Application {
	
	private static final Logger LOG = LoggerFactory.getLogger(Application.class);
	protected ParliamentBridge _server;
	
	public static void main(String[] args) {
		/*
		try (ConfigurableApplicationContext context = SpringApplication.run(Application.class, args)) {
			// Do nothing
			LOG.info("I am now shutting down!");
		}
		*/
		
		
		SpringApplication.run(Application.class, args);
	}
}
