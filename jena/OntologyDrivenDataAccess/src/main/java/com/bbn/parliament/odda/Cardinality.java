package com.bbn.parliament.odda;

public class Cardinality {
	public final long min;
	public final long max;

	public Cardinality(long min, long max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public String toString() {
		String fmtStr = (max == Long.MAX_VALUE)
			? "%1$d..*"
			: "%1$d..%2$d";
		return String.format(fmtStr, min, max);
	}

	public static Cardinality intersection(Cardinality c1, Cardinality c2) {
		return intersection(c1, c2.min, c2.max);
	}

	public static Cardinality intersection(Cardinality c1, long min, long max) {
		return new Cardinality(Math.max(c1.min, min), Math.min(c1.max, max));
	}

	public static Cardinality defaultCard() {
		return new Cardinality(0, Long.MAX_VALUE);
	}
}
