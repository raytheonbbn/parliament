package com.bbn.parliament.jena.query;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.Set;

import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.ResultSetStream;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import com.hp.hpl.jena.sparql.resultset.ResultSetCompare;
import com.hp.hpl.jena.sparql.resultset.ResultSetRewindable;

public class QueryTestUtil {
	private QueryTestUtil() {}	// prevents instantiation

	private static ResultSetRewindable makeUnique(ResultSet results) {
		// VERY crude.  Utilizes the fact that bindings have value equality.
		Set<Binding> seen = new LinkedHashSet<>();
		while (results.hasNext()) {
			seen.add(results.nextBinding());
		}
		QueryIterator qIter = new QueryIterPlainWrapper(seen.iterator());
		Model m = ModelFactory.createDefaultModel();
		ResultSet rs = new ResultSetStream(results.getResultVars(), m, qIter);
		return ResultSetFactory.makeRewindable(rs);
	}

	public static boolean equals(ResultSetRewindable expected, ResultSetRewindable actual,
			Query query, StringBuilder message) {
		if (query.isReduced()) {
			expected = makeUnique(expected);
			actual = makeUnique(actual);
		}
		boolean matches = query.isOrdered()
			? ResultSetCompare.equalsByValueAndOrder(expected, actual)
			: ResultSetCompare.equalsByValue(expected, actual);
		if (!matches) {
			expected.reset();
			actual.reset();
			message.append("Expected:%n%1$s%n".formatted(ResultSetFormatter.asText(expected)));
			message.append("Actual:%n%1$s".formatted(ResultSetFormatter.asText(actual)));
		}
		return matches;
	}

	public static ResultSetRewindable loadResultSet(String resultSet) {
		if (resultSet.toLowerCase().endsWith("srx")) {
			try (InputStream in = getResource(resultSet)) {
				return ResultSetFactory.makeRewindable(ResultSetFactory.fromXML(in));
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		} else {
			return ResultSetFactory.makeRewindable(ResultSetFactory.fromRDF(loadModel(resultSet, null)));
		}
	}

	public static Model loadModel(String resource, String base) {
		Model model = ModelFactory.createDefaultModel();
		RDFFormat dataFormat = RDFFormat.parseFilename(resource);
		if (RDFFormat.UNKNOWN.equals(dataFormat) && resource.toLowerCase().endsWith("rq")) {
			dataFormat = RDFFormat.TURTLE;
		}
		try (InputStream in = getResource(resource)) {
			model.read(in, base, dataFormat.toString());
		} catch (IOException ex) {
			fail("Could not load resource '%1$s': '%2$s'".formatted(resource, ex.getMessage()));
		}
		return model;
	}

	public static void loadResource(String resource, Graph graph) throws IOException {
		RDFFormat fmt = RDFFormat.parseFilename(resource);
		Model model = ModelFactory.createModelForGraph(graph);
		try (InputStream in = getResource(resource)) {
			model.read(in, null, fmt.toString());
		}
	}

	public static InputStream getResource(String resource) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream strm = cl.getResourceAsStream(resource);
		if (strm == null) {
			fail("Resource not found: '%1$s'".formatted(resource));
		}
		return strm;
	}
}
