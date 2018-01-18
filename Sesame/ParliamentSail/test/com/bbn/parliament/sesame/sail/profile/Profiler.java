// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.sesame.sail.profile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.openrdf.model.Graph;
import org.openrdf.sesame.Sesame;
import org.openrdf.sesame.admin.AdminListener;
import org.openrdf.sesame.admin.StdOutAdminListener;
import org.openrdf.sesame.config.AccessDeniedException;
import org.openrdf.sesame.config.ConfigurationException;
import org.openrdf.sesame.config.RepositoryConfig;
import org.openrdf.sesame.config.SailConfig;
import org.openrdf.sesame.config.SystemConfig;
import org.openrdf.sesame.config.UnknownRepositoryException;
import org.openrdf.sesame.constants.QueryLanguage;
import org.openrdf.sesame.constants.RDFFormat;
import org.openrdf.sesame.repository.SesameRepository;
import org.openrdf.sesame.repository.local.LocalRepository;
import org.openrdf.sesame.repository.local.LocalService;
import org.openrdf.sesame.sail.StatementIterator;

public class Profiler
{
	private static final boolean DEBUG                = true;
	private static final String  PROP_SESAME_URL      = "url";
	private static final String  PROP_REPOSITORY_NAME = "repo.name";
	private static final String  PROP_USER            = "user";
	private static final String  PROP_PASS            = "password";

	private Properties           _kbConfig            = new Properties();
	public AdminListener         _sesameListener      = new StdOutAdminListener();
	private SesameRepository     _repository          = null;
	private String               _repositoryName      = null;
	private DecimalFormat        _format              = new DecimalFormat("0.00");
	private long                 _totalElapsedTime;
	private File                 _testDir             = new File("testDir");
	private long                 _numQueries          = 100;
	private boolean              _loadSerialized      = true; // Load data from serialized input?
	private String               _reportName          = "report.csv";
	private Writer               _report;
	private List<DataProps>      _testData;
	private File                 _sourceDir           = new File("C:/Temp/frumpy-test-data/1k");

	protected class DataProps
	{
		public int       numInstances;
		public String    dataURL;
		public RDFFormat format;
		public String    analysisName;
		public boolean   remoteKB;
		public boolean   loadKB;

		public DataProps(int numInstances, String dataURL, RDFFormat format,
			String analysisName, boolean remoteKB, boolean loadKB)
		{
			this.numInstances = numInstances;
			this.dataURL = dataURL;
			this.format = format;
			this.analysisName = analysisName;
			this.remoteKB = remoteKB;
			this.loadKB = loadKB;
		}
	}

