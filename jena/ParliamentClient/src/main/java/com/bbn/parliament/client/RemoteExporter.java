//Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jena.riot.Lang;

/**
 * Functions for exporting the contents of a Parliament triple store to a file.
 *
 * @author astein
 */
public class RemoteExporter {
	private static final int BUFFER_SIZE = 64 * 1024;
	private static final String HTTP_CONTENT_DISPOSITION = "Content-Disposition";
	private static final Pattern FILE_NAME_PATTERN = Pattern.compile(
		"^filename=\"([^\"]+)\"$", Pattern.CASE_INSENSITIVE);

	private final String endPointUrl;
	private final File saveDir;

	/**
	 * Requests, downloads and saves a file containing a complete export of the contents
	 * of a remote Parliament instance.
	 *
	 * @param args should be hostname, port then optional preferred path to save file
	 */
	public static void main(String[] args) {
		try {
			RemoteExporter program = new RemoteExporter(args);
			program.run();
		} catch (CmdLineException ex) {
			usage(ex.getMessage());
			System.exit(-2);
		} catch (Throwable ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	private static void usage(String message)
	{
		System.out.format("%n");
		if (message != null && !message.isEmpty()) {
			System.out.format("%1$s%n%n", message);
		}
		System.out.format("Usage: %1$s <hostname> <port> [<file-save-directory>]%n%n",
			RemoteExporter.class.getName());
	}

	/**
	 * Create an exporter directed at a specific parliament server
	 *
	 * @param args The command line arguments to main()
	 */
	private RemoteExporter(String[] args) throws CmdLineException {
		try {
			if (args.length < 2) {
				throw new CmdLineException("Too few command line arguments");
			} else if (args.length > 3) {
				throw new CmdLineException("Too many command line arguments");
			}
			String hostName = args[0];
			int port = Integer.parseInt(args[1].strip());
			if (port <= 0) {
				throw new CmdLineException("The port %1$d must be positive", port);
			}

			endPointUrl = RemoteModel.DEFAULT_BULK_ENDPOINT_URL.formatted(
				hostName, Integer.toString(port)) + "/export";
			saveDir = new File((args.length == 3) ? args[2] : ".");

			if (saveDir.exists() && !saveDir.isDirectory()) {
				throw new CmdLineException(
					"The export save path \"%1$s\" exists, but is not a directory.",
					saveDir.getPath());
			}
		} catch (NumberFormatException ex) {
			throw new CmdLineException(ex,
				"The given port \"%1$s\" is not an integer:  %2$s",
				args[1], ex.getMessage());
		}
	}

	/**
	 * POSTs an export request to the bulk service end point of this Parliament instance
	 * and saves the exported data to the local file system.
	 */
	private void run() throws ParliamentClientException, IOException {
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			// assemble an export directive to POST to the service
			HttpPost postMethod = new HttpPost(endPointUrl);
			List<NameValuePair> nvps = new ArrayList<>();
			nvps.add(new BasicNameValuePair("exportAll", "yes"));
			nvps.add(new BasicNameValuePair("dataFormat", Lang.NTRIPLES.getName()));
			postMethod.setEntity(new UrlEncodedFormEntity(nvps));

			// execute the directive and process the response (i.e. save the export to a file)
			try (CloseableHttpResponse response = httpclient.execute(postMethod)) {
				if (response.getStatusLine().getStatusCode() != 200) {
					System.out.format("Unable to process response, HTTP status was '%1$s'.",
						response.getStatusLine());
				} else {
					// Save the response body and ensure it is fully consumed:
					File file = new File(saveDir, getExportFileName(response));
					if (!saveDir.exists()) {
						saveDir.mkdirs();
					}
					try (
						OutputStream fileStream = new FileOutputStream(file);
						InputStream byteStream = response.getEntity().getContent();
					) {
						byte[] bytes = new byte[BUFFER_SIZE];
						int bytesRead = 0;
						while ((bytesRead = byteStream.read(bytes)) > 0) {
							fileStream.write(bytes, 0, bytesRead);
						}
					}
					System.out.format("Export complete, data written to file '%1$s'.%n", file.getPath());
				}
			}
		}
	}

	/**
	 * Extract the file name Parliament assigned to the export payload of the response
	 * from the response headers.
	 *
	 * @throws ParliamentClientException
	 */
	private static String getExportFileName(HttpResponse response) throws ParliamentClientException {
		Header[] headers = response.getHeaders(HTTP_CONTENT_DISPOSITION);
		if (headers.length < 1) {
			throw new ParliamentClientException(
				"Export response is missing expected header '%1$s'.", HTTP_CONTENT_DISPOSITION);
		} else if (headers.length > 1) {
			System.out.format(
				"WARNING: Multiple '%1$s' headers present in response.  Unpredictable behavior may result.%n",
				HTTP_CONTENT_DISPOSITION);
		}

		String result = Arrays.stream(headers)
			.map(header -> header.getValue().split(Pattern.quote(";")))
			.flatMap(headerFragments -> Arrays.stream(headerFragments))
			.map(headerFragment -> FILE_NAME_PATTERN.matcher(headerFragment.strip()))
			.filter(matcher -> matcher.matches())
			.map(matcher -> matcher.group(1))
			.findFirst()
			.orElse(null);

		if (result == null) {
			throw new ParliamentClientException(
				"Export response is missing a value for 'filename' in its '%1$s' header.",
				HTTP_CONTENT_DISPOSITION);
		}

		return result;
	}
}
