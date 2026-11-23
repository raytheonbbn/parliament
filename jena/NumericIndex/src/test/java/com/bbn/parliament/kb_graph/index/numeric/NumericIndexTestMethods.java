package com.bbn.parliament.kb_graph.index.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.XSD;

import com.bbn.parliament.kb_graph.index.IndexException;
import com.bbn.parliament.kb_graph.index.IndexFactory;
import com.bbn.parliament.kb_graph.index.IndexFactory.IndexFactoryHelper;
import com.bbn.parliament.kb_graph.index.Record;
import com.bbn.parliament.kb_graph.index.numeric.Constants.NumberType;
import com.bbn.parliament.kb_graph.query.index.QueryableIndexTestMethods;
import com.bbn.parliament.kb_graph.util.FileUtil;

public class NumericIndexTestMethods extends QueryableIndexTestMethods<NumericIndex<Integer>, Integer> {
	public static final String AGE_URI = "http://example.org#age";

	@Override
	protected IndexFactory<NumericIndex<Integer>, Integer> getIndexFactory() {
		NumericIndexFactory<Integer> f = new NumericIndexFactory.IntegerIndexFactory();
		Properties p = new Properties();
		p.setProperty(Constants.NUMBER_TYPE, NumberType.Integer.toString());
		p.setProperty(Constants.PROPERTY, AGE_URI);
		f.configure(p);
		return f;
	}

	@Override
	protected Record<Integer> createRecord(int seed) {
		Node key = NodeFactory.createURI(AGE_URI + seed);
		Record<Integer> record = Record.create(key, seed);
		return record;
	}

	@Override
	protected void doSetup() {
	}

	@Override
	protected boolean checkDeleted(NumericIndex<Integer> index, Graph graph, Node graphName) {
		String indexDir = IndexFactoryHelper.getIndexDirectory(graph, graphName);
		String predicate = index.getQuerier().getPredicate();
		String dirName = "numeric_%1$s".formatted(FileUtil.encodeStringForFilename(predicate));
		return !new File(indexDir, dirName).exists();
	}

	// =============== Test Methods ===============

	public void testFilter(NumericIndex<Integer> index, Model model) {
		// String example = "http://example.org/";
		Node p = NodeFactory.createURI(AGE_URI);
		Node o20 = ResourceFactory.createTypedLiteral(20).asNode();
		Node o25 = ResourceFactory.createTypedLiteral(25).asNode();
		Node o29 = ResourceFactory.createTypedLiteral(29).asNode();
		Node o32 = ResourceFactory.createTypedLiteral(32).asNode();
		Node o53 = ResourceFactory.createTypedLiteral(53).asNode();
		List<Statement> statements = new ArrayList<>();
		statements.add(model.asStatement(Triple.create(createRecord(1).getKey(),
			p, o20)));
		statements.add(model.asStatement(Triple.create(createRecord(2).getKey(),
			p, o25)));
		statements.add(model.asStatement(Triple.create(createRecord(3).getKey(),
			p, o29)));
		statements.add(model.asStatement(Triple.create(createRecord(4).getKey(),
			p, o32)));
		statements.add(model.asStatement(Triple.create(createRecord(5).getKey(),
			p, o53)));
		model.add(statements);
		// model.add(ResourceFactory.createResource(example + "1"),
		// ResourceFactory.createProperty(AGE_URI),
		// );
		// model.add(ResourceFactory.createResource(example + "2"),
		// ResourceFactory.createProperty(AGE_URI),
		// ResourceFactory.createTypedLiteral(25));
		// model.add(ResourceFactory.createResource(example + "3"),
		// ResourceFactory.createProperty(AGE_URI),
		// ResourceFactory.createTypedLiteral(29));
		// model.add(ResourceFactory.createResource(example + "4"),
		// ResourceFactory.createProperty(AGE_URI),
		// ResourceFactory.createTypedLiteral(32));
		// model.add(ResourceFactory.createResource(example + "5"),
		// ResourceFactory.createProperty(AGE_URI),
		// ResourceFactory.createTypedLiteral(53));
		String query = """
			PREFIX xsd: <%1$s>
			SELECT ?x WHERE {
				?x <%2$s> ?z .
				FILTER (?z < "30"^^xsd:integer && ?z >= "25"^^xsd:integer)
			}
			""".formatted(XSD.getURI(), AGE_URI);

		try {
			assertEquals(5L, index.size());
		} catch (IndexException ex) {
			fail(ex.getMessage());
		}
		assertEquals(2, countQuerySolutions(query, model));
	}

