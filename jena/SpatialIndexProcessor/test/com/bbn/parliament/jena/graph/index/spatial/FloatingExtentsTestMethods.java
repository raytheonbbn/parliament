package com.bbn.parliament.jena.graph.index.spatial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.client.CloseableQueryExec;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;

public class FloatingExtentsTestMethods extends SpatialTestDataset {
	private static final Logger LOG = LoggerFactory.getLogger(FloatingExtentsTestMethods.class);

	public FloatingExtentsTestMethods(Properties factoryProperties) {
		super(factoryProperties);
	}

	private static final String NOTEST_QUERY = ""
		+ "SELECT DISTINCT\n"
		+ "	?building1 ?building2 ?building3 ?building4 ?building5\n"
		+ "WHERE {\n"
		+ "	?circle a gml:Circle ;\n"
		+ "		gml:radius \"10000.0\"^^xsd:double .\n"
		+ "	?building1 a example:SpatialThing ;\n"
		+ "		georss:where ?sreg1 .\n"
		+ "	?sreg1 rcc:part ?circle .\n"
		+ "	?building2 a example:SpatialThing ;\n"
		+ "		georss:where ?sreg2 .\n"
		+ "	?sreg2	geo:representativeExtent ?reg2 .\n"
		+ "	?sreg2 rcc:part ?circle .\n"
		+ "	?building3 a example:SpatialThing ;\n"
		+ "		georss:where ?sreg3 .\n"
		+ "	?sreg3 rcc:part ?circle .\n"
		+ "	?building4 a example:SpatialThing ;\n"
		+ "		georss:where ?sreg4 .\n"
		+ "	?sreg4 rcc:part ?circle .\n"
		+ "	?building5 a example:SpatialThing ;\n"
		+ "		georss:where ?sreg5 .\n"
		+ "	?sreg5 rcc:part ?circle .\n"
		+ "	FILTER (?building1 != ?building2 &&\n"
		+ "		?building1 != ?building3 &&\n"
		+ "		?building1 != ?building4 &&\n"
		+ "		?building1 != ?building5 &&\n"
		+ "		?building2 != ?building3 &&\n"
		+ "		?building2 != ?building4 &&\n"
		+ "		?building2 != ?building5 &&\n"
		+ "		?building3 != ?building4 &&\n"
		+ "		?building3 != ?building5 &&\n"
		+ "		?building4 != ?building5\n"
		+ "	)\n"
		+ "}";

