package com.bbn.parliament.jena.graph.index.temporal.pt;

import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.DatatypeFormatException;

/**
 * Base literal type for those within the Parliament Time namespace.
 *
 * @author mhale
 */
public abstract class PTDatatype extends BaseDatatype {
	protected PTDatatype(String uri) {
		super(uri);
	}

	/** {@inheritDoc} */
	@Override
	public abstract TemporalExtent parse(String lexicalForm) throws DatatypeFormatException;
}
