// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.web.multipart.MultipartFile;

import com.bbn.parliament.server.exception.DataFormatException;
import com.bbn.parliament.server.exception.MissingGraphException;
import com.bbn.parliament.server.exception.TrackableException;
import com.bbn.parliament.server.tracker.TrackableInsert;
import com.bbn.parliament.server.tracker.Tracker;
import com.bbn.parliament.server.util.ConcurrentRequestController;
import com.bbn.parliament.server.util.ConcurrentRequestLock;

public class InsertHandler {
	// Returns the number of statements inserted
	@SuppressWarnings("static-method")
	public long handleRequest(String graphName, String dataFormat, String fileName,
		String requestor, Supplier<InputStream> strmSupplier)
		throws TrackableException, DataFormatException, MissingGraphException, IOException {

		Inserter inserter = Inserter.newGraphInserter(graphName, dataFormat, fileName,
			VerifyOption.VERIFY, null, strmSupplier);
		TrackableInsert ti = Tracker.getInstance().createInsert(List.of(inserter), requestor);

		try (ConcurrentRequestLock lock = ConcurrentRequestController.getWriteLock()) {
			@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
			ti.run();
			return ti.getNumStatements();
		}
	}

	// Returns the number of statements inserted
	@SuppressWarnings("static-method")
	public long handleRequest(String graphName, MultipartFile[] files, String requestor)
		throws TrackableException, DataFormatException, MissingGraphException, IOException {

		/*
		 * We Create all the inserters before we run them so we can (a) throw
		 * DataFormatException (if it's going to be thrown) before we start inserting
		 * data, and (b) perform all the inserts under one lock.
		 */
		List<Inserter> inserters = new ArrayList<>(files.length);
		for (MultipartFile file : files) {
			inserters.add(Inserter.newGraphInserter(graphName, file.getContentType(),
				file.getOriginalFilename(), VerifyOption.VERIFY, null,
				() -> getMultipartInputStream(file)));
		}
		TrackableInsert ti = Tracker.getInstance().createInsert(inserters, requestor);

		try (ConcurrentRequestLock lock = ConcurrentRequestController.getWriteLock()) {
			@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
			ti.run();
			return ti.getNumStatements();
		}
	}

	private static InputStream getMultipartInputStream(MultipartFile file) {
		try {
			return file.getInputStream();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
