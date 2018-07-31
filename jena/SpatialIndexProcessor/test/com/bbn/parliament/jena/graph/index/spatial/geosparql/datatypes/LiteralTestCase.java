package com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes;

public interface LiteralTestCase {
	void testDefaultCRS();
	void testInvalidCRS();
	void testInvalidFormat();
	void testValidCRS();
	void testParseUnparse();
}
