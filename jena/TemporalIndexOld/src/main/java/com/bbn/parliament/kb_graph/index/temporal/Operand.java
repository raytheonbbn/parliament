// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.kb_graph.index.temporal;

/** @author dkolas */
public enum Operand {
	INTERVAL_OVERLAPS("intervalOverlaps", ExtentTester.INTERVAL_OVERLAPS),
	INTERVAL_BEFORE("intervalBefore", ExtentTester.INTERVAL_BEFORE),
	INTERVAL_AFTER("intervalAfter", ExtentTester.INTERVAL_AFTER),
	INTERVAL_STARTS_BEFORE("intervalStartsBefore", ExtentTester.INTERVAL_STARTS_BEFORE),
	INTERVAL_FINISHES_AFTER("intervalFinishesAfter", ExtentTester.INTERVAL_FINISHES_AFTER),
	INTERVAL_EQUALS("intervalEquals", ExtentTester.EQUALS),
	INTERVAL_MEETS("intervalMeets", ExtentTester.INTERVAL_MEETS),
	INTERVAL_STARTS("intervalStarts", ExtentTester.INTERVAL_STARTS),
	INTERVAL_DURING("intervalDuring", ExtentTester.INTERVAL_DURING),
	INTERVAL_FINISHES("intervalFinishes", ExtentTester.INTERVAL_FINISHES),
	INTERVAL_MET_BY("intervalMetBy", ExtentTester.INTERVAL_MET_BY),
	INTERVAL_OVERLAPPED_BY("intervalOverlappedBy", ExtentTester.INTERVAL_OVERLAPPED_BY),
	INTERVAL_STARTED_BY("intervalStartedBy", ExtentTester.INTERVAL_STARTED_BY),
	INTERVAL_CONTAINS("intervalContains", ExtentTester.INTERVAL_CONTAINS),
	INTERVAL_FINISHED_BY("intervalFinishedBy", ExtentTester.INTERVAL_FINISHED_BY);

	private String _uri;
	private ExtentTester _extentTester;

	private Operand(String relativeUri, ExtentTester extentTester) {
		_uri = Constants.TIME_NS + relativeUri;
		_extentTester = extentTester;
	}

	public String getUri() {
		return _uri;
	}

	public ExtentTester getExtentTester() {
		return _extentTester;
	}

	public static Operand valueOfUri(String uri) {
		for (Operand op : values()) {
			if (op.getUri().equals(uri)) {
				return op;
			}
		}
		throw new IllegalArgumentException("'%1$s' is not a valid Operand URI".formatted(uri));
	}
}

