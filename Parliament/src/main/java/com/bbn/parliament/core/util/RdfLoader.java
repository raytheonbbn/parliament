// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2026, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.core.util;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.riot.RDFParser;

import com.bbn.parliament.core.jni.KbConfig;
import com.bbn.parliament.core.jni.KbInstance;

public class RdfLoader {
	private final List<Path> files;
	private final KbConfig config;

	private static void usage(String msg) {
		if (msg != null && !msg.isBlank()) {
			System.out.println(msg);
			System.out.println();
		}
		System.out.println(
			"Usage: java com.bbn.parliament.core.util.RdfLoader <file> ...");
		System.out.println();
	}

	public static void main(String[] args) {
		try {
			var app = new RdfLoader(args);
			app.run();
		} catch (CmdLineException ex) {
			usage(ex.getMessage());
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	private RdfLoader(String[] args) throws CmdLineException {
		config = new KbConfig();
		config.readFromFile();
		config.m_kbDirectoryPath = ".";
		config.m_readOnly = false;

		files = new ArrayList<>();
		for (var arg : args) {
			var file = new File(arg);
			if (!file.exists()) {
				throw new CmdLineException("'%1$s' does not exist", arg);
			} if (!file.isFile()) {
				throw new CmdLineException("'%1$s' is not a file", arg);
			} else {
				files.add(file.toPath());
			}
		}
		if (files.isEmpty()) {
			throw new CmdLineException();
		}
	}

	private void run() throws Throwable {
		try (KbInstance kb = new KbInstance(config)) {
			for (var file : files) {
				var destination = new TripleHandler(kb);
				RDFParser.create()
					.source(file)
					.build()
					.parse(destination);
			}
		}
	}
}
