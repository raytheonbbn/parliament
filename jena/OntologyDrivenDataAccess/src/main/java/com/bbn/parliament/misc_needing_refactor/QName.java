package com.bbn.parliament.misc_needing_refactor;

import com.hp.hpl.jena.rdf.model.Resource;

public class QName {
	public static String asQName(Resource uri) {
		return asQName(uri.getURI());
	}

	public static String asQName(String uri) {
		//TODO: Implement this for reals:
		return uri;
	}
}
