// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.stresstest;

import java.net.URI;
import java.net.URISyntaxException;

import com.bbn.parliament.client.RemoteModel;

/** @author jlerner */
public class RemoteStressTest extends AbstractStressTest {
	private static final String PROP_SPARQL_URL = "remote.server.sparql.url";
	private static final String PROP_BULK_URL   = "remote.server.bulk.url";

	private RemoteModel _repository;

	public RemoteStressTest(int numThreads, int numThreadLoops, int writerPercentage) {
		super(numThreads, numThreadLoops, writerPercentage);
	}

	public RemoteStressTest(int numThreads, int numThreadLoops, int writerPercentage,
		boolean csv) {

		super(numThreads, numThreadLoops, writerPercentage, csv);
	}

	@Override
	protected void cleanupTestRepository() {
		if (getWriterPercentage() > 0) {
			try {
				System.out.println("Clearing remote repository");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Skipping remote repository clear, since there were no writers.");
		}
	}

	@Override
	protected RemoteModel prepareTestRepository() throws URISyntaxException {
		var sparqlUrl = new URI(getProperties().getProperty(PROP_SPARQL_URL));
		var bulkUrl = new URI(getProperties().getProperty(PROP_BULK_URL));
		_repository = new RemoteModel(sparqlUrl.toString(), bulkUrl.toString());

		return _repository;
	}
}
