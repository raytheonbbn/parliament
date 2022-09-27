// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2014-2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.bridge.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogUtil {
	public static String formatForLog(String msgPrefix, String queryString) {
		String tmp = queryString;
		tmp = tmp.replace('\n', ' ');
		tmp = tmp.replace('\r', ' ');
		return msgPrefix + tmp;
	}

	public static String getExceptionInfo(Exception ex) {
		StringBuilder st = new StringBuilder();
		for (StackTraceElement ste : ex.getStackTrace()) {
			st.append(ste.toString());
			st.append("\n");
		}
		return "Exception Class:\n%s\nMessage:\n%s\nStackTrace:\n%s"
			.formatted(ex.getClass().getName(), ex.getMessage(), st.toString());
	}

	public static String fixEolsForLogging(String s) {
		String platformEOL = System.getProperty("line.separator");
		if ("\n".equals(platformEOL)) {
			return s.replaceAll(Pattern.quote("\r\n"), platformEOL);
		} else if ("\r\n".equals(platformEOL)) {
			Pattern p = Pattern.compile("([^\r])\n");
			Matcher m = p.matcher(s);
			return m.replaceAll("\\1\r\n");
		} else {
			return s;
		}
	}
}
