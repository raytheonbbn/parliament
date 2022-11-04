package com.bbn.parliament.jena.query;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.jena.graph.Graph;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.apache.jena.sparql.resultset.RDFInput;
import org.apache.jena.sparql.resultset.ResultSetCompare;

import com.bbn.parliament.jena.joseki.client.RDFFormat;

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
			return ResultSetFactory.makeRewindable(RDFInput.fromRDF(loadModel(resultSet, null)));
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
