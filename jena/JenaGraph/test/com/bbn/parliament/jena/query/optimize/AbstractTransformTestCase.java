package com.bbn.parliament.jena.query.optimize;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.bbn.parliament.jena.query.AbstractKbTestCase;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.optimizer.reorder.ReorderTransformation;

public abstract class AbstractTransformTestCase extends AbstractKbTestCase {
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
				System.err.println(String.format("Invalid index: %d.  Expected %s but saw %s", i, pattern.get(i), reordered.get(index)));
				return valid;
			}
		}
		return valid;
	}

	/** Create and return the transformation. */
	protected abstract ReorderTransformation setupTransformation();

	protected ReorderTransformation transformation;

	@Override
	@Before
	public void setUp() {
		super.setUp();
		transformation = setupTransformation();
	}

	@Test
	public void testNoTransformation() throws IOException {
		loadResource("data/data-r2/triple-match/data-02.ttl", getGraph());
		BasicPattern pattern = new BasicPattern();
		pattern.add(Triple.create(ResourceFactory
			.createResource("http://example.org/data/x")
			.asNode(), Var.alloc("p"), Var.alloc("o")));
		pattern.add(Triple.create(Var.alloc("s"), Var.alloc("p"),
			Var.alloc("o")));
		BasicPattern reordered = transformation.reorder(pattern);

		assertTrue(checkIndexes(new int[] { 0, 1}, pattern, reordered));
	}

	@Test
	public void testSimpleTransformation() throws IOException {
		loadResource("data/data-r2/triple-match/data-02.ttl", getGraph());
		BasicPattern pattern = new BasicPattern();
		pattern.add(Triple.create(Var.alloc("s"), Var.alloc("p"),
			Var.alloc("o")));
		pattern.add(Triple.create(ResourceFactory
			.createResource("http://example.org/data/x")
			.asNode(), Var.alloc("p"), Var.alloc("o")));

		BasicPattern reordered = transformation.reorder(pattern);

		assertTrue(checkIndexes(new int[] { 1, 0 }, pattern, reordered));
	}

	@Test
	@Ignore
	public void testCompleteTransformation() {
	}
}
