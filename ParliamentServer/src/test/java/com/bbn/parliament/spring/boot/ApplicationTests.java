package com.bbn.parliament.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.InputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.spring.boot.controller.QueryController;
import com.bbn.parliament.test_util.MatchAny;
import com.bbn.parliament.test_util.RdfResourceLoader;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
class ApplicationTests {
	private static final String QUERY_ENDPOINT = "/parliament/sparql";
	private static final String UPDATE_ENDPOINT = "/parliament/update";
	private static final String GRAPHSTORE_ENDPOINT = "/parliament/graphstore";
	private static final String[] RSRCS_TO_LOAD = { "univ-bench.owl", "University15_20.owl.zip" };
	private static final MediaType SPARQL_RESULTS_JSON = new MediaType(
		"application", "sparql-results+json");
	private static final MediaType SPARQL_QUERY = new MediaType(
		"application", "sparql-query");
	private static final MediaType SPARQL_UPDATE = new MediaType(
		"application", "sparql-update");
	private static final Logger LOG = LoggerFactory.getLogger(ApplicationTests.class);

	private static final String EVERYTHING_QUERY = """
		select distinct ?s ?o ?p ?g where {
			{ ?s ?p ?o }
			union
			{ graph ?g { ?s ?p ?o } }
		}
		""";
	private static final String CLASS_QUERY = """
		prefix owl: <http://www.w3.org/2002/07/owl#>
		select distinct ?class where {
			?class a owl:Class .
			filter (!isblank(?class))
		}
		""";
	private static final String THING_QUERY = """
		prefix owl: <http://www.w3.org/2002/07/owl#>
		prefix ex:  <http://www.example.org/>
		select distinct ?a where {
			bind ( ex:TestThing as ?a )
			?a a owl:Thing .
		}
		""";
	private static final String THING_INSERT = """
		prefix owl: <http://www.w3.org/2002/07/owl#>
		prefix ex:  <http://www.example.org/>
		insert data {
			ex:TestThing a owl:Thing .
		}
		""";
	private static final String THING_DELETE = """
		prefix owl: <http://www.w3.org/2002/07/owl#>
		prefix ex:  <http://www.example.org/>
		delete data {
			ex:TestThing a owl:Thing .
		}
		""";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private QueryController controller;

	@BeforeAll
	public static void setup() {
		//Load Data
	}

	@AfterAll
	public static void tearDown() {
		//Remove Data
	}

	@Test
	public void testApplicationContextAndLogging() {
		LOG.info("Executing test method testApplicationContextAndLogging()");

		assertThat(controller).isNotNull();

		LOG.error("Test message at the error level");
		LOG.warn("Test message at the warn level");
		LOG.info("Test message at the info level");
		LOG.debug("Test message at the debug level");
		LOG.trace("Test message at the trace level");
	}

	@Test
	public void testEmptySelectQuery() throws Exception {
		LOG.info("Executing test method testEmptySelectQuery()");

		RequestBuilder requestBuilder = MockMvcRequestBuilders.get(QUERY_ENDPOINT);
		mvc.perform(requestBuilder)
			.andDo(print())
			.andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
	}

	@Test
	public void testValidSelectQuery() throws Exception {
		LOG.info("Executing test method testValidSelectQuery()");

		String query = "select * where { ?x ?y ?z }";
		RequestBuilder requestBuilder = MockMvcRequestBuilders.get(QUERY_ENDPOINT)
			.queryParam("query", query);
		String respBody = mvc.perform(requestBuilder)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(SPARQL_RESULTS_JSON))
			.andExpect(new MatchAny(
				content().string(blankString()),
				jsonPath("$.results.bindings", hasSize(0))))
			.andReturn()
			.getResponse()
			.getContentAsString();
		LOG.info("Query response content: '{}'", respBody);
	}

	@Test
	public void generalKBFunctionalityTest() throws Exception {
		LOG.info("Executing test method generalKBFunctionalityTest()");

		deleteDefaultGraph();
		verifySelectQueryResultCount(EVERYTHING_QUERY, 0);
		for (String rsrcName : RSRCS_TO_LOAD) {
			RdfResourceLoader.load(rsrcName, this::insertRsrc);
		}
		verifySelectQueryResultCount(CLASS_QUERY, 43);
		verifySelectQueryResultCount(THING_QUERY, 0);
		doUpdate(THING_INSERT);
		verifySelectQueryResultCount(THING_QUERY, 1);
		doUpdate(THING_DELETE);
		verifySelectQueryResultCount(THING_QUERY, 0);
		deleteDefaultGraph();
		verifySelectQueryResultCount(EVERYTHING_QUERY, 0);
	}

	private void deleteDefaultGraph() throws Exception {
		RequestBuilder requestBuilder = MockMvcRequestBuilders.delete(GRAPHSTORE_ENDPOINT)
			.queryParam("default", "");
		mvc.perform(requestBuilder)
			.andDo(print())
			.andExpect(status().isOk());
	}

	private void verifySelectQueryResultCount(String query, int expectedCount) throws Exception {
		RequestBuilder requestBuilder = MockMvcRequestBuilders.post(QUERY_ENDPOINT)
			.contentType(SPARQL_QUERY)
			.content(query);
		ResultMatcher jsonCountMatcher = jsonPath("$.results.bindings", hasSize(expectedCount));
		ResultMatcher jsonOrBlankCountMatcher = (expectedCount > 0)
			? jsonCountMatcher
			: new MatchAny(
				content().string(blankString()),
				jsonCountMatcher);
		mvc.perform(requestBuilder)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(SPARQL_RESULTS_JSON))
			.andExpect(jsonOrBlankCountMatcher);
	}

	private void insertRsrc(String rsrcName, RDFFormat rdfFormat, InputStream input) throws Exception {
		LOG.debug("Inserting resource '{}' as {} ...", rsrcName, rdfFormat);
		String fileName = new File(rsrcName).getName();
		RequestBuilder requestBuilder = MockMvcRequestBuilders.multipart(GRAPHSTORE_ENDPOINT)
			.file(new MockMultipartFile("file", fileName, rdfFormat.getMediaType(), input))
			.queryParam("default", "");
		mvc.perform(requestBuilder)
			.andDo(print())
			.andExpect(status().isOk());
		LOG.debug("Inserted resource '{}'", rsrcName);
	}

	private void doUpdate(String update) throws Exception {
		RequestBuilder requestBuilder = MockMvcRequestBuilders.post(UPDATE_ENDPOINT)
			.contentType(SPARQL_UPDATE)
			.content(update);
		mvc.perform(requestBuilder)
			.andDo(print())
			.andExpect(status().isOk());
	}
}
