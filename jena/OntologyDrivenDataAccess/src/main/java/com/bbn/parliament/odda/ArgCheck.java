package com.bbn.parliament.odda;

import java.util.Collection;
import java.util.Map;

public class ArgCheck {
	public static <T> T throwIfNull(T arg, String argName) {
		if (arg == null) {
			throw new IllegalArgumentException(
				String.format("'%1$s' must not be null", argName));
		}
		return arg;
	}

	public static String throwIfNullOrEmpty(String arg, String argName) {
		if (arg == null || arg.isEmpty()) {
			throw new IllegalArgumentException(
				String.format("'%1$s' must not be null or empty", argName));
		}
		return arg;
	}

	public static <T extends Collection<?>> T throwIfNullOrEmpty(T arg, String argName) {
		if (arg == null || arg.isEmpty()) {
			throw new IllegalArgumentException(
				String.format("'%1$s' must not be null or empty", argName));
		}
		return arg;
	}

	public static <T extends Map<?, ?>> T throwIfNullOrEmpty(T arg, String argName) {
		if (arg == null || arg.isEmpty()) {
			throw new IllegalArgumentException(
				String.format("'%1$s' must not be null or empty", argName));
		}
		return arg;
	}
}
