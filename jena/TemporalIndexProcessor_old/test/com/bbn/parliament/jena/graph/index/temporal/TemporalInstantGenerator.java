// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * @author rbattle
 *
 */
public class TemporalInstantGenerator {

    private static final SimpleDateFormat XSD_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");

    public static void main(String[] args) {
        PrintWriter pw;

        String file = "";
        if (args.length == 0) {
            file = "./test/test.n3";
        } else {
            file = args[0];
        }
        try {
            pw = new PrintWriter(new File(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        generateHeader(pw);
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        c.set(2008, 1, 1, 0, 0, 0);
        long ms = c.getTimeInMillis();
        for (int i = 0; i < 100; i++) {
            c =  new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            c.setTimeInMillis(ms + (1000 * 60 * 60 * i));
            generateInstant(pw, i, c.getTime());
        }

        pw.flush();
        pw.close();
    }

    public static StringBuffer generateTestStream(int count) {
   	 StringWriter writer = new StringWriter();
   	 PrintWriter pw = new PrintWriter(writer);
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
    }

    public static void generateHeader(PrintWriter pw) {

    }

    public static void generateInstant(PrintWriter pw, int id, Date date) {

//        []
//              a       time:DateTimeInterval ;
//              time:xsdDateTime "2008-04-03T00:00:01"^^xsd:dateTime .

        String res = "<http://example.org/instant/" + id + ">";
        pw.println(res + " <" + RDF.type.getURI() + "> <" + Constants.TIME_NS + "DateTimeInterval> .");
        pw.println(res + " <" + Constants.TIME_NS + "xsdDateTime> \"" + XSD_DATE_FORMAT.format(date) + "\"^^<" + XSD.dateTime.getURI() + "> .");
        pw.println();
    }

//    public static void generateInterval(PrintWriter pw, int id, Date startDate, long length) {
//        String res = "<http://example.org/interval/" + id + ">";
//        Calendar c = new GregorianCalendar();
//    }
}
