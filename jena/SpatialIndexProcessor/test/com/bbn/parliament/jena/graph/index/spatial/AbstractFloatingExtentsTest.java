// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public abstract class AbstractFloatingExtentsTest extends AbstractSpatialTest {

	public void notest() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		ResultSet result = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT"
			+ "	?building1 ?building2 ?building3 ?building4 ?building5 "
			+ "WHERE {	"
			+ "	?circle a gml:Circle ;"
			+ "		gml:radius \"10000.0\"^^xsd:double ."
			+ "	?building1 a example:SpatialThing ;"
			+ "		georss:where ?sreg1 ."
			+ "	?sreg1 rcc:part ?circle .	"
			+ "	?building2 a example:SpatialThing ;"
			+ "		georss:where ?sreg2 ."
			+ "	?sreg2	geo:representativeExtent ?reg2 ."
			+ "	?sreg2 rcc:part ?circle .	"
			+ "	?building3 a example:SpatialThing ;"
			+ "		georss:where ?sreg3 ."
			+ "	?sreg3 rcc:part ?circle .	"
			+ "	?building4 a example:SpatialThing ;"
			+ "		georss:where ?sreg4 ."
			+ "	?sreg4 rcc:part ?circle .	"
			+ "	?building5 a example:SpatialThing ;"
			+ "		georss:where ?sreg5 ."
			+ "	?sreg5 rcc:part ?circle .	"
			+ "	FILTER (?building1 != ?building2 &&"
			+ "		?building1 != ?building3 &&"
			+ "		?building1 != ?building4 &&"
			+ "		?building1 != ?building5 &&"
			+ "		?building2 != ?building3 &&"
			+ "		?building2 != ?building4 &&"
			+ "		?building2 != ?building5 &&"
			+ "		?building3 != ?building4 &&"
			+ "		?building3 != ?building5 &&"
			+ "		?building4 != ?building5"
			+ "	)"
			+ "}");
		assertTrue(result.hasNext());
		QuerySolution solution = result.nextSolution();
		Resource obs1 = solution.getResource("building1");
		Resource obs2 = solution.getResource("building2");
		Resource obs3 = solution.getResource("building3");
		Resource obs4 = solution.getResource("building4");
		Resource obs5 = solution.getResource("building5");

		assertNotSame(obs1, obs2);
		assertNotSame(obs1, obs3);
		assertNotSame(obs1, obs4);
		assertNotSame(obs1, obs5);

	}

	@Test
	public void testThreeExtentsInCircle() {
		loadData("queries/BuildingExample2.ttl");
		ResultSet result = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT"
			+ "  ?building1 ?building2 ?building3 "
			+ "WHERE { "
			+ " (?sreg1 ?sreg2 ?sreg3) rcc:part ?circle ."
			+ "  ?building1 a example:SpatialThing ;"
			+ "     georss:where ?sreg1 ."
			+ "  ?building2 a example:SpatialThing ;"
			+ "     georss:where ?sreg2 ."
			+ "  ?building3 a example:SpatialThing ;"
			+ "     georss:where ?sreg3 ."
			+ "  ?circle a gml:Circle ;"
			+ "     gml:radius \"0.1\"^^xsd:double ."
			//+ "?circle ogc:covers ?sreg1 ."
			//+ "  ?sreg1 rcc:part ?circle .   "
			//+ "  ?sreg2 rcc:part ?circle .   "
			//+ "  ?sreg3 rcc:part ?circle .   "
			+ "  FILTER (?building1 != ?building2 &&"
			+ "     ?building1 != ?building3 &&"
			+ "     ?building2 != ?building3"
			+ "  )"
			+ "}");
		assertTrue(result.hasNext());
		List<Resource[]> triples = new ArrayList<>();
		Resource sl3 = ResourceFactory.createResource(EXAMPLE2_NS + "building3");
		Resource sl4 = ResourceFactory.createResource(EXAMPLE2_NS + "building4");
		Resource sl5 = ResourceFactory.createResource(EXAMPLE2_NS + "building5");

		triples.add(new Resource[] { sl3, sl4, sl5 });
		triples.add(new Resource[] { sl3, sl5, sl4 });
		triples.add(new Resource[] { sl4, sl3, sl5 });
		triples.add(new Resource[] { sl4, sl5, sl3 });
		triples.add(new Resource[] { sl5, sl3, sl4 });
		triples.add(new Resource[] { sl5, sl4, sl3 });

		while (result.hasNext()) {
			QuerySolution solution = result.nextSolution();
			Resource obs1 = solution.getResource("building1");
			Resource obs2 = solution.getResource("building2");
			Resource obs3 = solution.getResource("building3");

			assertNotSame(obs1, obs2);
			assertNotSame(obs1, obs3);
			assertNotSame(obs2, obs3);
			boolean found = false;
			for (Resource[] triple : triples) {
				Resource t1 = triple[0];
				Resource t2 = triple[1];
				Resource t3 = triple[2];

				if (obs1.equals(t1) && obs2.equals(t2) && obs3.equals(t3)) {
					found = true;
					triples.remove(triple);
					break;
				}
			}
			LOG.debug("{}found: {}, {}, {}",
				new Object[] { (found ? "" : "not "), obs1, obs2, obs3 });
			assertTrue(found);
		}
		if (LOG.isDebugEnabled() && triples.size() > 0) {
			for (Resource[] r : triples) {
				LOG.debug(" Not found: {} {} {}", new Object[] { r[0], r[1], r[2] });
			}
		}
		assertEquals(0, triples.size());
	}

	@Test
	public void testSinglePoint() {
		loadData("queries/BuildingExample1.ttl");
		ResultSet result = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT"
			+ "  ?building1 "
			+ "WHERE { "
			+ "  ?circle a gml:Circle ;"
			+ "     gml:radius \"5\"^^xsd:double ."
			+ "  ?building1 a example:SpatialThing ."
			+ "  ?building1 georss:where example1:point1 ."
			+ "  example1:point1 rcc:part ?circle .   "
			+ "}");
		assertTrue(result.hasNext());
		QuerySolution solution = result.nextSolution();

		Resource obs1 = solution.getResource("building1");

		assertNotNull(obs1);

		Resource sl1 = ResourceFactory.createResource(EXAMPLE1_NS + "building1");
		assertEquals(sl1, obs1);
	}

	@Test
	public void testCircleExtentsKnown() {
		loadData("queries/BuildingExample1.ttl");
		ResultSet result = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT"
			+ "  ?building1 ?building2 "
			+ "WHERE { "
			+ "  ?circle a gml:Circle ;"
			+ "     gml:radius \"280\"^^xsd:double ."
			+ "  ?building1 a example:SpatialThing ."
			+ "  ?building1 georss:where example1:point1 . "
			//+ "  example1:point1 rcc:part ?circle .   "
			+ "  ?building2 a example:SpatialThing ."
			+ "  ?building2 georss:where example1:point2 ."
			+ "  (example1:point1 example1:point2) rcc:part ?circle .   "
			+ " FILTER(?building1 != ?building2) "
			+ "}");
		assertTrue(result.hasNext());
		QuerySolution solution = result.nextSolution();
		Resource obs1 = solution.getResource("building1");
		Resource obs2 = solution.getResource("building2");

		assertNotSame(obs1, obs2);

		assertNotNull(obs1);
		assertNotNull(obs2);

		assertEquals(ResourceFactory.createResource(EXAMPLE1_NS + "building1"), obs1);
		assertEquals(ResourceFactory.createResource(EXAMPLE1_NS + "building2"), obs2);
	}

	@Test
	public void testCircleReturnFloater() {
		loadData("queries/BuildingExample1.ttl");
		ResultSet result = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT"
			+ "  ?building1 ?building2 ?circle "
			+ "WHERE { "
			+ "  ?circle a gml:Circle ;"
			+ "     gml:radius \"280\"^^xsd:double ."
			+ "  ?building1 a example:SpatialThing ."
			+ "  ?building1 georss:where example1:point1 ."
			//+ "  example1:point1 rcc:part ?circle .   "
			+ "  ?building2 a example:SpatialThing ."
			+ "  ?building2 georss:where example1:point2 ."
			+ "  (example1:point1 example1:point2) rcc:part ?circle .   "
			+ " FILTER(?building1 != ?building2) "
			+ "}");
		assertTrue(result.hasNext());
		QuerySolution solution = result.nextSolution();
		//printQuerySolution(solution);
		Resource obs1 = solution.getResource("building1");
		Resource obs2 = solution.getResource("building2");
		Resource circle = solution.getResource("circle");
		assertNotSame(obs1, obs2);

		assertNotNull(obs1);
		assertNotNull(obs2);

		assertEquals(ResourceFactory.createResource(EXAMPLE1_NS + "building1"), obs1);
		assertEquals(ResourceFactory.createResource(EXAMPLE1_NS + "building2"), obs2);

		assertNotNull(circle);
	}

	@Test
	public void testCircleExtentsUnknown() {
		loadData("queries/BuildingExample1.ttl");
		ResultSet result = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT"
			+ "  ?building1 ?building2 "
			+ "WHERE { "
			+ "  ?circle a gml:Circle ;"
			+ "     gml:radius \"280\"^^xsd:double ."
			+ "  ?building1 a example:SpatialThing ."
			+ "  ?building1 georss:where ?sreg1 ."
			//+ "  ?sreg1 rcc:part ?circle .   "
			+ "  ?building2 a example:SpatialThing ."
			+ "  ?building2 georss:where ?sreg2 ."
			+ "  (?sreg1 ?sreg2) rcc:part ?circle .   "
			+ " FILTER(?building1 != ?building2) "
			+ "}");
		assertTrue(result.hasNext());

		Resource sl1 = ResourceFactory.createResource(EXAMPLE1_NS + "building1");
		Resource sl2 = ResourceFactory.createResource(EXAMPLE1_NS + "building2");

		List<Resource[]> pairs = new ArrayList<>();
		pairs.add(new Resource[] { sl1, sl2 });
		pairs.add(new Resource[] { sl2, sl1 });

		while (result.hasNext()) {
			QuerySolution solution = result.nextSolution();
			Resource obs1 = solution.getResource("building1");
			Resource obs2 = solution.getResource("building2");

			assertNotSame(obs1, obs2);

			assertNotNull(obs1);
			assertNotNull(obs2);

			boolean found = false;
			for (Resource[] pair : pairs) {
				Resource p1 = pair[0];
				Resource p2 = pair[1];
				if (p1.equals(obs1)) {
					if (p2.equals(obs2)) {
						found = true;
						pairs.remove(pair);
						break;
					}
				}
			}
			assertTrue(found);
		}
		assertEquals(0, pairs.size());
	}

	@Test
	public void testExtentsUnknownReorderedQuery() {
		loadData("queries/BuildingExample1.ttl");

		ResultSet result = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT"
			+ "  ?building1 ?building2 "
			+ "WHERE { "
			+ "  ?circle a gml:Circle ;"
			+ "     gml:radius \"280\"^^xsd:double ."
			+ "  (?sreg1 ?sreg2) rcc:part ?circle .   "
			//+ "  ?sreg2 rcc:part ?circle .   "
			+ "  ?building1 a example:SpatialThing ."
			+ "  ?building1 georss:where ?sreg1 ."
			+ "  ?building2 a example:SpatialThing ."
			+ "  ?building2 georss:where ?sreg2 ."
			+ " FILTER(?building1 != ?building2) "
			+ "}");
		assertTrue(result.hasNext());

		Resource sl1 = ResourceFactory.createResource(EXAMPLE1_NS + "building1");
		Resource sl2 = ResourceFactory.createResource(EXAMPLE1_NS + "building2");

		List<Resource[]> pairs = new ArrayList<>();
		pairs.add(new Resource[] { sl1, sl2 });
		pairs.add(new Resource[] { sl2, sl1 });

		while (result.hasNext()) {
			QuerySolution solution = result.nextSolution();
			Resource obs1 = solution.getResource("building1");
			Resource obs2 = solution.getResource("building2");

			assertNotSame(obs1, obs2);

			assertNotNull(obs1);
			assertNotNull(obs2);

			boolean found = false;
			for (Resource[] pair : pairs) {
				Resource p1 = pair[0];
				Resource p2 = pair[1];
				if (p1.equals(obs1)) {
					if (p2.equals(obs2)) {
						found = true;
						pairs.remove(pair);
						break;
					}
				}
			}
			if (!found) {
				System.err.println("not found: ");
				System.err.println(obs1.getURI());
				System.err.println(obs2.getURI());
			}
			assertTrue(found);
			System.out.println("found: ");
			System.out.println(obs1.getURI());
			System.out.println(obs2.getURI());

		}
		assertEquals(0, pairs.size());
	}

	@Test
	public void testExtentsUnknownSize0Circle() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		ResultSet result = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT"
			+ "  ?building1 ?building2 "
			+ "WHERE { "
			+ "  ?circle a gml:Circle ."
			+ "  ?circle gml:radius \"0.0\"^^xsd:double ."
			+ "  ?building1 a example:SpatialThing ."
			+ "  ?building1 georss:where ?sreg1 ."
			//+ "  ?sreg1 rcc:part ?circle .   "
			+ "  ?building2 a example:SpatialThing ."
			+ "  ?building2 georss:where ?sreg2 ."
			+ "  (?sreg1 ?sreg2) rcc:part ?circle .   "
			+ " FILTER(?building1 != ?building2) "
			+ "}");

		assertFalse(result.hasNext());
	}

	@Test
	public void testExtentsSmallCircle() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		ResultSet result = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT"
			+ "  ?reg1 ?reg2 "
			+ "WHERE { "
			+ "  ?circle a gml:Circle ."
			+ "  ?circle gml:radius \"0.000000000005\"^^xsd:double ."
			+ "  (?reg1 ?reg2) rcc:part ?circle .   "
			//+ "  ?reg2 rcc:part ?circle .   "
			+ " FILTER(?reg1 != ?reg2) "
			+ "}");
		while (result.hasNext()) {
			QuerySolution solution = result.nextSolution();
			Resource obs1 = solution.getResource("building1");
			Resource obs2 = solution.getResource("building2");

			System.out.println(obs1);
			System.out.println(obs2);
			System.out.println();
		}
		//assertFalse(result.hasNext());
	}

	@Test
	public void testExtentsUnknownMultipleResults() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		ResultSet result = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT"
			+ "  ?building1 ?building2 "
			+ "WHERE { "
			+ "  ?circle a gml:Circle ."
			+ "  ?circle gml:radius \"500\"^^xsd:double ."
			+ "  ?building1 a example:SpatialThing ."
			+ "  ?building1 georss:where ?sreg1 ."
			+ "  ?building2 a example:SpatialThing ."
			+ "  ?building2 georss:where ?sreg2 ."
			+ " FILTER(?building1 != ?building2) "
			+ "  (?sreg1 ?sreg2) rcc:part ?circle . "
			+ "}");

		assertTrue(result.hasNext());

		Resource sl1 = ResourceFactory.createResource(EXAMPLE1_NS + "building1");
		Resource sl2 = ResourceFactory.createResource(EXAMPLE1_NS + "building2");
		Resource sl3 = ResourceFactory.createResource(EXAMPLE2_NS + "building3");
		Resource sl4 = ResourceFactory.createResource(EXAMPLE2_NS + "building4");
		Resource sl5 = ResourceFactory.createResource(EXAMPLE2_NS + "building5");

		List<Resource[]> pairs = new ArrayList<>();
		pairs.add(new Resource[] { sl1, sl2 });
		pairs.add(new Resource[] { sl2, sl1 });
		pairs.add(new Resource[] { sl3, sl4 });
		pairs.add(new Resource[] { sl3, sl5 });
		pairs.add(new Resource[] { sl4, sl3 });
		pairs.add(new Resource[] { sl4, sl5 });
		pairs.add(new Resource[] { sl5, sl3 });
		pairs.add(new Resource[] { sl5, sl4 });

		while (result.hasNext()) {
			QuerySolution solution = result.nextSolution();
			Resource obs1 = solution.getResource("building1");
			Resource obs2 = solution.getResource("building2");
			boolean found = false;
			for (Resource[] pair : pairs) {
				Resource p1 = pair[0];
				Resource p2 = pair[1];
				if (p1.equals(obs1)) {
					if (p2.equals(obs2)) {
						found = true;
						pairs.remove(pair);
						break;
					}
				}
			}
			assertTrue(found);
			//System.out.println("*******");
			//System.out.println(obs1.getURI());
			//System.out.println(obs2.getURI());
		}
		assertEquals(0, pairs.size());
	}
}
