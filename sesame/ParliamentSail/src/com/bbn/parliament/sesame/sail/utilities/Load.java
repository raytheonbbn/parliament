// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: Load.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail.utilities;

import java.io.File;

import org.openrdf.sesame.Sesame;
import org.openrdf.sesame.admin.StdOutAdminListener;
import org.openrdf.sesame.config.RepositoryConfig;
import org.openrdf.sesame.config.SailConfig;
import org.openrdf.sesame.constants.RDFFormat;
import org.openrdf.sesame.repository.SesameRepository;
import org.openrdf.sesame.repository.local.LocalService;

public class Load
{
	private static final boolean LOAD_RDF_XML = false;
	private static final boolean LOAD_OWL_DIR = false;
	private static final String REP_NAME = "myRep";

	private static void loadNTriples(SesameRepository repository, String path,
		String uri) throws Exception
	{
		repository.addData(new File(path), uri, RDFFormat.NTRIPLES,
			true, new StdOutAdminListener());
	}

	private static void loadRDFXML(SesameRepository repository, String path,
		String uri) throws Exception
	{
		repository.addData(new File(path), uri, RDFFormat.RDFXML,
			true, new StdOutAdminListener());
	}

	private static void loadOWLDirectory(SesameRepository repository,
		File directory) throws Exception
	{
		File[] files = directory.listFiles();
		for (File file : files)
		{
			if (file.isDirectory())
			{
				loadOWLDirectory(repository, file);
			}
			else
			{
				String path = file.getPath();
				System.out.println("loading " + path); // XXX
				loadRDFXML(repository, path, "baseURL");
			}
		}
	}

	public static void main(String args[])
	{
		// parse arguments
		boolean useNativeSail = false;
		for (String arg : args)
		{
			if (arg.equals("-native"))
			{
				useNativeSail = true;
				break;
			}
		}

		try
		{
			SailConfig sailCfg = new SailConfig(useNativeSail
				? "org.openrdf.sesame.sailimpl.nativerdf.NativeRdfRepository"
				: "com.bbn.parliament.sesame.sail.RdfSchemaRepository");
			sailCfg.setParameter("dir", ".");
			RepositoryConfig config = new RepositoryConfig(REP_NAME);
			config.addSail(sailCfg);
			config.setWorldReadable(true);
			config.setWorldWriteable(true);
			LocalService service = Sesame.getService();
			service.addRepository(config);
			SesameRepository rep = service.getRepository(REP_NAME);

			if (LOAD_OWL_DIR)
			{
				loadOWLDirectory(rep, new File("/fooble/webcache"));
			}
			loadNTriples(rep, "/fooble/refload/fooble-refload-2005-09-12.nt", "baseURI");
			if (LOAD_RDF_XML)
			{
				loadRDFXML(rep, "mini.owl", "http://bbn.com/2004/03/fooble/mini");
			}
			loadRDFXML(rep, "/fooble/fooble-ont-GM_14Jul05.owl",
				"http://bbn.com/2004/03/fooble/fooble-ont");
			loadRDFXML(rep, "../data2fooble/namespaces.owl",
				"http://bbn.com/2004/03/fooble/fooble-ont");

			System.out.println("addData done");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
