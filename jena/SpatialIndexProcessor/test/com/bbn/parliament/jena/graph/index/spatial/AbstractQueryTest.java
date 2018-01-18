// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.bbn.parliament.jena.from_jena_test.NodeCreateUtils;
import com.bbn.parliament.jena.graph.index.spatial.sql.postgres.PostgresIndex;
import com.bbn.parliament.jena.graph.index.spatial.standard.Constants;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.vocabulary.RDF;

/** @author Robert Battle */
public abstract class AbstractQueryTest extends AbstractSpatialTest {
	@Test
	public void testTangentialProperPartPoints() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		ResultSet rs = performQuery(PREFIXES + "SELECT DISTINCT ?a "
			+ "WHERE { "
			+ "?polygon a gml:Polygon . "
			+ "?polygon gml:exterior ?ext . "
			+ "?ext a gml:LinearRing . "
			+ "?ext gml:posList \"-0.5 -0.5 -0.5 6 0.5 6 0.5 -0.5 -0.5 -0.5\" . "
			+ "?a rcc:tangentialProperPart ?polygon . "
			+ "}");
		checkResults(rs, EXAMPLE3_NS + "region1");
	}

	@Test
	public void testNonTangentialProperPartPoints() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");

		ResultSet rs = performQuery(PREFIXES + "SELECT DISTINCT ?a "
			+ "WHERE { "
			+ "?polygon a gml:Polygon . "
			+ "?polygon gml:exterior ?ext . "
			+ "?ext a gml:LinearRing . "
			+ "?ext gml:posList \"34.8448761696609 33 34.8448761696609 35.9148048779863 34.8448761696609 37 40 37 40 33 34.8448761696609 33\" . "
			+ "?a rcc:nonTangentialProperPart ?polygon . "
			+ "}");

		checkResults(rs, EXAMPLE2_NS + "point3", EXAMPLE2_NS + "point4", EXAMPLE2_NS + "point5", EXAMPLE3_NS + "region2" );
	}

	@Test
	public void testNonTangentialProperPartMultiple() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		ResultSet rs = performQuery(PREFIXES + "SELECT DISTINCT ?a "
			+ "WHERE { "
			+ "?polygon a gml:Polygon . "
			+ "?polygon gml:exterior ?ext . "
			+ "?ext a gml:LinearRing . "
			+ "?ext gml:posList \"-1 -1 -1 6 1 16 1 -1 -1 -1\" . "
			+ "?a rcc:nonTangentialProperPart ?polygon . "
			+ "}");

		checkResults(rs, EXAMPLE3_NS + "region1", EXAMPLE1_NS + "point1");
	}

	@Test
	public void testProperPartMultipleResults() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		ResultSet rs = performQuery(PREFIXES + "SELECT DISTINCT ?a "
			+ "WHERE { "
			+ "?polygon a gml:Polygon . "
			+ "?polygon gml:exterior ?ext . "
			+ "?ext a gml:LinearRing . "
			+ "?ext gml:posList \"34.8448761696609 33 34.8448761696609 35.9148048779863 34.8448761696609 37 40 37 40 33 34.8448761696609 33\" . "
			+ "?a rcc:properPart ?polygon . "
			+ "}");

		checkResults(rs, EXAMPLE2_NS + "point3", EXAMPLE2_NS + "point4", EXAMPLE2_NS + "point5", EXAMPLE3_NS + "region2");
	}

	@Test
	public void testTangentialProperPartRegion() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		ResultSet rs = performQuery(PREFIXES
			+ "SELECT DISTINCT ?a "
			+ "WHERE { "
			+ "?polygon a gml:Polygon . "
			+ "?polygon gml:exterior ?ext . "
			+ "?ext a gml:LinearRing . "
			+ "?ext gml:posList \"34.8448780 33 34.8448780 35.9148060 34.8448780 37 40 37 40 33 34.8448780 33\" . "
			+ "?a rcc:tangentialProperPart ?polygon . "
			+ "}");

		// I deemed the second expected result to be invalid on the assumption
		// that a point cannot be a TPP of any polygon, since no point can touch
		// both the border of a polygon and the interior of a polygon simultaneously.
		checkResults(rs, EXAMPLE3_NS + "region2"/*, EXAMPLE2_NS + "point4"*/);

		// TODO define ?a where "?a rcc:tangentialProperPart ?polygon . "
		// for the case in which ?a represents a gml:Point and not a gml:Polygon
	}

	@Test
	public void testQuery() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");

		ResultSet rs = performQuery(PREFIXES
			+ "SELECT DISTINCT ?a  "
			+ "WHERE { "
			+ "?a a example:SpatialThing . "
			+ "?a georss:where ?extent . "
			+ "?extent a gml:Polygon . "
			+ "?extent gml:exterior ?ext . "
			+ "?ext a gml:LinearRing . "
			+ "}");

		checkResults(rs, EXAMPLE3_NS + "campus1", EXAMPLE3_NS + "campus2", EXAMPLE3_NS + "campus3");
	}

	@Test
	public void testBuildingQuery() {
		loadData("queries/BuildingQueryExample.ttl");
		ResultSet rs = performQuery(PREFIXES
			+ "SELECT DISTINCT  ?building1 ?building2 ?building3 ?building4 "
			+ "WHERE {"
			+ "?circle   rdf:type              gml:Circle ;"
			+ "          gml:radius            \"50\"^^xsd:double . "
			+ "?building1 "
			+ "          rdf:type              example:Building ;"
			+ "          georss:where          ?sreg1 ."

			+ "?building2 "
			+ "          rdf:type              example:Building;"
			+ "          georss:where          ?sreg2 ."
			+ "?building3 "
			+ "          rdf:type              example:Building ;"
			+ "          georss:where          ?sreg3 ."
			+ "?building4 "
			+ "          rdf:type              example:Building ;"
			+ "          georss:where          ?sreg4 ."
			+ "(?sreg1 ?sreg2 ?sreg3 ?sreg4) rcc:part ?circle ."
			//+ "?sreg1 rcc:part ?circle ."	These four lines return a different result than the above line.
			//+ "?sreg2 rcc:part ?circle ."	FIXME Something is wrong here
			//+ "?sreg3 rcc:part ?circle ."	Also this test causes the ARQInternalErrorException
			//+ "?sreg4 rcc:part ?circle ."	with the message "Attempt to reassign ?building1 to ?building1" etc.
			+ "FILTER (( ?building1 != ?building2 )"
			+ " && "
			+ "( ?building1 != ?building3 ) && "
			+ "( ?building1 != ?building4 ) && "
			+ "( ?building2 != ?building3 ) && "
			+ "( ?building2 != ?building4 ) && "
			+ "( ?building3 != ?building4 ) "
			+ ")"
			+ "}");

		assertTrue(rs.hasNext());
		int count = 0;
		while (rs.hasNext()) {
			rs.nextSolution();
			count++;
		}
		assertEquals(24, count);
	}

	@Test
	public void testAQuery() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		ResultSet rs = performQuery(PREFIXES
			+ "SELECT DISTINCT  ?a "
			+ "WHERE {"
			+ "?circle   rdf:type              gml:Circle ;"
			+ "          gml:radius       \"10\"^^xsd:double ; "
			+ "          gml:pos          \"34.85 35.91\" . "
			+ "?a "
			+ "          rdf:type              example:SpatialThing ;"
			+ "          georss:where          ?sreg1 ."
			+ "?sreg1      rcc:part              ?circle ."
			+ "}");

		checkResults(rs, EXAMPLE2_NS + "building3", EXAMPLE2_NS + "building4", EXAMPLE2_NS + "building5", EXAMPLE3_NS + "campus2");
	}

	@Test
	public void testQueryCircle() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		ResultSet rs = performQuery(PREFIXES
			+ "SELECT DISTINCT  ?a "
			+ "WHERE {"
			+ "?circle   rdf:type              gml:Circle ;"
			+ "          gml:radius       \"50\"^^xsd:double ; "
			+ "          gml:pos          \"35 36\" . "
			+ "?a "
			+ "          rdf:type              example:Building ;"
			+ "          georss:where          ?sreg1 ."
			+ "?sreg1      rcc:part              ?circle ."
			+ "}");

		checkResults(rs, EXAMPLE2_NS + "building3", EXAMPLE2_NS + "building4", EXAMPLE2_NS + "building5");
	}

	@Test
	public void testQueryCoveredCampus() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		ResultSet rs = performQuery(PREFIXES
			+ "SELECT DISTINCT ?a ?c "
			+ "WHERE {"
			+ "?a a example:Building ; "
			+ "	georss:where ?buildingLoc . "
			+ "?c a example:Campus ; "
			+ " georss:where ?campusLoc . "
			+ "?campusLoc ogc:covers ?buildingLoc ."
			+ "}");


		Map<String, List<String>> values = new HashMap<>();
		values.put(EXAMPLE3_NS + "campus3", Arrays.asList(new String[] {  EXAMPLE3_NS + "campus3building1", EXAMPLE3_NS + "campus3building2" }));
		values.put(EXAMPLE3_NS + "campus2", Arrays.asList(new String[] {  EXAMPLE2_NS + "building3", EXAMPLE2_NS + "building4", EXAMPLE2_NS + "building5" }));
		values.put(EXAMPLE3_NS + "campus1", Arrays.asList(new String[] {  EXAMPLE1_NS + "building1" }));

		int count = 0;
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
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
			count++;
		}

		for (List<String> buildings : values.values()) {
			if (buildings.size() > 0) {
				LOG.warn("Still have {}", buildings);
			}
			assertEquals(0, buildings.size());
		}

		assertEquals(6, count);
	}

	@Test
	public void testOnlyPropertyFunctionQuery() {
		loadData("queries/Cities.ttl");
		ResultSet rs = performQuery(PREFIXES + "SELECT DISTINCT ?a "
			+ "WHERE { "
			+ "<http://parliament.semwebcentral.org/spatial/examples/cities#polyLondon> ogc:covers ?a ."
			+ "}");
		checkResults(rs, EXAMPLE_CITIES_NS + "pointLondon", EXAMPLE_CITIES_NS + "polyLondon");
	}

	@Test
	public void testOnlyPropertyFunctionQueryUnbound() {
		loadData("queries/Cities.ttl");
		ResultSet rs = performQuery(PREFIXES + "SELECT DISTINCT ?a "
			+ "WHERE { "
			+ "?a ogc:covers ?b . "
			+ "FILTER (?a != ?b) . "
			+ "}");
		checkResults(rs, EXAMPLE_CITIES_NS + "polyLondon");
	}

	@Test
	public void testOnlyPropertyFunctionQueryNonIndexedURI() {
		loadData("queries/Cities.ttl");
		ResultSet rs = performQuery(PREFIXES + "SELECT DISTINCT ?a "
			+ "WHERE { "
			+ "<http://parliament.semwebcentral.org/spatial/examples/cities#polyNodnol> ogc:covers ?a ."
			+ "}");
		checkResults(rs);
	}

	@Test
	public void testSharedContext() {
		//loadData("queries/Cities.ttl");
		loadData("queries/ExtraLondon.ttl");
		ResultSet rs = performQuery(PREFIXES + "SELECT DISTINCT * "
			+ "WHERE {"
			//+ "?a ?b ?c .  "
			+ "?z a gml:Polygon . "
			+ "?z rdfs:label ?cover ."
			+ "?z ogc:covers ?a . "
			+ "?z ogc:covers ?b ."
			+ "FILTER (?a != ?b && ?z != ?a && ?z != ?b)."
			+ "}");
		checkResults(rs, EXAMPLE_CITIES_NS + "pointVictoriaMemorial", EXAMPLE_CITIES_NS + "pointBuckinghamPalace");
	}

	@Ignore
	@Test
	public void testFirstVarBound() {
		if (!(index instanceof PostgresIndex)) {
			return;
		}
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");

		List<Triple> triples = new ArrayList<>();

		//triples.add(Triple.create(NodeCreateUtils.create("?place"),
		//	NodeCreateUtils.create(Constants.GML_NS + "representativeExtent"),
		//	NodeCreateUtils.create("?placeExtent")));
		triples.add(Triple.create(NodeCreateUtils.create("?ext"),
			NodeCreateUtils.create(Constants.RCC_NS + "invProperPart"),
			NodeCreateUtils.create("?placeExtent")));
		triples.add(Triple.create(NodeCreateUtils.create("?reg"),
			NodeCreateUtils.create(RDF.type.getURI()),
			NodeCreateUtils.create(Constants.BUFFER_NS + "Buffer")));
		triples.add(Triple.create(NodeCreateUtils.create("?reg"),
			NodeCreateUtils.create(Constants.BUFFER_NS + "distance"),
			Node.createLiteral("\"580\"^^xsd:double")));
		triples.add(Triple.create(NodeCreateUtils.create("?reg"),
			NodeCreateUtils.create(Constants.BUFFER_NS + "extent"),
			NodeCreateUtils.create("?ext")));
		triples.add(Triple.create(NodeCreateUtils.create("?ext"),
			NodeCreateUtils.create(RDF.type.getURI()),
			NodeCreateUtils.create(Constants.GML_NS + "Point")));
		triples.add(Triple.create(NodeCreateUtils.create("?ext"),
			NodeCreateUtils.create(Constants.GML_NS + "pos"),
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
		ResultSet rs = performQuery(query);
		int count = 0;
		while (rs.hasNext()) {
			count++;
			printQuerySolution(rs.nextSolution());
		}
		assertEquals(2, count);
	}
}
