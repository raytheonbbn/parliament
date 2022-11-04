package com.bbn.parliament.jena.graph.index.temporal.pt;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.DatatypeFormatException;

import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;

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
