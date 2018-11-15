// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.sesame.sail.stresstest;

import java.net.URL;

import org.openrdf.model.Graph;
import org.openrdf.sesame.Sesame;
import org.openrdf.sesame.admin.DummyAdminListener;
import org.openrdf.sesame.constants.QueryLanguage;
import org.openrdf.sesame.constants.RDFFormat;
import org.openrdf.sesame.repository.SesameRepository;
import org.openrdf.sesame.repository.remote.HTTPRepository;
import org.openrdf.sesame.sail.StatementIterator;

/** @author jlerner */
public class RemoteStressTest extends AbstractStressTest
{
	private static final String PROP_SESAME_URL         = "remote.server.url";
	private static final String PROP_REPOSITORY_NAME    = "remote.repository.name";
	private static final String DEFAULT_REPOSITORY_NAME = "testrepo";
	private static final String WATERMARK_STATEMENT     = "<http://foo> <http://doowacky> <http://bar> .";
	private static final String WATERMARK_QUERY         = "CONSTRUCT * FROM {<http://foo>} <http://doowacky> {<http://bar>}";

	private HTTPRepository _repository;

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
				_repository.clear(new DummyAdminListener());
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
	protected SesameRepository prepareTestRepository() throws Exception
	{
		URL url = new URL(getProperties().getProperty(PROP_SESAME_URL));
		String repo = getProperties().getProperty(PROP_REPOSITORY_NAME,
			DEFAULT_REPOSITORY_NAME);
		_repository = (HTTPRepository) Sesame.getService(url).getRepository(repo);

		// Check for our watermark property; if it's not there, assume the repo
		// has been cleared and must be reloaded.
		Graph results = _repository.performGraphQuery(QueryLanguage.SERQL,
			WATERMARK_QUERY);
		StatementIterator si = results.getStatements();
		try
		{
			if (si.hasNext())
			{
				System.out.println("Watermark statement found, skipping remote repository load.");
			}
			else
			{
				System.out
				.println("Watermark statement missing, reloading remote repository");
				_repository.addData(WATERMARK_STATEMENT, null, RDFFormat.NTRIPLES,
					false, new DummyAdminListener());
				addData(_repository);
			}
		}
		finally
		{
			si.close();
		}
		return _repository;
	}
}
