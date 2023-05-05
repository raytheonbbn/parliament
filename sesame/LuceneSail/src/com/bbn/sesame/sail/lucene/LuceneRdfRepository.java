// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: LuceneRdfRepository.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.sail.lucene;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.sesame.sail.RdfRepository;
import org.openrdf.sesame.sail.Sail;
import org.openrdf.sesame.sail.SailChangedListener;
import org.openrdf.sesame.sail.SailInitializationException;
import org.openrdf.sesame.sail.SailInternalException;
import org.openrdf.sesame.sail.SailUpdateException;

import com.bbn.parliament.core.jni.KbInstance;
import com.bbn.parliament.sesame.sail.KbValue;

public class LuceneRdfRepository extends LuceneRdfSource implements RdfRepository
{
	private RdfRepository   _baseSail;
	private IndexWriter     _indexWriter;
	private long            _startIndex;
	private Set<String>     _indicesAdded = new HashSet<>();

	public LuceneRdfRepository()
	{
		super();
	}

	@Override
	public void setBaseSail(Sail baseSail)
	{
		super.setBaseSail(baseSail);
		if (baseSail instanceof RdfRepository)
		{
			_baseSail = (RdfRepository) baseSail;
		}
		else
		{
			throw new SailInternalException(
				"RdfRepository:  base Sail should be an RdfRepository");
		}
	}

	@Override
	public void initialize(Map configParams) throws SailInitializationException
	{
		super.initialize(configParams);
	}

	@Override
	public void shutDown()
	{
		if (_baseSail != null)
		{
			_baseSail.shutDown();
		}
	}

	@Override
	public void startTransaction() throws SailInternalException
	{
		try
		{
			_indexWriter = new IndexWriter(_indexPath, _analyzer, false);
			@SuppressWarnings("resource")
			KbInstance kb = getKb();
			_startIndex = kb.rsrcCount();
			_indicesAdded.clear();
		}
		catch (CorruptIndexException ex)
		{
			throw new SailInternalException(ex);
		}
		catch (LockObtainFailedException ex)
		{
			throw new SailInternalException(ex);
		}
		catch (IOException ex)
		{
			throw new SailInternalException(ex);
		}
		_baseSail.startTransaction();
	}

	@Override
	public void commitTransaction() throws SailInternalException
	{
		_baseSail.commitTransaction();
		try
		{
			_indexWriter.close();
			_indexWriter = null;
		}
		catch (CorruptIndexException ex)
		{
			throw new SailInternalException(ex);
		}
		catch (IOException ex)
		{
			throw new SailInternalException(ex);
		}
	}

	@Override
	public boolean transactionStarted()
	{
		return _baseSail.transactionStarted();
	}

	@Override
	public void addStatement(Resource subj, URI pred, Value obj)
		throws SailUpdateException
	{
		if (obj instanceof Literal)
		{
			@SuppressWarnings("resource")
			KbInstance kb = getKb();
			long index = ((KbValue) obj).getIndex(kb);
			kb = null;
			String indexString = "" + index;

			// look for existing index
			if (index >= _startIndex && !_indicesAdded.contains(indexString))
			{
				// add new index
				try
				{
					Document document = new Document();
					document.add(new Field("literal", obj.toString(), Field.Store.NO,
						Field.Index.TOKENIZED, Field.TermVector.YES));
					document.add(new Field("index", indexString, Field.Store.YES,
						Field.Index.NO, Field.TermVector.NO));
					_indexWriter.addDocument(document);
					_indicesAdded.add(indexString);
				}
				catch (CorruptIndexException ex)
				{
					throw new SailUpdateException(ex);
				}
				catch (IOException ex)
				{
					throw new SailUpdateException(ex);
				}
			}
		}
		_baseSail.addStatement(subj, pred, obj);
	}

	@Override
	public int removeStatements(Resource subj, URI pred, Value obj)
		throws SailUpdateException
	{
		// nothing - resource not removed
		return _baseSail.removeStatements(subj, pred, obj);
	}

	@Override
	public void clearRepository() throws SailUpdateException
	{
		// replace the current index
		try
		{
			_indexWriter.close();
			_indexWriter = new IndexWriter(_indexPath, _analyzer, true);
		}
		catch (CorruptIndexException ex)
		{
			throw new SailInternalException(ex);
		}
		catch (LockObtainFailedException ex)
		{
			throw new SailInternalException(ex);
		}
		catch (IOException ex)
		{
			throw new SailInternalException(ex);
		}

		_baseSail.clearRepository();
	}

	@Override
	public void changeNamespacePrefix(String namespace, String prefix)
		throws SailUpdateException
	{
		_baseSail.changeNamespacePrefix(namespace, prefix);
	}

	@Override
	public void addListener(SailChangedListener listener)
	{
		_baseSail.addListener(listener);
	}

	@Override
	public void removeListener(SailChangedListener listener)
	{
		_baseSail.removeListener(listener);
	}
}