	public Profiler(String configFileName, String testParamsFileName)
	{
		// Load the KB properties
		try (InputStream in = new FileInputStream(configFileName))
		{
			_kbConfig.load(in);
		}
		catch (IOException e2)
		{
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		try
		{
			loadTestParams(testParamsFileName);
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}

		try
		{
			_report = new BufferedWriter(new FileWriter(_reportName));
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void loadTestParams(String testParamsFileName) throws IOException
	{
		_testData = new ArrayList<>();

		try (BufferedReader rdr = Files.newBufferedReader(Paths.get(testParamsFileName), StandardCharsets.UTF_8)) {
			String line = null;
			while (null != (line = rdr.readLine()))
			{
				if (!line.trim().startsWith("#") && (line.trim().length() > 0))
				{
					List<String> testParam = CSVParser.parse(line);

					int numInstances = Integer.parseInt(testParam.get(0));
					String dataURL = testParam.get(1);
					String formatString = testParam.get(2);
					String analysisName = testParam.get(3);
					boolean remoteKB = Boolean.parseBoolean(testParam.get(4));
					boolean loadKB = Boolean.parseBoolean(testParam.get(5));

					RDFFormat format = null;
					if (formatString.equalsIgnoreCase("RDFXML"))
					{
						format = RDFFormat.RDFXML;
					}
					else if (formatString.equalsIgnoreCase("N3"))
					{
						format = RDFFormat.N3;
					}
					else if (formatString.equalsIgnoreCase("NTRIPLES"))
					{
						format = RDFFormat.NTRIPLES;
					}
					else if (formatString.equalsIgnoreCase("TURTLE"))
					{
						format = RDFFormat.TURTLE;
					}

					DataProps dataProps = new DataProps(numInstances, dataURL,
						format, analysisName, remoteKB, loadKB);
					_testData.add(dataProps);
				}
			}
		}
	}

	protected void initLocalKB()
	{
		if (DEBUG)
		{
			System.out.println("Loading local Sesame-over-Parliament repository...");
		}

		// configurations taken from system.conf file in example tomcat instance
		SailConfig syncSail = new SailConfig(
			"com.bbn.parliament.sesame.sail.KbSyncRdfSchemaRepository");

		SailConfig kbSail = new SailConfig(
			"com.bbn.parliament.sesame.sail.KbRdfSchemaRepository");
		// _tempDir = TemporaryDirectory.create();
		kbSail.setParameter("dir", _testDir.getAbsolutePath());

		_repositoryName = _testDir.getAbsolutePath();

		RepositoryConfig repositoryConfiguration = new RepositoryConfig(
			_repositoryName);
		repositoryConfiguration.addSail(syncSail);
		repositoryConfiguration.addSail(kbSail);
		repositoryConfiguration.setWorldReadable(true);
		repositoryConfiguration.setWorldWriteable(true);

		SystemConfig sysConfigWithTempDir = new SystemConfig();
		sysConfigWithTempDir.setTmpDir(System.getProperty("java.io.tmpdir"));

		LocalService localService = Sesame.getService();
		localService.setSystemConfig(sysConfigWithTempDir);

		// -----------------------------

		try
		{
			_repository = localService.createRepository(repositoryConfiguration);
		}
		catch (ConfigurationException e)
		{
			// TODO this should throw an exception that is caught by the calling
			// process
			e.printStackTrace();
		}

		if (DEBUG)
		{
			System.out.println("Done loading repository!");
		}
	}

	protected void initRemoteKB()
	{
		if (DEBUG)
		{
			System.out.println("Loading remote Sesame-over-Parliament repository...");
		}

		try
		{
			URL url = new URL(_kbConfig.getProperty(PROP_SESAME_URL));

			String repo = _kbConfig.getProperty(PROP_REPOSITORY_NAME);

			String username = _kbConfig.getProperty(PROP_USER);
			String password = _kbConfig.getProperty(PROP_PASS);

			Sesame.getService(url).login(username, password);

			_repository = Sesame.getService(url).getRepository(repo);
		}
		catch (MalformedURLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (UnknownRepositoryException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (AccessDeniedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (DEBUG)
		{
			System.out.println("Done loading repository!");
		}
	}

	protected void addData(String dataURL, RDFFormat format)
	{
		if (DEBUG)
		{
			System.out.println("Adding data to repository: " + dataURL);
		}

		try
		{
			_repository.addData(new URL(dataURL), "http://www.example.org/ns",
				format, true, _sesameListener);
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

		if (DEBUG)
		{
			System.out.println("Done adding data!");
		}
	}

	/**
	 * Copies a file from one location to another.
	 *
	 * @param src
	 *           The source file
	 * @param dst
	 *           The destination file
	 * @throws IOException
	 *            if an IOException occurs during the copy
	 */
	protected static void copyFile(File src, File dst) throws IOException
	{
		if (DEBUG)
		{
			System.out.println("Copying '" + src.getAbsolutePath() + "' to '"
				+ dst.getPath() + "'...");
		}

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

	/**
	 * Copies the Parliament files to the temporary test directory.
	 *
	 * @throws IOException
	 *            if an IOException occurs during copying.
	 */
	protected void copyKB(File sourceDir) throws IOException
	{
		if (!_testDir.exists())
		{
			_testDir.mkdir();
		}
		File[] files = sourceDir.listFiles();
		for (int i = 0; i < files.length; ++i)
		{
			String filename = files[i].getName();
			File destination = new File(_testDir, filename);
			copyFile(files[i], destination);
		}
	}

	protected void cleanupTestRepository()
	{
		if (_repository != null)
		{
			if (_repository instanceof LocalRepository)
			{
				((LocalRepository) _repository).shutDown();
			}
		}

		if (DEBUG)
		{
			System.out.println("Deleting temporary KB...");
		}

		if (_testDir.isDirectory())
		{
			File[] files = _testDir.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				if (DEBUG)
				{
					System.out.println(files[i].getName() + ": "
						+ _format.format(((double) files[i].length() / 1024 / 1024))
						+ "MB");
				}

				if (!files[i].delete())
				{
					System.err
					.println("Error deleting KB files. Some thread probably still has a lock on them.");
				}
			}
		}
	}

	public Graph executeQuery(String query) throws Exception
	{
		long startTime = System.nanoTime();
		Graph g = _repository.performGraphQuery(QueryLanguage.SERQL, query);
		_totalElapsedTime += System.nanoTime() - startTime;

		return g;
	}

	public void listInstances(DataProps data) throws Exception
	{
		_totalElapsedTime = 0;
		Graph g = null;

		String instanceQuery = "CONSTRUCT DISTINCT * FROM {<" + data.analysisName
			+ ">} " + "<http://bbn.com/frumpy-ont#includes> {instance} "
			+ "UNION CONSTRUCT DISTINCT * FROM {<" + data.analysisName + ">} "
			+ "<http://bbn.com/frumpy-ont#includesMerge> {instance}";

		for (int i = 0; i < _numQueries; i++)
		{
			g = executeQuery(instanceQuery);
		}

		int graphSize = 0;
		if (g != null) {
			StatementIterator iter = g.getStatements();
			try
			{
				while (iter.hasNext())
				{
					iter.next();
					++graphSize;
				}
			}
			finally
			{
				iter.close();
			}
		}

		long elapsedTime = _totalElapsedTime / 1000000;
		long avgTime = (_totalElapsedTime / 1000000) / _numQueries;

		System.out.println("Elapsed time: " + elapsedTime + "ms");
		System.out.println("Average time: " + avgTime + "ms");
		System.out.println("Graph size: " + graphSize);

		// _report.write(graphSize + ",");
		_report.write(data.numInstances + ",");
		_report.write(data.remoteKB ? "Remote," : "Local,");
		_report.write("ListInstances,");
		_report.write(avgTime + "\n");
		_report.flush();
	}

	/**
	 * Load the KB with the data. This will either create a new KB and load it
	 * with the specified data, or copy preloaded KB's and load it.
	 */
	protected void loadLocalKB(DataProps data)
	{
		// Clean up the test directory first
		cleanupTestRepository();

		// See if we are to use an already populated Parliament, or create a new one
		if (!_loadSerialized)
		{
			try
			{
				copyKB(_sourceDir);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			initLocalKB();
			addData(null, data.format);
		}
		else
		{
			initLocalKB();
			addData(data.dataURL, data.format);
		}
	}

	protected void loadRemoteKB(DataProps data)
	{
		// Clear the remote KB
		if (null != _repository)
		{
			try
			{
				_repository.clear(_sesameListener);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (AccessDeniedException e)
			{
				e.printStackTrace();
			}
		}

		// Load the remote KB
		initRemoteKB();
		addData(data.dataURL, data.format);
	}

	public void allTests()
	{
		try
		{
			for (DataProps data : _testData)
			{
				// Load the KB
				if (data.remoteKB)
				{
					loadRemoteKB(data);
				}
				else
				{
					loadLocalKB(data);
				}

				// Perform the tests
				listInstances(data);
			}
		}
		catch (Exception e)
		{
			System.out.println("Error writing to report!");
			e.printStackTrace();
		}

		// Close the report
		try
		{
			_report.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		Profiler profiler = new Profiler("test/kb.config", "test/TestFiles.csv");
		profiler.allTests();
	}
}
