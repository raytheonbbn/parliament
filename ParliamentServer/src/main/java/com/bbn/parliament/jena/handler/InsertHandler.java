// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import com.bbn.parliament.jena.bridge.ConcurrentRequestController;
import com.bbn.parliament.jena.bridge.ConcurrentRequestLock;
import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.TrackableInsert;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;

public class InsertHandler {
	// Returns the number of statements inserted
	@SuppressWarnings("static-method")
	public long handleRequest(String graphName, String dataFormat, String fileName,
		String requestor, Supplier<InputStream> strmSupplier)
		throws TrackableException, DataFormatException, MissingGraphException, IOException {

		Inserter inserter = Inserter.newGraphInserter(graphName, dataFormat, fileName,
			VerifyOption.VERIFY, null, strmSupplier);
		TrackableInsert ti = Tracker.getInstance().createInsert(inserter, requestor);

		try (ConcurrentRequestLock lock = ConcurrentRequestController.getWriteLock()) {
			@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
			ti.run();
			return ti.getInserter().getNumStatements();
		}
	}
}
