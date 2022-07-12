// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.sesame.sail.stresstest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;

import org.openrdf.model.Graph;
import org.openrdf.model.Statement;
import org.openrdf.sesame.admin.DummyAdminListener;
import org.openrdf.sesame.config.AccessDeniedException;
import org.openrdf.sesame.constants.QueryLanguage;
import org.openrdf.sesame.constants.RDFFormat;
import org.openrdf.sesame.repository.SesameRepository;
import org.openrdf.sesame.sail.StatementIterator;

/** @author jlerner */
public abstract class AbstractStressTest
{
	protected static final String DEFAULT_DATA_DIR              = "data";
	protected static final String PROP_DATA_DIR                 = "data.dir";
	protected static final String PROP_TEST_PARAM_COUNT         = "test.parameter.count";
	protected static final String DEFAULT_TEST_PARAM_COUNT      = "0";
	protected static final String PROP_TEST_QUERY_COUNT         = "test.query.count";
	protected static final String DEFAULT_TEST_QUERY_COUNT      = "0";
	protected static final String PROP_FILE                     = "StressTest.properties";
	protected static final String PROP_SLEEP_SHORT              = "sleep.short";
	protected static final String DEFAULT_SLEEP_SHORT           = "0";
	protected static final String PROP_SLEEP_SHORT_DEVIATION    = "sleep.short.deviation";
	protected static final String DEFAULT_SLEEP_SHORT_DEVIATION = "0";
	protected static final String PROP_SLEEP_LONG               = "sleep.long";
	protected static final String DEFAULT_SLEEP_LONG            = "0";
	protected static final String PROP_SLEEP_LONG_DEVIATION     = "sleep.long.deviation";
	protected static final String DEFAULT_SLEEP_LONG_DEVIATION  = "0";
	protected static final String PROP_SLEEP_LONG_PERCENTAGE    = "sleep.long.percentage";
	protected static final String DEFAULT_SLEEP_LONG_PERCENTAGE = "0";

	private static final boolean  DEBUG                         = false;
	private static final Random   _rand                         = new Random();
	List<String>                  _resources                    = new ArrayList<>();
	List<String>                  _properties                   = new ArrayList<>();
	private Barrier               _barrier                      = new Barrier();
	private int                   _numThreads;
	private int                   _numThreadLoops;
	private double                _averageResponseTime;
	private long                  _totalElapsedTime;
	private boolean               _csv                          = false;
	private int                   _writerPercentage;
	private Properties            _props;
	String[]                      _queries;
	private Map<String, String>   _parameters                   = new HashMap<>();
	private int                   _sleepShort;
	private int                   _sleepLong;
	private int                   _sleepShortDeviation;
	private int                   _sleepLongDeviation;
	private int                   _sleepLongPercentage;

	/**
	 * Returns a random sleep time based on the various sleep.* values defined in
	 * StressTest.properties. If both the short and long sleep times are zero, it
	 * is taken as a special case and a negative value is returned, indicating
	 * that threads should not sleep between operations.
	 *
	 * @return A random sleep time.
	 */
	int getSleepTime()
	{
		if (_sleepShort == 0 && _sleepLong == 0)
		{
			return -1;
		}

		if (_rand.nextInt(100) < _sleepLongPercentage)
		{
			int deviation = _rand.nextInt(2 * _sleepLongDeviation)
				- _sleepLongDeviation;
			return Math.max(0, _sleepLong + deviation);
		}
		else
		{
			int deviation = _rand.nextInt(2 * _sleepShortDeviation)
				- _sleepShortDeviation;
			return Math.max(0, _sleepShort + deviation);
		}
	}

	protected static String getTestParamNamePropertyName(int index)
	{
		return "test.parameter.name." + index;
	}

	protected static String getTestParamValuePropertyName(int index)
	{
		return "test.parameter.value." + index;
	}

	protected static String getTestQueryPropertyName(int index)
	{
		return "test.query." + index;
	}

	protected abstract SesameRepository prepareTestRepository() throws Exception;

	protected abstract void cleanupTestRepository();

