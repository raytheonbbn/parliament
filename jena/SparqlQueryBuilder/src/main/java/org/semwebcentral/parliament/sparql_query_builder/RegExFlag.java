// Copyright (c) 2019, 2020 Raytheon BBN Technologies Corp.

package org.semwebcentral.parliament.sparql_query_builder;

/**
 * An enumeration of flags that can be used to modify the behavior of a regular
 * expression.
 *
 * @author iemmons
 */
public enum RegExFlag {
	/**
	 * By default, the meta-character '.' matches any character except a newline
	 * (#x0A) or carriage return (#x0D). If this flag is present, '.' matches any
	 * character.
	 */
	DOT_ALL("s"),

	/**
	 * By default, the meta-character ^ matches the start of the entire string,
	 * while $ matches the end of the entire string. If this flag is present, ^
	 * matches the start of any line, while $ matches the end of any line.
	 */
	MULTI_LINE("m"),

	/**
	 * By default, matches are case-sensitive. If this flag is present, matches
	 * are case-insensitive.
	 */
	CASE_INSENSITIVE("i"),

	/**
	 * If present, whitespace characters (#x9, #xA, #xD and #x20) in the regular
	 * expression are removed prior to matching, except whitespace characters
	 * within character class expressions. This flag can be used, to break up
	 * long regular expressions into readable lines.
	 */
	IGNORE_WHITESPACE("x"),

	/**
	 * If present, all characters in the regular expression are treated as
	 * representing themselves, not as meta-characters. Furthermore, when this
	 * flag is present, the characters $ and \ have no special significance when
	 * used in the replacement string.
	 */
	LITERAL_MATCH("q");

	private String xPathFlag;

	private RegExFlag(String flag) {
		xPathFlag = flag;
	}

	String getXPathFlag() {
		return xPathFlag;
	}
}
