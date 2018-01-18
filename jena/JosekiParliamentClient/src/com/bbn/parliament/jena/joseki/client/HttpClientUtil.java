// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/** Utility methods for HTTP clients.  Taken from Sesame 1.2.6. */
public class HttpClientUtil {

	private static final String CRLF = "\r\n";

	/*---------------------+
| Methods              |
+---------------------*/

	/**
	 * Prepares an HttpURLConnection for a POST request that sends the
	 * supplied parameters.
	 *
	 * @param connection The connection to prepare for the request.
	 * @param parameters The key-value pairs to send in the POST request.
	 *
	 * @exception IOException If an I/O error occurs.
	 */
	public static void preparePostRequest(HttpURLConnection connection, Map<String, Object> parameters)
		throws IOException
	{
		// Create x-www-url-encoded parameter string
		String postData = buildQueryString(parameters);

		// Set up request
		connection.setRequestMethod("POST");
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setUseCaches(false);

		// Write the form data to the connection
		try (
			OutputStream postStream = connection.getOutputStream();
			Writer postWriter = new OutputStreamWriter(postStream);
		) {
			postWriter.write(postData);
		}
	}

	public static void prepareMultipartPostRequest(HttpURLConnection connection, Map<String,Object> parameters, String encoding)
		throws UnsupportedEncodingException, IOException
	{
		String boundary = "---8qP3mZ1yyysss---";
		byte[] postData = buildMultipartFormData(parameters, boundary, encoding);

		// Build the form data as a multipart/form-data byte array.
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		connection.setRequestProperty("Content-Length", String.valueOf(postData.length));

		// Set up request
		connection.setRequestMethod("POST");
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setUseCaches(false);

		// Write the form data to the connection
		try (OutputStream postStream = connection.getOutputStream()) {
			postStream.write(postData);
		}
	}

	public static void prepareMultipartPostRequestInputStreamAware(HttpURLConnection connection, Map<String,Object> parameters, String encoding)
		throws UnsupportedEncodingException, IOException
	{
		// Partition the parameters into InputStream or other, so that
		// we can write the non-InputStream parameters first as they
		// are likely to be much smaller than the InputStreams
		Map<String, Object> inputStreamParameters = new HashMap<>();
		Map<String, Object> nonInputStreamParameters = new HashMap<>();

		for (Map.Entry<String, Object> entry : parameters.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (value instanceof InputStream) {
				inputStreamParameters.put(key, value);
			} else {
				nonInputStreamParameters.put(key, value);
			}
		}

		String boundary = "---8qP3mZ1yyysss---";
		byte[] postParams = buildMultipartFormData(nonInputStreamParameters, boundary, encoding, false);

		// Build the form data as a multipart/form-data byte array.
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

		// Set up request
		connection.setRequestMethod("POST");
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setUseCaches(false);

		// Set "transfer-encoding: chunked" on the connection to allow
		// the request to be streamed in chunks to the remote repository
		//
		// This method is only availible in JDK >1.5 (see Sun bug #5026745:
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5026745 )
		//
		// Also, because of a separate bug in commons-fileupload-1.1,
		// the remote Sesame repository will not accept transfers
		// without a Content-Length header.  This bug is fixed in
		// commons-fileupload-1.2, so the remote Sesame server needs
		// to have this new library.
		// See Apache bugs #37395 and #39162:
		// http://issues.apache.org/bugzilla/show_bug.cgi?id=37794
		// http://issues.apache.org/bugzilla/show_bug.cgi?id=39162
		connection.setChunkedStreamingMode(10000);

