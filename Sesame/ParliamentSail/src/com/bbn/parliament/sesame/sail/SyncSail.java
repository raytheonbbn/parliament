// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: SyncSail.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

/**
 * Extend Sesame sync Sails to allow access to locks, to extend
 * read locks to cover the lifetime of lazy StatementIterators
 * returned by KbRdfSource.getStatements.
 */
public interface SyncSail
{
   public void _getWriteLock();
   public void _releaseWriteLock();
   public void _getReadLock();
   public void _releaseReadLock();

   public boolean isWriting();
}
