// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: Save.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail.utilities;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.openrdf.rio.RdfDocumentWriter;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.sesame.Sesame;
import org.openrdf.sesame.config.RepositoryConfig;
import org.openrdf.sesame.config.SailConfig;
import org.openrdf.sesame.repository.local.LocalRepository;
import org.openrdf.sesame.repository.local.LocalService;

public class Save
{
	public static void main(String args[]) throws Exception
	{
		String repositoryName = "myRep";
		RepositoryConfig config = new RepositoryConfig(repositoryName);
		SailConfig sail = new SailConfig(
			"com.bbn.parliament.sesame.sail.RdfSchemaRepository");
		sail.setParameter("dir", ".");
		config.addSail(sail);
		config.setWorldReadable(true);
		config.setWorldWriteable(true);
		LocalService service = Sesame.getService();
		service.addRepository(config);
		LocalRepository repository = (LocalRepository) service.getRepository(repositoryName);

		try (
			OutputStream fos = new FileOutputStream("refload.nt");
			Writer writer = new OutputStreamWriter(fos, "UTF-8");
		) {
			RdfDocumentWriter rdfDocWriter = new NTriplesWriter(writer);
			repository.extractRDF(rdfDocWriter, true, true, true, false);
		}
	}
}
