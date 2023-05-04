// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.client;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.jena.riot.Lang;

/**
 * Enum that specifies the various RDF formats Jena can handle. The .toString() method
 * will return a string that can be passed to Jena methods to specify the RDF format.
 * The class also contains methods to interpret file extensions and media types.
 *
 * @author sallen
 */
public enum RDFFormat {
	/** RDF/XML format */
	RDFXML(true, Lang.RDFXML,
		new String[]{ "RDF/XML", "RDF/XML-ABBREV", "RDFXML", "RDFXML-ABBREV" },
		new String[]{ "rdf", "owl", "xml" },
		new String[]{ "application/rdf+xml" }),

	/** Turtle format */
	TURTLE(true, Lang.TURTLE,
		new String[]{ "Turtle", "TTL" },
		new String[]{ "ttl" },
		new String[]{ "text/turtle", "application/turtle", "application/x-turtle" }),

	/** N-Triples format */
	NTRIPLES(true, Lang.NTRIPLES,
		new String[]{ "N-Triples", "NTriples", "N-Triple", "NTriple", "NT" },
		new String[]{ "nt" },
		new String[]{ "application/n-triples", "text/plain" }),

	/** N3 format */
	N3(true, Lang.N3,
		new String[]{ "N3" },
		new String[]{ "n3" },
		new String[]{ "text/rdf+n3", "text/n3", "application/n3" }),

	/** JSON-LD format */
	JSON_LD(true, Lang.JSONLD,
		new String[]{ "JSON-LD", "JSONLD" },
		new String[]{ "jsonld" },
		new String[]{ "application/ld+json" }),

	/** Zip format */
	ZIP(false, null,
		new String[]{ "ZIP" },
		new String[]{ "zip" },
		new String[]{ "application/zip" }),

	/** Unknown format */
	UNKNOWN(false, Lang.RDFNULL,
		new String[]{ "UNKNOWN" },
		new String[]{},
		new String[]{});

	private final boolean isJenaReadable;
	private final Lang lang;
	private final String[] formatStrList;
	private final String[] fileExtList;
	private final String[] mediaTypeList;

	private RDFFormat(boolean isReadableByJena, Lang jenaLang, String[] formatStrings, String[] fileExtensions, String[] mediaTypes) {
		isJenaReadable = isReadableByJena;
		lang = jenaLang;
		formatStrList = formatStrings;
		fileExtList = fileExtensions;
		mediaTypeList = mediaTypes;
	}

	/**
	 * Parses a string into the proper RDFFormat value. This can handle slightly
	 * non-standard variations, so it is somewhat appropriate for user input.
	 */
	public static RDFFormat parse(String formatStr) {
		RDFFormat result = UNKNOWN;
		formatStr = formatStr.strip();
		outerLoop:
		for (RDFFormat f : RDFFormat.values()) {
			for (String fStr : f.formatStrList) {
				if (fStr.equalsIgnoreCase(formatStr)) {
					result = f;
					break outerLoop;
				}
			}
		}
		return result;
	}

	/**
	 * Parses a string into the proper RDFFormat value, allowing only those possibilities
	 * that Jena can parse. This can handle slightly non-standard variations, so it is
	 * somewhat appropriate for user input.
	 *
	 * @throws IllegalArgumentException If the given string does not represent a format
	 * that Jena will understand.
	 */
	public static RDFFormat parseJenaFormatString(String formatStr) {
		RDFFormat format = RDFFormat.parse(formatStr);
		if (!format.isJenaReadable()) {
			String errMsg = Stream.of(RDFFormat.values())
				.filter((fmt) -> fmt.isJenaReadable())
				.flatMap((fmt) -> Stream.of(fmt.formatStrList))
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.distinct()
				.collect(Collectors.joining("\", \"", "Jena format string must be one of \"", "\"."));
			throw new IllegalArgumentException(errMsg);
		}
		return format;
	}

