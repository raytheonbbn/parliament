package com.bbn.parliament.kb_graph.index.spatial.geosparql.datatypes;

public interface LiteralTestCase {
	void testDefaultCRS();
	void testInvalidCRS();
	void testInvalidFormat();
	void testValidCRS();
	void testParseUnparse();
}
