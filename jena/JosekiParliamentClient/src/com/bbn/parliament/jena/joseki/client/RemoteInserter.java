// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/** @author sallen */
public class RemoteInserter {
	private static void usage(String msg, Object... args)
	{
		if (msg != null && !msg.isEmpty()) {
			System.out.format(msg, args);
			System.out.format("%n%n");
		}
		System.out.format("Usage: %1$s %2$s %3$s %4$s %5$s%n%n",
			RemoteInserter.class.getName(),
			"hostname",
			"port",
			"inputfile",
			"[graph name]");
		System.exit(-1);
	}

	public static void main(String[] args) {
		try {
			if (args.length < 3 || args.length > 4) {
				usage(null);
			}

			String hostName = args[0];
			int port = Integer.parseInt(args[1]);
			File inputFile = new File(args[2]);
			if (!inputFile.exists()) {
				usage("Input file \"%1$s\" does not exist", inputFile.getPath());
			} else if (!inputFile.isFile()) {
				usage("The given input path \"%1$s\" is not a file", inputFile.getPath());
			}
			RDFFormat format = RDFFormat.parseFilename(inputFile);
			if (!format.isJenaReadable() && format != RDFFormat.JSON_LD) {
				usage("Unrecognized file extension:  \"%1$s\"", inputFile.getName());
			}
			String graphName = (args.length == 4) ? args[3] : "";

			String dummySparqlEndpoint = "";
			String bulkEndpoint = String.format(RemoteModel.DEFAULT_BULK_ENDPOINT_URL, hostName,
				Integer.toString(port));
			RemoteModel remote = new RemoteModel(dummySparqlEndpoint, bulkEndpoint);

			try (InputStream in = new FileInputStream(inputFile)) {
				long numStmts = remote.insertStatements(in, format, null, graphName, true);
				System.out.format("Successfully inserted %1$d statements.%n", numStmts);
			}
		} catch (NumberFormatException ex) {
			usage("The given port \"%1$s\" is not a number:  %2$s", args[1], ex.getMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