	public AbstractStressTest(int numThreads, int numThreadLoops,
		int writerPercentage)
	{
		_numThreads = numThreads;
		_numThreadLoops = numThreadLoops;
		_writerPercentage = writerPercentage;
		_props = new Properties();
		System.out.println("---------- " + _numThreads + " threads, "
			+ _numThreadLoops + " ops per thread, " + _writerPercentage
			+ "% writers ----------");
		try
		{
			try (InputStream in = new FileInputStream(PROP_FILE)) {
				_props.load(in);
			}

			_sleepShort = Integer.parseInt(_props.getProperty(PROP_SLEEP_SHORT,
				DEFAULT_SLEEP_SHORT));
			_sleepShortDeviation = Integer.parseInt(_props.getProperty(
				PROP_SLEEP_SHORT_DEVIATION, DEFAULT_SLEEP_SHORT_DEVIATION));
			_sleepLong = Integer.parseInt(_props.getProperty(PROP_SLEEP_LONG,
				DEFAULT_SLEEP_LONG));
			_sleepLongDeviation = Integer.parseInt(_props.getProperty(
				PROP_SLEEP_LONG_DEVIATION, DEFAULT_SLEEP_LONG_DEVIATION));
			_sleepLongPercentage = Integer.parseInt(_props.getProperty(
				PROP_SLEEP_LONG_PERCENTAGE, DEFAULT_SLEEP_LONG_PERCENTAGE));
			System.out.println("Short sleep: " + _sleepShort + "ms +- "
				+ _sleepShortDeviation + "ms");
			System.out.println("Long sleep: " + _sleepLong + " +- "
				+ _sleepLongDeviation + "ms");
			System.out.println(_sleepLongPercentage + "% long sleeps");

			int paramCount = Integer.parseInt(_props.getProperty(
				PROP_TEST_PARAM_COUNT, DEFAULT_TEST_PARAM_COUNT));
			System.out.println("Loading " + paramCount + " parameters");
			for (int i = 0; i < paramCount; i++)
			{
				String name = _props.getProperty(getTestParamNamePropertyName(i));
				String value = _props.getProperty(getTestParamValuePropertyName(i));
				_parameters.put(name, value);
				System.out.println("Added parameter " + name + "=" + value);
			}

			// Cache queries up-front to avoid a lot of pointless re-parsing.
			int queryCount = Integer.parseInt(_props.getProperty(
				PROP_TEST_QUERY_COUNT, DEFAULT_TEST_QUERY_COUNT));
			System.out.println("Loading " + queryCount + " queries");
			_queries = new String[queryCount];
			for (int i = 0; i < queryCount; i++)
			{
				StringBuffer sb = new StringBuffer(readQuery(_props
					.getProperty(getTestQueryPropertyName(i))));
				int start = sb.indexOf("##");
				while (start >= 0)
				{
					int end = sb.indexOf("##", start + 2);
					String name = sb.substring(start + 2, end);
					String value = _parameters.get(name);
					sb.replace(start, end + 2, value);
					start = sb.indexOf("##");
				}
				_queries[i] = sb.toString();
				System.out.println("Added query:\n\t" + _queries[i]);
			}
		}
		catch (IOException ioe)
		{
			System.err.println("Unable to load " + PROP_FILE
				+ ". This isn't gonna work so great.");
		}
	}

