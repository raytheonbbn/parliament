package com.bbn.parliament.kb_graph.index.temporal.pt;

import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.kb_graph.index.temporal.Constants;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalInterval;

/**
 * Implementation for the Parliament Time interval data type
 * (pt:intervalLiteral). Uses regex to tokenize literals of the data type
 * pt:intervalLiteral in query expressions. Each token is a lexical
 * representation which in turn is parsed using a {@link DatatypeFactory}
 * instance, to make a TemporalInterval representative of the literal from the
 * query expression.
 *
 * @author mhale
 */
public class PTInterval extends PTDatatype {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(PTInterval.class);

	private static final String PLUS = "+";
	private static final String MINUS = "-";

	/**
	 * Denotes an absolute duration, i.e., that the duration will be used in
	 * conjunction with a DateTime value in order to create an interval whose
	 * endpoints are equidistant from the original DateTime value, which is in
	 * turn located at the center of the created interval.
	 */
	private static final String PLUSMINUS = "+-";

	/** Represents number of tokens that a pt:intervalLiteral must contain. */
	private static final int DIMENSION = 2;

	/** Character which delimits tokens within all literals of type pt:Timeinterval. */
	private static final String DELIMITER = ",";

	protected PTInterval(String uri) {
		super(uri);
	}

	/** {@inheritDoc}
	 * <p> Parser method for literals with data type <code>pt:intervalLiteral</code>
	 */
	@Override
	public TemporalInterval parse(String lexicalForm)
		throws DatatypeFormatException {
		String[] items = lexicalForm.split(Pattern.quote(DELIMITER), -1);
		if (items.length != DIMENSION) {
			throw new DatatypeFormatException(
				"Invalid interval syntax \"%2$s\" -- must have exactly %1$d comma(s)"
				.formatted(DIMENSION - 1, lexicalForm));
		}
		//Process each member
		for (int i = 0; i < DIMENSION; i++) {
			items[i] = items[i].strip();
		}

		XMLGregorianCalendar start;
		XMLGregorianCalendar end;

		try {
			if ((start = tryParse(items[0])) != null) {
				//Left hand lexical expression defined
				if ((end = tryParseWithDuration(start, items[1])) != null) {
					//Both lexical expressions defined
					GregorianCalendar gcstart = start.toGregorianCalendar();
					GregorianCalendar gcend = end.toGregorianCalendar();
					return new TemporalInterval(new TemporalInstant(gcstart, null, true),
						new TemporalInstant(gcend, null, false));
				}
				//Only left hand lexical expression defined
				return new TemporalInterval(new TemporalInstant(start.toGregorianCalendar()), null);
			}
			else {
				//Left hand lexcial expression not defined
				if ((end = tryParse(items[1])) != null) {
					//Only right hand lexical expression is defined
					GregorianCalendar gcend = end.toGregorianCalendar();
					return new TemporalInterval(null, new TemporalInstant(gcend));
				}
				//Neither lexical expressions are defined (empty (",") or error)
				return new TemporalInterval(null, null);
			}
		} catch (IllegalArgumentException e) {
			throw new DatatypeFormatException("Failed to parse interval");
		}
	}

	/**
	 * Parses a string into an instance of {@link XMLGregorianCalendar}.
	 *
	 * @param lexical The lexical representation.
	 * @return XMLGregorianCalendar parsed from lexical, or null empty.
	 * @throws DatatypeFormatException If the string cannot be parsed.
	 */
	private static XMLGregorianCalendar tryParse(String lexical) {
		if (lexical == null || lexical.isEmpty()) {
			return null;
		}
		try {
			return Constants.XML_DT_FACTORY.newXMLGregorianCalendar(lexical);
		}
		catch (IllegalArgumentException e) {
			throw new DatatypeFormatException("Invalid DateTime (%1$s) - check syntax"
				.formatted(lexical));
		}
	}

	/**
	 * Parses the lexical input into a {@link Duration} and returns the endpoint
	 * relative to firstref. The lexical expression must begin with +, -, or +-
	 * in order for it to be parsed as a duration. If the expression does not
	 * start with a sign determining its direction, it will be parsed as an
	 * {@link XMLGregorianCalendar} instead. Note that this method has an
	 * intended side effect: it may rewind the DateTime object referred to by
	 * firstref in order to produce intervals from negative durations.
	 *
	 * @param firstref A reference to the former DateTime endpoint, as an
	 *        XMLGregorianCalendar.
	 * @param lexical The second lexical representation to parse.
	 * @return The latter DateTime endpoint, as an XMLGregorianCalendar.
	 * @see {@link #tryParse(String lexical)}
	 */
	private static XMLGregorianCalendar tryParseWithDuration(XMLGregorianCalendar firstref, String lexical) {
		boolean isPlusMinus = false;
		Duration d;

		if (lexical == null || lexical.isEmpty()) {
			return null;
		}
		if (lexical.startsWith(PLUSMINUS)) {
			lexical = lexical.substring(PLUSMINUS.length()).strip();
			isPlusMinus = true;
		} else if (lexical.startsWith(PLUS)) {
			lexical = lexical.substring(PLUS.length()).strip();
		} else if (!lexical.startsWith(MINUS)) {
			return tryParse(lexical);
		}
		try {
			d = Constants.XML_DT_FACTORY.newDuration(lexical);
		} catch (IllegalArgumentException e) {
			throw new DatatypeFormatException(
				"'%1$s' is not a properly formatted XSDDuration literal".formatted(lexical));
		}
		XMLGregorianCalendar secondref = (XMLGregorianCalendar) firstref.clone();

		//Determine which endpoint to extend (firstref goes backward, secondref goes forward)
		//This maintains chronology in the interval

		if (d.getSign() != 0) {
			XMLGregorianCalendar pointer = (d.getSign() == 1) ? secondref : firstref;
			pointer.add(d);
			if (isPlusMinus) {
				//Extend the opposite endpoint as well
				XMLGregorianCalendar otherptr = (d.getSign() == 1) ? firstref : secondref;
				otherptr.add(d.negate());
			}
		} else {
			throw new DatatypeFormatException(
				"Duration '%1$s' must be of length greater than 0".formatted(lexical));
		}
		return secondref;
	}
}
