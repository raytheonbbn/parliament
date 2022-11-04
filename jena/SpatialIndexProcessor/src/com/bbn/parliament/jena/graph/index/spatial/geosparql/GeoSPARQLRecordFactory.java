package com.bbn.parliament.jena.graph.index.spatial.geosparql;

import java.util.List;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.xsd.impl.XSDBaseStringType;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.graph.index.spatial.GeometryRecord;
import com.bbn.parliament.jena.graph.index.spatial.GeometryRecordFactory;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GMLLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.WKTLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.Geo;

/** @author rbattle */
public class GeoSPARQLRecordFactory implements GeometryRecordFactory {

	private static final List<Triple> MATCHES = List.of(
		Triple.create(Node.ANY, Geo.Nodes.asWKT, Node.ANY),
		Triple.create(Node.ANY, Geo.Nodes.asGML, Node.ANY));

	public GeoSPARQLRecordFactory() {
		TypeMapper.getInstance().registerDatatype(new WKTLiteral());
		TypeMapper.getInstance().registerDatatype(new GMLLiteral());
		TypeMapper.getInstance().registerDatatype(new WKTLiteral.WKTLiteralSFNamespace());
		TypeMapper.getInstance().registerDatatype(new GMLLiteral.GMLLiteralGMLNamespace());
	}

	/** {@inheritDoc} */
	@Override
	public GeometryRecord createRecord(Triple triple) {
		Node object = triple.getObject();
		if (!object.isLiteral()) {
			return null;
		}
		Node predicate = triple.getPredicate();
		if (!predicate.isURI()) {
			return null;
		}

		Node subject = triple.getSubject();
		RDFDatatype objectType = object.getLiteralDatatype();
		if (Geo.Nodes.asWKT.equals(predicate)) {
			if (objectType instanceof WKTLiteral) {
				Geometry geo = (Geometry)object.getLiteralValue();
				if (!geo.isValid()) {
					return null;
				}
				return GeometryRecord.create(subject, geo);
			} else if (objectType instanceof XSDBaseStringType || null == objectType) {
				WKTLiteral lit = new WKTLiteral();
				Geometry geo = lit.parse(object.getLiteralValue().toString());
				if (!geo.isValid()) {
					return null;
				}
				return GeometryRecord.create(subject, geo);
			}
		} else if (Geo.Nodes.asGML.equals(predicate)) {
			if (objectType instanceof GMLLiteral) {
				Geometry geo = (Geometry)object.getLiteralValue();
				if (!geo.isValid()) {
					return null;
				}
				return GeometryRecord.create(subject, geo);
			}
		}
		return null;
	}

	@Override
	public List<Triple> getTripleMatchers() {
		return MATCHES;
	}
}
