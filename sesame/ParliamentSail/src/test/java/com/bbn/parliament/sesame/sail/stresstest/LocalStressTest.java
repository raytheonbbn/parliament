// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.sesame.sail.stresstest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import org.openrdf.sesame.Sesame;
import org.openrdf.sesame.config.ConfigurationException;
import org.openrdf.sesame.config.RepositoryConfig;
import org.openrdf.sesame.config.SailConfig;
import org.openrdf.sesame.repository.SesameRepository;
import org.openrdf.sesame.repository.local.LocalRepository;

/** @author jlerner */
public class LocalStressTest extends AbstractStressTest
{
	private static final String PROP_DATA_KB_DIR    = "local.data.kb.dir";
	private static final String DEFAULT_DATA_KB_DIR = "createdkb";
	private static final String PROP_TEST_KB_DIR    = "local.test.kb.dir";
	private static final String DEFAULT_TEST_KB_DIR = "testkb";
	private static final String PROP_SAILSTACK      = "local.sailstack";
	private static final String DEFAULT_SAILSTACK   = "com.bbn.parliament.sesame.sail.SyncRdfSchemaRepository;"
		+ "com.bbn.parliament.sesame.sail.RdfSchemaRepository?dir=testkb";

	private LocalRepository     _repository         = null;
	private DecimalFormat       _format             = new DecimalFormat("0.00");
	private File                _kbDir              = null;

	public LocalStressTest(int numThreads, int numThreadLoops,
		int writerPercentage)
	{
		super(numThreads, numThreadLoops, writerPercentage);
	}

	public LocalStressTest(int numThreads, int numThreadLoops,
		int writerPercentage, boolean csv)
	{
		super(numThreads, numThreadLoops, writerPercentage, csv);
	}

	@Override
	protected SesameRepository prepareTestRepository() throws Exception
	{
		createTestKB();
		String testKbDir = getProperties().getProperty(PROP_TEST_KB_DIR,
			DEFAULT_TEST_KB_DIR);
		_kbDir = new File(testKbDir);
		if (!_kbDir.exists())
		{
			_kbDir.mkdir();
		}

		String sailstack = getProperties().getProperty(PROP_SAILSTACK,
			DEFAULT_SAILSTACK);
		String[] sails = sailstack.split(";");
		RepositoryConfig config = new RepositoryConfig("testrepo",
			"Test Repository", true, true);
		for (int i = 0; i < sails.length; i++)
		{
			String classname, params;
			int iPos = sails[i].indexOf('?');
			if (iPos < 0)
			{
				classname = sails[i];
				params = "";
			}
			else
			{
				classname = sails[i].substring(0, iPos);
				params = sails[i].substring(iPos + 1);
			}
			System.out.println("Adding " + classname + " to SAIL stack.");
			SailConfig sailConfig = new SailConfig(classname);
			if (params.length() > 0)
			{
				String[] pairs = params.split("&");
				for (int j = 0; j < pairs.length; j++)
				{
					iPos = pairs[j].indexOf('=');
					String key = pairs[j].substring(0, iPos);
					String value = pairs[j].substring(iPos + 1);
					sailConfig.setParameterValue(key, value);
				}
			}
			config.addSail(sailConfig);
		}
		_repository = null;
		try
		{
			_repository = Sesame.getService().createRepository(config);
		}
		catch (ConfigurationException e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		// generate some notional data we can use for the write operations
		// _resources = new ArrayList();
		// for(int i = 0; i < 50; i++){
		// _resources.add("http://example.org/stresstest#resource" + i);
		// }
		// _properties = new ArrayList();
		// for(int i = 0; i < 10; i++){
		// _properties.add("http://example.org/stresstest#property" + i);
		// }

		// add all of the .owl files in the data directory
		// StringBuffer toAdd = createLinkedRDFStream(_resources, _properties);
		// _repository.addData(toAdd.toString(), "", RDFFormat.NTRIPLES, true,
		// new
		// DummyAdminListener());

		return _repository;

	}

	@Override
	protected void cleanupTestRepository()
	{
		_repository.shutDown();
		if (_kbDir.isDirectory())
		{
			File[] files = _kbDir.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				System.out.println(files[i].getName() + ": "
					+ _format.format(((double) files[i].length() / 1024 / 1024))
					+ "MB");
				if (!files[i].delete())
				{
					System.err
					.println("Error deleting KB files. Some thread probably still has a lock on them.");
				}
			}
		}
	}

	public static void copyFile(File src, File dst) throws IOException
	{
		// make sure the destination file exists
		if (!dst.exists())
		{
			dst.createNewFile();
		}

		try (
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dst);
			) {
			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0)
			{
				out.write(buf, 0, len);
			}
		}
	}

	public void createTestKB() throws Exception
	{
		String createdKbDir = getProperties().getProperty(PROP_DATA_KB_DIR,
			DEFAULT_DATA_KB_DIR);
		File createdKBDir = new File(createdKbDir);
		if (!createdKBDir.exists())
		{
			createdKBDir.mkdir();
		}
		File[] files = createdKBDir.listFiles();
		if (files.length < 4) // need to regenerate the test kb
		{
			for (int i = 0; i < files.length; ++i)
			{
				files[i].delete();
			}
			RepositoryConfig config = new RepositoryConfig("testrepo",
				"Test Repository", true, true);
			SailConfig sailConfig = new SailConfig(
				"com.bbn.parliament.sesame.sail.RdfSchemaRepository");
			sailConfig.setParameterValue("dir", createdKbDir + "/");
			config.addSail(sailConfig);
			sailConfig = new SailConfig(
				"com.bbn.parliament.sesame.sail.SyncRdfSchemaRepository");
			config.stackSail(sailConfig);
			try
			{
				LocalRepository repository
				= Sesame.getService().createRepository(config);
				addData(repository);
				repository.shutDown();
			}
			catch (ConfigurationException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
		// now copy the Parliament files to the test location
		String testKbDir = getProperties().getProperty(PROP_TEST_KB_DIR,
			DEFAULT_TEST_KB_DIR);
		File testDir = new File(testKbDir);
		if (!testDir.exists())
		{
			testDir.mkdir();
		}
		files = createdKBDir.listFiles();
		for (int i = 0; i < files.length; ++i)
		{
			String filename = files[i].getName();
			File destination = new File(testKbDir + File.separator + filename);
			copyFile(files[i], destination);
		}
	}

	public static void usage()
	{
		System.out
		.println("Usage: StressTest <#threads> <#operationsPerThread> <percentWrites>");
		System.exit(0);
	}

	public static void main(String[] args)
	{
		if (args.length < 3)
		{
			usage();
		}
		try
		{
			LocalStressTest st = new LocalStressTest(
				Integer.parseInt(args[0]), Integer.parseInt(args[1]),
				Integer.parseInt(args[2]), true);
			st.doit();
			System.out.println(st.getResults());
		}
		catch (NumberFormatException nfe)
		{
			usage();
		}
	}
}
