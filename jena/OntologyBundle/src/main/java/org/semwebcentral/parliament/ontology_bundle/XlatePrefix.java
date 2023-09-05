// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package org.semwebcentral.parliament.ontology_bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;

class XlatePrefix {
	private final File inFile;
	private final File outFile;
	private final String oldPrefix;
	private final String newPrefix;

	public static void main(String[] args) {
		try {
			XlatePrefix program = new XlatePrefix(args);
			program.run();
		} catch (CommandLineException ex) {
			usage(ex.getMessage());
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	private static void usage(String message) {
		if (message != null && !message.isEmpty()) {
			System.out.format("%1$s%n", message);
		}
		System.out.format("""

			%1$s <in-file> <out-file> <old-prefix> <new-prefix>

				<in-file> is the RDF file with a prefix to translate
				<out-file> is the file in which to write the output
				<old-prefix> is the RDF prefix to replace
				<new-prefix> is the replacement RDF prefix

			""", XlatePrefix.class.getSimpleName());
	}

	private XlatePrefix(String[] args) throws CommandLineException {
		if (args.length < 4) {
			throw new CommandLineException("Missing command line arguments");
		} else if (args.length > 4) {
			throw new CommandLineException("Too many command line arguments");
		}
		inFile = new File(args[0]);
		outFile = new File(args[1]);
		oldPrefix = args[2];
		newPrefix = args[3];
	}

	private void run() throws IOException {
		var inLang = RDFLanguages.filenameToLang(inFile.getName());
		if (inLang == null) {
			throw new IllegalStateException(
				"Unrecognized RDF serialization: '%1$s'".formatted(inFile.getPath()));
		}

		var outLang = RDFLanguages.filenameToLang(inFile.getName());
		if (outLang == null) {
			throw new IllegalStateException(
				"Unrecognized RDF serialization: '%1$s'".formatted(inFile.getPath()));
		}

		var model = ModelFactory.createDefaultModel();
		try (InputStream in = new FileInputStream(inFile)) {
			model.read(in, null, inLang.getName());
		}

		var nsIri = model.getNsPrefixURI(oldPrefix);
		model.removeNsPrefix(oldPrefix);
		model.setNsPrefix(newPrefix, nsIri);

		try (OutputStream os = new FileOutputStream(outFile)) {
			model.write(os, outLang.getName(), null);
		}
	}
}
