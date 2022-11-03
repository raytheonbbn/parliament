// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.utilities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfxml.xmlinput.ARP;
import org.xml.sax.SAXException;


/**
 * Class or Interface Description
 *
 * @author Paul Neves Created on Oct 29, 2002.
 */
public class LoadRDFCommand
{
	private ARP              _parser;
	private Model            _model;
	private String           _name;
	private String           _baseUri;
	private boolean          _verbose;
	private long             _stmtCount;
	private StatementHandler _stmtHandler;

	public LoadRDFCommand(String name)
	{
		this(name, null, new ARP(), null);
	}

	public LoadRDFCommand(String name, String baseUri)
	{
		this(name, baseUri, new ARP(), null);
	}

	public LoadRDFCommand(String name, StatementHandler statementHandler)
	{
		this(name, null, statementHandler);
	}

	public LoadRDFCommand(String name, String baseUri, ARP parser)
	{
		this(name, baseUri, parser, null);
	}

	public LoadRDFCommand(String name, String baseUri,
		StatementHandler statementHandler)
	{
		this(name, baseUri, new ARP(), statementHandler);
	}

	public LoadRDFCommand(String name, String baseUri, ARP parser,
		StatementHandler statementHandler)
	{
		setName(name);
		setBaseUri(baseUri);
		setParser(parser);
		setVerbose(false);
		setStatementCount(0L);
		setModel(null);
		setStatementHandler(statementHandler);
	}

	public void load() throws FileNotFoundException, IOException, SAXException
	{
		try (InputStream in = new FileInputStream(_name)) {
			load(in);
		}
	}

	public void load(InputStream in) throws IOException, SAXException
	{
		// if (getModel() == null)
		// {
		// throw new IllegalStateException("Target model is unspecified");
		// }

		if (getStatementHandler() == null)
		{
			throw new IllegalStateException("StatementHandler is unspecified");
		}

		_parser.getHandlers().setStatementHandler(getStatementHandler());

		if (getBaseUri() == null)
		{
			_parser.load(in);
		}
		else
		{
			_parser.load(in, getBaseUri());
		}
	}

	/**
	 * Returns the baseUri.
	 *
	 * @return String
	 */
	public String getBaseUri()
	{
		return _baseUri;
	}

	/**
	 * Returns the model.
	 *
	 * @return Model
	 */
	public Model getModel()
	{
		return _model;
	}

	/**
	 * Returns the statementCount.
	 *
	 * @return long
	 */
	public long getStatementCount()
	{
		return _stmtCount;
	}

	/**
	 * Returns the uri.
	 *
	 * @return String
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * Returns the verbose.
	 *
	 * @return boolean
	 */
	public boolean isVerbose()
	{
		return _verbose;
	}

	/**
	 * Sets the baseUri.
	 *
	 * @param baseUri The baseUri to set
	 */
	public void setBaseUri(String baseUri)
	{
		_baseUri = baseUri;
	}

	/**
	 * Sets the model.
	 *
	 * @param model The model to set
	 */
	public void setModel(Model model)
	{
		_model = model;
	}

	/**
	 * Sets the statementCount.
	 *
	 * @param statementCount The statementCount to set
	 */
	public void setStatementCount(long statementCount)
	{
		_stmtCount = statementCount;
	}

	/**
	 * Sets the uri.
	 *
	 * @param uri The uri to set
	 */
	public void setName(String uri)
	{
		if (uri == null)
		{
			throw new IllegalArgumentException("argument 'uri' is null");
		}

		_name = uri;
	}

	/**
	 * Sets the verbose.
	 *
	 * @param verbose The verbose to set
	 */
	public void setVerbose(boolean verbose)
	{
		_verbose = verbose;
	}

	/**
	 * Returns the parser.
	 *
	 * @return ARP
	 */
	public ARP getParser()
	{
		return _parser;
	}

	/**
	 * Sets the parser.
	 *
	 * @param parser The parser to set
	 */
	public void setParser(ARP parser)
	{
		if (parser == null)
		{
			throw new IllegalArgumentException("argument 'parser' is null");
		}

		_parser = parser;
	}

	/**
	 * Returns the statementHandler.
	 */
	public StatementHandler getStatementHandler()
	{
		return _stmtHandler;
	}

	/**
	 * Sets the statementHandler.
	 *
	 * @param statementHandler The statementHandler to set
	 */
	public void setStatementHandler(StatementHandler statementHandler)
	{
		_stmtHandler = statementHandler;
	}

	/**
	 * @see com.hp.hpl.jena.rdf.arp.ARPOptions#setDefaultErrorMode()
	 */
	public void setDefaultErrorMode()
	{
		_parser.getOptions().setDefaultErrorMode();
	}

	/**
	 * @see com.hp.hpl.jena.rdf.arp.ARPOptions#setLaxErrorMode()
	 */
	public void setLaxErrorMode()
	{
		_parser.getOptions().setLaxErrorMode();
	}

	/**
	 * @see com.hp.hpl.jena.rdf.arp.ARPOptions#setStrictErrorMode()
	 */
	public void setStrictErrorMode()
	{
		_parser.getOptions().setStrictErrorMode();
	}

	/**
	 * @see com.hp.hpl.jena.rdf.arp.ARPOptions#setStrictErrorMode(int)
	 */
	public void setStrictErrorMode(int nonErrorMode)
	{
		_parser.getOptions().setStrictErrorMode(nonErrorMode);
	}
}
