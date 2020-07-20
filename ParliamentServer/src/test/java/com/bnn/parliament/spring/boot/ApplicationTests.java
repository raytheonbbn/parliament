
package com.bnn.parliament.spring.boot;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.springframework.boot.web.server.LocalServerPort;

import io.restassured.RestAssured;

import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
class ApplicationTests {
	
	private static final String ENDPOINT = "/parliament/sparql";
	private static final Logger LOG = LoggerFactory.getLogger(ApplicationTests.class);
	
	
	@LocalServerPort
	private int port;
	
	@BeforeAll
	public void setup() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;

		//Load Data
	}
			
	@AfterAll
	public void tearDown() {
		//Remove Data
		
		
	}

	@Test
	public void contextLoads() {
	}

	@Test
	public void testEmptySelectQuery() {
		when()
		.get(ENDPOINT)
		.then()
		.assertThat()
		.statusCode(400);
	}
	
	@Test
	public void testValidSelectQuery() {
		
		String query = "?query=select ?x ?y ?z where { ?x ?y ?z }";
		
		given()
		.queryParam("query", query)
		.when()
		.get(ENDPOINT)
		.then()
		.assertThat()
		.statusCode(200);
	}
}
