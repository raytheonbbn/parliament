package com.bbn.parliament.build_logic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SharedLibFixupScript {
	private static final Pattern MACOS_PATTERN = Pattern.compile("(?m)^([^,]*),([^,]*),([^,]*)$");
	private static final Pattern LINUX_PATTERN = Pattern.compile("(?m)^([^\\n]*)$");

	public static void create(String macOsFixups, String linuxFixups, File fixupScript) throws IOException {
		String replacement = null;
		Matcher matcher = null;
		var os = System.getProperty("os.name").toLowerCase();
		if (os.contains("mac")) {
			// The parenthesis around the callee value cause the evaluation of the
			// glob to an array, and subsequent expansion of callee gets the first
			// (and hopefully only) element in the array:
			replacement = "callee=($2*) ; install_name_tool -change $3\\$callee @loader_path/\\$callee $1*";
			matcher = MACOS_PATTERN.matcher(macOsFixups);
		} else if (os.contains("linux")) {
			replacement = "patchelf --set-rpath \"\\$ORIGIN\" $1*";
			matcher = LINUX_PATTERN.matcher(linuxFixups);
		}

		if (replacement != null) {
			var commands = matcher.replaceAll(replacement);
			try (var pwtr = new PrintWriter(fixupScript, StandardCharsets.UTF_8)) {
				pwtr.println("#!/bin/bash");
				pwtr.println();
				pwtr.println(commands);
			}
		}
	}
}
