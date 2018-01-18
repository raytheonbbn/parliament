//Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
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

/**
 * Functions for creating an export file of the contents of a Parliament triple store.
 *
 * @author astein
 */
public class RemoteExporter {
	private static final String HTTP_CONTENT_DISPOSITION = "Content-Disposition";
	private static final Pattern FILE_NAME_PATTERN = Pattern.compile(
		"^filename=\"([^\"]+)\"$", Pattern.CASE_INSENSITIVE);
	private static final int BUFFER_SIZE = 16384;

	private final String _endPointUrl;

	private static void usage()
	{
		System.out.format("Usage: %1$s %2$s %3$s %4$s%n%n",
			RemoteExporter.class.getName(),
			"hostname",
			"port",
			"[file save path]");
		System.exit(-1);
	}

	/**
	 * Create an exporter directed at a specific parliament server
	 * @param parliamentHost server's hostname or IP address
	 * @param parliamentPort port that the server listens on
	 */
	public RemoteExporter(String parliamentHost, int parliamentPort) {
		_endPointUrl = String.format(RemoteModel.DEFAULT_BULK_ENDPOINT_URL, parliamentHost,
			Integer.toString(parliamentPort) + "/export");
	}

	/**
	 * POSTs an export request to the bulk service end point of this Parliament instance
	 * and saves the exported data to the local file system.
	 * @param savePath Directory into which the export will be saved
	 * @return The path name of the file in which the export is saved
	 */
	public File doExport(File savePath) throws ParliamentClientException, IOException {
		if (!savePath.isDirectory()) {
			if (savePath.exists()) {
				throw new ParliamentClientException(
					"The export save path \"%1$s\" exists, but is not a directory.",
					savePath.getPath());
			} else {
				savePath.mkdirs();
			}
		}

		File file = null;

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			// assemble an export directive to POST to the service
			HttpPost postMethod = new HttpPost(_endPointUrl);
			List<NameValuePair> nvps = new ArrayList<>();
			nvps.add(new BasicNameValuePair("exportAll", "yes"));
			nvps.add(new BasicNameValuePair("dataFormat", RDFFormat.NTRIPLES.toString()));
			postMethod.setEntity(new UrlEncodedFormEntity(nvps));

			// execute the directive and process the response (i.e. save the export to a file)
			try (CloseableHttpResponse response = httpclient.execute(postMethod)) {
				if (response.getStatusLine().getStatusCode() == 200) {
					// do something useful with the response body
					// and ensure it is fully consumed
					file = new File(savePath, getExportFileName(response));
					try (
						OutputStream fileStream = new FileOutputStream(file);
						InputStream byteStream = response.getEntity().getContent();
					) {
						byte[] bytes = new byte[BUFFER_SIZE];
						int len = 0;
						while ((len = byteStream.read(bytes)) > 0) {
							fileStream.write(bytes, 0, len);
						}
					}
				} else {
					System.out.format("Unable to process response, HTTP status was '%1$s'.",
						response.getStatusLine());
				}
			}
		}
		return file;
	}

	/**
	 * Extract the file name Parliament assigned to the export payload of the response
	 * from the response headers.
	 * @throws ParliamentClientException
	 */
	private static String getExportFileName(HttpResponse response) throws ParliamentClientException {
		Header[] headers = response.getHeaders(HTTP_CONTENT_DISPOSITION);
		if (headers.length <= 0) {
			throw new ParliamentClientException(
				"Export response is missing expected header '%1$s'.", HTTP_CONTENT_DISPOSITION);
		} else if (headers.length > 1) {
			System.out.format(
				"WARNING: Multiple '%1$s' headers are present in response.  Unpredictable behavior may result.%n",
				HTTP_CONTENT_DISPOSITION);
		}

		String toReturn = null;
		outerMostLoop:
		for (Header h : headers) {
			String [] frags = h.getValue().split(Pattern.quote(";"));
			for (String frag : frags) {
				Matcher m = FILE_NAME_PATTERN.matcher(frag.trim());
				if (m.matches()) {
					toReturn = m.group(1);
					break outerMostLoop;
				}
			}
		}

		if (toReturn == null) {
			throw new ParliamentClientException(
				"Export response is missing a value for 'filename' in its '%1$s' header.",
				HTTP_CONTENT_DISPOSITION);
		}
		return toReturn;
	}

	/**
	 * Requests, downloads and saves a file containing a complete exporte of the contents
	 * of a remote Parliament instance.
	 * @param args should be hostname, port then optional preferred path to save file
	 */
	public static void main(String [] args) {
		try {
			if (args.length != 2 && args.length != 3) {
				usage();
			}
			String hostName = args[0];
			int port = Integer.parseInt(args[1]);
			String savePath = (args.length == 3) ? args[2] : ".";

			RemoteExporter exporter = new RemoteExporter(hostName, port);
			File file = exporter.doExport(new File(savePath));
			System.out.format("Export complete, data written to file '%1$s'.%n", file.getPath());
		} catch (NumberFormatException ex) {
			System.out.format("The given port \"%1$s\" is not a number:  %2$s%n%n", args[1], ex.getMessage());
			usage();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
