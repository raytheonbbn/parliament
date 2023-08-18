// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.ontology_bundle;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import org.gradle.api.InvalidUserDataException;

public record PrefixInfo(String prefix, String className, String namespace) {
	public static PrefixInfo fromConfigurationLine(String prefixConfigurationLine) {
		String[] lineSplit = prefixConfigurationLine.split(Pattern.quote(","), 3);
		if (lineSplit.length != 3) {
			throw new InvalidUserDataException(
				"Invalid prefix entry: '%1$s'%n   Should be 'prefix, className, namespace'"
					.formatted(prefixConfigurationLine));
		}
		var prefix = lineSplit[0].strip();
		var className = lineSplit[1].strip();
		var namespace = lineSplit[2].strip();

		if (prefix.isEmpty()) {
			System.out.format("Warning: Your declared prefixes assign the empty prefix to"
				+ " namespace '%1$s'. The empty prefix is not recommended. (See the %2$s"
				+ " extension of your gradle build script.)%n",
				namespace, OntologyBundlePlugin.EXT_NAME);
		}

		if (namespace.isEmpty()) {
			throw new InvalidUserDataException(("Blank namespace for prefix '%1$s:' declared"
				+ " in the %2$s extension of your gradle build script")
					.formatted(prefix, OntologyBundlePlugin.EXT_NAME));
		}

		try {
			@SuppressWarnings("unused")
			var nsUri = new URI(namespace);
		} catch (NullPointerException | URISyntaxException ex) {
			throw new InvalidUserDataException(("Invalid namespace '%1$s' declared under"
				+ " prefixes in the %2$s extension of your gradle build script: %3$s")
					.formatted(namespace, OntologyBundlePlugin.EXT_NAME, ex.getMessage()));
		}

		return new PrefixInfo(prefix, className, namespace);
	}
}
