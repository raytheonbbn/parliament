// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.client.jena;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** @author sallen */
public class RemoteInserter {
	private final String sparqlEndPointUrl;
	private final String bulkEndPointUrl;
	private final File inputFile;
	private final RDFFormat inputFormat;
	private final String graphName;

	public static void main(String[] args) {
		try {
			RemoteInserter program = new RemoteInserter(args);
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
		System.out.format("Usage: %1$s <hostname> <port> <inputfile> [<graph-name>]%n%n",
			RemoteInserter.class.getName());
	}

	private RemoteInserter(String[] args) throws CmdLineException {
		try {
			if (args.length < 3) {
				throw new CmdLineException("Too few command line arguments");
			} else if (args.length > 4) {
				throw new CmdLineException("Too many command line arguments");
			}

			String hostName = args[0];
			int port = Integer.parseInt(args[1]);
			if (port <= 0) {
				throw new CmdLineException("The port %1$d must be positive", port);
			}
			sparqlEndPointUrl = RemoteModel.DEFAULT_SPARQL_ENDPOINT_URL.formatted(
				hostName, Integer.toString(port));
			bulkEndPointUrl = RemoteModel.DEFAULT_BULK_ENDPOINT_URL.formatted(
				hostName, Integer.toString(port));

			inputFile = new File(args[2]);
			if (!inputFile.exists()) {
				throw new CmdLineException("The given input file \"%1$s\" does not exist",
					inputFile.getPath());
			} else if (!inputFile.isFile()) {
				throw new CmdLineException("The given input path \"%1$s\" is not a file",
					inputFile.getPath());
			}

			inputFormat = RDFFormat.parseFilename(inputFile);
			if (!inputFormat.isJenaReadable()) {
				throw new CmdLineException("Unrecognized file extension:  \"%1$s\"", inputFile.getName());
			}

			graphName = (args.length == 4) ? args[3] : "";
		} catch (NumberFormatException ex) {
			throw new CmdLineException(ex,
				"The given port \"%1$s\" is not an integer:  %2$s",
				args[1], ex.getMessage());
		}
	}

	private void run() throws IOException {
		try (InputStream in = new FileInputStream(inputFile)) {
			RemoteModel remote = new RemoteModel(sparqlEndPointUrl, bulkEndPointUrl);
			long numStmts = remote.insertStatements(in, inputFormat, null, graphName, true);
			System.out.format("Successfully inserted %1$d statements.%n", numStmts);
		}
	}
}
