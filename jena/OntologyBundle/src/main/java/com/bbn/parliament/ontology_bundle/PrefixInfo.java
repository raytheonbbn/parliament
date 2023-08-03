package com.bbn.parliament.ontology_bundle;

import java.util.regex.Pattern;

import org.gradle.api.InvalidUserDataException;

public record PrefixInfo(String prefix, String className, String namespace) {
	public static PrefixInfo fromConfigurationLine(String prefixConfigurationLine) {
		String[] lineSplit = prefixConfigurationLine.split(Pattern.quote(","), 3);
		if (lineSplit.length != 3) {
			throw new InvalidUserDataException(
				"Invalid prefix: '%1$s'%n   Should be 'prefix, className, namespace'"
					.formatted(prefixConfigurationLine));
		}
		return new PrefixInfo(
			lineSplit[0].strip(),
			lineSplit[1].strip(),
			lineSplit[2].strip());
	}
}
