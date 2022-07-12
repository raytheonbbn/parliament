// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: UnimplementedException.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

import org.openrdf.sesame.sail.SailInternalException;

public class UnimplementedException extends SailInternalException
{
	private static final long serialVersionUID = 3792365231456525479L;

	public UnimplementedException()
	{
		super("Unimplemented");
	}
}
