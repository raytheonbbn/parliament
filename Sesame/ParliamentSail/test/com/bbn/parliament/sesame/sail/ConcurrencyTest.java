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
import java.net.URL;
import java.util.Properties;

import org.openrdf.sesame.Sesame;
import org.openrdf.sesame.admin.DummyAdminListener;
import org.openrdf.sesame.admin.StdOutAdminListener;
import org.openrdf.sesame.config.AccessDeniedException;
import org.openrdf.sesame.config.ConfigurationException;
import org.openrdf.sesame.config.RepositoryConfig;
import org.openrdf.sesame.config.SailConfig;
import org.openrdf.sesame.constants.RDFFormat;
import org.openrdf.sesame.repository.local.LocalRepository;
import org.openrdf.sesame.sail.Sail;
import org.openrdf.sesame.sail.StackedSail;

import junit.framework.TestCase;

/**
 * @author jlerner
 */
public class ConcurrencyTest extends TestCase
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

	LocalRepository           _repository;
	private WaitingSchemaSail _waitSail;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		File kbDir = new File("TestKb");
		if (!kbDir.exists())
		{
			kbDir.mkdir();
		}
		RepositoryConfig config = new RepositoryConfig("testrepo",
			"Test Repository", true, true);
		SailConfig sailConfig = new SailConfig(
			"com.bbn.parliament.sesame.sail.RdfSchemaRepository");
		sailConfig.setParameterValue("dir", "TestKb/");
		config.addSail(sailConfig);
		sailConfig = new SailConfig(
			"com.bbn.parliament.sesame.sail.WaitingSchemaSail");
		config.stackSail(sailConfig);
		sailConfig = new SailConfig(
			"com.bbn.parliament.sesame.sail.SyncRdfSchemaRepository");
		config.stackSail(sailConfig);
		try
		{
			_repository = Sesame.getService().createRepository(config);
		}
		catch (ConfigurationException e)
		{
			fail("Error configuring test repository: " + e.getMessage());
		}

		Sail sail = _repository.getSail();
		while (sail != null)
		{
			if (sail instanceof WaitingSchemaSail)
			{
				_waitSail = (WaitingSchemaSail) sail;
			}

			if (sail instanceof StackedSail)
			{
				sail = ((StackedSail) sail).getBaseSail();
			}
			else
			{
				sail = null;
			}
		}

		_repository.addData(new URL(
			"http://owl-eclipse.projects.semwebcentral.org/owl/vehicle-ont"), "",
			RDFFormat.RDFXML, true, new DummyAdminListener());
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();

		_repository.shutDown();

		File kbDir = new File("TestKb/");
		if (kbDir.isDirectory())
		{
			File[] files = kbDir.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				if (!files[i].delete())
				{
					System.out
					.println("Error deleting KB files.  Some thread probably still has a lock on them.");
				}
			}
		}
	}

	class RDFExtractor implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				_repository.extractRDF(RDFFormat.RDFXML, true, true, false, true);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (AccessDeniedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void testConcurrentReads() throws InterruptedException
	{
		Thread blocked = new Thread(new RDFExtractor(), "blocked");

		Thread notBlocked = new Thread(new RDFExtractor(), "notBlocked");

		WaitingSchemaSail.Wait wait = new WaitingSchemaSail.Wait();
		_waitSail.setWaitObject(wait);
		blocked.start();
		blocked.join(1000);
		assertTrue(blocked.isAlive());
		_waitSail.setWaitObject(null);
		notBlocked.start();
		notBlocked.join(30000);
		assertTrue(blocked.isAlive());
		// Can't fail with an assert like a normal unit test because that would
		// cause tearDown to be called, which would deadlock the unit tests due
		// to the other threads that are blocked on the repository.
		boolean failure = notBlocked.isAlive();
		synchronized (wait)
		{
			wait.stopWaiting();
			wait.notifyAll();
		}
		// Let both threads finish before reacting to a possible earlier
		// failure, because again with the deadlock.
		blocked.join();
		notBlocked.join();
		if (failure)
		{
			fail("Concurrent read failed.");
		}
		assertFalse(blocked.isAlive());
	}

	class StatementAdder implements Runnable
	{
		String _toAdd;

		public StatementAdder(String subject, String predicate, String object)
		{
			_toAdd = subject + " " + predicate + " " + object + " .";
		}

		@Override
		public void run()
		{
			try
			{
				_repository.addData(_toAdd, "", RDFFormat.NTRIPLES, true,
					new StdOutAdminListener());
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (AccessDeniedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void testConcurrentWrites() throws InterruptedException
	{
		Thread first = new Thread(new StatementAdder(
			"<http://example.org/test#Something>",
			"<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
			"<http://example.org/test#Foo>"), "first");
		Thread second = new Thread(new StatementAdder(
			"<http://example.org/test#Something>",
			"<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
			"<http://example.org/test#Bar>"), "second");

		WaitingSchemaSail.Wait wait = new WaitingSchemaSail.Wait();
		_waitSail.setWaitObject(wait);
		first.start();
		first.join(500);
		assertTrue(first.isAlive());
		_waitSail.setWaitObject(null);
		second.start();
		second.join(5000);
		assertTrue(second.isAlive());
		assertTrue(first.isAlive());
		synchronized (wait)
		{
			wait.stopWaiting();
			wait.notifyAll();
		}
		first.join(30000);
		assertFalse(first.isAlive());
		second.join(30000);
		assertFalse(second.isAlive());
	}

	public void testReadDuringWrite() throws InterruptedException
	{
		Thread write = new Thread(new StatementAdder(
			"<http://example.org/test#Something>",
			"<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
			"<http://example.org/test#Foo>"), "write");

		Thread read = new Thread(new RDFExtractor(), "read");

		WaitingSchemaSail.Wait wait = new WaitingSchemaSail.Wait();
		_waitSail.setWaitObject(wait);
		write.start();
		write.join(500);
		assertTrue(write.isAlive());
		_waitSail.setWaitObject(null);
		read.start();
		read.join(30000);
		assertTrue(read.isAlive());
		assertTrue(write.isAlive());
		synchronized (wait)
		{
			wait.stopWaiting();
			wait.notifyAll();
		}
		write.join(30000);
		read.join(30000);
		assertFalse(write.isAlive());
		assertFalse(read.isAlive());
	}

	public void testWriteDuringRead() throws InterruptedException
	{
		Thread write = new Thread(new StatementAdder(
			"<http://example.org/test#Something>",
			"<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
			"<http://example.org/test#Foo>"), "write");

		Thread read = new Thread(new RDFExtractor(), "read");

		WaitingSchemaSail.Wait wait = new WaitingSchemaSail.Wait();
		_waitSail.setWaitObject(wait);
		read.start();
		read.join(500);
		assertTrue(read.isAlive());
		_waitSail.setWaitObject(null);
		write.start();
		write.join(30000);
		assertTrue(write.isAlive());
		assertTrue(read.isAlive());
		synchronized (wait)
		{
			wait.stopWaiting();
			wait.notifyAll();
		}
		read.join(30000);
		write.join(30000);
		assertFalse(read.isAlive());
		assertFalse(write.isAlive());
	}
}
