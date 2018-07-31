package com.bbn.parliament.jena.graph.index.spatial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.NodeCreateUtils;
import com.bbn.parliament.jena.graph.index.spatial.sql.postgres.PostgresIndex;
import com.bbn.parliament.jena.graph.index.spatial.standard.StdConstants;
import com.bbn.parliament.jena.joseki.client.CloseableQueryExec;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.vocabulary.RDF;

public class QueryTestMethods extends SpatialTestDataset {
	private static final Logger LOG = LoggerFactory.getLogger(QueryTestMethods.class);

	public QueryTestMethods(Properties factoryProperties) {
		super(factoryProperties);
	}

	private static final String TANGENTIAL_PROPER_PART_POINTS_QUERY = ""
		+ "SELECT DISTINCT ?a\n"
		+ "WHERE {\n"
		+ "?polygon a gml:Polygon .\n"
		+ "?polygon gml:exterior ?ext .\n"
		+ "?ext a gml:LinearRing .\n"
		+ "?ext gml:posList \"-0.5 -0.5 -0.5 6 0.5 6 0.5 -0.5 -0.5 -0.5\" .\n"
		+ "?a rcc:tangentialProperPart ?polygon .\n"
		+ "}";

	public void testTangentialProperPartPoints() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		try (CloseableQueryExec qexec = performQuery(TANGENTIAL_PROPER_PART_POINTS_QUERY)) {
			checkResults(qexec, "example3:region1");
		}
	}

	private static final String NON_TANGENTIAL_PROPER_PART_POINTS_QUERY = ""
		+ "SELECT DISTINCT ?a\n"
		+ "WHERE {\n"
		+ "?polygon a gml:Polygon .\n"
		+ "?polygon gml:exterior ?ext .\n"
		+ "?ext a gml:LinearRing .\n"
		+ "?ext gml:posList \"34.8448761696609 33 34.8448761696609 35.9148048779863 34.8448761696609 37 40 37 40 33 34.8448761696609 33\" .\n"
		+ "?a rcc:nonTangentialProperPart ?polygon .\n"
		+ "}";

	public void testNonTangentialProperPartPoints() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");

		try (CloseableQueryExec qexec = performQuery(NON_TANGENTIAL_PROPER_PART_POINTS_QUERY)) {
			checkResults(qexec, "example2:point3", "example2:point4", "example2:point5", "example3:region2");
		}
	}

	private static final String NON_TANGENTIAL_PROPER_PART_MULTIPLE_QUERY = ""
		+ "SELECT DISTINCT ?a\n"
		+ "WHERE {\n"
		+ "?polygon a gml:Polygon .\n"
		+ "?polygon gml:exterior ?ext .\n"
		+ "?ext a gml:LinearRing .\n"
		+ "?ext gml:posList \"-1 -1 -1 6 1 16 1 -1 -1 -1\" .\n"
		+ "?a rcc:nonTangentialProperPart ?polygon .\n"
		+ "}";

	public void testNonTangentialProperPartMultiple() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		try (CloseableQueryExec qexec = performQuery(NON_TANGENTIAL_PROPER_PART_MULTIPLE_QUERY)) {
			checkResults(qexec, "example3:region1", "example1:point1");
		}
	}

	private static final String PROPER_PART_MULTIPLE_RESULTS_QUERY = ""
		+ "SELECT DISTINCT ?a\n"
		+ "WHERE {\n"
		+ "?polygon a gml:Polygon .\n"
		+ "?polygon gml:exterior ?ext .\n"
		+ "?ext a gml:LinearRing .\n"
		+ "?ext gml:posList \"34.8448761696609 33 34.8448761696609 35.9148048779863 34.8448761696609 37 40 37 40 33 34.8448761696609 33\" .\n"
		+ "?a rcc:properPart ?polygon .\n"
		+ "}";

	public void testProperPartMultipleResults() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		try (CloseableQueryExec qexec = performQuery(PROPER_PART_MULTIPLE_RESULTS_QUERY)) {
			checkResults(qexec, "example2:point3", "example2:point4", "example2:point5", "example3:region2");
		}
	}

	private static final String TANGENTIAL_PROPER_PART_REGION_QUERY = ""
		+ "SELECT DISTINCT ?a\n"
		+ "WHERE {\n"
		+ "?polygon a gml:Polygon .\n"
		+ "?polygon gml:exterior ?ext .\n"
		+ "?ext a gml:LinearRing .\n"
		+ "?ext gml:posList \"34.8448780 33 34.8448780 35.9148060 34.8448780 37 40 37 40 33 34.8448780 33\" .\n"
		+ "?a rcc:tangentialProperPart ?polygon .\n"
		+ "}";

	public void testTangentialProperPartRegion() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		try (CloseableQueryExec qexec = performQuery(TANGENTIAL_PROPER_PART_REGION_QUERY)) {
			// I deemed the second expected result to be invalid on the assumption
			// that a point cannot be a TPP of any polygon, since no point can touch
			// both the border of a polygon and the interior of a polygon simultaneously.
			checkResults(qexec, "example3:region2"/*, EXAMPLE2_NS + "point4"*/);
		}

		// TODO: Define ?a where "?a rcc:tangentialProperPart ?polygon ."
		// for the case in which ?a represents a gml:Point and not a gml:Polygon
	}

	private static final String A_QUERY_1 = ""
		+ "SELECT DISTINCT ?a\n"
		+ "WHERE {\n"
		+ "?a a example:SpatialThing .\n"
		+ "?a georss:where ?extent .\n"
		+ "?extent a gml:Polygon .\n"
		+ "?extent gml:exterior ?ext .\n"
		+ "?ext a gml:LinearRing .\n"
		+ "}";

	public void testQuery() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");

		try (CloseableQueryExec qexec = performQuery(A_QUERY_1)) {
			checkResults(qexec, "example3:campus1", "example3:campus2", "example3:campus3");
		}
	}

	private static final String BUILDING_QUERY = ""
		+ "SELECT DISTINCT  ?building1 ?building2 ?building3 ?building4\n"
		+ "WHERE {\n"
		+ "?circle   rdf:type              gml:Circle ;\n"
		+ "          gml:radius            \"50\"^^xsd:double .\n"
		+ "?building1\n"
		+ "          rdf:type              example:Building ;\n"
		+ "          georss:where          ?sreg1 .\n"
		+ "?building2\n"
		+ "          rdf:type              example:Building ;\n"
		+ "          georss:where          ?sreg2 .\n"
		+ "?building3\n"
		+ "          rdf:type              example:Building ;\n"
		+ "          georss:where          ?sreg3 .\n"
		+ "?building4\n"
		+ "          rdf:type              example:Building ;\n"
		+ "          georss:where          ?sreg4 .\n"
		+ "(?sreg1 ?sreg2 ?sreg3 ?sreg4) rcc:part ?circle .\n"
		//+ "?sreg1 rcc:part ?circle .\n"	FIXME: Something is wrong here
		//+ "?sreg2 rcc:part ?circle .\n"	These four lines return a different result than the line above.
		//+ "?sreg3 rcc:part ?circle .\n"	Also this test causes the ARQInternalErrorException
		//+ "?sreg4 rcc:part ?circle .\n"	with the message "Attempt to reassign ?building1 to ?building1" etc.
		+ "FILTER (( ?building1 != ?building2 )\n"
		+ " &&\n"
		+ "( ?building1 != ?building3 ) &&\n"
		+ "( ?building1 != ?building4 ) &&\n"
		+ "( ?building2 != ?building3 ) &&\n"
		+ "( ?building2 != ?building4 ) &&\n"
		+ "( ?building3 != ?building4 )\n"
		+ ")\n"
		+ "}";

	public void testBuildingQuery() {
		loadData("queries/BuildingQueryExample.ttl");
		try (CloseableQueryExec qexec = performQuery(BUILDING_QUERY)) {
			ResultSet rs = qexec.execSelect();
			assertTrue(rs.hasNext());

			int count = 0;
			while (rs.hasNext()) {
				rs.next();
				++count;
			}
			assertEquals(24, count);
		}
	}

	private static final String A_QUERY_2 = ""
		+ "SELECT DISTINCT ?a WHERE {\n"
		+ "?circle a            gml:Circle ;\n"
		+ "        gml:radius   \"10\"^^xsd:double ;\n"
		+ "        gml:pos      \"34.85 35.91\" .\n"
		+ "?a      a            example:SpatialThing ;\n"
		+ "        georss:where ?sreg1 .\n"
		+ "?sreg1  rcc:part     ?circle .\n"
		+ "}";

	public void testAQuery() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		try (CloseableQueryExec qexec = performQuery(A_QUERY_2)) {
			checkResults(qexec, "example2:building3", "example2:building4", "example2:building5", "example3:campus2");
		}
	}

	private static final String CIRCLE_QUERY = ""
		+ "SELECT DISTINCT  ?a\n"
		+ "WHERE {\n"
		+ "?circle a            gml:Circle ;\n"
		+ "        gml:radius   \"50\"^^xsd:double ;\n"
		+ "        gml:pos      \"35 36\" .\n"
		+ "?a      a            example:Building ;\n"
		+ "        georss:where ?sreg1 .\n"
		+ "?sreg1  rcc:part     ?circle .\n"
		+ "}";

	public void testQueryCircle() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		try (CloseableQueryExec qexec = performQuery(CIRCLE_QUERY)) {
			checkResults(qexec, "example2:building3", "example2:building4", "example2:building5");
		}
	}

	private static final String COVERED_CAMPUS_QUERY = ""
		+ "SELECT DISTINCT ?a ?c\n"
		+ "WHERE {\n"
		+ "?a a example:Building ;\n"
		+ "	georss:where ?buildingLoc .\n"
		+ "?c a example:Campus ;\n"
		+ " georss:where ?campusLoc .\n"
		+ "?campusLoc ogc:covers ?buildingLoc .\n"
		+ "}";

	public void testQueryCoveredCampus() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		try (CloseableQueryExec qexec = performQuery(COVERED_CAMPUS_QUERY)) {
			ResultSet rs = qexec.execSelect();

			Map<String, List<String>> values = new HashMap<>();
			values.put("example3:campus3", Arrays.asList("example3:campus3building1", "example3:campus3building2"));
			values.put("example3:campus2", Arrays.asList("example2:building3", "example2:building4", "example2:building5"));
			values.put("example3:campus1", Arrays.asList("example1:building1"));

			int count = 0;
			while (rs.hasNext()) {
				QuerySolution qs = rs.next();
				//printQuerySolution(qs);
				String building = qs.getResource("a").getURI();
				String campus = qs.getResource("c").getURI();

				if (!values.containsKey(campus)) {
					LOG.warn("Campus {} should not be returned", campus);
					continue;
				}
				List<String> buildings = values.get(campus);
				if (!buildings.contains(building)) {
					LOG.warn("Building {} for campus {} should not be returned", building, campus);
					continue;
				}
				buildings.remove(building);
				++count;
			}

			for (List<String> buildings : values.values()) {
				assertEquals(0, buildings.size(), String.format("Still have %1$s", buildings));
			}

			assertEquals(6, count);
		}
	}

	private static final String ONLY_PROPERTY_FUNCTION_QUERY = ""
		+ "SELECT DISTINCT ?a\n"
		+ "WHERE {\n"
		+ "cities:polyLondon ogc:covers ?a .\n"
		+ "}";

	public void testOnlyPropertyFunctionQuery() {
		loadData("queries/Cities.ttl");
		try (CloseableQueryExec qexec = performQuery(ONLY_PROPERTY_FUNCTION_QUERY)) {
			checkResults(qexec, "cities:pointLondon", "cities:polyLondon");
		}
	}

	private static final String ONLY_PROPERTY_FUNCTION_UNBOUND_QUERY = ""
		+ "SELECT DISTINCT ?a\n"
		+ "WHERE {\n"
		+ "?a ogc:covers ?b .\n"
		+ "FILTER (?a != ?b) .\n"
		+ "}";

	public void testOnlyPropertyFunctionQueryUnbound() {
		loadData("queries/Cities.ttl");
		try (CloseableQueryExec qexec = performQuery(ONLY_PROPERTY_FUNCTION_UNBOUND_QUERY)) {
			checkResults(qexec, "cities:polyLondon");
		}
	}

	private static final String ONLY_PROPERTY_FUNCTION_NON_INDEXED_URI_QUERY = ""
		+ "SELECT DISTINCT ?a\n"
		+ "WHERE {\n"
		+ "cities:polyNodnol ogc:covers ?a .\n"
		+ "}";

	public void testOnlyPropertyFunctionQueryNonIndexedURI() {
		loadData("queries/Cities.ttl");
		try (CloseableQueryExec qexec = performQuery(ONLY_PROPERTY_FUNCTION_NON_INDEXED_URI_QUERY)) {
			checkResults(qexec);
		}
	}

	private static final String SHARED_CONTEXT_QUERY = ""
		+ "SELECT DISTINCT *\n"
		+ "WHERE {\n"
		//+ "?a ?b ?c .\n"
		+ "?z a gml:Polygon .\n"
		+ "?z rdfs:label ?cover .\n"
		+ "?z ogc:covers ?a .\n"
		+ "?z ogc:covers ?b .\n"
		+ "FILTER (?a != ?b && ?z != ?a && ?z != ?b).\n"
		+ "}";

	public void testSharedContext() {
		//loadData("queries/Cities.ttl");
		loadData("queries/ExtraLondon.ttl");
		try (CloseableQueryExec qexec = performQuery(SHARED_CONTEXT_QUERY)) {
			checkResults(qexec, "cities:pointVictoriaMemorial", "cities:pointBuckinghamPalace");
		}
	}

	public void testFirstVarBound() {
		if (!(getIndex() instanceof PostgresIndex)) {
			return;
		}
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");

		List<Triple> triples = new ArrayList<>();

		//triples.add(Triple.create(NodeCreateUtils.create("?place"),
		//	NodeCreateUtils.create(StdConstants.GML_NS + "representativeExtent"),
		//	NodeCreateUtils.create("?placeExtent")));
		triples.add(Triple.create(NodeCreateUtils.create("?ext"),
			NodeCreateUtils.create(StdConstants.RCC_EXT_NS + "invProperPart"),
			NodeCreateUtils.create("?placeExtent")));
		triples.add(Triple.create(NodeCreateUtils.create("?reg"),
			NodeCreateUtils.create(RDF.type.getURI()),
			NodeCreateUtils.create(StdConstants.BUFFER_NS + "Buffer")));
		triples.add(Triple.create(NodeCreateUtils.create("?reg"),
			NodeCreateUtils.create(StdConstants.BUFFER_NS + "distance"),
			Node.createLiteral("\"580\"^^xsd:double")));
		triples.add(Triple.create(NodeCreateUtils.create("?reg"),
			NodeCreateUtils.create(StdConstants.BUFFER_NS + "extent"),
			NodeCreateUtils.create("?ext")));
		triples.add(Triple.create(NodeCreateUtils.create("?ext"),
			NodeCreateUtils.create(RDF.type.getURI()),
			NodeCreateUtils.create(StdConstants.GML_NS + "Point")));
		triples.add(Triple.create(NodeCreateUtils.create("?ext"),
			NodeCreateUtils.create(StdConstants.GML_NS + "pos"),
			Node.createLiteral("0 0")));

		String query = "SELECT ?placeExtent WHERE {\n";
		for (Triple t : triples) {
			query += t.getSubject() + " <" + t.getPredicate().getURI() + "> ";
			if (t.getObject().isLiteral()) {
				query += "\"" + t.getObject().getLiteralValue() + "\"";
				if (t.getObject().getLiteralDatatypeURI() != null) {
					query += "^^<" + t.getObject().getLiteralDatatypeURI() + ">";
				}
			} else if (t.getObject().isURI()) {
				query += "<" + t.getObject().getURI() + ">";
			} else {
				query += t.getObject();
			}
			query += " . \n";
		}
		query += "}";
		LOG.info(query);
		try (CloseableQueryExec qexec = performQuery(query)) {
			ResultSet rs = qexec.execSelect();
			int count = 0;
			while (rs.hasNext()) {
				++count;
				printQuerySolution(rs.next());
			}
			assertEquals(2, count);
		}
	}
}
