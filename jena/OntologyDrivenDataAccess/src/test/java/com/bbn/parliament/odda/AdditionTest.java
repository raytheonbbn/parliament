package com.bbn.parliament.odda;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AdditionTest {
	@SuppressWarnings("static-method")
	@Test
	public void testAdd() {
		assertEquals(42, Integer.sum(19, 23));
	}
}
