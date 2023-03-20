package com.bbn.parliament.ontology_packager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Util {

	private Util() {}		// prevent instantiation

	public static InputStream getRsrcAsInputStream(String rsrcName) throws FileNotFoundException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream is = cl.getResourceAsStream(rsrcName);
		if (is == null) {
			throw new FileNotFoundException(String.format("Unable to find resource '%1$s'%n", rsrcName));
		}
		return is;
	}

	public static String getRsrcAsString(String rsrcName) throws IOException {
		try (
			InputStream is = getRsrcAsInputStream(rsrcName);
			Reader rdr = new InputStreamReader(is, StandardCharsets.UTF_8);
			BufferedReader brdr = new BufferedReader(rdr);
		) {
			return brdr.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	public static Map<String, Set<String>> reverseMap(Map<String, String> map) {
		Map<String, Set<String>> reverseMap = new TreeMap<>();
		map.forEach((k, v) ->
			{ reverseMap.computeIfAbsent(v, key -> new TreeSet<>()).add(k); } );
		return reverseMap;
	}
}
