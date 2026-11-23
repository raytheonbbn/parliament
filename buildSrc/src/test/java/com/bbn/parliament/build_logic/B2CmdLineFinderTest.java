package com.bbn.parliament.build_logic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class B2CmdLineFinderTest {
	@Test
	public void test() throws IOException {
		var cmdLine = B2CmdLineFinder.getB2CommandLine(
			new File("../build.properties"), new File("../build.properties.default"));
		assertTrue(cmdLine[0].equals("b2"));
		assertTrue(cmdLine[1].equals("-q"));
	}
}
