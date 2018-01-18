// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: KbNamespaceIterator.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

import org.openrdf.model.Statement;
import org.openrdf.sesame.sail.NamespaceIterator;
import org.openrdf.sesame.sail.StatementIterator;

public class KbNamespaceIterator implements NamespaceIterator
{
   private StatementIterator _iterator;
   private Statement         _statement;

   KbNamespaceIterator(StatementIterator iterator)
   {
      _iterator = iterator;
   }

   @Override
	public void close()
   {
      _iterator.close();
   }

   @Override
	public String getName()
   {
      return _statement.getSubject().toString();
   }

   @Override
	public String getPrefix()
   {
      return _statement.getObject().toString();
   }

   @Override
	public boolean hasNext()
   {
      return _iterator.hasNext();
   }

   @Override
	public void next()
   {
      _statement = _iterator.next();
   }
}
