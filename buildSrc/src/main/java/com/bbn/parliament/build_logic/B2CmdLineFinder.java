package com.bbn.parliament.build_logic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class B2CmdLineFinder {
	private static final Pattern PATTERN = Pattern.compile("^nativeBuildParams[ \t]*=(.*)$");

	private static String getToolSet() {
		var os = System.getProperty("os.name").toLowerCase();
		if (os.contains("windows")) {
			return "msvc";
		} else if (os.contains("mac")) {
			return "clang";
		} else {
			return "gcc";
		}
	}

	public static String[] getB2CommandLine(File buildPropsFile, File defaultBuildPropsFile) throws IOException {
		var toolSet = getToolSet();
		var propsFile = buildPropsFile.exists() ? buildPropsFile : defaultBuildPropsFile;
		List<String[]> paramsList = Files.lines(propsFile.toPath(), StandardCharsets.UTF_8)
				.map(String::strip)
				.map(PATTERN::matcher)
				.filter(Matcher::matches)
				.map(m -> m.group(1))
				.map(String::strip)
				.filter(str -> str.contains(toolSet))
				.map(str -> str.split(" +"))
				.toList();
		if (paramsList.size() < 1) {
			throw new IllegalStateException("Unable to find a build parameter set in $propsFile");
		} else if (paramsList.size() > 1) {
			System.out.println("Warning: Multiple build parameter sets are active in $propsFile");
		}
		var params = new ArrayList<String>();
		params.add("b2");
		params.add("-q");
		params.addAll(List.of(paramsList.getFirst()));
		return params.toArray(new String[0]);
	}
}
