// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.ontology_bundle;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.shared.PrefixMapping;
import org.gradle.api.InvalidUserDataException;

final class PrefixFileLoader {
	private final PrefixMapping declaredPrefixMapping;
	private final Map<String, String> clsNameToNsMap;
	private final Map<String, String> nsToPfxMap;

	public PrefixFileLoader(List<String> prefixes) {
		var prefixInfoList = prefixes.stream()
			.map(PrefixInfo::fromConfigurationLine)
			.collect(Collectors.toUnmodifiableList());

		validateUniqueness(prefixInfoList, "RDF prefixes", PrefixInfo::prefix);
		validateUniqueness(prefixInfoList, "class names", PrefixInfo::className);
		validateUniqueness(prefixInfoList, "namespaces", PrefixInfo::namespace);

		declaredPrefixMapping = PrefixMapping.Factory.create().setNsPrefixes(
			prefixInfoList.stream()
				.collect(Collectors.toMap(PrefixInfo::prefix, PrefixInfo::namespace)));

		clsNameToNsMap = Collections.unmodifiableMap(
			prefixInfoList.stream()
				.filter(pfxInfo -> !pfxInfo.className().isEmpty())
				.collect(Collectors.toMap(
					PrefixInfo::className,	// key mapper
					PrefixInfo::namespace,	// value mapper
					(ns1, ns2) -> ns1,		// merge function: never happens based on validation above
					TreeMap::new)));			// map supplier

		nsToPfxMap = Collections.unmodifiableMap(
			prefixInfoList.stream()
				.collect(Collectors.toMap(PrefixInfo::namespace, PrefixInfo::prefix)));
	}

	private static void validateUniqueness(List<PrefixInfo> prefixInfoList, String itemLabel,
			Function<PrefixInfo, String> itemMapper) {
		var multiDefinedItems = prefixInfoList.stream()
			.collect(Collectors.groupingBy(itemMapper))
			.values().stream()
			.filter(pfxInfoList -> pfxInfoList.size() > 1)
			.map(pfxInfoList -> itemMapper.apply(pfxInfoList.get(0)))
			.collect(Collectors.joining(", "));
		if (multiDefinedItems.length() > 0) {
			throw new InvalidUserDataException(
				"These %1$s occur multiple times in the 'prefixes' configuration element: %2$s"
					.formatted(itemLabel, multiDefinedItems));
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
		Set<String> declaredNamespaces = new HashSet<>(
			declaredPrefixMapping.getNsPrefixMap().values());

		// Check for namespaces in inputFile that are not declared in prefixes:
		String errors = inputFilePrefixMapping.getNsPrefixMap().entrySet().stream()
			.filter(e -> !declaredNamespaces.contains(e.getValue()))
			.map(e -> "%1$s: <%2$s>".formatted(e.getKey(), e.getValue()))
			.collect(Collectors.joining("%n   ".formatted()));
		if (!errors.isEmpty()) {
			throw new InvalidUserDataException(("These namespaces are declared in '%1$s' but"
				+ " missing from the 'prefixes' configuration element:%n   %2$s")
					.formatted(inputFile.getPath(), errors));
		}
	}

	/**
	 * Modify the given prefix mapping so that it uses the declared prefixes.
	 *
	 * @param inputFilePrefixMapping The prefix mapping to modify. (After loading
	 *                               the input file into a Model, that model can be
	 *                               passed directly to this parameter.)
	 */
	public void switchToPreferredPrefixes(PrefixMapping inputFilePrefixMapping, File inputFile) {
		// Collect the (input, preferred) prefix pairs that don't match:
		var prefixesToRemap = inputFilePrefixMapping.getNsPrefixMap().entrySet().stream()
			.map(entry -> Pair.of(entry.getKey(), nsToPfxMap.get(entry.getValue())))
			.filter(pair -> !Objects.equals(pair.getLeft(), pair.getRight()))
			.toList();

		// We convert the stream above to a list and then start a new stream below
		// so that we don't modify a collection while iterating over it.

		// Now re-map them:
		prefixesToRemap.forEach(pair -> {
			var inputPrefix = pair.getLeft();
			var preferredPrefix = pair.getRight();
			var namespace = declaredPrefixMapping.getNsPrefixURI(preferredPrefix);
			inputFilePrefixMapping.removeNsPrefix(inputPrefix);
			inputFilePrefixMapping.setNsPrefix(preferredPrefix, namespace);
			System.out.format("Replaced prefix '%1$s:' from input file %2$s with preferred"
				+ " prefix '%3$s:'.%n",
				inputPrefix, inputFile.getPath(), preferredPrefix);
		});
	}

	public void addDeclaredPrefixesTo(PrefixMapping pm) {
		pm.setNsPrefixes(declaredPrefixMapping);
	}

	public Map<String, String> getClsNameToNamespaceMap() {
		return clsNameToNsMap;
	}
}
