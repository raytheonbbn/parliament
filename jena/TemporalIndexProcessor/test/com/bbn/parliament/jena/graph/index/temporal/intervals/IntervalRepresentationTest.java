package com.bbn.parliament.jena.graph.index.temporal.intervals;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.bbn.parliament.jena.graph.index.temporal.Constants;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInterval;
import com.bbn.parliament.jena.graph.index.temporal.pt.TemporalIndexField;
import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.impl.JenaParameters;

/** @author mhale */
@RunWith(JUnitPlatform.class)
public class IntervalRepresentationTest {
	private static final String MIN = "MIN";
	private static final String MAX = "MAX";
	private TemporalInterval expected;

	@BeforeEach
	public void setUp() {
		expected = new TemporalInterval();
		JenaParameters.enableEagerLiteralValidation = true;
	}

	@Test
	public void testProperIntervals() {
		String[] times;
		times = new String[] {"2015-07-24T09:43:00", "2015-07-24T09:43:01"};
		parseAndCompare(times[0] + "," + times[1], times);
		times = new String[] {"2015-07-24T09:43:00", "2015-07-24T09:43:60"};
		parseAndCompare(times[0] + "," + times[1], times);
		times = new String[] {"2015-07-24T09:43:00Z", "2015-07-24T09:44:01Z"};
		parseAndCompare(times[0] + "," + times[1], times);
		times = new String[] {"2015-07-24T09:43:16", "2015-07-24T10:42:15"};
		parseAndCompare(times[0] + "," + times[1], times);
		times = new String[] {"2015-07-24T09:43:00", "2016-06-23T08:42:59"};
		parseAndCompare(times[0] + "," + times[1], times);
		times = new String[] {"2015-12-31T23:59:59", "2016-01-01T00:00:00"};
		parseAndCompare(times[0] + "," + times[1], times);
		times = new String[] {"2015-02-28T09:43:00", "2015-03-01T09:43:59"};
		parseAndCompare(times[0] + "," + times[1], times);
	}

	@Test
	public void testTimeZoneIntervals() {
		parseAndCompare("2015-07-24T09:43:00+01:00, 2015-07-24T09:43:00-01:00",
			new String[] {"2015-07-24T08:43:00Z", "2015-07-24T10:43:00Z"});
		parseAndCompare("2015-07-24T10:43:00+02:00, 2015-07-24T08:44:01-02:00",
			new String[] {"2015-07-24T08:43:00Z", "2015-07-24T10:44:01Z"});
	}

	@Test
	public void testUnboundedIntervals() {
		parseAndCompare("2015-07-24T09:43:00, ", new String[] {"2015-07-24T09:43:00", MAX});
		parseAndCompare(", 2015-07-24T09:44:01Z", new String[] {MIN, "2015-07-24T09:44:01Z"});
		parseAndCompare(",", new String[] {MIN, MAX});
	}

	@Test
	public void testDurations() {
		String baseline;
		String extended;
		baseline = "2015-07-24T09:43:00";
		extended = "2015-07-24T10:44:01";
		parseAndCompare(baseline + ", +PT1H1M1S", new String[] {baseline, extended});
		baseline = "2015-07-24T09:43:00";
		extended = "2015-07-24T08:41:59";
		parseAndCompare(baseline + ", -PT1H1M1S", new String[] {extended, baseline});
		baseline = "2015-07-24T09:43:00";
		extended = "2015-07-31T09:43:00";
		parseAndCompare(baseline + ", +P7D", new String[] {baseline, extended});
		baseline = "2015-07-24T09:43:00";
		extended = "2016-02-24T09:43:00";
		parseAndCompare(baseline + ", +P7M", new String[] {baseline, extended});
		baseline = "2015-07-24T09:43:00";
		extended = "2020-10-05T10:44:00";
		parseAndCompare(baseline + ", +P5Y2M10DT24H60M60S", new String[] {baseline, extended});
		baseline = "2015-07-24T09:43:00";
		extended = "2016-07-24T09:43:00";
		parseAndCompare(baseline + ", +P1Y", new String[] {baseline, extended});
		baseline = "2015-07-24T09:43:00";
		extended = "2016-07-23T09:43:00";
		parseAndCompare(baseline + ", +PT525600M", new String[] {baseline, extended});
		baseline = "2015-07-24T09:43:00";
		extended = "2014-07-24T09:43:00";
		parseAndCompare(baseline + ", -PT525600M", new String[] {extended, baseline});
	}

	@Test
	public void testAbsoluteDurations() {
		String baseline;
		String ahead;
		String behind;
		baseline = "2015-07-24T09:43:00";
		ahead = "2015-07-24T10:44:01";
		behind = "2015-07-24T08:41:59";
		parseAndCompare(baseline + ", +-PT1H1M1S", new String[] {behind, ahead});
		baseline = "2015-07-24T09:43:00";
		ahead = "2015-07-31T09:43:00";
		behind = "2015-07-17T09:43:00";
		parseAndCompare(baseline + ", +-P7D", new String[] {behind, ahead});
		baseline = "2015-07-24T09:43:00";
		ahead = "2016-02-24T09:43:00";
		behind = "2014-12-24T09:43:00";
		parseAndCompare(baseline + ", +-P7M", new String[] {behind, ahead});
		baseline = "2015-07-24T09:43:00";
		ahead = "2020-10-05T10:44:00";
		behind = "2010-05-13T08:42:00";
		parseAndCompare(baseline + ", +-P5Y2M10DT24H60M60S", new String[] {behind, ahead});
		baseline = "2015-07-24T09:43:00";
		ahead = "2016-07-24T09:43:00";
		behind = "2014-07-24T09:43:00";
		parseAndCompare(baseline + ", +-P1Y", new String[] {behind, ahead});
		baseline = "2015-07-24T09:43:00";
		ahead = "2016-07-23T09:43:00";
		behind = "2014-07-24T09:43:00";
		parseAndCompare(baseline + ", +-PT525600M", new String[] {behind, ahead});
	}

	/**
	 * Parses an interval data type and a pair of calendars representing the expected
	 * interval end points, and asserts their equality.
	 *
	 * @param lexicalInput Interval data type
	 * @param endpoints Pair of calendars (to be parsed)
	 */
	private void parseAndCompare(String lexicalInput, String[] endpoints) {
		TemporalInterval actual = null;
		try {
			actual = (TemporalInterval) ResourceFactory.createTypedLiteral(lexicalInput,
				TemporalIndexField.INTERVAL.getDatatype()).getValue();
			Calendar start = null;
			Calendar end = null;
			if (endpoints[0].equals(MIN)) {
				start = new GregorianCalendar();
				start.setTimeInMillis(Long.MIN_VALUE);
			}
			else {
				start = Constants.XML_DT_FACTORY.newXMLGregorianCalendar(endpoints[0]).toGregorianCalendar();
			}
			if (endpoints[1].equals(MAX)) {
				end = new GregorianCalendar();
				end.setTimeInMillis(Long.MAX_VALUE);
			}
			else {
				end = Constants.XML_DT_FACTORY.newXMLGregorianCalendar(endpoints[1]).toGregorianCalendar();
			}
			expected.setStart(new TemporalInstant(start));
			expected.setEnd(new TemporalInstant(end));
		} catch (DatatypeFormatException ex) {
			fail(ex.getMessage());
		}
		assertTrue(expected.sameAs(actual));
	}
}
