package com.bbn.parliament.jena.graph.index.temporal.pt;

import javax.xml.datatype.XMLGregorianCalendar;

import com.bbn.parliament.jena.graph.index.temporal.Constants;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.hp.hpl.jena.datatypes.DatatypeFormatException;

/** @deprecated
 * The implementation of the Parliament Time instant data type.
 *
 * @author mhale
 */
@Deprecated
public class PTInstant extends PTDatatype {
	protected PTInstant(String uri) {
		super(uri);
	}

	/** {@inheritDoc} */
	@Override
	public TemporalInstant parse(String lexicalForm) throws DatatypeFormatException {
		String strippedForm = lexicalForm.strip();
		try {
			XMLGregorianCalendar xgc = Constants.XML_DT_FACTORY.newXMLGregorianCalendar(strippedForm);
			return new TemporalInstant(xgc.toGregorianCalendar());
		} catch (IllegalArgumentException e) {
			throw new DatatypeFormatException(strippedForm, this, "Invalid PTInstant");
		}
	}
}
