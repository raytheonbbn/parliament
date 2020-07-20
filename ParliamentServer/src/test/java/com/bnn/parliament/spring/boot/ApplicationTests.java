
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
	
	private static final String EVERYTHING_QUERY = ""
			+ "select ?s ?o ?p ?g where {%n"
			+ "	{ ?s ?p ?o }%n"
			+ "	union%n"
			+ "	{ graph ?g { ?s ?p ?o } }%n"
			+ "}";
		private static final String CLASS_QUERY = ""
			+ "prefix owl: <http://www.w3.org/2002/07/owl#>%n"
			+ "%n"
			+ "select distinct ?class where {%n"
			+ "	?class a owl:Class .%n"
			+ "	filter (!isblank(?class))%n"
			+ "}";
		private static final String THING_QUERY = ""
			+ "prefix owl:  <http://www.w3.org/2002/07/owl#>%n"
			+ "prefix ex:   <http://www.example.org/>%n"
			+ "%n"
			+ "select ?a where {%n"
			+ "	bind ( ex:Test as ?a )%n"
			+ "	?a a owl:Thing .%n"
			+ "}";
		private static final String THING_INSERT = ""
			+ "prefix owl:  <http://www.w3.org/2002/07/owl#>%n"
			+ "prefix ex:   <http://www.example.org/>%n"
			+ "%n"
			+ "insert data {%n"
			+ "	ex:Test a owl:Thing .%n"
			+ "}";
		private static final String THING_DELETE = ""
			+ "prefix owl:  <http://www.w3.org/2002/07/owl#>%n"
			+ "prefix ex:   <http://www.example.org/>%n"
			+ "%n"
			+ "delete data {%n"
			+ "	ex:Test a owl:Thing .%n"
			+ "}";
		private static final String CSV_QUOTING_TEST_QUERY = ""
			+ "prefix ex: <http://example.org/#>%n"
			+ "select ?s ?p ?o where {%n"
			+ "	bind( ex:comment as ?p )%n"
			+ "	?s ?p ?o .%n"
			+ "} order by ?o";
	
	@LocalServerPort
	private int port;
	
	@BeforeAll
	public void setup() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;

		//Load Data
		post()
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
