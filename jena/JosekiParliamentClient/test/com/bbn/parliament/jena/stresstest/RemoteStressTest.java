// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.stresstest;

import java.net.URL;
import com.bbn.parliament.jena.joseki.client.RemoteModel;

/** @author jlerner */
public class RemoteStressTest extends AbstractStressTest
{
	private static final String PROP_SPARQL_URL       = "remote.server.sparql.url";
	private static final String PROP_BULK_URL         = "remote.server.bulk.url";
	//private static final String WATERMARK_STATEMENT   = "<http://foo> <http://doowacky> <http://bar> .";
	//private static final String WATERMARK_QUERY       = "CONSTRUCT * FROM {<http://foo>} <http://doowacky> {<http://bar>}";

	private RemoteModel _repository;

	public RemoteStressTest(int numThreads, int numThreadLoops,
		int writerPercentage)
	{
		super(numThreads, numThreadLoops, writerPercentage);
	}

	public RemoteStressTest(int numThreads, int numThreadLoops,
		int writerPercentage, boolean csv)
	{
		super(numThreads, numThreadLoops, writerPercentage, csv);
	}

	@Override
	protected void cleanupTestRepository()
	{
		if (getWriterPercentage() > 0)
		{
			try
			{
				System.out.println("Clearing remote repository");
				//_repository.clear(new DummyAdminListener());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("Skipping remote repository clear, since there were no writers.");
		}
	}

	@Override
	protected RemoteModel prepareTestRepository() throws Exception
	{
		URL sparqlUrl = new URL(getProperties().getProperty(PROP_SPARQL_URL));
		URL bulkUrl = new URL(getProperties().getProperty(PROP_BULK_URL));
		_repository = new RemoteModel(sparqlUrl.toString(), bulkUrl.toString());

		return _repository;
	}
}