	private static String readQuery(String filename) throws IOException {
		try (BufferedReader rdr = Files.newBufferedReader(Paths.get(filename), StandardCharsets.UTF_8)) {
			return rdr.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	public AbstractStressTest(int numThreads, int numThreadLoops,
		int writerPercentage, boolean csv)
	{
		this(numThreads, numThreadLoops, writerPercentage);
		_csv = csv;
	}

	public double getAverageResponseTime()
	{
		return _averageResponseTime / 1000000;
	}

	public int getNumberOperations()
	{
		return _numThreads * _numThreadLoops;
	}

	public long getTotalElapsedTime()
	{
		return _totalElapsedTime;
	}

	public int getNumberThreads()
	{
		return _numThreads;
	}

	public double getThroughput()
	{
		double tp = (1000000000 * ((double) getNumberOperations() / getTotalElapsedTime()));
		return tp;
	}

	public int getWriterPercentage()
	{
		return _writerPercentage;
	}

	protected Properties getProperties()
	{
		return _props;
	}

	public String getResults()
	{
		String rv = "";
		DecimalFormat format = new DecimalFormat("0.000");
		if (_csv)
		{
			rv = getNumberThreads() + "," + format.format(getThroughput()) + ","
				+ format.format(getAverageResponseTime());
		}
		else
		{
			rv += ("Threads: " + getNumberThreads() + '\n');
			rv += ("Operations: " + getNumberOperations() + '\n');
			rv += ("Throughput: " + format.format(getThroughput()) + " ops/s\n");
			rv += ("Response Time: " + format.format(getAverageResponseTime()) + "ms\n");
		}
		return rv;
	}

	private static String randomUri(List<String> uris)
	{
		synchronized (uris)
		{
			return uris.get(_rand.nextInt(uris.size())).toString();
		}
	}

	static StringBuffer createLinkedRDFStream(List<String> nodeUris,
		List<String> propertyUris)
	{
		StringBuffer toAdd = new StringBuffer();
		for (int i = 0; i < nodeUris.size(); i++)
		{
			String subject = nodeUris.get(i).toString();
			for (int j = 0; j < nodeUris.size(); j++)
			{
				if (j == i)
				{
					continue;
				}
				String object = nodeUris.get(j).toString();
				String predicate = randomUri(propertyUris);

				toAdd.append("<" + subject + "> <" + predicate + "> <" + object
					+ "> .\n");
			}
		}
		return toAdd;
	}

	/**
	 * Adds all of the .owl files in the directory specified by data.dir to a
	 * repository.
	 *
	 * @param repository
	 *           The repository to populate.
	 */
	public void addData(SesameRepository repository) throws Exception
	{
		System.out.println("Loading test data into Parliament");
		long startLoad = System.currentTimeMillis();
		File dataDir = new File(getProperties().getProperty(PROP_DATA_DIR,
			DEFAULT_DATA_DIR));
		System.out.println("Data directory is " + dataDir.getAbsolutePath());
		File[] files = dataDir.listFiles();
		for (int i = 0; i < files.length; i++)
		{
			System.out.println("Adding file: " + files[i].getName());
			repository.addData(files[i], "http://www.example.org/ns",
				RDFFormat.RDFXML, true, new DummyAdminListener());
		}
		System.out.println("Load time: "
			+ (double) (System.currentTimeMillis() - startLoad) / 1000 + "s");
	}

	public void doit()
	{
		SesameRepository repository = null;
		long actualTime = 0;
		try
		{
			repository = prepareTestRepository();
			// now create all of the threads and start them
			TestThread[] threads = new TestThread[_numThreads];

			for (int i = 0; i < _numThreads; i++)
			{
				threads[i] = new TestThread(i, _barrier, repository,
					_numThreadLoops, _writerPercentage);
				threads[i].start();
			}

			long startTime = System.nanoTime();
			_barrier.go();

			for (int i = 0; i < _numThreads; ++i)
			{
				threads[i].join();
			}
			actualTime = System.nanoTime() - startTime;

			// calculate the average response times
			for (int i = 0; i < _numThreads; ++i)
			{
				_totalElapsedTime += threads[i].getTotalElapsedTime();
			}
			_averageResponseTime = ((double) _totalElapsedTime / (double) getNumberOperations());
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		finally
		{
			cleanupTestRepository();
		}
		System.out.println("Total thread runtime: "
			+ (_totalElapsedTime / 1000000) + "ms");
		System.out
		.println("Actual time passed: " + (actualTime / 1000000) + "ms");
	}

	class TestThread extends Thread
	{
		private long             _threadElapsedTime = 0;
		private int              _id;
		@SuppressWarnings("hiding")
		private Barrier          _barrier;
		private int              _numOperations;
		private int              _percentWriters;
		private Random           _random;
		private SesameRepository _repository;
		private boolean          _hasPrinted       = false;

		TestThread(int id, Barrier barrier, SesameRepository repository,
			int numOperations, int writerPercentage)
		{
			_id = id;
			_barrier = barrier;
			_repository = repository;
			_numOperations = numOperations;
			_percentWriters = writerPercentage;
			_random = new Random(_id);
		}

		@Override
		public void run()
		{
			// wait on the green flag
			_barrier.chill();

			int sleeptime;
			for (int i = 0; i < _numOperations; ++i)
			{
				// determine if I should read or write
				if (_random.nextInt(100) < _percentWriters)
				{
					write();
				}
				else
				{
					String query = _queries[_random.nextInt(_queries.length)];
					read(query);
				}

				sleeptime = getSleepTime();
				if (sleeptime >= 0)
				{
					try
					{
						Thread.sleep(sleeptime);
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		private void printStatements(Graph g)
		{
			System.out.println("Statements:");
			StatementIterator si = g.getStatements();
			try
			{
				while (si.hasNext())
				{
					Statement element = si.next();
					System.out.println(element.toString());
				}
			}
			finally
			{
				si.close();
			}
		}

		private void read(String query)
		{
			// String resource = randomUri(_resources);
			// String predicate = randomUri(_properties);
			long startTime;
			// int selection = _random.nextInt(3);
			//
			// switch(selection)
			// {
			// case 0:
			// _query = "CONSTRUCT * FROM {<" + resource + ">} pred {obj}";
			// break;
			// case 1:
			// _query = "CONSTRUCT * FROM {subj} pred {<" + resource + ">}";
			// break;
			// default:
			// _query = "CONSTRUCT * FROM {subj} <" + predicate + "> {obj}";
			// break;
			// }

			try
			{
				// start timing and perform the read operation
				startTime = System.nanoTime();
				Graph g = _repository.performGraphQuery(QueryLanguage.SERQL, query);
				_threadElapsedTime += (System.nanoTime() - startTime);
				if (DEBUG)
				{
					if (!_hasPrinted)
					{
						_hasPrinted = true;
						printStatements(g);
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		private void write()
		{
			List<String> newResources = new ArrayList<>();
			long startTime;
			int offset = _resources.size();

			for (int i = 0; i < 50; i++)
			{
				newResources.add("http://example.org/stresstest#resource"
					+ (offset + i) + "" + _numOperations);
			}
			synchronized (_resources)
			{
				_resources.addAll(newResources);
			}
			offset = _properties.size();
			List<String> newProperties = new ArrayList<>();
			for (int i = 0; i < 10; i++)
			{
				newProperties.add("http://example.org/stresstest#property"
					+ (offset + i) + "" + _numOperations);
			}
			synchronized (_properties)
			{
				_properties.addAll(newProperties);
			}
			StringBuffer toAdd = null;

			try
			{
				toAdd = createLinkedRDFStream(newResources, newProperties);

				// start timing and perform the write operation
				startTime = System.nanoTime();
				_repository.addData(toAdd.toString(), "", RDFFormat.NTRIPLES, true,
					new DummyAdminListener());
				_threadElapsedTime += (System.nanoTime() - startTime);
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

		protected void timestamp(String message)
		{
			Calendar current = Calendar.getInstance();
			System.out.println(this.getClass().getSimpleName() + " " + _id + " "
				+ message + " at " + current.get(Calendar.HOUR) + ":"
				+ current.get(Calendar.MINUTE) + ":" + current.get(Calendar.SECOND)
				+ "." + current.get(Calendar.MILLISECOND));
		}

		long getTotalElapsedTime()
		{
			return _threadElapsedTime;
		}

		long getNumberOperations()
		{
			return _numOperations;
		}
	}

	class Barrier
	{
		boolean _greenFlag = false;

		synchronized void go()
		{
			_greenFlag = true;
			notifyAll();
		}

		synchronized void chill()
		{
			while (!_greenFlag)
			{
				try
				{
					wait();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}
