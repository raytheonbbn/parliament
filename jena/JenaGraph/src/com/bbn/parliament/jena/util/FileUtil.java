// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author sallen */
public class FileUtil {
	private static Logger log = LoggerFactory.getLogger(FileUtil.class);

	private static final String[] DOS_DEVICE_NAMES = { "AUX", "CLOCK$", "COM1",
		"COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "CON",
		"LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8",
		"LPT9", "NUL", "PRN" };

	private static final String INVALID_URL_CHARS = "*:<>?\\/\"|";

	/**
	 * Encodes a string into a valid file name. We replace any invalid characters
	 * with an underscore.
	 *
	 * @param str a string to encode.
	 * @return A string that can be used as a valid filename.
	 */
	public static String encodeStringForFilename(String str) {
		// Some helpful comments here:
		// http://stackoverflow.com/questions/62771/how-check-if-given-string-is-legal-allowed-file-name-under-windows

		String strippedStr = str.strip();

		// These DOS device names are not allowed
		for (String dos : DOS_DEVICE_NAMES) {
			if (strippedStr.equalsIgnoreCase(dos)) {
				return "_" + strippedStr;
			}
		}

		StringBuilder sb = new StringBuilder(strippedStr.length());
		for (int i = 0; i < strippedStr.length(); ++i) {
			char c = strippedStr.charAt(i);
			int cInt = c;
			if ((cInt >= 0x0 && cInt <= 0x1F) || INVALID_URL_CHARS.indexOf(cInt) >= 0) {
				sb.append('_');
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}

	/**
	 * Delete a file. If the file is a directory, recursively delete all files in
	 * the directory.
	 *
	 * @param f a file to delete.
	 */
	public static boolean delete(File f) {
		boolean success = true;
		if (f.isDirectory()) {
			for (File file : f.listFiles()) {
				success = success && delete(file);
			}
		}

		if (success) {
			if (f.exists()) {
				try {
					Files.delete(f.toPath());
				} catch (IOException ex) {
					log.warn("Error deleting '{}': {}", f, ex.getMessage());
				}
			} else {
				log.trace("Attempted to delete nonexistent file/directory '{}'", f);
			}
		}
		return success;
	}
}
