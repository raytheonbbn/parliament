/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This code was "borrowed" from the Jena Core 2.7.2 test code to work around
 * the fact that the jena-core-test JAR file disappeared from binary Jena
 * distributions starting with version 2.7.4.
 */

package com.bbn.parliament.jena.from_jena_test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.hp.hpl.jena.graph.Factory;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphListener;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Reifier;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.CollectionFactory;

/**
 * AbstractTestGraph provides a bunch of basic tests for something that purports
 * to be a Graph. The abstract method getGraph must be overridden in subclasses
 * to deliver a Graph of interest.
 *
 * @author kers
 */
public class AbstractTestGraph extends GraphTestBase
{
	public AbstractTestGraph( String name )
	{ super( name ); }

	@SuppressWarnings("static-method")
	public Graph getGraph() { return Factory.createGraphMem(); }

	public Graph getGraphWith( String facts )
	{
		Graph g = getGraph();
		graphAdd( g, facts );
		return g;
	}

	/** Check that contains respects by-value semantics. */
	public void testContainsByValue()
	{
		if (getGraph().getCapabilities().handlesLiteralTyping())
		{ // TODO fix the adhocness of this
			Graph g1 = getGraphWith( "x P '1'xsd:integer" );
			assertTrue( g1.contains( triple( "x P '01'xsd:int" ) ) );
			//
			Graph g2 = getGraphWith( "x P '1'xsd:int" );
			assertTrue( g2.contains( triple( "x P '1'xsd:integer" ) ) );
			//
			Graph g3 = getGraphWith( "x P '123'xsd:string" );
			assertTrue( g3.contains( triple( "x P '123'" ) ) );
		}
	}

	static final Triple [] tripleArray = tripleArray( "S P O; A R B; X Q Y" );

	static final List<Triple> tripleList = Arrays.asList( tripleArray( "i lt j; p equals q" ) );

	static final Triple [] setTriples = tripleArray
		( "scissors cut paper; paper wraps stone; stone breaks scissors" );

	static final Set<Triple> tripleSet = CollectionFactory.createHashedSet( Arrays.asList( setTriples ) );

	protected static void xSPOyXYZ( Reifier r )
	{
		xSPO( r );
		r.reifyAs( NodeCreateUtils.create( "y" ), NodeCreateUtils.createTriple( "X Y Z" ) );
	}

	protected static void aABC( Reifier r )
	{ r.reifyAs( NodeCreateUtils.create( "a" ), NodeCreateUtils.createTriple( "A B C" ) ); }

	protected static void xSPO( Reifier r )
	{ r.reifyAs( NodeCreateUtils.create( "x" ), NodeCreateUtils.createTriple( "S P O" ) ); }

	protected static boolean canBeEmpty( Graph g )
	{ return g.isEmpty(); }

	/** Handy triple for test purposes. */
	protected Triple SPO = NodeCreateUtils.createTriple( "S P O" );
	protected RecordingListener L = new RecordingListener();

	/** Utility: get a graph, register L with its manager, return the graph. */
	protected Graph getAndRegister( GraphListener gl )
	{
		Graph g = getGraph();
		g.getEventManager().register( gl );
		return g;
	}

	protected static Set<Node> listSubjects( Graph g )
	{
		return g.queryHandler().subjectsFor( Node.ANY, Node.ANY ).toSet();
	}

	protected static Set<Node> listPredicates( Graph g )
	{
		return g.queryHandler().predicatesFor( Node.ANY, Node.ANY ).toSet();
	}

	protected static Set<Node> listObjects( Graph g )
	{
		return g.queryHandler().objectsFor( Node.ANY, Node.ANY ).toSet();
	}

	public static Iterator<Triple> asIterator( Triple [] triples )
	{ return Arrays.asList( triples ).iterator(); }

