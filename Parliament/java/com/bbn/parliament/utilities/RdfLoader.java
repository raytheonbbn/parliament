// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.utilities;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.bbn.parliament.jni.KbConfig;
import com.bbn.parliament.jni.KbInstance;
import com.hp.hpl.jena.rdf.arp.ARP;

/** @author Paul Neves Created on Oct 29, 2002. */
public class RdfLoader
{
	public static final int    EXIT_FAILURE   = 1;
	public static final int    EXIT_SUCCESS   = 0;
	public static final String HELP_FLAG      = "--help";
	public static final String LOAD_FLAG      = "--load";
	public static final String TIME_FLAG      = "--time";
	public static final String BATCH_FLAG     = "--batch";
	public static final String SUFFIX_FLAG    = "--suffix";
	public static final String DEFAULT_SUFFIX = ".rdf";

	private static boolean checkOptFlag(final String arg, final String flag,
		final boolean acceptShortFlag)
	{
		return ((acceptShortFlag && arg.length() == 2 && arg.equals(flag
			.substring(1, 2))) || arg.equals(flag));
	}

	public static void usage(String[] args)
	{
		System.err.println(
			"Usage:  java com.bbn.parliament.utilities.RdfLoader  [--time] "
				+ "{--suffix <suffix>} {--load file uri} {--batch dir uri} ...");
		System.exit(EXIT_FAILURE);
	}

	public static void main(String[] args)
	{
		ARP parser = new ARP();
		List<LoadRDFCommand> commands = new ArrayList<>();
		Timer timer = new Timer();
		boolean showTime = false;
		int exitStatus = EXIT_SUCCESS;
		String batchSuffix = DEFAULT_SUFFIX;

		KbConfig config = new KbConfig();
		config.readFromFile();
		config.m_kbDirectoryPath = ".";
		config.m_readOnly = false;
		try (KbInstance kb = new KbInstance(config))
		{
			for (int i = 0; i < args.length; i++)
			{
				String arg = args[i];

				if (checkOptFlag(arg, TIME_FLAG, true))
				{
					showTime = true;
				}
				else if (checkOptFlag(arg, HELP_FLAG, true))
				{
					usage(args);
				}
				else if (checkOptFlag(arg, SUFFIX_FLAG, true))
				{
					if (i + 1 >= args.length)
					{
						throw new IllegalArgumentException(
							"--suffix argument requires one argument");
					}

					batchSuffix = args[i + 1];

					i++;
				}
				else if (checkOptFlag(arg, BATCH_FLAG, true))
				{
					if (i + 2 >= args.length)
					{
						throw new IllegalArgumentException(
							"--batch flag requires two arguments");
					}

					File directory = new File(args[i + 1]);
					URI uri = new URI(args[i + 2]);

					if (!directory.isDirectory())
					{
						throw new IllegalArgumentException(
							"--batch argument requires a directory");
					}

					FilenameFilter filter = new RdfFilenameFilter(batchSuffix);

					String[] filesToLoad = directory.list(filter);

					for (int j = 0; j < filesToLoad.length; j++)
					{
						String name = filesToLoad[j];
						File file = new File(directory, name);
						String baseName = dropFileExtension(name);
						String baseUri = uri.toString() + "/" + baseName;

						LoadRDFCommand cmd = new LoadRDFCommand(file.toString(),
							baseUri, parser);
						commands.add(cmd);
					}

					i += 2;
				}
				else if (checkOptFlag(arg, LOAD_FLAG, true))
				{
					if (i + 2 >= args.length)
					{
						throw new IllegalArgumentException(
							"--load flag requires two arguments");
					}

					LoadRDFCommand cmd = new LoadRDFCommand(args[i + 1],
						args[i + 2], parser);
					commands.add(cmd);

					i += 2;
				}
				else
				{
					usage(args);
				}
			}

			// check for something to do
			if (commands.size() == 0)
			{
				usage(args);
			}

			if (showTime)
			{
				timer.start();
			}

			for (LoadRDFCommand cmd : commands)
			{
				cmd.setStatementHandler(new StatementHandler(kb));
				cmd.load();
				cmd.setStatementHandler(null);
			}

			if (showTime)
			{
				timer.stop();

				System.out.println("elapsed time = "
					+ timer.getElapsedTime().getSec() + "."
					+ timer.getElapsedTime().getUsec() + " seconds ");
			}

		}
		catch (Throwable t)
		{
			System.err.println("FATAL ERROR: " + t);
			exitStatus = EXIT_FAILURE;
		}

		System.exit(exitStatus);
	}

	public static String dropFileExtension(String name)
	{
		String retval = name;
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex != -1)
		{
			retval = name.substring(0, dotIndex);
		}
		return retval;
	}

	static public class RdfFilenameFilter implements FilenameFilter
	{
		private String _fileSuffix;

		public RdfFilenameFilter(String fileSuffix)
		{
			setFileSuffix(fileSuffix);
		}

		/** @see java.io.FilenameFilter#accept(File, String) */
		@Override
		public boolean accept(File dir, String name)
		{
			return name.endsWith(getFileSuffix());
		}

		private void setFileSuffix(String fileSuffix)
		{
			if (fileSuffix == null)
			{
				throw new NullPointerException();
			}

			_fileSuffix = fileSuffix;
		}

		private String getFileSuffix()
		{
			return _fileSuffix;
		}
	}
}
