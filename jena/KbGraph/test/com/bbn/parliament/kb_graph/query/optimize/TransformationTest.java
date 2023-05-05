package com.bbn.parliament.kb_graph.query.optimize;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderTransformation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.bbn.parliament.kb_graph.KbGraph;
import com.bbn.parliament.kb_graph.TestingDataset;
import com.bbn.parliament.kb_graph.query.QueryTestUtil;

public class TransformationTest {
	private static TestingDataset dataset;
	private static List<ReorderTransformation> transformations;

	@BeforeAll
	public static void beforeAll() {
		dataset = new TestingDataset();
		@SuppressWarnings("resource")
		KbGraph defaultGraph = dataset.getDefaultGraph();
		transformations = List.of(
			new DefaultCountTransformation(defaultGraph),
			new UpdatedStaticCountTransformation(defaultGraph)
		);
	}

	@AfterAll
	public static void afterAll() {
		dataset.clear();
	}

	@SuppressWarnings("static-method")
	@AfterEach
	public void afterEach() {
		dataset.reset();
	}

	public static Stream<ReorderTransformation> generateReorderTransformations() {
		return transformations.stream();
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@MethodSource("generateReorderTransformations")
	public void testNoTransformation(ReorderTransformation transformation) throws IOException {
		@SuppressWarnings("resource")
		KbGraph defaultGraph = dataset.getDefaultGraph();
		QueryTestUtil.loadResource("data/data-r2/triple-match/data-02.ttl", defaultGraph);
		BasicPattern pattern = new BasicPattern();
		pattern.add(Triple.create(ResourceFactory
			.createResource("http://example.org/data/x")
			.asNode(), Var.alloc("p"), Var.alloc("o")));
		pattern.add(Triple.create(Var.alloc("s"), Var.alloc("p"),
			Var.alloc("o")));
		BasicPattern reordered = transformation.reorder(pattern);

		assertTrue(checkIndexes(new int[] { 0, 1 }, pattern, reordered));
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@MethodSource("generateReorderTransformations")
	public void testSimpleTransformation(ReorderTransformation transformation) throws IOException {
		@SuppressWarnings("resource")
		KbGraph defaultGraph = dataset.getDefaultGraph();
		QueryTestUtil.loadResource("data/data-r2/triple-match/data-02.ttl", defaultGraph);
		BasicPattern pattern = new BasicPattern();
		pattern.add(Triple.create(Var.alloc("s"), Var.alloc("p"),
			Var.alloc("o")));
		pattern.add(Triple.create(ResourceFactory
			.createResource("http://example.org/data/x")
			.asNode(), Var.alloc("p"), Var.alloc("o")));

		BasicPattern reordered = transformation.reorder(pattern);

		assertTrue(checkIndexes(new int[] { 1, 0 }, pattern, reordered));
	}

	/**
	 * Check the indexes of the new pattern
	 * @param indexes the indexes of the triples in the original pattern.
	 * @param pattern the original pattern.
	 * @param reordered the reordered pattern.
	 * @return <code>true</code> if the indexes are correct, otherwise <code>false</code>.
	 */
	private static boolean checkIndexes(int[] indexes, BasicPattern pattern,
		BasicPattern reordered) {
		boolean valid = true;

		for (int i = 0; i < indexes.length; i++) {
			int index = indexes[i];
			valid = valid && pattern.get(index).equals(reordered.get(i));
			if (!valid) {
				System.err.format("Invalid index: %d.  Expected %s but saw %s%n",
					i, pattern.get(i), reordered.get(index));
				return valid;
			}
		}
		return valid;
	}
}
