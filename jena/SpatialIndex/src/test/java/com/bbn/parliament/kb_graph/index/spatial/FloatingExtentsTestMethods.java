package com.bbn.parliament.kb_graph.index.spatial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FloatingExtentsTestMethods extends SpatialTestDataset {
	private static final Logger LOG = LoggerFactory.getLogger(FloatingExtentsTestMethods.class);

	public FloatingExtentsTestMethods(Properties factoryProperties) {
		super(factoryProperties);
	}

	private static final String NOTEST_QUERY = """
		SELECT DISTINCT
			?building1 ?building2 ?building3 ?building4 ?building5
		WHERE {
			?circle a gml:Circle ;
				gml:radius "10000.0"^^xsd:double .
			?building1 a example:SpatialThing ;
				georss:where ?sreg1 .
			?sreg1 rcc:part ?circle .
			?building2 a example:SpatialThing ;
				georss:where ?sreg2 .
			?sreg2	geo:representativeExtent ?reg2 .
			?sreg2 rcc:part ?circle .
			?building3 a example:SpatialThing ;
				georss:where ?sreg3 .
			?sreg3 rcc:part ?circle .
			?building4 a example:SpatialThing ;
				georss:where ?sreg4 .
			?sreg4 rcc:part ?circle .
			?building5 a example:SpatialThing ;
				georss:where ?sreg5 .
			?sreg5 rcc:part ?circle .
			FILTER (?building1 != ?building2
				&& ?building1 != ?building3
				&& ?building1 != ?building4
				&& ?building1 != ?building5
				&& ?building2 != ?building3
				&& ?building2 != ?building4
				&& ?building2 != ?building5
				&& ?building3 != ?building4
				&& ?building3 != ?building5
				&& ?building4 != ?building5)
		}
		""";

	public void testNot() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		try (var qexec = performQuery(NOTEST_QUERY)) {
			ResultSet result = qexec.execSelect();
			assertTrue(result.hasNext());

			QuerySolution solution = result.next();
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
	}

	private static final String THREE_EXTENTS_IN_CIRCLE_QUERY = """
		select distinct ?building1 ?building2 ?building3 where {
			(?sreg1 ?sreg2 ?sreg3) rcc:part ?circle .
			?building1 a example:SpatialThing ;
				georss:where ?sreg1 .
			?building2 a example:SpatialThing ;
				georss:where ?sreg2 .
			?building3 a example:SpatialThing ;
				georss:where ?sreg3 .
			?circle a gml:Circle ;
				gml:radius "0.1"^^xsd:double .
			# ?circle ogc:covers ?sreg1 .
			# ?sreg1 rcc:part ?circle .
			# ?sreg2 rcc:part ?circle .
			# ?sreg3 rcc:part ?circle .
			filter (?building1 != ?building2
				&& ?building1 != ?building3
				&& ?building2 != ?building3
			)
		}
		""";

	public void testThreeExtentsInCircle() {
		loadData("queries/BuildingExample2.ttl");
		try (var qexec = performQuery(THREE_EXTENTS_IN_CIRCLE_QUERY)) {
			ResultSet result = qexec.execSelect();
			assertTrue(result.hasNext());

			Resource sl3 = createResource("example2:building3");
			Resource sl4 = createResource("example2:building4");
			Resource sl5 = createResource("example2:building5");

			List<Resource[]> triples = new ArrayList<>();
			triples.add(new Resource[] { sl3, sl4, sl5 });
			triples.add(new Resource[] { sl3, sl5, sl4 });
			triples.add(new Resource[] { sl4, sl3, sl5 });
			triples.add(new Resource[] { sl4, sl5, sl3 });
			triples.add(new Resource[] { sl5, sl3, sl4 });
			triples.add(new Resource[] { sl5, sl4, sl3 });

			while (result.hasNext()) {
				QuerySolution solution = result.next();
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
	}

	private static final String SINGLE_POINT_QUERY = """
		select distinct ?building1 where {
			?circle a gml:Circle ;
				gml:radius "5"^^xsd:double .
			?building1 a example:SpatialThing ;
				georss:where example1:point1 .
			example1:point1 rcc:part ?circle .
		}
		""";

	public void testSinglePoint() {
		loadData("queries/BuildingExample1.ttl");
		try (var qexec = performQuery(SINGLE_POINT_QUERY)) {
			ResultSet result = qexec.execSelect();
			assertTrue(result.hasNext());

			QuerySolution solution = result.next();
			Resource obs1 = solution.getResource("building1");
			assertNotNull(obs1);

			Resource sl1 = createResource("example1:building1");
			assertEquals(sl1, obs1);
		}
	}

	private static final String CIRCLE_EXTENTS_KNOWN_QUERY = """
		select distinct ?building1 ?building2 where {
			?circle a gml:Circle ;
				gml:radius "280"^^xsd:double .
			?building1 a example:SpatialThing ;
				georss:where example1:point1 .
			# example1:point1 rcc:part ?circle .
			?building2 a example:SpatialThing ;
				georss:where example1:point2 .
			(example1:point1 example1:point2) rcc:part ?circle .
			filter(?building1 != ?building2)
		}
		""";

	public void testCircleExtentsKnown() {
		loadData("queries/BuildingExample1.ttl");
		try (var qexec = performQuery(CIRCLE_EXTENTS_KNOWN_QUERY)) {
			ResultSet result = qexec.execSelect();
			assertTrue(result.hasNext());

			QuerySolution solution = result.next();
			Resource obs1 = solution.getResource("building1");
			Resource obs2 = solution.getResource("building2");

			assertNotNull(obs1);
			assertNotNull(obs2);
			assertNotSame(obs1, obs2);

			assertEquals(createResource("example1:building1"), obs1);
			assertEquals(createResource("example1:building2"), obs2);
		}
	}

	private static final String CIRCLE_RETURN_FLOATER_QUERY = """
		select distinct ?building1 ?building2 ?circle where {
			?circle a gml:Circle ;
				gml:radius "280"^^xsd:double .
			?building1 a example:SpatialThing ;
				georss:where example1:point1 .
			# example1:point1 rcc:part ?circle .
			?building2 a example:SpatialThing ;
				georss:where example1:point2 .
			(example1:point1 example1:point2) rcc:part ?circle .
			filter(?building1 != ?building2)
		}
		""";

	public void testCircleReturnFloater() {
		loadData("queries/BuildingExample1.ttl");
		try (var qexec = performQuery(CIRCLE_RETURN_FLOATER_QUERY)) {
			ResultSet result = qexec.execSelect();
			assertTrue(result.hasNext());

			QuerySolution solution = result.next();
			//printQuerySolution(solution);
			Resource obs1 = solution.getResource("building1");
			Resource obs2 = solution.getResource("building2");
			Resource circle = solution.getResource("circle");
			assertNotSame(obs1, obs2);

			assertNotNull(obs1);
			assertNotNull(obs2);

			assertEquals(createResource("example1:building1"), obs1);
			assertEquals(createResource("example1:building2"), obs2);

			assertNotNull(circle);
		}
	}

	private static final String CIRCLE_EXTENTS_UNKNOWN_QUERY = """
		select distinct ?building1 ?building2 where {
			?circle a gml:Circle ;
				gml:radius "280"^^xsd:double .
			?building1 a example:SpatialThing ;
				georss:where ?sreg1 .
			# ?sreg1 rcc:part ?circle .
			?building2 a example:SpatialThing ;
				georss:where ?sreg2 .
			(?sreg1 ?sreg2) rcc:part ?circle .
			filter(?building1 != ?building2)
		}
		""";

	public void testCircleExtentsUnknown() {
		loadData("queries/BuildingExample1.ttl");
		try (var qexec = performQuery(CIRCLE_EXTENTS_UNKNOWN_QUERY)) {
			ResultSet result = qexec.execSelect();
			assertTrue(result.hasNext());

			Resource sl1 = createResource("example1:building1");
			Resource sl2 = createResource("example1:building2");

			List<Resource[]> pairs = new ArrayList<>();
			pairs.add(new Resource[] { sl1, sl2 });
			pairs.add(new Resource[] { sl2, sl1 });

			while (result.hasNext()) {
				QuerySolution solution = result.next();
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
	}

	private static final String EXTENTS_UNKNOWN_REORDERED_QUERY = """
		select distinct ?building1 ?building2 where {
			?circle a gml:Circle ;
				gml:radius "280"^^xsd:double .
			(?sreg1 ?sreg2) rcc:part ?circle .
			# ?sreg2 rcc:part ?circle .
			?building1 a example:SpatialThing ;
				georss:where ?sreg1 .
			?building2 a example:SpatialThing ;
				georss:where ?sreg2 .
			filter(?building1 != ?building2)
		}
		""";

	public void testExtentsUnknownReorderedQuery() {
		loadData("queries/BuildingExample1.ttl");

		try (var qexec = performQuery(EXTENTS_UNKNOWN_REORDERED_QUERY)) {
			ResultSet result = qexec.execSelect();
			assertTrue(result.hasNext());

			Resource sl1 = createResource("example1:building1");
			Resource sl2 = createResource("example1:building2");

			List<Resource[]> pairs = new ArrayList<>();
			pairs.add(new Resource[] { sl1, sl2 });
			pairs.add(new Resource[] { sl2, sl1 });

			while (result.hasNext()) {
				QuerySolution solution = result.next();
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
					LOG.error("not found:");
					LOG.error("   {}", obs1.getURI());
					LOG.error("   {}", obs2.getURI());
				}
				assertTrue(found);
				LOG.debug("found:");
				LOG.debug("   {}", obs1.getURI());
				LOG.debug("   {}", obs2.getURI());
			}
			assertEquals(0, pairs.size());
		}
	}

	private static final String EXTENTS_UNKNOWN_SIZE0_CIRCLE_QUERY = """
		select distinct ?building1 ?building2 where {
			?circle a gml:Circle ;
				gml:radius "0.0"^^xsd:double .
			?building1 a example:SpatialThing ;
				georss:where ?sreg1 .
			# ?sreg1 rcc:part ?circle .
			?building2 a example:SpatialThing ;
				georss:where ?sreg2 .
			(?sreg1 ?sreg2) rcc:part ?circle .
			filter(?building1 != ?building2)
		}
		""";

	public void testExtentsUnknownSize0Circle() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		try (var qexec = performQuery(EXTENTS_UNKNOWN_SIZE0_CIRCLE_QUERY)) {
			assertFalse(qexec.execSelect().hasNext());
		}
	}

	private static final String EXTENTS_SMALL_CIRCLE_QUERY = """
		select distinct ?reg1 ?reg2 where {
			?circle a gml:Circle ;
				gml:radius "0.000000000005"^^xsd:double .
			(?reg1 ?reg2) rcc:part ?circle .
			# ?reg2 rcc:part ?circle .
			filter(?reg1 != ?reg2)
		}
		""";

	public void testExtentsSmallCircle() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		try (var qexec = performQuery(EXTENTS_SMALL_CIRCLE_QUERY)) {
			ResultSet result = qexec.execSelect();
			while (result.hasNext()) {
				QuerySolution solution = result.next();
				Resource obs1 = solution.getResource("building1");
				Resource obs2 = solution.getResource("building2");

				System.out.println(obs1);
				System.out.println(obs2);
				System.out.println();
			}
		}
	}

	private static final String EXTENTS_UNKNOWN_MULTIPLE_RESULTS_QUERY = """
		select distinct ?building1 ?building2 where {
			?circle a gml:Circle ;
				gml:radius "500"^^xsd:double .
			?building1 a example:SpatialThing ;
				georss:where ?sreg1 .
			?building2 a example:SpatialThing ;
				georss:where ?sreg2 .
			filter(?building1 != ?building2)
			(?sreg1 ?sreg2) rcc:part ?circle .
		}
		""";

	public void testExtentsUnknownMultipleResults() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		try (var qexec = performQuery(EXTENTS_UNKNOWN_MULTIPLE_RESULTS_QUERY)) {
			ResultSet result = qexec.execSelect();
			assertTrue(result.hasNext());

			Resource sl1 = createResource("example1:building1");
			Resource sl2 = createResource("example1:building2");
			Resource sl3 = createResource("example2:building3");
			Resource sl4 = createResource("example2:building4");
			Resource sl5 = createResource("example2:building5");

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
				QuerySolution solution = result.next();
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
}
