// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: Test.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail.utilities;

import org.openrdf.model.Value;
import org.openrdf.sesame.Sesame;
import org.openrdf.sesame.config.RepositoryConfig;
import org.openrdf.sesame.config.SailConfig;
import org.openrdf.sesame.constants.QueryLanguage;
import org.openrdf.sesame.constants.RDFFormat;
import org.openrdf.sesame.query.QueryResultsTable;
import org.openrdf.sesame.query.RdfGraphWriter;
import org.openrdf.sesame.repository.SesameRepository;
import org.openrdf.sesame.repository.local.LocalRepository;
import org.openrdf.sesame.repository.local.LocalService;
import org.openrdf.sesame.sail.NamespaceIterator;
import org.openrdf.sesame.sail.RdfSource;

public class Test
{
	private static final boolean LIST_NAMESPACES = false;

	static void query(SesameRepository repository, String query) throws Exception
	{
		System.out.println(query);
		QueryResultsTable resultsTable = repository.performTableQuery(
			QueryLanguage.SERQL, query);
		for (int row = 0; row < resultsTable.getRowCount(); ++row)
		{
			for (int column = 0; column < resultsTable.getColumnCount(); ++column)
			{
				Value value = resultsTable.getValue(row, column);
				System.out.print(value.toString());
				System.out.print(" ");
			}
			System.out.println();
		}
		System.out.println("count = " + resultsTable.getRowCount());
	}

	static void graphQuery(SesameRepository repository, String query) throws Exception
	{
		System.out.println(query);
		repository.performGraphQuery(QueryLanguage.SERQL, query,
			new RdfGraphWriter(RDFFormat.RDFXML, System.out));
	}

	public static void main(String args[])
	{
		// parse arguments
		boolean nativeSail = false;
		for (int i = 0; i < args.length; ++i)
		{
			if (args[i].equals("-native"))
			{
				nativeSail = true;
				break;
			}
		}

		try
		{
			String repositoryName = "myRep";
			RepositoryConfig config = new RepositoryConfig(repositoryName);
			SailConfig sameAsSail = new SailConfig(
				"com.bbn.sesame.sail.sameas.SameAsRdfSchemaRepository");
			SailConfig syncSail = new SailConfig(
				"com.bbn.parliament.sesame.sail.KbSyncRdfSchemaRepository");
			SailConfig sail;
			if (nativeSail)
			{
				sail = new SailConfig(
					"org.openrdf.sesame.sailimpl.nativerdf.NativeRdfRepository");
				sail.setParameter("dir", ".");
			}
			else
			{
				sail = new SailConfig(
					"com.bbn.parliament.sesame.sail.KbRdfSchemaRepository");
				sail.setParameter("dir", "/test-kb");
			}

			config.addSail(syncSail);
			config.addSail(sameAsSail);
			config.addSail(sail);
			config.setWorldReadable(true);
			LocalService service = Sesame.getService();
			service.addRepository(config);
			SesameRepository repository = service.getRepository(repositoryName);

			// query(repository, "select o from {s} p {o}");

			// query(repository, "select c from {c} <rdf:type> {<owl:Class>} using
			// namespace owl = <!http://www.w3.org/2002/07/owl#>");

			// query(repository, "select ontology, version from {ontology}
			// <rdf:type> {<owl:Ontology>}, {ontology} <owl:versionInfo> {version}
			// using namespace owl = <!http://www.w3.org/2002/07/owl#>");

			// query(repository, "select s, p from {s} p {\"Vietnam\"}");

			// query(repository, "select s, o from {s} <owl:sameAs> {o} using
			// namespace owl = <!http://www.w3.org/2002/07/owl#>");

			// query(repository, "select name, country from {c}
			// <factbook:administrativeDivision> {s}, {s} <factbook:name> {name},
			// {c} <factbook:conventionalShortCountryName> {country} using
			// namespace factbook =
			// <!http://www.daml.org/2003/09/factbook/factbook-ont#>");

			// query(repository, "select s, name from {s} <rdf:type>
			// {<factbook:AdministrativeDivision>}, {s} <factbook:name> {name}
			// using namespace factbook =
			// <!http://www.daml.org/2003/09/factbook/factbook-ont#>");

			// query(repository, "select p, o from
			// {<!http://www.daml.org/2001/09/countries/iso#US>} p {o}"); // XXX -
			// what should s be?

			// graphQuery(repository, "CONSTRUCT * FROM [{sub} prop
			// {<!http://www.daml.org/2001/09/countries/fips#SY>}],
			// [{<!http://www.daml.org/2001/09/countries/fips#SY>} prop2 {obj}]");

			// query(repository, "select s, p from {s} p
			// {<!http://www.daml.org/2003/09/factbook/naturalResources#gypsum>}");

			// query(repository, "select s, o from {s}
			// <!http://www.daml.org/2001/10/html/nyse-ont#flag> {o}");

			// list namespaces
			if (LIST_NAMESPACES)
			{
				RdfSource rdfSource = (RdfSource) ((LocalRepository) repository).getSail();
				NamespaceIterator namespaces = rdfSource.getNamespaces();
				while (namespaces.hasNext())
				{
					namespaces.next();
					System.out.println(namespaces.getPrefix() + " " + namespaces.getName());
				}
			}

			// graphQuery(repository, "construct distinct * from {Sub} <rdf:type>
			// {<owl:FunctionalProperty>} using namespace owl =
			// <!http://www.w3.org/2002/07/owl#>");

			// graphQuery(repository, "CONSTRUCT * FROM [{sub} prop
			// {<!http://www.daml.org/2001/09/countries/fips#SY>}],
			// [{<!http://www.daml.org/2001/09/countries/fips#SY>} prop2 {obj}]");

			// ((org.openrdf.sesame.repository.local.LocalRepository)
			// repository).extractRDF(new
			// org.openrdf.rio.ntriples.NTriplesWriter(new
			// java.io.FileOutputStream("test.nt")), true, true, true, false);

			graphQuery(repository,
				"CONSTRUCT distinct * FROM {<http://www.daml.org/cgi-bin/nyse?IBM>} prop2 {obj}, "
					+ "[{obj} rdfs:label {lbl}], [{obj} rdf:type {typ}]");

			System.err.println("done");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
