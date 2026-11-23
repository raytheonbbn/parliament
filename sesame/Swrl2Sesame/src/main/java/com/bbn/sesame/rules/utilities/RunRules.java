// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: RunRules.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.rules.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.openrdf.model.Value;
import org.openrdf.sesame.query.QueryErrorType;
import org.openrdf.sesame.query.TableQuery;
import org.openrdf.sesame.query.TableQueryResultListener;
import com.bbn.parliament.sesame.sail.KbRdfRepository;
import com.bbn.sesame.rules.SWRL2Sesame;
import com.bbn.sesame.rules.SWRLRule;

public class RunRules
{
	static KbRdfRepository _repository = new KbRdfRepository();

	static void loadRules(String path) throws Exception
	{
		System.out.println("running " + path);
		List<SWRLRule> rules = SWRL2Sesame.getRules(path);

		for (final SWRLRule rule : rules)
		{
			final TableQuery query = rule.getQuery();
			query.evaluate(_repository, new TableQueryResultListener()
			{
				String      _colHdrs[] = null;
				int         _count     = 0;
				List<Value> _values    = new ArrayList<>();

				@Override
				public void endTableQueryResult()
				{
					System.out.println("fired " + _count + " times");
				}

				@Override
				public void endTuple()
				{
					_count++;
					try
					{
						rule.runHead(_repository, _colHdrs, _values);
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}

				@Override
				public void error(QueryErrorType errType, String message)
				{
					System.err.println(errType + ":  " + message);
				}

				@Override
				public void startTableQueryResult()
				{
				}

				@Override
				public void startTableQueryResult(String[] columnHeaders)
				{
					_colHdrs = columnHeaders;
				}

				@Override
				public void startTuple()
				{
					_values.clear();
				}

				@Override
				public void tupleValue(Value value)
				{
					_values.add(value);
				}
			});
		}
	}

	static void usage()
	{
		System.err.println("Usage:  rulepath ...");
		System.exit(1);
	}

	public static void main(String args[])
	{
		if (args.length < 1)
		{
			usage();
		}

		try
		{
			Map<String, String> map = new TreeMap<>();
			map.put("dir", ".");
			_repository.initialize(map);
			_repository.startTransaction();

			for (int i = 0; i < args.length; ++i)
			{
				loadRules(args[i]);
			}

			_repository.commitTransaction();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
