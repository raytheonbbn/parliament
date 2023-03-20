package com.bbn.parliament.ontology_bundle;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.gradle.api.InvalidUserDataException;

import com.hp.hpl.jena.shared.PrefixMapping;

final class PrefixFileLoader {
	private final File pfxFile;
	private final PrefixMapping declaredPrefixMapping;
	private final Map<String, String> clsNameToNsMap;

	public PrefixFileLoader(File prefixFile) throws IOException {
		pfxFile = prefixFile;

		// Load the properties file:
		Properties p = new Properties();
		try (Reader rdr = Files.newBufferedReader(pfxFile.toPath(), StandardCharsets.UTF_8)) {
			p.load(rdr);
		}

		// Transfer the key-value pairs from the properties into a prefix mapping:
		declaredPrefixMapping = PrefixMapping.Factory.create();
		Map<String, String> prefixToClassNameMap = new HashMap<>();
		p.forEach((k, v) -> {
			String[] valueSplit = v.toString().split(Pattern.quote(","), 2);
			if (valueSplit.length != 2) {
				throw new InvalidUserDataException(String.format(
					"Invalid prefix in '%1$s':%n   '%2$s=%3$s'%n   Should be 'prefix=class-name,namespace'",
					pfxFile.getPath(), k, v));
			}
			String className = valueSplit[0].trim();
			String namespace = valueSplit[1].trim();
			declaredPrefixMapping.setNsPrefix(k.toString(), namespace);
			if (className != null && !className.isEmpty()) {
				prefixToClassNameMap.put(k.toString(), className);
			}
		});

		// Check for namespaces with multiple prefixes:
		requireInvertibleMap(declaredPrefixMapping.getNsPrefixMap(), "prefixes", "namespaces");

		// Check for class names used with multiple prefixes:
		requireInvertibleMap(prefixToClassNameMap, "prefixes", "class names");

		clsNameToNsMap = prefixToClassNameMap.entrySet().stream().collect(Collectors.toMap(
			entry -> entry.getValue(),	// keyMapper
			entry -> declaredPrefixMapping.getNsPrefixURI(entry.getKey()),	// valueMapper
			(ns1, ns2) -> {	// mergeFunction: shouldn't happen based on validation above
				throw new InvalidUserDataException(String.format(
					"Merge collision between namespaces '%1$s' and '%2$s'", ns1, ns2));
			},
			TreeMap::new));	// mapSupplier
	}

	/**
	 * Create the inverse map from values to sets of keys and use it to check for
	 * values with multiple keys.  The labels should be plural.
	 */
	private static void requireInvertibleMap(Map<String, String> map, String keyLabel,
			String valueLabel) {
		Map<String, Set<String>> inverseMap = invertMap(map);
		String errors = inverseMap.entrySet().stream()
			.filter(entry -> entry.getValue().size() > 1)
			.map(entry -> { return String.format("<%1$s>:  %2$s", entry.getKey(),
				entry.getValue().stream().collect(Collectors.joining("', '", "'", "'"))); } )
			.collect(Collectors.joining(String.format("%n   ")));
		if (!errors.isEmpty()) {
			throw new InvalidUserDataException(String.format(
				"The following %2$s have multiple %1$s:%n   %3$s",
				keyLabel, valueLabel, errors));
		}
	}

	/**
	 * Validate the prefixes declared in an input file to the combined ontology. If
	 * the input file passes inspection, then this method has no side effects. If
	 * not, this method will throw an InvalidUserDataException whose message
	 * describes the error(s).
	 *
	 * @param inputFilePrefixMapping The prefix mapping contained in the input file.
	 *                               (After loading the input file into a Model,
	 *                               that model can be passed directly to this
	 *                               parameter.)
	 * @param inputFile              The input file in question.
	 * @throws InvalidUserDataException when the input file does not pass
	 *                                  validation. The exception message describes
	 *                                  the error(s).
	 */
	public void validateInputFilePrefixes(PrefixMapping inputFilePrefixMapping, File inputFile) {
		Map<String, String> declaredPrefixMap = declaredPrefixMapping.getNsPrefixMap();
		Map<String, String> inputFilePrefixMap = inputFilePrefixMapping.getNsPrefixMap();

		// Check for namespaces in inputFile that are not declared in pfxFile:
		String errors = inputFilePrefixMap.entrySet().stream()
			.filter(e -> !declaredPrefixMap.values().contains(e.getValue()))
			.map(e -> String.format("%1$s: <%2$s>", e.getKey(), e.getValue()))
			.collect(Collectors.joining(String.format("%n   ")));
		if (!errors.isEmpty()) {
			throw new InvalidUserDataException(String.format(
				"These namespaces are declared in '%1$s' but missing from '%2$s':%n   %3$s",
				inputFile.getPath(), pfxFile.getPath(), errors));
		}

		// Checks for namespaces declared in both files, but with different prefixes:
		// Create reverse maps from namespaces to prefixes:
		Map<String, Set<String>> reverseInputMap = invertMap(inputFilePrefixMap);
		Map<String, Set<String>> reverseDeclaredMap = invertMap(declaredPrefixMap);
		errors = reverseDeclaredMap.entrySet().stream()
			// Only pay attention to input namespaces that are in pfxFile:
			.filter(e -> reverseInputMap.containsKey(e.getKey()))
			// Ignore the empty prefix in the input model:
			.filter(e -> reverseInputMap.get(e.getKey()).size() != 1 || !reverseInputMap.get(e.getKey()).contains(""))
			// Check for mismatches:
			.filter(e -> !reverseInputMap.get(e.getKey()).containsAll(e.getValue())
				|| !e.getValue().containsAll(reverseInputMap.get(e.getKey())))
			// Toss away the prefixes:
			.map(e -> e.getKey())
			.collect(Collectors.joining(String.format("%n   ")));
		if (!errors.isEmpty()) {
			throw new InvalidUserDataException(String.format(
				"These namespaces have different prefixes in '%1$s' and '%2$s':%n   %3$s",
				inputFile.getPath(), pfxFile.getPath(), errors));
		}
	}

	public void addDeclaredPrefixesTo(PrefixMapping pm) {
		pm.setNsPrefixes(declaredPrefixMapping);
	}

	public Map<String, String> getClsNameToNamespaceMap() {
		return Collections.unmodifiableMap(clsNameToNsMap);
	}

	public static Map<String, Set<String>> invertMap(Map<String, String> map) {
		Map<String, Set<String>> reverseMap = new TreeMap<>();
		map.forEach((k, v) ->
			{ reverseMap.computeIfAbsent(v, key -> new TreeSet<>()).add(k); } );
		return reverseMap;
	}
}