	@SuppressWarnings("static-method")
	public void testComplexQuery(NumericIndex<Integer> index, Model model) {
		Property ageProp = ResourceFactory.createProperty("http://example.org#age");
		Property nameProp = ResourceFactory.createProperty("http://example.org#name");
		Property friendProp = ResourceFactory.createProperty("http://example.org#friend");

		final String example = "http://example.org/";

		Resource one = ResourceFactory.createResource(example + "1");
		Resource two = ResourceFactory.createResource(example + "2");
		Resource three = ResourceFactory.createResource(example + "3");
		Resource four = ResourceFactory.createResource(example + "4");
		Resource five = ResourceFactory.createResource(example + "5");

		model.add(one, ageProp,
			ResourceFactory.createTypedLiteral(20));
		model.add(one, nameProp, ResourceFactory.createTypedLiteral("One"));
		model.add(one, friendProp, two);

		model.add(two, ageProp,
			ResourceFactory.createTypedLiteral(25));
		model.add(two, nameProp, ResourceFactory.createTypedLiteral("Two"));
		model.add(two, friendProp, one);
		model.add(two, friendProp, five);

		model.add(three, ageProp,
			ResourceFactory.createTypedLiteral(29));
		model.add(three, nameProp, ResourceFactory.createTypedLiteral("Three"));
		model.add(three, friendProp, five);

		model.add(four, ageProp,
			ResourceFactory.createTypedLiteral(32));
		model.add(four, nameProp, ResourceFactory.createTypedLiteral("Four"));

		model.add(five, ageProp,
			ResourceFactory.createTypedLiteral(53));
		model.add(five, nameProp, ResourceFactory.createTypedLiteral("Five"));
		model.add(five, friendProp, two);
		model.add(five, friendProp, three);
		String query = """
			SELECT ?x ?name ?y WHERE {
				?x <http://example.org#age> ?age .
				?x <http://example.org#name> ?name .
				OPTIONAL {
					?x <http://example.org#friend> ?y .
				}
				FILTER (!bound(?y))
				FILTER (?age > 25.0)
			}
			""";

		try {
			assertEquals(5L, index.size());
		} catch (IndexException ex) {
			fail(ex.getMessage());
		}
		assertEquals(1, countQuerySolutions(query, model));
	}

	private static int countQuerySolutions(String query, Model model) {
		try (QueryExecution qExec = QueryExecutionFactory.create(query, model)) {
			ResultSet result = qExec.execSelect();
			int counter = 0;
			while (result.hasNext()) {
				result.next();
				++counter;
			}
			return counter;
		}
	}

	public void testRangeIterator(NumericIndex<Integer> index) {
		try {
			index.add(createRecord(1));
			index.add(createRecord(2));
			index.add(createRecord(3));
			index.add(createRecord(4));
		} catch (IndexException ex) {
			fail(ex.getMessage());
		}

		Iterator<Record<Integer>> it;

		it = index.iterator();
		assertTrue(it.hasNext());

		int counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals(4, counter);

		it = index.iterator(2, 3);
		counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals(2, counter);

		it = index.iterator(1, 3);
		counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals(3, counter);

		it = index.iterator(2, 10);
		counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals(3, counter);

		it = index.iterator(5, 10);
		counter = 0;
		assertFalse(it.hasNext());

		it = index.iterator(null, 2);
		counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals(2, counter);

		it = index.iterator(2, null);
		counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals(3, counter);
	}
}