	public void testNot() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		try (CloseableQueryExec qexec = performQuery(NOTEST_QUERY)) {
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

	private static final String THREE_EXTENTS_IN_CIRCLE_QUERY = ""
		+ "SELECT DISTINCT\n"
		+ "  ?building1 ?building2 ?building3\n"
		+ "WHERE {\n"
		+ " (?sreg1 ?sreg2 ?sreg3) rcc:part ?circle .\n"
		+ "  ?building1 a example:SpatialThing ;\n"
		+ "     georss:where ?sreg1 .\n"
		+ "  ?building2 a example:SpatialThing ;\n"
		+ "     georss:where ?sreg2 .\n"
		+ "  ?building3 a example:SpatialThing ;\n"
		+ "     georss:where ?sreg3 .\n"
		+ "  ?circle a gml:Circle ;\n"
		+ "     gml:radius \"0.1\"^^xsd:double .\n"
		//+ "?circle ogc:covers ?sreg1 .\n"
		//+ "  ?sreg1 rcc:part ?circle .\n"
		//+ "  ?sreg2 rcc:part ?circle .\n"
		//+ "  ?sreg3 rcc:part ?circle .\n"
		+ "  FILTER (?building1 != ?building2 &&\n"
		+ "     ?building1 != ?building3 &&\n"
		+ "     ?building2 != ?building3\n"
		+ "  )\n"
		+ "}";

	public void testThreeExtentsInCircle() {
		loadData("queries/BuildingExample2.ttl");
		try (CloseableQueryExec qexec = performQuery(THREE_EXTENTS_IN_CIRCLE_QUERY)) {
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

	private static final String SINGLE_POINT_QUERY = ""
		+ "SELECT DISTINCT\n"
		+ "  ?building1\n"
		+ "WHERE {\n"
		+ "  ?circle a gml:Circle ;\n"
		+ "     gml:radius \"5\"^^xsd:double .\n"
		+ "  ?building1 a example:SpatialThing .\n"
		+ "  ?building1 georss:where example1:point1 .\n"
		+ "  example1:point1 rcc:part ?circle .\n"
		+ "}";

	public void testSinglePoint() {
		loadData("queries/BuildingExample1.ttl");
		try (CloseableQueryExec qexec = performQuery(SINGLE_POINT_QUERY)) {
			ResultSet result = qexec.execSelect();
			assertTrue(result.hasNext());

			QuerySolution solution = result.next();
			Resource obs1 = solution.getResource("building1");
			assertNotNull(obs1);

			Resource sl1 = createResource("example1:building1");
			assertEquals(sl1, obs1);
		}
	}

	private static final String CIRCLE_EXTENTS_KNOWN_QUERY = ""
		+ "SELECT DISTINCT\n"
		+ "  ?building1 ?building2\n"
		+ "WHERE {\n"
		+ "  ?circle a gml:Circle ;\n"
		+ "     gml:radius \"280\"^^xsd:double .\n"
		+ "  ?building1 a example:SpatialThing .\n"
		+ "  ?building1 georss:where example1:point1 .\n"
		//+ "  example1:point1 rcc:part ?circle .\n"
		+ "  ?building2 a example:SpatialThing .\n"
		+ "  ?building2 georss:where example1:point2 .\n"
		+ "  (example1:point1 example1:point2) rcc:part ?circle .\n"
		+ " FILTER(?building1 != ?building2)\n"
		+ "}";

	public void testCircleExtentsKnown() {
		loadData("queries/BuildingExample1.ttl");
		try (CloseableQueryExec qexec = performQuery(CIRCLE_EXTENTS_KNOWN_QUERY)) {
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

	private static final String CIRCLE_RETURN_FLOATER_QUERY = ""
		+ "SELECT DISTINCT\n"
		+ "  ?building1 ?building2 ?circle\n"
		+ "WHERE {\n"
		+ "  ?circle a gml:Circle ;\n"
		+ "     gml:radius \"280\"^^xsd:double .\n"
		+ "  ?building1 a example:SpatialThing .\n"
		+ "  ?building1 georss:where example1:point1 .\n"
		//+ "  example1:point1 rcc:part ?circle .\n"
		+ "  ?building2 a example:SpatialThing .\n"
		+ "  ?building2 georss:where example1:point2 .\n"
		+ "  (example1:point1 example1:point2) rcc:part ?circle .\n"
		+ " FILTER(?building1 != ?building2)\n"
		+ "}";

	public void testCircleReturnFloater() {
		loadData("queries/BuildingExample1.ttl");
		try (CloseableQueryExec qexec = performQuery(CIRCLE_RETURN_FLOATER_QUERY)) {
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

	private static final String CIRCLE_EXTENTS_UNKNOWN_QUERY = ""
		+ "SELECT DISTINCT\n"
		+ "  ?building1 ?building2\n"
		+ "WHERE {\n"
		+ "  ?circle a gml:Circle ;\n"
		+ "     gml:radius \"280\"^^xsd:double .\n"
		+ "  ?building1 a example:SpatialThing .\n"
		+ "  ?building1 georss:where ?sreg1 .\n"
		//+ "  ?sreg1 rcc:part ?circle .\n"
		+ "  ?building2 a example:SpatialThing .\n"
		+ "  ?building2 georss:where ?sreg2 .\n"
		+ "  (?sreg1 ?sreg2) rcc:part ?circle .\n"
		+ " FILTER(?building1 != ?building2)\n"
		+ "}";

	public void testCircleExtentsUnknown() {
		loadData("queries/BuildingExample1.ttl");
		try (CloseableQueryExec qexec = performQuery(CIRCLE_EXTENTS_UNKNOWN_QUERY)) {
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

	private static final String EXTENTS_UNKNOWN_REORDERED_QUERY = ""
		+ "SELECT DISTINCT\n"
		+ "  ?building1 ?building2\n"
		+ "WHERE {\n"
		+ "  ?circle a gml:Circle ;\n"
		+ "     gml:radius \"280\"^^xsd:double .\n"
		+ "  (?sreg1 ?sreg2) rcc:part ?circle .\n"
		//+ "  ?sreg2 rcc:part ?circle .\n"
		+ "  ?building1 a example:SpatialThing .\n"
		+ "  ?building1 georss:where ?sreg1 .\n"
		+ "  ?building2 a example:SpatialThing .\n"
		+ "  ?building2 georss:where ?sreg2 .\n"
		+ " FILTER(?building1 != ?building2)\n"
		+ "}";

	public void testExtentsUnknownReorderedQuery() {
		loadData("queries/BuildingExample1.ttl");

		try (CloseableQueryExec qexec = performQuery(EXTENTS_UNKNOWN_REORDERED_QUERY)) {
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

	private static final String EXTENTS_UNKNOWN_SIZE0_CIRCLE_QUERY = ""
		+ "SELECT DISTINCT\n"
		+ "  ?building1 ?building2\n"
		+ "WHERE {\n"
		+ "  ?circle a gml:Circle .\n"
		+ "  ?circle gml:radius \"0.0\"^^xsd:double .\n"
		+ "  ?building1 a example:SpatialThing .\n"
		+ "  ?building1 georss:where ?sreg1 .\n"
		//+ "  ?sreg1 rcc:part ?circle .\n"
		+ "  ?building2 a example:SpatialThing .\n"
		+ "  ?building2 georss:where ?sreg2 .\n"
		+ "  (?sreg1 ?sreg2) rcc:part ?circle .\n"
		+ " FILTER(?building1 != ?building2)\n"
		+ "}";

	public void testExtentsUnknownSize0Circle() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		try (CloseableQueryExec qexec = performQuery(EXTENTS_UNKNOWN_SIZE0_CIRCLE_QUERY)) {
			assertFalse(qexec.execSelect().hasNext());
		}
	}

	private static final String EXTENTS_SMALL_CIRCLE_QUERY = ""
		+ "SELECT DISTINCT\n"
		+ "  ?reg1 ?reg2\n"
		+ "WHERE {\n"
		+ "  ?circle a gml:Circle .\n"
		+ "  ?circle gml:radius \"0.000000000005\"^^xsd:double .\n"
		+ "  (?reg1 ?reg2) rcc:part ?circle .\n"
		//+ "  ?reg2 rcc:part ?circle .\n"
		+ " FILTER(?reg1 != ?reg2)\n"
		+ "}";

	public void testExtentsSmallCircle() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		try (CloseableQueryExec qexec = performQuery(EXTENTS_SMALL_CIRCLE_QUERY)) {
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

	private static final String EXTENTS_UNKNOWN_MULTIPLE_RESULTS_QUERY = ""
		+ "SELECT DISTINCT\n"
		+ "  ?building1 ?building2\n"
		+ "WHERE {\n"
		+ "  ?circle a gml:Circle .\n"
		+ "  ?circle gml:radius \"500\"^^xsd:double .\n"
		+ "  ?building1 a example:SpatialThing .\n"
		+ "  ?building1 georss:where ?sreg1 .\n"
		+ "  ?building2 a example:SpatialThing .\n"
		+ "  ?building2 georss:where ?sreg2 .\n"
		+ " FILTER(?building1 != ?building2)\n"
		+ "  (?sreg1 ?sreg2) rcc:part ?circle .\n"
		+ "}";

	public void testExtentsUnknownMultipleResults() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		try (CloseableQueryExec qexec = performQuery(EXTENTS_UNKNOWN_MULTIPLE_RESULTS_QUERY)) {
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
