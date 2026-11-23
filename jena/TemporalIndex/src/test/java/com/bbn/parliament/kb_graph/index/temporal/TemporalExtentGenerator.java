// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph.index.temporal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.kb_graph.index.temporal.pt.TemporalIndexField;

/** @author rbattle */
public class TemporalExtentGenerator {
	private static final SimpleDateFormat XSD_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private static final Logger LOG = LoggerFactory.getLogger(TemporalExtentGenerator.class);

	public static void main(String[] args) {
		String file = (args.length == 0)
			? "./test/test.n3"
			: args[0];
		try (PrintWriter pw = new PrintWriter(file)) {
			generateHeader(pw);
			Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
			c.set(2008, 1, 1, 0, 0, 0);
			long ms = c.getTimeInMillis();
			for (int i = 0; i < 100; i++) {
				c =  new GregorianCalendar(TimeZone.getTimeZone("GMT"));
				c.setTimeInMillis(ms + (1000 * 60 * 60 * i));
				generateInstant(pw, i, c.getTime());
			}
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
	}

	public static StringBuffer generateTestInstantStream(int count) {
		try (
			StringWriter writer = new StringWriter();
			PrintWriter pw = new PrintWriter(writer);
		) {
			generateHeader(pw);
			Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
			long ms = c.getTimeInMillis();
			for(int i = 0; i < count; i++) {
				c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
				c.setTimeInMillis(ms + (1000 * 60 * 60 * i));
				generateInstant(pw, i, c.getTime());
			}
			pw.flush();
			return writer.getBuffer();
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Note that the intervals generated are all in the GMT time zone.
	 * @param count # of test intervals to produce.
	 * @return Buffer of test intervals.
	 * @author mhale
	 */
	public static StringBuffer generateTestIntervalStream(int count) {
		try (
			StringWriter writer = new StringWriter();
			PrintWriter pw = new PrintWriter(writer);
		) {
			generateHeader(pw);
			Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
			Calendar d = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
			long ms = c.getTimeInMillis();
			for(int i = 1; i < count + 1; i++) {
				c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
				d = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
				c.setTimeInMillis(ms + (1000 * 60 * 60 * i));
				d.setTimeInMillis(ms + (1000 * 60 * 60 * i * 2));
				generateInterval(pw, i, c.getTime(), d.getTime());
			}
			pw.flush();
			return writer.getBuffer();
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static void generateHeader(PrintWriter pw) {
	}

	public static void generateInstant(PrintWriter pw, int id, Date date) {
		String res = "<http://example.org/instant/%s>".formatted(id);
		String timeassign = "%1$s <%2$s> \"%3$s\"^^<%4$s> .".formatted(res,
			Constants.PT_AS_INSTANT.getURI(), XSD_DATE_FORMAT.format(date),
			TemporalIndexField.INSTANT.getDatatype().getURI());
		LOG.debug("Generating {}", timeassign);
		pw.println(timeassign);
		pw.println();
	}

	public static void generateInterval(PrintWriter pw, int id, Date startDate, Date endDate) {
		String res = "<http://example.org/interval/%s>".formatted(id);
		String timeassign = "%1$s <%2$s> \"%3$s, %4$s\"^^<%5$s> .".formatted(res,
			Constants.PT_AS_INTERVAL.getURI(), XSD_DATE_FORMAT.format(startDate),
			XSD_DATE_FORMAT.format(endDate), TemporalIndexField.INTERVAL.getDatatype().getURI());
		LOG.debug("Generating {}", timeassign);
		pw.println(timeassign);
		pw.println();
	}
}
