package com.bbn.parliament.jena.graph.index.spatial.geosparql;

import java.util.Properties;

import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapStd;
import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.graph.index.spatial.GeometryRecordFactory;
import com.bbn.parliament.jena.graph.index.spatial.IterableFunctionFactory;
import com.bbn.parliament.jena.graph.index.spatial.IterablePropertyFunctionFactory;
import com.bbn.parliament.jena.graph.index.spatial.OperationFactory;
import com.bbn.parliament.jena.graph.index.spatial.Profile;
import com.bbn.parliament.jena.graph.index.spatial.SpatialPropertyFunctionFactory;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.GeoSPARQLFunctionFactory;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.GML;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.Geo;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.Geof;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.UOM;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.WKT;
import com.bbn.parliament.kb_graph.query.index.operand.OperandFactory;

public class GeoSPARQLProfile implements Profile {
	@SuppressWarnings("unused")
	private Properties props;
	private GeometryRecordFactory recordFactory;
	private OperationFactory operationFactory;
	private SpatialPropertyFunctionFactory pfuncFactory;

	public GeoSPARQLProfile(Properties props) {
		this.props = props;
		this.recordFactory = new GeoSPARQLRecordFactory();
		this.operationFactory = new GeoSPARQLOperationFactory();
		this.pfuncFactory = new SpatialPropertyFunctionFactory(operationFactory, GeoSPARQLOperandFactory.class);
	}

	/** {@inheritDoc} */
	@Override
	public GeometryRecordFactory getRecordFactory() {
		return recordFactory;
	}

	/** {@inheritDoc} */
	@Override
	public IterableFunctionFactory getFunctionFactory() {
		return new GeoSPARQLFunctionFactory();
	}

	/** {@inheritDoc} */
	@Override
	public PrefixMap getPrefixes() {
		PrefixMap prefixes = new PrefixMapStd();

		prefixes.add("geo", Geo.uri);
		prefixes.add("geof", Geof.uri);
		prefixes.add("uom", UOM.uri);
		prefixes.add("sf", WKT.DATATYPE_URI);
		prefixes.add("gml", GML.DATATYPE_URI);

		return prefixes;
	}

	/** {@inheritDoc} */
	@Override
	public OperationFactory getOperationFactory() {
		return operationFactory;
	}

	/** {@inheritDoc} */
	@Override
	public IterablePropertyFunctionFactory getPropertyFunctionFactory() {
		return pfuncFactory;
	}

	/** {@inheritDoc} */
	@Override
	public Class<? extends OperandFactory<Geometry>> getOperandFactoryClass() {
		return GeoSPARQLOperandFactory.class;
	}
}
