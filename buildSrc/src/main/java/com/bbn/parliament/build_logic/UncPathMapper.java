package com.bbn.parliament.build_logic;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UncPathMapper {
	// Pattern for lines that look like this:
	//OK           K:        \\vm-host\iemmons         Microsoft Windows Network
	static final Pattern PATTERN = Pattern.compile("^OK +([A-Z]:) +([^ ]+) +Microsoft Windows Network$");

	private static List<String> getNetUseOutputLines() throws IOException {
		var process = new ProcessBuilder("net", "use")
			.redirectErrorStream(true)
			.start();
		try (
			var is = process.getInputStream();
			var rdr = new InputStreamReader(is, StandardCharsets.UTF_8);
		) {
			return rdr.readAllLines();
		}
	}

	private static String normalizeUncPath(String uncPath, boolean ensureEndsWithSlash) {
		Objects.requireNonNull(uncPath, "uncPath");
		if (uncPath.isBlank()) {
			throw new IllegalArgumentException("uncPath is blank");
		}
		uncPath = uncPath.replace("\\", "/");
		if (ensureEndsWithSlash && !uncPath.endsWith("/")) {
			uncPath += "/";
		}
		return uncPath;
	}

	static String findUncDMapping(List<String> netUseOutputLines, String normalizedUncPath) {
		Map<String, String> driveMap = netUseOutputLines.stream()
			.map(PATTERN::matcher)
			.filter(Matcher::matches)
			.collect(Collectors.toMap(
				m -> m.group(1),															// key mapper
				m -> normalizeUncPath(m.group(2), true),									// value mapper
				(m1, m2) -> { throw new IllegalStateException("Should never happen"); },	// merge function
				TreeMap::new));																// supplier
		var mapping = driveMap.entrySet().stream()
			.filter(e -> normalizedUncPath.startsWith(e.getValue()))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException(
				"No drive letter mapping includes the UNC path '%1$s'".formatted(normalizedUncPath)));
		var slashIndex = mapping.getValue().length();
		return mapping.getKey() + normalizedUncPath.substring(slashIndex - 1);
	}

	public static String mapUncPath(String uncPath) throws IOException {
		var normalizedUncPath = normalizeUncPath(uncPath, false);
		var os = System.getProperty("os.name").toLowerCase();
		if (!os.contains("windows") || !normalizedUncPath.startsWith("//")) {
			return normalizedUncPath;
		} else {
			return findUncDMapping(getNetUseOutputLines(), normalizedUncPath);
		}
	}
}