		// Write the form data to the connection
		try (OutputStream postStream = connection.getOutputStream()) {
			postStream.write(postParams);

			byte[] startBoundary = (CRLF + "--" + boundary + CRLF).getBytes();
			byte[] endBoundary = (CRLF + "--" + boundary + "--" + CRLF).getBytes();

			Iterator<Map.Entry<String, Object>> isIter = inputStreamParameters.entrySet().iterator();
			while (isIter.hasNext()) {
				Map.Entry<String, Object> entry = isIter.next();
				String key = entry.getKey();
				Object value = entry.getValue();

				byte[] partHeader = ("Content-Disposition: form-data; name=\"" + key + "\"" + CRLF + CRLF).getBytes();

				postStream.write(startBoundary);
				postStream.write(partHeader);

				try (InputStream in = (InputStream) value) {
					transfer(in, postStream);
				}
			}
			postStream.write(endBoundary);
		}
	}

	/**
	 * Transfers all bytes that can be read from <tt>in</tt> to <tt>out</tt>.
	 *
	 * @param in The InputStream to read data from.
	 * @param out The OutputStream to write data to.
	 * @return The total number of bytes transfered.
	 */
	public static final long transfer(InputStream in, OutputStream out)
		throws IOException
	{
		long totalBytes = 0;
		int bytesInBuf = 0;
		byte[] buf = new byte[4096];

		while ((bytesInBuf = in.read(buf)) != -1) {
			out.write(buf, 0, bytesInBuf);
			totalBytes += bytesInBuf;
		}

		return totalBytes;
	}

	/**
	 * Builds a multipart/form-data encoded byte array of the specified parameters
	 * (This method includes the end boundary by default).
	 *
	 * @see #buildMultipartFormData(Map, String, String, boolean)
	 */
	public static byte[] buildMultipartFormData(Map<String, Object> parameters, String boundary, String encoding)
		throws UnsupportedEncodingException
	{
		return buildMultipartFormData(parameters, boundary, encoding, true);
	}

	/**
	 * Builds a multipart/form-data encoded byte array of the specified parameters,
	 * that complies to <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>.
	 * Note that the request sent to the server should have a header of the following
	 * form set: <tt>Content-type: multipart/form-data, boundary=boundary-parameter</tt>.
	 * E.g.:
	 * <pre>
	 * HttpURLConnection connection = (HttpURLConnection)url.openConnection();
	 * connection.setRequestProperty("Content-type", "multipart/form-data, boundary=AaB03x");
	 * </pre>
	 *
	 * @param parameters A map of String keys to values that are either <tt>FilePart</tt>s, in
	 * which case its contents will be uploaded; byte arrays, which will be uploaded as-is; or
	 * any other Object, in which case the value of the object's <tt>toString()</tt> method
	 * will be used.
	 * @param boundary A boundary to use as separator between the encoded parts of the data.
	 * @param encoding The character encoding for the data, e.g. "UTF-8".
	 * @param includeEndBoundary Whether or not to include the end boundary
	 * @return A byte array in multipart/form-data format.
	 */
	public static byte[] buildMultipartFormData(Map<String, Object> parameters, String boundary, String encoding, boolean includeEndBoundary)
		throws UnsupportedEncodingException
	{
		List<byte[]> parts = new ArrayList<>(parameters.size());
		int partLengthSum = 0;

		Iterator<Map.Entry<String, Object>> iter = parameters.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, Object> entry = iter.next();
			String key = entry.getKey();
			Object value = entry.getValue();

			byte[] partHeader, partContents;

			//if (value instanceof FilePart) {
			//	FilePart fp = (FilePart)value;
			//	partHeader =
			//		("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + fp.getName() + "\"" + CRLF +
			//			"Content-Type: " + fp.getContentType() + CRLF + CRLF).getBytes();
			//	partContents = fp.getBytes();
			//} else {
			partHeader = ("Content-Disposition: form-data; name=\"" + key + "\"" + CRLF + CRLF).getBytes();

			if (value instanceof byte[]) {
				partContents = (byte[])value;
			} else {
				partContents = value.toString().getBytes(encoding);
				//				}
			}

			byte[] part = new byte[partHeader.length + partContents.length];
			System.arraycopy(partHeader, 0, part, 0, partHeader.length);
			System.arraycopy(partContents, 0, part, partHeader.length, partContents.length);
			parts.add(part);

			partLengthSum += part.length;
		}

		byte[] startBoundary = (CRLF + "--" + boundary + CRLF).getBytes();
		byte[] endBoundary = (CRLF + "--" + boundary + "--" + CRLF).getBytes();

		int totalLength = parts.size() * startBoundary.length +
			(includeEndBoundary ? endBoundary.length : 0) + partLengthSum;

		byte[] result = new byte[totalLength];
		int idx = 0;

		for (int i = 0; i < parts.size(); ++i) {
			byte[] part = parts.get(i);

			System.arraycopy(startBoundary, 0, result, idx, startBoundary.length);
			idx += startBoundary.length;

			System.arraycopy(part, 0, result, idx, part.length);
			idx += part.length;
		}
		if (includeEndBoundary) {
			System.arraycopy(endBoundary, 0, result, idx, endBoundary.length);
		}

		return result;
	}

	/**
	 * Builds a query string from the provided key-value-pairs. All
	 * spaces are substituted by '+' characters, and all non US-ASCII
	 * characters are escaped to hexadecimal notation (%xx).
	 */
	public static String buildQueryString(Map<String, Object> keyValuePairs) {
		StringBuffer result = new StringBuffer(20*keyValuePairs.size());

		Set<Map.Entry<String, Object>> entrySet = keyValuePairs.entrySet();
		Iterator<Map.Entry<String, Object>> iter = entrySet.iterator();

		while (iter.hasNext()) {
			// Iterate over all key-value pairs
			Map.Entry<String, Object> keyValuePair = iter.next();

			String key   = keyValuePair.getKey();
			String value = (String) keyValuePair.getValue();

			// Escape both key and value and combine them with an '='
			_formUrlEncode(key, result);
			result.append('=');
			_formUrlEncode(value, result);

			// If there are more key-value pairs, append an '&'
			if (iter.hasNext()) {
				result.append('&');
			}
		}

		return result.toString();
	}

	/**
	 * Encodes a string according to RFC 1738 : Uniform Resource locators (URL).
	 * According to this spec, any characters outside the range 0x20 - 0x7E must
	 * be escaped because they are not printable characters. Within the range
	 * a number of characters are deemed unsafe or are marked as reserved. In
	 * short: According to the spec only the alphanumerics and the special
	 * characters from <tt>$-_.+!*'(),</tt> can be left unencoded. To be save
	 * this method will encode all characters that are not alphanumerics.
	 */
	private static void _formUrlEncode(String s, StringBuffer buf) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int cInt = c;

			// Only characters in the range 48 - 57 (numbers), 65 - 90 (upper
			// case letters), 97 - 122 (lower case letters) can be left
			// unencoded. The rest needs to be escaped.

			if (cInt >= 48 && cInt <= 57 ||
				cInt >= 65 && cInt <= 90 ||
				cInt >= 97 && cInt <= 122)
			{
				// alphanumeric character
				buf.append(c);
			}
			else {
				// Escape all non-alphanumerics
				buf.append('%');
				String hexVal = Integer.toHexString(cInt);

				// Ensure use of two characters
				if (hexVal.length() == 1) {
					buf.append('0');
				}

				buf.append(hexVal);
			}
		}
	}

	/**
	 * Sets a request property on the supplied connection indicating that a
	 * server can respond with gzip-encoded data if it wants to.
	 *
	 * @see #getInputStream
	 */
	public static void setAcceptGZIPEncoding(URLConnection conn) {
		conn.setRequestProperty("Accept-Encoding", "gzip");
	}

	/**
	 * Gets the InputStream for reading the response from a server. This method
	 * handles any encoding-related decoding of the data, e.g. gzip.
	 *
	 * @see #setAcceptGZIPEncoding
	 */
	public static InputStream getInputStream(URLConnection conn)
		throws IOException
	{
		InputStream responseStream = conn.getInputStream();

		String contentEncoding = conn.getContentEncoding();
		if ("gzip".equalsIgnoreCase(contentEncoding)) {
			responseStream = new GZIPInputStream(responseStream);
		}

		return responseStream;
	}
}