	/**
	 * Returns the RDFFormat matching the given file extension, or RDFFormat.UNKNOWN if
	 * there is no match.
	 */
	public static RDFFormat parseExtension(String extension) {
		RDFFormat result = UNKNOWN;
		if (extension != null) {
			if (extension.startsWith(".")) {
				extension = extension.substring(1);
			}

			outerLoop:
			for (RDFFormat f : RDFFormat.values()) {
				for (String fExt : f.fileExtList) {
					if (fExt.equalsIgnoreCase(extension)) {
						result = f;
						break outerLoop;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns the RDFFormat matching the file extension of the given Path, or
	 * RDFFormat.UNKNOWN if there is no match.
	 */
	public static RDFFormat parseFilename(Path filename) {
		Path name = filename.getFileName();
		int dotIndex = (name == null) ? -1 : name.toString().lastIndexOf('.');
		@SuppressWarnings("null")
		String ext = (dotIndex == -1) ? "" : name.toString().substring(dotIndex + 1);
		return parseExtension(ext);
	}

	/**
	 * Returns the RDFFormat matching the file extension of the given File, or
	 * RDFFormat.UNKNOWN if there is no match.
	 */
	public static RDFFormat parseFilename(File filename) {
		String name = filename.getName();
		int dotIndex = name.lastIndexOf('.');
		String ext = (dotIndex == -1) ? "" : name.substring(dotIndex + 1);
		return parseExtension(ext);
	}

	/**
	 * Returns the RDFFormat matching the file extension of the given path name, or
	 * RDFFormat.UNKNOWN if there is no match.
	 */
	public static RDFFormat parseFilename(String filename) {
		return parseFilename(new File(filename));
	}

	/**
	 * Returns the RDFFormat matching the file extension of the given zip file entry, or
	 * RDFFormat.UNKNOWN if there is no match.
	 */
	public static RDFFormat parseFilename(ZipEntry zipEntry) {
		return parseFilename(new File(zipEntry.getName()));
	}

	/**
	 * Returns the RDFFormat matching the given media type, or RDFFormat.UNKNOWN if
	 * there is no match.  The argument may contain optional parameters, such as charset.
	 */
	public static RDFFormat parseMediaType(String mediaType) {
		RDFFormat result = UNKNOWN;
		if (mediaType != null) {
			int semiIndex = mediaType.indexOf(';');
			if (semiIndex != -1) {
				mediaType = mediaType.substring(0, semiIndex).strip();
			}

			outerLoop:
			for (RDFFormat f : RDFFormat.values()) {
				for (String mt : f.mediaTypeList) {
					if (mt.equalsIgnoreCase(mediaType)) {
						result = f;
						break outerLoop;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns a filename extension string that matches this RDFFormat, or "txt" for
	 * RDFFormat.UNKNOWN.
	 */
	public String getExtension() {
		return (fileExtList.length == 0) ? null : fileExtList[0];
	}

	/**
	 * Returns a filename extension string that matches this RDFFormat, or "txt" for
	 * RDFFormat.UNKNOWN.
	 */
	public List<String> getExtensions() {
		return List.of(fileExtList);
	}

	/** Returns media type string for this RDFFormat, or null for RDFFormat.UNKNOWN. */
	public String getMediaType() {
		return (mediaTypeList.length == 0) ? null : mediaTypeList[0];
	}

	public List<String> getMediaTypes() {
		return List.of(mediaTypeList);
	}

	public List<String> getFormatStrs() {
		return List.of(formatStrList);
	}

	/** Returns true if this is a format that Jena can parse */
	public boolean isJenaReadable() {
		return isJenaReadable;
	}

	public Lang getLang() {
		return lang;
	}

	/**
	 * Returns a String that is suitable for passing into Jena methods that call for a
	 * <code>lang</code> parameter.
	 */
	@Override
	public String toString() {
		return (formatStrList.length == 0) ? null : formatStrList[0];
	}
}
