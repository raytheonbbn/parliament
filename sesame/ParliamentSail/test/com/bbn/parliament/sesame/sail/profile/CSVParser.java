// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.sesame.sail.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class takes a String containing one line of comma separated values and
 * returns those values in a List<String>.
 */
public class CSVParser
{
	private static final Pattern TOKEN_PATTERN        = Pattern
		.compile("[^,]*?(?=[,|\\n])");
	private static final Pattern QUOTED_TOKEN_PATTERN = Pattern
		.compile("((?>\\\")[^\\\"]*?\\\"(?=[,|\\n|\\\"]))+");

	private String               _toParse             = null;
	private int                  _index               = 0;

	/**
	 * Constructs a CSVParser with a string to parse.
	 *
	 * @param toParse The string to parse.
	 */
	private CSVParser(String toParse)
	{
		setString(toParse);
	}

	/**
	 * Sets the string to parse.
	 *
	 * @param toParse
	 */
	private void setString(String toParse)
	{
		_toParse = toParse + "\n";
		_index = 0;
	}

	private String nextToken()
	{
		String toReturn = null;
		String parseSubset = _toParse.substring(_index);
		Matcher quoted = QUOTED_TOKEN_PATTERN.matcher(parseSubset);
		Matcher m = TOKEN_PATTERN.matcher(parseSubset);
		if (quoted.lookingAt())
		{
			toReturn = quoted.group();
			_index += quoted.end() + 1;
		}
		else if (m.lookingAt())
		{
			toReturn = m.group();
			_index += m.end() + 1;
		}
		if (toReturn != null)
		{
			if (toReturn.startsWith("\""))
			{
				toReturn = toReturn.substring(1);
			}
			if (toReturn.endsWith("\""))
			{
				toReturn = toReturn.substring(0, toReturn.length() - 1);
			}
			toReturn = toReturn.replaceAll("(?<=[^\\\"])?+[\\\"]{2}(?=[^\\\"])",
				"\"");
		}
		return toReturn;
	}

	private List<String> getTokensList()
	{
		List<String> allTokens = new ArrayList<>();
		String token = null;
		while ((token = nextToken()) != null)
		{
			allTokens.add(token);
		}
		return allTokens;
	}

	/**
	 * The method to parse a string and return a list of tokens.
	 *
	 * @param toParse The string to parse.
	 * @return a List<String> of tokens parsed from the string.
	 */
	public static List<String> parse(String toParse)
	{
		return new CSVParser(toParse).getTokensList();
	}
}
