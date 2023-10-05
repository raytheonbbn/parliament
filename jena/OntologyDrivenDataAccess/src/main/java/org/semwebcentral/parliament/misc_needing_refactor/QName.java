package org.semwebcentral.parliament.misc_needing_refactor;

import org.apache.jena.rdf.model.Resource;

public class QName {
	public static String asQName(Resource uri) {
		return asQName(uri.getURI());
	}

	public static String asQName(String uri) {
		//TODO: Implement this for reals:
		return uri;
	}
}
