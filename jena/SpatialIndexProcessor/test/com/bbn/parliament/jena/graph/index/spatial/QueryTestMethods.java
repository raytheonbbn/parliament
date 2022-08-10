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

	private static final String TANGENTIAL_PROPER_PART_POINTS_QUERY = """
		select distinct ?a where {
			?polygon a gml:Polygon ;
				gml:exterior ?ext .
			?ext a gml:LinearRing ;
				gml:posList "-0.5 -0.5 -0.5 6 0.5 6 0.5 -0.5 -0.5 -0.5" .
			?a rcc:tangentialProperPart ?polygon .
		}
		""";

	public void testTangentialProperPartPoints() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		try (CloseableQueryExec qexec = performQuery(TANGENTIAL_PROPER_PART_POINTS_QUERY)) {
			checkResults(qexec, "example3:region1");
		}
	}

	private static final String NON_TANGENTIAL_PROPER_PART_POINTS_QUERY = """
		select distinct ?a where {
			?polygon a gml:Polygon ;
				gml:exterior ?ext .
			?ext a gml:LinearRing ;
				gml:posList "34.8448761696609 33 34.8448761696609 35.9148048779863 34.8448761696609 37 40 37 40 33 34.8448761696609 33" .
			?a rcc:nonTangentialProperPart ?polygon .
		}
		""";

	public void testNonTangentialProperPartPoints() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");

		try (CloseableQueryExec qexec = performQuery(NON_TANGENTIAL_PROPER_PART_POINTS_QUERY)) {
			checkResults(qexec, "example2:point3", "example2:point4", "example2:point5", "example3:region2");
		}
	}

	private static final String NON_TANGENTIAL_PROPER_PART_MULTIPLE_QUERY = """
		select distinct ?a where {
			?polygon a gml:Polygon ;
				gml:exterior ?ext .
			?ext a gml:LinearRing ;
				gml:posList "-1 -1 -1 6 1 16 1 -1 -1 -1" .
			?a rcc:nonTangentialProperPart ?polygon .
		}
		""";

	public void testNonTangentialProperPartMultiple() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		try (CloseableQueryExec qexec = performQuery(NON_TANGENTIAL_PROPER_PART_MULTIPLE_QUERY)) {
			checkResults(qexec, "example3:region1", "example1:point1");
		}
	}

	private static final String PROPER_PART_MULTIPLE_RESULTS_QUERY = """
		select distinct ?a where {
			?polygon a gml:Polygon ;
				gml:exterior ?ext .
			?ext a gml:LinearRing ;
				gml:posList "34.8448761696609 33 34.8448761696609 35.9148048779863 34.8448761696609 37 40 37 40 33 34.8448761696609 33" .
			?a rcc:properPart ?polygon .
		}
		""";

	public void testProperPartMultipleResults() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		try (CloseableQueryExec qexec = performQuery(PROPER_PART_MULTIPLE_RESULTS_QUERY)) {
			checkResults(qexec, "example2:point3", "example2:point4", "example2:point5", "example3:region2");
		}
	}

	private static final String TANGENTIAL_PROPER_PART_REGION_QUERY = """
		select distinct ?a where {
			?polygon a gml:Polygon ;
				gml:exterior ?ext .
			?ext a gml:LinearRing ;
				gml:posList "34.8448780 33 34.8448780 35.9148060 34.8448780 37 40 37 40 33 34.8448780 33" .
			?a rcc:tangentialProperPart ?polygon .
		}
		""";

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

	private static final String A_QUERY_1 = """
		select distinct ?a where {
			?a a example:SpatialThing ;
				georss:where ?extent .
			?extent a gml:Polygon ;
				gml:exterior ?ext .
			?ext a gml:LinearRing .
		}
		""";

	public void testQuery() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");

		try (CloseableQueryExec qexec = performQuery(A_QUERY_1)) {
			checkResults(qexec, "example3:campus1", "example3:campus2", "example3:campus3");
		}
	}

	private static final String BUILDING_QUERY = """
		select distinct ?building1 ?building2 ?building3 ?building4 where {
			?circle a gml:Circle ;
				gml:radius "50"^^xsd:double .
			?building1 a example:Building ;
				georss:where ?sreg1 .
			?building2 a example:Building ;
				georss:where ?sreg2 .
			?building3 a example:Building ;
				georss:where ?sreg3 .
			?building4 a example:Building ;
				georss:where ?sreg4 .
			(?sreg1 ?sreg2 ?sreg3 ?sreg4) rcc:part ?circle .
			# ?sreg1 rcc:part ?circle .	# FIXME: Something is wrong here. These four lines
			# ?sreg2 rcc:part ?circle .	# return a different result than the line above. Also,
			# ?sreg3 rcc:part ?circle .	# this test causes the ARQInternalErrorException with
			# ?sreg4 rcc:part ?circle .	# message "Attempt to reassign ?building1 to ?building1".
			filter (( ?building1 != ?building2 )
				&& ( ?building1 != ?building3 )
				&& ( ?building1 != ?building4 )
				&& ( ?building2 != ?building3 )
				&& ( ?building2 != ?building4 )
				&& ( ?building3 != ?building4 ))
		}
		""";

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

	private static final String A_QUERY_2 = """
		select distinct ?a where {
			?circle a gml:Circle ;
				gml:radius "10"^^xsd:double ;
				gml:pos "34.85 35.91" .
			?a a example:SpatialThing ;
				georss:where ?sreg1 .
			?sreg1 rcc:part ?circle .
		}
		""";

	public void testAQuery() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		try (CloseableQueryExec qexec = performQuery(A_QUERY_2)) {
			checkResults(qexec, "example2:building3", "example2:building4", "example2:building5", "example3:campus2");
		}
	}

	private static final String CIRCLE_QUERY = """
		select distinct ?a where {
			?circle a gml:Circle ;
				gml:radius "50"^^xsd:double ;
				gml:pos "35 36" .
			?a a example:Building ;
				georss:where ?sreg1 .
			?sreg1 rcc:part ?circle .
		}
		""";

	public void testQueryCircle() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		try (CloseableQueryExec qexec = performQuery(CIRCLE_QUERY)) {
			checkResults(qexec, "example2:building3", "example2:building4", "example2:building5");
		}
	}

	private static final String COVERED_CAMPUS_QUERY = """
		select distinct ?a ?c where {
			?a a example:Building ;
				georss:where ?buildingLoc .
			?c a example:Campus ;
				georss:where ?campusLoc .
			?campusLoc ogc:covers ?buildingLoc .
		}
		""";

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
				assertEquals(0, buildings.size(), "Still have %1$s".formatted(buildings));
			}

			assertEquals(6, count);
		}
	}

	private static final String ONLY_PROPERTY_FUNCTION_QUERY = """
		select distinct ?a where {
			cities:polyLondon ogc:covers ?a .
		}
		""";

	public void testOnlyPropertyFunctionQuery() {
		loadData("queries/Cities.ttl");
		try (CloseableQueryExec qexec = performQuery(ONLY_PROPERTY_FUNCTION_QUERY)) {
			checkResults(qexec, "cities:pointLondon", "cities:polyLondon");
		}
	}

	private static final String ONLY_PROPERTY_FUNCTION_UNBOUND_QUERY = """
		select distinct ?a where {
			?a ogc:covers ?b .
			filter (?a != ?b) .
		}
		""";

	public void testOnlyPropertyFunctionQueryUnbound() {
		loadData("queries/Cities.ttl");
		try (CloseableQueryExec qexec = performQuery(ONLY_PROPERTY_FUNCTION_UNBOUND_QUERY)) {
			checkResults(qexec, "cities:polyLondon");
		}
	}

	private static final String ONLY_PROPERTY_FUNCTION_NON_INDEXED_URI_QUERY = """
		select distinct ?a where {
			cities:polyNodnol ogc:covers ?a .
		}
		""";

	public void testOnlyPropertyFunctionQueryNonIndexedURI() {
		loadData("queries/Cities.ttl");
		try (CloseableQueryExec qexec = performQuery(ONLY_PROPERTY_FUNCTION_NON_INDEXED_URI_QUERY)) {
			checkResults(qexec);
		}
	}

	private static final String SHARED_CONTEXT_QUERY = """
		select distinct * where {
			?z a gml:Polygon ;
				rdfs:label ?cover ;
				ogc:covers ?a ;
				ogc:covers ?b .
			filter (?a != ?b && ?z != ?a && ?z != ?b).
		}
		""";

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
		triples.add(Triple.create(
			NodeCreateUtils.create("?ext"),
			NodeCreateUtils.create(StdConstants.RCC_EXT_NS + "invProperPart"),
			NodeCreateUtils.create("?placeExtent")));
		triples.add(Triple.create(
			NodeCreateUtils.create("?reg"),
			NodeCreateUtils.create(RDF.type.getURI()),
			NodeCreateUtils.create(StdConstants.BUFFER_NS + "Buffer")));
		triples.add(Triple.create(
			NodeCreateUtils.create("?reg"),
			NodeCreateUtils.create(StdConstants.BUFFER_NS + "distance"),
			Node.createLiteral("\"580\"^^xsd:double")));
		triples.add(Triple.create(
			NodeCreateUtils.create("?reg"),
			NodeCreateUtils.create(StdConstants.BUFFER_NS + "extent"),
			NodeCreateUtils.create("?ext")));
		triples.add(Triple.create(
			NodeCreateUtils.create("?ext"),
			NodeCreateUtils.create(RDF.type.getURI()),
			NodeCreateUtils.create(StdConstants.GML_NS + "Point")));
		triples.add(Triple.create(
			NodeCreateUtils.create("?ext"),
			NodeCreateUtils.create(StdConstants.GML_NS + "pos"),
			Node.createLiteral("0 0")));

		String query = "select ?placeExtent where {\n";
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