	/**
	 * Test cases for RemoveSPO(); each entry is a triple (add, remove, result).
	 * <ul>
	 * <li>add - the triples to add to the graph to start with
	 * <li>remove - the pattern to use in the removal
	 * <li>result - the triples that should remain in the graph
	 * </ul>
	 */
	protected String[][] cases =
	{
		{ "x R y", "x R y", "" },
		{ "x R y; a P b", "x R y", "a P b" },
		{ "x R y; a P b", "?? R y", "a P b" },
		{ "x R y; a P b", "x R ??", "a P b" },
		{ "x R y; a P b", "x ?? y", "a P b" },
		{ "x R y; a P b", "?? ?? ??", "" },
		{ "x R y; a P b; c P d", "?? P ??", "x R y" },
		{ "x R y; a P b; x S y", "x ?? ??", "a P b" },
	};

	/**
	 * Test that remove(s, p, o) works, in the presence of inferencing graphs
	 * that mean emptyness isn't available. This is why we go round the houses
	 * and test that expected ~= initialContent + addedStuff - removed -
	 * initialContent.
	 */
	public void testRemoveSPO()
	{
		for (int i = 0; i < cases.length; i += 1)
			for (int j = 0; j < 3; j += 1)
			{
				Graph content = getGraph();
				Graph baseContent = copy( content );
				graphAdd( content, cases[i][0] );
				Triple remove = triple( cases[i][1] );
				Graph expected = graphWith( cases[i][2] );
				content.getBulkUpdateHandler().remove( remove.getSubject(), remove.getPredicate(), remove.getObject() );
				Graph finalContent = remove( copy( content ), baseContent );
				assertIsomorphic( cases[i][1], expected, finalContent );
			}
	}

	/**
	 * testIsomorphism from file data
	 *
	 * @throws FileNotFoundException
	 */
	public void testIsomorphismFile() throws IOException {
		testIsomorphismFile(1, RDFFormat.RDFXML, true);
		testIsomorphismFile(2, RDFFormat.RDFXML, true);
		testIsomorphismFile(3, RDFFormat.RDFXML, true);
		testIsomorphismFile(4, RDFFormat.RDFXML, true);
		testIsomorphismFile(5, RDFFormat.RDFXML, false);
		testIsomorphismFile(6, RDFFormat.RDFXML, false);
		testIsomorphismFile(7, RDFFormat.NTRIPLES, true);
		testIsomorphismFile(8, RDFFormat.NTRIPLES, false);
	}

	private static final String FILE_FMT = "testing/regression/testModelEquals/%1$d%2$d.%3$s";
	private static final String BASE_URI = "http://www.example.org/";
	private void testIsomorphismFile(int n, RDFFormat lang, boolean result) throws IOException {
		Graph g1 = getGraph();
		Graph g2 = getGraph();
		Model m1 = ModelFactory.createModelForGraph(g1);
		Model m2 = ModelFactory.createModelForGraph(g2);

		try (InputStream in = new FileInputStream(String.format(FILE_FMT, n, 1, lang.getExtension()))) {
			m1.read(in, BASE_URI, lang.toString());
		}
		try (InputStream in = new FileInputStream(String.format(FILE_FMT, n, 2, lang.getExtension()))) {
			m2.read(in, BASE_URI, lang.toString());
		}

		boolean rslt = g1.isIsomorphicWith(g2) == result;
		if (!rslt) {
			System.out.println("g1:");
			m1.write(System.out, "N-TRIPLE");
			System.out.println("g2:");
			m2.write(System.out, "N-TRIPLE");
		}
		assertTrue("Isomorphism test failed",rslt);
	}

	protected static void add( Graph toUpdate, Graph toAdd )
	{
		toUpdate.getBulkUpdateHandler().add( toAdd );
	}

	protected static Graph remove( Graph toUpdate, Graph toRemove )
	{
		toUpdate.getBulkUpdateHandler().delete( toRemove );
		return toUpdate;
	}

	protected static Graph copy( Graph g )
	{
		Graph result = Factory.createDefaultGraph();
		result.getBulkUpdateHandler().add( g );
		return result;
	}

	protected Graph getClosed()
	{
		Graph result = getGraph();
		result.close();
		return result;
	}
}
