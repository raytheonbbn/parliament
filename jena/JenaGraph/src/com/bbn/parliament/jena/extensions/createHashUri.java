package com.bbn.parliament.jena.extensions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QueryBuildException;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprEvalException;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.expr.VariableNotBoundException;
import com.hp.hpl.jena.sparql.function.Function;
import com.hp.hpl.jena.sparql.function.FunctionEnv;

/**
 * The createHashUri class implements a custom filter function for ARQ that
 * takes a list of arbitrary inputs and creates from them a new URI.  It
 * does this by converting the items to strings, combining those into a
 * single string, and applying a cryptographic hash to the result.
 *
 * To use this function in a SPARQL query, first declare a prefix like so
 * (the final period is significant!):
 *
 * PREFIX ext: &lt;java:com.bbn.parliament.jena.extensions.&gt;
 *
 * Then in the WHERE clause of your query, add a bind statement like so:
 *
 * bind (ext:createHashUri("http://example.org/foo#", ?x, ?y, ?z) as ?newUri)
 *
 * This will cause the variable ?newUri to be bound to a hash URI created
 * from ?x, ?y, and ?z.  The resulting URI will look like the following:
 *
 * http://example.org/foo#_8a06dc0eda4a2e9ded49481c5e523945620ae5ef324c428f0630e5eaf36addfe
 *
 * If an unbound variable is included in the list of items to hash, it will be
 * handled correctly, in the sense that a "null" placeholder will be inserted
 * into hash computation.  However, the query writer must take care to ensure
 * that unbound variables will not result in a nonsensical URI.  For instance,
 * if all of the items in the list were unbound variables, then the resulting
 * URI would not likely be a unique identifier of anything.
 *
 * Note that because the class name will appear in SPARQL queries as a
 * function, the class name begins with a lower case letter in opposition
 * to the usual coding conventions for Java.
 *
 * The filter function parameters are (in order):
 *
 * (1) A base URI to which the hash will be appended, and
 *
 * (2) One or more items to hash.
 *
 * @author iemmons
 */
public class createHashUri implements Function {
	private static final String ARGS_ERROR_MSG = "Function %1$s requires at least two arguments"
		.formatted(createHashUri.class.getName());
	private static final String ALGORITHM = "SHA-256"; // MD2, MD5, SHA-1, SHA-256, SHA-384, or SHA-512
	private static final char[] HEX_CHAR = { '0', '1', '2', '3', '4', '5',
		'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	private static final Logger LOG = LoggerFactory.getLogger(createHashUri.class);

	/** Implementation of the build method from the Function interface. */
	@Override
	public void build(String uri, ExprList args) {
		if (args.size() < 2) {
			LOG.error(ARGS_ERROR_MSG);
			throw new QueryBuildException(ARGS_ERROR_MSG);
		}
	}

	/** Implementation of the exec method from the Function interface. */
	@Override
	public NodeValue exec(Binding binding, ExprList args, String uri, FunctionEnv env) {
		if (args.size() < 2) {
			LOG.error(ARGS_ERROR_MSG);
			throw new ExprEvalException(ARGS_ERROR_MSG);
		}

		try {
			String baseUri = "";
			String[] stringsToBeHashed = new String[args.size() - 1];
			Iterator<?> iter = args.iterator();
			for (int i = -1; iter.hasNext(); ++i)
			{
				Expr e = (Expr) iter.next();
				String s = null;

				try {
					NodeValue x = e.eval(binding, env);
					s = x.asUnquotedString();
				} catch (VariableNotBoundException ex) {
					// Do nothing
				}

				if (i < 0) {
					baseUri = s;
				} else {
					stringsToBeHashed[i] = s;
				}
			}

			String csvRow = createCsvRowFromStringArray(stringsToBeHashed);
			LOG.trace("CSV row used to create hashed URI:  {}", csvRow);
			String result = baseUri + computeHash(csvRow);
			return NodeValue.makeNode(Node.createURI(result));
		} catch (RuntimeException ex) {
			LOG.error("Unable to create hash URI", ex);
			throw ex;
		}
	}

	/**
	 * This method does the opposite of parsing a row from a CSV file. In other
	 * words, it takes an array of strings and creates from them a legal CSV file
	 * row. The created row is minimal, in that the elements are quoted only if
	 * they need to be.
	 *
	 * @param strings A variable length list of strings from which to create the row.
	 * @return A CSV file row corresponding to the given string array. This does
	 *         not include the end-of-line character(s).
	 */
	private static String createCsvRowFromStringArray(String... strings) {
		StringBuilder buffer = new StringBuilder(256);
		boolean pastFirstRow = false;
		for (String str : strings) {
			if (pastFirstRow) {
				buffer.append(',');
			} else {
				pastFirstRow = true;
			}

			if (str == null) {
				// This is a trick.  We append the string "null" (with the quotes)
				// to distinguish between an actual string with a value containing
				// the letters 'n', 'u', 'l', and 'l' (which would not be quoted),
				// and a null string reference.  This is important in the case
				// where one of the items passed to the filter function is unbound.
				buffer.append("\"null\"");
			} else if (str.length() > 0) {
				if (str.indexOf(',') >= 0 || str.indexOf('"') >= 0 || str.indexOf('\n') >= 0
					|| str.indexOf('\r') >= 0 || str.startsWith(" ") || str.startsWith("\t")
					|| str.endsWith(" ") || str.endsWith("\t")) {
					buffer.append('"');
					buffer.append(str.replace("\"", "\"\""));
					buffer.append('"');
				} else {
					buffer.append(str);
				}
			}
		}
		return buffer.toString();
	}

	/**
	 * Generates a hashed URI based on an arbitrary number of string inputs and a
	 * base URI. Note: If you are using this method to generate a reification URI
	 * fragment, then you need to pass in three strings in this order: subject,
	 * predicate, and object.
	 *
	 * @param strings A variable length list of strings to hash into the URI fragment
	 * @return A hashed URI fragment generated from the input strings
	 */
	private static String computeHash(String strToBeHashed) {
		try {
			MessageDigest msgDigest = MessageDigest.getInstance(ALGORITHM);
			byte[] hash = msgDigest.digest(strToBeHashed.getBytes(StandardCharsets.UTF_8));
			return toHexString(hash);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalArgumentException(
				"Unsupported message digest algorithm:  " + ALGORITHM, ex);
		}
	}

	/** Converts a byte array into a character string of hexadecimal digits. */
	private static String toHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2 + 1);
		sb.append('_');	// ensure IRI fragment doesn't start with a digit
		for (byte b : bytes) {
			int hiNibble = (b >>> 4) & 0x0f;
			int loNibble = b & 0x0f;

			sb.append(HEX_CHAR[hiNibble]);
			sb.append(HEX_CHAR[loNibble]);
		}
		return sb.toString();
	}
}
