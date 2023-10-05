package org.semwebcentral.parliament.odda;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AdditionTest {
	@SuppressWarnings("static-method")
	@Test
	public void testAdd() {
		assertEquals(42, Integer.sum(19, 23));
	}
}
