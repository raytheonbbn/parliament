// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.ontology_bundle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FileUtil {
	private FileUtil() {}	// prevents instantiation

	public static String getFileNameStem(String fileName) {
		int i = fileName.lastIndexOf('.');
		return (i == -1)
			? fileName
			: fileName.substring(0, i);
	}

	public static File getCodeFile(File srcDir, String packageName, String fileName) {
		var relDir = packageName.replaceAll(Pattern.quote("."), Matcher.quoteReplacement("/"));
		var packageDir = new File(srcDir, relDir);
		return new File(packageDir, fileName);
	}

	public static void writeString(File file, String string) {
		file.getParentFile().mkdirs();
		try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
			writer.write(string);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
