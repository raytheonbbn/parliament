package com.bbn.parliament.build_logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Test;

public class UncPathMapperTest {
	private static final String NET_USE_OUTPUT = """
		New connections will be remembered.


		Status       Local     Remote                    Network

		-------------------------------------------------------------------------------
		OK           K:        \\\\vm-host\\iemmons         Microsoft Windows Network
		OK           O:        \\\\vm-host\\opt             Microsoft Windows Network
		The command completed successfully.


		""";
	private static final String KB_CORE_UNC_PATH = "//vm-host/iemmons/dev/kb/Parliament/KbCore";
	private static final String KB_CORE_MAPPED_PATH = "K:/dev/kb/Parliament/KbCore";

	private static List<String> parseIntoLines(String input) throws IOException {
		try (var rdr = new StringReader(input)) {
			return rdr.readAllLines();
		}
	}

	@Test
	public void testUncPathMapper() throws IOException {
		var lines = parseIntoLines(NET_USE_OUTPUT);
		var mappedPath = UncPathMapper.findUncDMapping(lines, KB_CORE_UNC_PATH);
		assertEquals(KB_CORE_MAPPED_PATH, mappedPath);
	}
}
