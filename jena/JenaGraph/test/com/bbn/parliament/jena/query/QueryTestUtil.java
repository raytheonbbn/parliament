package com.bbn.parliament.jena.query;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

	private static ResultSetRewindable makeUnique(ResultSetRewindable results) {
		// VERY crude.  Utilizes the fact that bindings have value equality.
		Set<Binding> seen = new HashSet<>();
		List<Binding> seenInOriginalOrder = new ArrayList<>();

		while (results.hasNext()) {
			Binding b = results.nextBinding();
			if (!seen.contains(b)) {
				seen.add(b);
				seenInOriginalOrder.add(b);
			}
		}
		QueryIterator qIter = new QueryIterPlainWrapper(seenInOriginalOrder.iterator());
		ResultSet rs = new ResultSetStream(results.getResultVars(), ModelFactory.createDefaultModel(), qIter);
		return ResultSetFactory.makeRewindable(rs);
	}

	public static boolean equals(ResultSet expected, ResultSet actual, Query query) {
		ResultSetRewindable exp = ResultSetFactory.makeRewindable(expected);
		ResultSetRewindable act = ResultSetFactory.makeRewindable(actual);
		if (query.isReduced()) {
			exp = makeUnique(exp);
			act = makeUnique(act);
		}
		return query.isOrdered()
			? ResultSetCompare.equalsByValueAndOrder(exp, act)
			: ResultSetCompare.equalsByValue(exp, act);
	}

	public static boolean equals(ResultSet expected, ResultSet actual, Query query, StringBuilder message) {
		ResultSetRewindable e = ResultSetFactory.makeRewindable(expected);
		ResultSetRewindable a = ResultSetFactory.makeRewindable(actual);
		boolean matches = QueryTestUtil.equals(e, a, query);
		if (!matches) {
			e.reset();
			a.reset();
			message.append(String.format("Expected:%n%1$s%n", ResultSetFormatter.asText(e)));
			message.append(String.format("Actual:%n%1$s", ResultSetFormatter.asText(a)));
		}
		return matches;
	}

	public static ResultSet loadResultSet(String resultSet) {
		return resultSet.toLowerCase().endsWith("srx")
			? ResultSetFactory.fromXML(getResource(resultSet))
			: ResultSetFactory.fromRDF(loadModel(resultSet));
	}

	public static Model loadModel(String model) {
		return loadModel(model, null);
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
			fail(String.format("Could not load resource '%1$s':  '%2$s'", resource, ex.getMessage()));
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
			fail(String.format("Resource not found: '%1$s'", resource));
		}
		return strm;
	}
}
