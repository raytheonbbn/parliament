// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.core.recovery;

/** @author dkolas */
public interface Recoverable
{
	public void recoverAdd(String subject, String predicate, String object);
	public void recoverDelete(String subject, String predicate, String object);
	public void recoverFlush();
}
