// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package org.openrdf.sesame.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.sesame.admin.StdOutAdminListener;
import org.openrdf.sesame.config.AccessDeniedException;
import org.openrdf.sesame.constants.QueryLanguage;
import org.openrdf.sesame.constants.RDFFormat;
import org.openrdf.sesame.query.MalformedQueryException;
import org.openrdf.sesame.query.QueryEvaluationException;
import org.openrdf.sesame.query.QueryResultsTable;
import org.openrdf.sesame.sail.StatementIterator;

import junit.framework.TestCase;

public abstract class RepositoryTest extends TestCase
{
	private static final String OUTPUT_PATH = "/tmp/";
	private static final String FILE_SERVER = "http://www.openrdf.org/sesame/";

	protected SesameRepository  _repository;

	/**
	 * Creates a new HTTP repository test.
	 */
	public RepositoryTest(String name)
	{
		super(name);
	}

	@Override
	protected void setUp() throws Exception
	{
		_repository = _createRepository();
	}

	/**
	 * Creates the repository to perform the tests on.
	 */
	protected abstract SesameRepository _createRepository();

	public void testPerformTableQuery()
	{
		try
		{
			QueryResultsTable resultsTable = _repository.performTableQuery(
				QueryLanguage.SERQL, "select * from {X} rdf:type {rdfs:Resource}");

			assertTrue(resultsTable != null);
		}
		catch (Exception e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
	}

	public void testPerformMalformedTableQuery()
	{
		try
		{
			_repository.performTableQuery(QueryLanguage.SERQL, "foobar");
			fail("Expected a MalformedQueryException");
		}
		catch (MalformedQueryException e)
		{
			// This is correct
		}
		catch (Exception e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
	}

	public void testPerformGraphQuery()
	{
		try
		{
			_repository.performGraphQuery(QueryLanguage.SERQL,
				"construct * from {X} rdf:type {rdfs:Resource}");
		}
		catch (Exception e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
	}

	public void testPerformMalformedGraphQuery()
	{
		try
		{
			_repository.performGraphQuery(QueryLanguage.SERQL, "foobar");

			fail("Expected a MalformedQueryException");
		}
		catch (MalformedQueryException e)
		{
			// This is correct
		}
		catch (Exception e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
	}

	public void testExtractRDF()
	{
		try
		{
			URL dataURL = new URL(FILE_SERVER + "files/museum/schema1.rdf");
			String baseURI = "http://www.icom.com/schema.rdf";

			_repository.clear(new StdOutAdminListener());
			_repository.addData(dataURL, baseURI, RDFFormat.RDFXML, true,
				new StdOutAdminListener());

			try (
				InputStream in = _repository.extractRDF(RDFFormat.RDFXML,
					true, true, false, true);
				FileOutputStream os = new FileOutputStream(OUTPUT_PATH
					+ "extractRDF-test.rdf");
			) {
				for (int i = in.read(); i != -1; i = in.read())
				{
					os.write(i);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
	}

	public void testQueryWithOptional() throws Exception
	{
		_repository.performGraphQuery(QueryLanguage.SERQL,
			"construct * from {X} rdf:type {rdf:Jimmy}, [{X} prop {rdf:Steve}]");
	}

	public void testAddData_URL()
	{
		try
		{
			URL dataURL = new URL(FILE_SERVER + "files/museum/schema1.rdf");
			String baseURI = "http://www.icom.com/schema.rdf";

			_repository.clear(new StdOutAdminListener());
			_repository.addData(dataURL, baseURI, RDFFormat.RDFXML, true,
				new StdOutAdminListener());
		}
		catch (MalformedURLException e)
		{
			fail("malformed url: " + e);
		}
		catch (IOException e)
		{
			fail("I/O error: " + e.getMessage());
		}
		catch (AccessDeniedException e)
		{
			fail("Access denied: " + e.getMessage());
		}
	}

	public void testAddData_File()
	{
		try
		{
			String workingDir = System.getProperty("user.dir");

			File dataFile = new File(workingDir + "/test/files/museum/schema1.rdf");
			String baseURI = "http://www.icom.com/schema.rdf";

			_repository.clear(new StdOutAdminListener());
			_repository.addData(dataFile, baseURI, RDFFormat.RDFXML, true,
				new StdOutAdminListener());
		}
		catch (MalformedURLException e)
		{
			fail("malformed url: " + e);
		}
		catch (IOException e)
		{
			fail("I/O error: " + e);
		}
		catch (AccessDeniedException e)
		{
			fail("Access denied: " + e.getMessage());
		}
	}

	public void testAddGraph()
	{
		try
		{
			String workingDir = System.getProperty("user.dir");
			File dataFile = new File(workingDir + "/test/files/museum/schema1.rdf");
			String baseURI = "http://www.icom.com/schema.rdf";

			_repository.clear(new StdOutAdminListener());
			_repository.addData(dataFile, baseURI, RDFFormat.RDFXML, true,
				new StdOutAdminListener());
		}
		catch (MalformedURLException e)
		{
			fail("malformed url: " + e);
		}
		catch (IOException e)
		{
			fail("I/O error: " + e);
		}
		catch (AccessDeniedException e)
		{
			fail("Access denied: " + e.getMessage());
		}

		try
		{
			Graph transformedGraph = _repository
				.performGraphQuery(
					QueryLanguage.SERQL,
					"construct {Y} <http://www.foo.com/bar> {X} from {X} rdfs:subClassOf {Y} where Y != X");
			_repository.addGraph(transformedGraph);

			Graph otherGraph = _repository.performGraphQuery(QueryLanguage.SERQL,
				"construct * from {X} <http://www.foo.com/bar> {Y}");

			ValueFactory factory = otherGraph.getValueFactory();

			URI predicate = factory.createURI("http://www.foo.com/bar");

			assertTrue(otherGraph.contains(null, predicate, null));

			Statement st = null;
			StatementIterator iter = otherGraph.getStatements(null, predicate,
				null);// predicate.getPredicateStatements();
			try
			{
				st = iter.next();
			}
			finally
			{
				iter.close();
			}

			Statement st2 = factory.createStatement(st.getSubject(), st
				.getPredicate(), st.getObject());
			assertTrue(otherGraph.contains(st));
			assertTrue(otherGraph.contains(st2));
		}
		catch (QueryEvaluationException e)
		{
			fail("query evaluation exception: " + e.getMessage());
		}
		catch (IOException e)
		{
			fail("I/O error: " + e.getMessage());
		}
		catch (MalformedQueryException e)
		{
			fail("Malformed query: " + e.getMessage());
		}
		catch (AccessDeniedException e)
		{
			fail("Access denied: " + e.getMessage());
		}
	}

	public void testAddGraph_bNodes_notJoined()
	{
		try
		{
			String workingDir = System.getProperty("user.dir");
			File dataFile = new File(workingDir
				+ "/test/files/testcases/bnodes/foaf.rdf");

			_repository.clear(new StdOutAdminListener());
			_repository.addData(dataFile, "", RDFFormat.RDFXML, true,
				new StdOutAdminListener());

			Graph toBeAdded = _repository.performGraphQuery(QueryLanguage.SERQL,
				" construct {y} foaf:knows {x} from {x} foaf:knows {y} "
					+ " using namespace foaf = <http://xmlns.com/foaf/0.1/>");

			_repository.addGraph(toBeAdded, false);

			Graph result = _repository.performGraphQuery(QueryLanguage.SERQL,
				" construct * from {x} foaf:knows {y} foaf:knows {x} "
					+ " using namespace foaf = <http://xmlns.com/foaf/0.1/>");

			ValueFactory factory = result.getValueFactory();

			URI predicate = factory.createURI("http://xmlns.com/foaf/0.1/knows");

			// this add should create new bnodes, so the relations should not be
			// symmetric.
			assertFalse(result.contains(null, predicate, null));

		}
		catch (QueryEvaluationException e)
		{
			fail("query evaluation exception: " + e.getMessage());
		}
		catch (IOException e)
		{
			fail("I/O error: " + e.getMessage());
		}
		catch (MalformedQueryException e)
		{
			fail("Malformed query: " + e.getMessage());
		}
		catch (AccessDeniedException e)
		{
			fail("Access denied: " + e.getMessage());
		}
	}

	public void testMergeGraph()
	{
		try
		{
			String workingDir = System.getProperty("user.dir");
			File dataFile = new File(workingDir
				+ "/test/files/testcases/bnodes/foaf.rdf");

			_repository.clear(new StdOutAdminListener());
			_repository.addData(dataFile, "", RDFFormat.RDFXML, true,
				new StdOutAdminListener());

			Graph toBeMerged = _repository.performGraphQuery(QueryLanguage.SERQL,
				" construct {y} foaf:knows {x} from {x} foaf:knows {y} "
					+ " using namespace foaf = <http://xmlns.com/foaf/0.1/>");

			_repository.addGraph(toBeMerged);

			Graph result = _repository.performGraphQuery(QueryLanguage.SERQL,
				" construct * from {x} foaf:knows {y} foaf:knows {x} "
					+ " using namespace foaf = <http://xmlns.com/foaf/0.1/>");

			ValueFactory factory = result.getValueFactory();

			URI predicate = factory.createURI("http://xmlns.com/foaf/0.1/knows");

			// if the add was successful all foaf:knows relations are now
			// 'symmetric'
			assertTrue(result.contains(null, predicate, null));

		}
		catch (QueryEvaluationException e)
		{
			fail("query evaluation exception: " + e.getMessage());
		}
		catch (IOException e)
		{
			fail("I/O error: " + e.getMessage());
		}
		catch (MalformedQueryException e)
		{
			fail("Malformed query: " + e.getMessage());
		}
		catch (AccessDeniedException e)
		{
			fail("Access denied: " + e.getMessage());
		}
	}

	public void testRemoveGraph()
	{
		try
		{
			String workingDir = System.getProperty("user.dir");
			File dataFile = new File(workingDir + "/test/files/museum/culture.rdf");
			String baseURI = "http://www.icom.com/schema.rdf";

			_repository.clear(new StdOutAdminListener());
			_repository.addData(dataFile, baseURI, RDFFormat.RDFXML, true,
				new StdOutAdminListener());

			Graph toBeRemoved = _repository
				.performGraphQuery(QueryLanguage.SERQL,
					"construct * from {X} <http://www.icom.com/schema.rdf#paints> {Y} where Y != X");
			_repository.removeGraph(toBeRemoved);

			StatementIterator iter = toBeRemoved.getStatements();
			try
			{
				while (iter.hasNext())
				{
					Statement st = iter.next();

					StringBuffer query = new StringBuffer(64);
					query.append("construct * from ");
					query.append("{<" + st.getSubject() + ">} ");
					query.append("<" + st.getPredicate() + "> ");
					Value object = st.getObject();
					if (object instanceof Resource)
					{
						query.append("{<" + object + ">}");
					}
					else
					{
						query.append("{\"" + object + "\"}");
					}

					Graph graph = _repository.performGraphQuery(QueryLanguage.SERQL,
						query.toString());

					assertFalse(graph.contains(st));
				}
			}
			finally
			{
				iter.close();
			}
		}
		catch (QueryEvaluationException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
		catch (IOException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
		catch (MalformedQueryException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
		catch (AccessDeniedException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
	}

	public void testRemoveStatements()
	{
		try
		{
			String workingDir = System.getProperty("user.dir");
			File dataFile = new File(workingDir + "/test/files/museum/culture.rdf");
			String baseURI = "http://www.icom.com/schema.rdf";

			_repository.clear(new StdOutAdminListener());
			_repository.addData(dataFile, baseURI, RDFFormat.RDFXML, true,
				new StdOutAdminListener());
		}
		catch (MalformedURLException e)
		{
			fail("malformed url: " + e);
		}
		catch (IOException e)
		{
			fail("I/O error: " + e);
		}
		catch (AccessDeniedException e)
		{
			fail("Access denied: " + e.getMessage());
		}

		URI picasso = new URIImpl("http://www.european-history.com/picasso.html");
		try
		{
			_repository.removeStatements(picasso, null, null,
				new StdOutAdminListener());

			Graph result = _repository.performGraphQuery(QueryLanguage.SERQL,
				"construct * from  {<" + picasso.toString() + ">} p {Y}");

			assertFalse(result.contains(null, null, null));
		}
		catch (IOException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
		catch (AccessDeniedException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
		catch (QueryEvaluationException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
		catch (MalformedQueryException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
	}

	public void testRemoveGraphQuery()
	{
		try
		{
			String workingDir = System.getProperty("user.dir");
			File dataFile = new File(workingDir + "/test/files/museum/culture.rdf");
			String baseURI = "http://www.icom.com/schema.rdf";

			_repository.clear(new StdOutAdminListener());
			_repository.addData(dataFile, baseURI, RDFFormat.RDFXML, true,
				new StdOutAdminListener());
		}
		catch (IOException e)
		{
			fail("I/O error: " + e);
		}
		catch (AccessDeniedException e)
		{
			fail("Access denied: " + e.getMessage());
		}

		try
		{
			_repository
			.removeGraph(QueryLanguage.SERQL,
				"construct * from {X} <http://www.icom.com/schema.rdf#paints> {Y} where Y != X");
		}
		catch (IOException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
		catch (AccessDeniedException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
	}

	public void testAddGraphQuery()
	{
		try
		{
			String workingDir = System.getProperty("user.dir");
			File dataFile = new File(workingDir + "/test/files/museum/culture.rdf");
			String baseURI = "http://www.icom.com/schema.rdf";

			_repository.clear(new StdOutAdminListener());
			_repository.addData(dataFile, baseURI, RDFFormat.RDFXML, true,
				new StdOutAdminListener());
		}
		catch (IOException e)
		{
			fail("I/O error: " + e);
		}
		catch (AccessDeniedException e)
		{
			fail("Access denied: " + e.getMessage());
		}

		try
		{
			_repository
			.addGraph(
				QueryLanguage.SERQL,
				"construct {Y} <http://www.foo.com/bar> {X} from {X} rdfs:subClassOf {Y} where Y != X");

			Graph graph = _repository.performGraphQuery(QueryLanguage.SERQL,
				"construct * from {X} <http://www.foo.com/bar> {Y}");

			ValueFactory factory = graph.getValueFactory();

			URI predicate = factory.createURI("http://www.foo.com/bar");

			assertTrue(graph.contains(null, predicate, null));

			Statement st = null;
			StatementIterator iter = graph.getStatements(null, predicate,
				null);// predicate.getPredicateStatements();
			try
			{
				st = iter.next();
			}
			finally
			{
				iter.close();
			}

			Statement st2 = factory.createStatement(st.getSubject(), st
				.getPredicate(), st.getObject());
			assertTrue(graph.contains(st));
			assertTrue(graph.contains(st2));
		}
		catch (QueryEvaluationException e)
		{
			fail("query evaluation exception: " + e.getMessage());
		}
		catch (IOException e)
		{
			fail("io exception: " + e.getMessage());
		}
		catch (MalformedQueryException e)
		{
			fail("mfq exception: " + e.getMessage());
		}
		catch (AccessDeniedException e)
		{
			fail("access denied exception: " + e.getMessage());
		}
	}

	public void testRemoveBNodes()
	{
		try
		{
			String workingDir = System.getProperty("user.dir");
			File file = new File(workingDir
				+ "/test/files/testcases/bnodes/foaf.rdf");

			_repository.clear(new StdOutAdminListener());
			_repository.addData(file, "foo:bar", RDFFormat.RDFXML, true,
				new StdOutAdminListener());
		}
		catch (MalformedURLException e)
		{
			fail("malformed url: " + e);
		}
		catch (IOException e)
		{
			fail("I/O error: " + e);
		}
		catch (AccessDeniedException e)
		{
			fail("Access denied: " + e.getMessage());
		}

		try
		{
			Graph myGraph = _repository.performGraphQuery(QueryLanguage.SERQL,
				"construct * " + "from {X} rdf:type {foaf:Person}; "
					+ "         foaf:name {\"Jeen Broekstra\"}; "
					+ "         p {y} "
					+ "using namespace foaf = <http://xmlns.com/foaf/0.1/>");

			StatementIterator iter = myGraph.getStatements();
			try
			{
				while (iter.hasNext())
				{
					Statement st = iter.next();
					_repository.removeStatements(st.getSubject(), st.getPredicate(), st
						.getObject(), new StdOutAdminListener());
				}
			}
			finally
			{
				iter.close();
			}

			Graph myGraph2 = _repository.performGraphQuery(QueryLanguage.SERQL,
				"construct * " + "from {X} rdf:type {foaf:Person}; "
					+ "         foaf:name {\"Jeen Broekstra\"}; "
					+ "         p {y} "
					+ "using namespace foaf = <http://xmlns.com/foaf/0.1/>");

			assertFalse(myGraph2.contains(null, null, null));
		}
		catch (QueryEvaluationException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
		catch (IOException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
		catch (MalformedQueryException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
		catch (AccessDeniedException e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
	}

	public void testUnicodeQuery()
	{
		try
		{
			String workingDir = System.getProperty("user.dir");
			File dataFile = new File(workingDir
				+ "/test/files/testcases/unicode/multilingual.rdf");

			_repository.clear(new StdOutAdminListener());
			_repository.addData(dataFile, "foo:bar", RDFFormat.RDFXML, true,
				new StdOutAdminListener());

			QueryResultsTable resultsTable = _repository
				.performTableQuery(
					QueryLanguage.SERQL,
					"select * from {<http://www.dictionary.com/words/bg/\u041A\u043E\u0440\u043D\u0438\u0437>} P {Y}");

			assertEquals(6, resultsTable.getRowCount());
		}
		catch (Exception e)
		{
			fail("[" + e.getClass() + "]: " + e.getMessage());
		}
	}
}
