package com.bbn.parliament.kb_graph.index.spatial.geosparql.builtin;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.kb_graph.index.spatial.geosparql.datatypes.WKTLiteral;
import com.bbn.parliament.kb_graph.index.spatial.geosparql.vocabulary.Geo;

/** @author rbattle */
public class WKTBuiltin extends BaseBuiltin {
	private static final Logger LOG = LoggerFactory.getLogger(WKTBuiltin.class);

	public WKTBuiltin() {
		super();
	}

	@Override
	public String getName() {
		return "wkt";
	}

	@Override
	public int getArgLength() {
		return 3;
	}

	@Override
	public void headAction(Node[] args, int length, RuleContext context) {
		if (length > 3) {
			checkArgs(length, context);
		}

		Geometry g;
		try {
			Node geom = getArg(1, args, context);
			g = new WKTReader().read(geom.getLiteralValue().toString());
		} catch (ParseException e) {
			LOG.error("Parse error", e);
			return;
		}
		g.setUserData("EPSG:4326");
		WKTLiteral x = new WKTLiteral();
		String lex = x.unparse(g);

		context.add(Triple.create(getArg(0, args, context), Geo.Nodes.asWKT,
			ResourceFactory.createTypedLiteral(lex, x).asNode()));
	}
}
