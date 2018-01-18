// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.sesame.sail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.openrdf.sesame.Sesame;
import org.openrdf.sesame.config.ConfigurationException;
import org.openrdf.sesame.config.RepositoryConfig;
import org.openrdf.sesame.config.SailConfig;
import org.openrdf.sesame.repository.RepositoryTest;
import org.openrdf.sesame.repository.SesameRepository;
import org.openrdf.sesame.repository.local.LocalRepository;

/** @author jlerner */
public class HighLevelSailTest extends RepositoryTest
{
	static
	{
		try
		{
			// Force loading of BerkeleyDB and Parliament DLLs from
			// the location specified in build.properties. This
			// should short-circuit the System.loadLibrary call
			// in KbInstance and allow the SAIL to operate for
			// the tests.
			Properties p = System.getProperties();
			try (InputStream in = new FileInputStream("build.properties")) {
				p.load(in);
			}
			String kbBinDir = p.getProperty("parliament.bin.dir");
			if (kbBinDir.startsWith("/"))
			{
				kbBinDir = "c:" + kbBinDir;
			}
			System.load(kbBinDir + "/libdb47.dll");
			System.load(kbBinDir + "/Parliament.dll");
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public HighLevelSailTest(String name)
	{
		super(name);
	}

	@Override
	protected SesameRepository _createRepository()
	{
		File kbDir = new File("TestKb");
		if (!kbDir.exists())
		{
			kbDir.mkdir();
		}
		RepositoryConfig config = new RepositoryConfig("testrepo",
			"Test Repository", true, true);
		SailConfig sail = new SailConfig(
			"com.bbn.parliament.sesame.sail.RdfSchemaRepository");
		sail.setParameterValue("dir", "TestKb/");
		config.addSail(sail);
		sail = new SailConfig(
			"com.bbn.parliament.sesame.sail.SyncRdfSchemaRepository");
		config.stackSail(sail);

		try
		{
			return Sesame.getService().createRepository(config);
		}
		catch (ConfigurationException e)
		{
			fail("Error configuring test repository: " + e.getMessage());
		}

		return null;
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();

		((LocalRepository) _repository).shutDown();

		File kbDir = new File("TestKb");
		if (kbDir.isDirectory())
		{
			File[] files = kbDir.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				files[i].delete();
			}
		}
	}

	// These four tests seem to have already been failing on the Parliament SAIL,
	// before any of the KbInstance refactoring or concurrency fixes. These
	// overrides should probalby be commented out at some point so the underlying
	// problems can be fixed; for now, I just want to keep track of which
	// failures
	// I'm expecting in case the other 12 tests break.

	@Override
	public void testAddGraphQuery()
	{
		fail("Expected failure.  This should get fixed, eventually.");
	}

	@Override
	public void testMergeGraph()
	{
		fail("Expected failure.  This should get fixed, eventually.");
	}

	@Override
	public void testRemoveBNodes()
	{
		fail("Expected failure.  This should get fixed, eventually.");
	}

	@Override
	public void testUnicodeQuery()
	{
		fail("Expected failure.  This should get fixed, eventually.");
	}
}
