package com.bbn.parliament.kb_graph.index.spatial.geosparql;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.locationtech.jts.geom.Point;

import com.bbn.parliament.kb_graph.index.spatial.geosparql.datatypes.WKTLiteral;
import com.bbn.parliament.kb_graph.index.spatial.geosparql.vocabulary.Geo;

public class DataGenerator {
	private static final WKTLiteral WKT = new WKTLiteral();
	private static final String RES_NS = "http://demo1.example.org/resource/%d";
	private static final String GEOM_NS = "http://demo1.example.org/geometry/%d";

	public static void main(String[] args) {
		//Point center = (Point)WKT.parse("POINT(-77.072675 38.893755)");
		Point center = (Point)WKT.parse("POINT(-83.4 34.3)");
		Model m = ModelFactory.createDefaultModel();

		List<Statement> triples = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			Point p = createPoint(center, 0.2);
			Node r = NodeFactory.createURI(RES_NS.formatted(i));
			Node g = NodeFactory.createURI(GEOM_NS.formatted(i));
			triples.add(m.asStatement(Triple.create(r, Geo.Nodes.hasGeometry, g)));
			triples.add(m.asStatement(Triple.create(g, Geo.Nodes.asWKT,
				NodeFactory.createLiteral(WKT.unparse(p), null, WKT))));
		}

		m.add(triples);
		m.write(System.out, "N-TRIPLES");
	}

	public static Point createPoint(Point center, double maxDistance) {
		double x = center.getX();
		double y = center.getY();

		double distance = Math.random() * maxDistance;
		if (Math.random() < 0.5) {
			distance *= -1;
		}
		x += distance;
		y += distance;

		return (Point)WKT.parse("POINT(" + y + " " + x + ")");
	}
}
