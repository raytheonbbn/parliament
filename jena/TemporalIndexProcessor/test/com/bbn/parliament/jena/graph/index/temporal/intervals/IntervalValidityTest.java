package com.bbn.parliament.jena.graph.index.temporal.intervals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bbn.parliament.jena.graph.index.temporal.pt.TemporalIndexField;
import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.impl.JenaParameters;

public class IntervalValidityTest {
	@SuppressWarnings("static-method")
	@BeforeEach
	public void setUp() {
		JenaParameters.enableEagerLiteralValidation = true;
	}

	@SuppressWarnings("static-method")
	@Test
	public void testBasicCases() {
		testValidity("2015-07-24T09:43:00, 2015-07-24T09:43:01", true);
		testValidity("2015-07-24T09:43:00, 2015-07-24T09:43:60", true);
		testValidity("2015-07-24T09:43:00Z, 2015-07-24T09:44:01Z", true);
		testValidity("2015-07-24T09:43:16, 2015-07-24T10:42:15", true);
		testValidity("2015-07-24T09:43:00, 2016-06-25T10:44:01", true);
		testValidity("2015-12-31T23:59:59, 2016-01-01T00:00:00", true);
		testValidity("2015-02-28T09:43:00, 2015-03-01T09:43:00", true);
		testValidity("2016-02-28T09:43:00, 2016-02-29T09:43:00", true);
		testValidity("2100-02-28T09:43:00, 2100-03-01T09:43:00", true);
		testValidity("2000-02-28T09:43:00, 2000-02-29T09:43:00", true);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testTimeZoneCases() {
		testValidity("2015-07-24T09:43:00+00:00, 2015-07-24T09:43:01-00:00", true);
		testValidity("2015-07-24T09:43:00+01:00, 2015-07-24T09:43:00-01:00", true);
		testValidity("2015-07-24T10:43:00+02:00, 2015-07-24T08:43:01-02:00", true);
		testValidity("2015-07-01T00:00:00+02:00, 2015-06-30T23:00:00Z", true);
		testValidity("1969-12-31T23:00:00-01:00, 1970-01-01T00:00:01Z", true);
		testValidity("2015-07-15T19:43:01+14:00, 2015-07-15T05:43:02Z", true);
		testValidity("2016-01-01T01:59:59+14:00, 2015-12-31T12:00:00Z", true);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testLexicalCases() {
		testValidity("2008-04-03T01:01:01, ", true);
		testValidity(", 2008-04-03T01:01:01", true);
		testValidity(" , ", true);
		testValidity("2008-04-03T01:01:01, +P5Y2M10D", true);
		testValidity("2008-04-03T01:01:01, -P10D", true);
		testValidity("2008-04-03T01:01:01, +-P10DT3H10M", true);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testChronologyCheck() {
		testValidity("2015-07-24T09:43:00Z, 2015-07-24T09:43:00Z", false);
		testValidity("2008-04-03T01:01:01Z, 2007-04-03T01:01:01Z", false);
		testValidity("2008-04-03T01:01:01Z, 2008-03-03T01:01:01Z", false);
		testValidity("2008-04-03T01:01:01Z, 2008-04-02T01:01:01Z", false);
		testValidity("2008-04-03T01:01:01Z, 2008-04-03T01:00:01Z", false);
		testValidity("2008-04-03T01:01:01Z, 2008-04-03T01:01:00Z", false);
		testValidity("2015-02-28T09:43:00Z, 2015-02-29T09:43:00Z", false);
		testValidity(", -292269055-12-02T16:47:04Z", true);
		testValidity("292278994-08-17T07:12:55.807Z,", false);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testInvalidCases() {
		testValidity("", false);
		testValidity("2015-07-24T09:43:00Z", false);
		testValidity("2008-04-03T01:01:01 2008-04-03T01:01:02", false);
		testValidity("2008-04-03T01:01:01Z, 2008-04-03T01:01:01Z", false);
		testValidity("2008-04-03T01:01:01, P0Y", false);
		testValidity("\"2015-07-24T09:43:00, 2015-07-24T09:43:01\"", false);
		testValidity("201500-0075-24T09:43:00, 2015-07-24T09:43:01", false);
		testValidity("2015-07-24T9:43:00, 2015-07-24T9:43:01", false);
		testValidity("2015-07-24T09:43:00, 2015-07-24T09:43:01, 2015-07-24T09:43:02", false);
		testValidity("2015:07:24T09-43-00, 2015-07-24T09:43:01", false);
		testValidity("0001-01-01T00:00:00Z, -292269055-12-02T16:47:04Z", false);
		testValidity("P5Y2M10D, 2008-04-03T01:01:01", false);
		testValidity("2008-04-03T01:01:01, P0D", false);
		testValidity("2008-04-03T01:01:01, -P0Y", false);
		testValidity("2008-04-03T01:01:01, -+P10DT3H10M", false);
		testValidity("292278994-08-17T03:12:55-04:00, P10DT3H10M", false);
		testValidity("2008-04-03T01:01:01, P5Y2M10D", false);
	}

	/** Tests parsing of lexical expressions into
	 * {@link com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInterval}.
	 *
	 * @param lex The expression
	 * @param isValid The expected result
	 */
	private static void testValidity(String lex, boolean isValid) {
		Literal literal = null;
		boolean parseThrewException = false;
		String exType = null;
		String exMsg = null;
		try {
			literal = ResourceFactory.createTypedLiteral(lex,
				TemporalIndexField.INTERVAL.getDatatype());
		} catch (DatatypeFormatException ex) {
			parseThrewException = true;
			exType = ex.getClass().getName();
			exMsg = ex.getMessage();
		}
		if (isValid) {
			assertFalse(parseThrewException,
				"Failed to parse '%1$s' into TemporalInterval.  %2$s:  %3$s"
				.formatted(lex, exType, exMsg));
			assertNotNull(literal);
		} else {
			String msg = "Parsed '%1$s' into TemporalInterval '%2$s', but parse should have failed"
				.formatted(lex, literal);
			assertNull(literal, msg);
			assertTrue(parseThrewException, msg);
		}
	}
}
