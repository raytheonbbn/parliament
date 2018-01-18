package com.bbn.parliament.jena.graph.index.numeric;

/**
 * Constants used by the {@link NumericIndex}.
 *
 * @author rbattle
 */
public class Constants {
	/** The type of number to index. */
	public static enum NumberType {
		Integer, Long, Double, Float
	}

	/** The type of number to index property key. */
	public static final String NUMBER_TYPE = "type";

	/** The predicate to index property key. */
	public static final String PROPERTY = "property";
}
