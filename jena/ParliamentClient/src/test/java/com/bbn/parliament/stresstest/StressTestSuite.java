// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.stresstest;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/** @author jlerner */
public class StressTestSuite
{
	private static void usage(String message)
	{
		if (message != null)
		{
			System.out.println(message);
			System.out.println();
		}
		System.out.println("Stress test options:");
		System.out.println("    -r <n>: ");
		System.out.println("        Specifies the number of repetitions for the test.");
		System.out.println("        The full suite of tests will be run to completion");
		System.out.println("        on each pass, rather than running each test n");
		System.out.println("        times, so you can see a variety of results while");
		System.out.println("        additional test runs are still executing.");
		System.out.println("    -tc <count1> <count2> ... <countN>:");
		System.out.println("        Specifies the thread count(s) to use for the");
		System.out.println("        stress test.  Each count specified will be");
		System.out.println("        paired with each percentage specified by -wp.");
		System.out.println("    -oc <count1> <count2> ... <countN>:");
		System.out.println("        Specifies the number of operations that");
		System.out.println("        each thread will execute.");
		System.out.println("    -wp <percent1> <percent2> ... <percentN>:");
		System.out.println("        Specifies the writer percentages to use for");
		System.out.println("        the stress test.  Each percentage will be");
		System.out.println("        paired with each count specified by -tc.");
		System.out.println("    -outdir <directory>:");
		System.out.println("        The directory where output files should be written.");
		System.out.println("        If this parameter is omitted, files will be placed in");
		System.out.println("        results/.  You probably want to set this to something");
		System.out.println("        unique every time, because we weren't very creative");
		System.out.println("        with the filenames, and they'll probably keep overwriting");
		System.out.println("        each other in the default directory.");
		System.out.println("    -local:");
		System.out.println("        Specifies that the tests should be run locally, using");
		System.out.println("        the settings specified by the local.* values in ");
		System.out.println("        StressTest.properties.  If neither -local nor ");
		System.out.println("        -remote is specified, local tests are assumed.");
		System.out.println("    -remote:");
		System.out.println("        Specifies that the tests should be run against a");
		System.out.println("        remote repository, using the settings specified ");
		System.out.println("        by the remote.* values in StressTest.properties.");
		System.out.println("        If neither -local nor -remote is specified, local ");
		System.out.println("        tests are assumed.");
		System.exit(0);
	}

	private static int     _reps            = 1;
	private static int[]   _threadCounts;
	private static int[]   _operationCounts;
	private static int[]   _writerPercents;
	private static boolean _csv             = true;
	private static boolean _bLocal          = true;
	private static String  _outputDirectory = "results";

	private static void parseArgs(String[] args)
	{
		List<Integer> threadCounts = new ArrayList<>();
		List<Integer> opCounts = new ArrayList<>();
		List<Integer> percents = new ArrayList<>();

		if (args.length == 0)
		{
			usage("Parameters required.");
		}
		for (int i = 0; i < args.length; i++)
		{
			String flag = args[i];
			if (flag.equals("-r"))
			{
				String value = args[++i];
				_reps = Integer.parseInt(value);
				if (_reps <= 0)
				{
					usage("Positive numbers only!");
				}
			}
			else if (flag.equals("-tc"))
			{
				int j;
				for (j = i + 1; j < args.length; j++)
				{
					try
					{
						// This throws if the string isn't just digits:
						int num = Integer.parseInt(args[j]);
						// ...throw it anyway if it came back negative:
						if (num <= 0)
						{
							usage("Positive numbers only!");
						}
						threadCounts.add(num);
					}
					catch (NumberFormatException e)
					{
						if (args[j].equals("-csv") || args[j].equals("-wp")
							|| args[j].equals("-oc") || args[j].equals("-r")
							|| args[j].equals("-local") || args[j].equals("-remote")
							|| args[j].equals("-outdir"))
						{
							j--;
							break;
						}
						else
						{
							usage("Unknown option " + args[j]);
						}
					}
				}
				i = j;
			}
			else if (flag.equals("-wp"))
			{
				int j;
				for (j = i + 1; j < args.length; j++)
				{
					try
					{
						// This throws if the string isn't just digits:
						int num = Integer.parseInt(args[j]);
						// ...throw it anyway if it came back negative:
						if (num < 0)
						{
							usage("NonNegative numbers only!");
						}
						percents.add(num);
					}
					catch (NumberFormatException e)
					{
						if (args[j].equals("-csv") || args[j].equals("-tc")
							|| args[j].equals("-oc") || args[j].equals("-r")
							|| args[j].equals("-local") || args[j].equals("-remote")
							|| args[j].equals("-outdir"))
						{
							j--;
							break;
						}
						else
						{
							usage("Unknown option " + args[j]);
						}
					}
				}
				i = j;
			}
			else if (flag.equals("-oc"))
			{
				int j;
				for (j = i + 1; j < args.length; j++)
				{
					try
					{
						// This throws if the string isn't just digits:
						int num = Integer.parseInt(args[j]);
						// ...throw it anyway if it came back negative:
						if (num <= 0)
						{
							usage("Positive numbers only!");
						}
						opCounts.add(num);
					}
					catch (NumberFormatException e)
					{
						if (args[j].equals("-csv") || args[j].equals("-tc")
							|| args[j].equals("-wp") || args[j].equals("-r")
							|| args[j].equals("-local") || args[j].equals("-remote")
							|| args[j].equals("-outdir"))
						{
							j--;
							break;
						}
						else
						{
							usage("Unknown option " + args[j]);
						}
					}
				}
				i = j;
			}
			else if (flag.equals("-csv"))
			{
				_csv = true;
			}
			else if (flag.equals("-local"))
			{
				_bLocal = true;
			}
			else if (flag.equals("-remote"))
			{
				_bLocal = false;
			}
			else if (flag.equals("-outdir"))
			{
				_outputDirectory = args[i + 1];
				System.out.println("Output directory is " + _outputDirectory);
				i++;
			}
			else
			{
				usage("Unknown option " + flag);
			}
		}

		_threadCounts = new int[threadCounts.size()];
		System.out.print("Thread counts: ");
		for (int i = 0; i < threadCounts.size(); i++)
		{
			_threadCounts[i] = threadCounts.get(i).intValue();
			System.out.print(_threadCounts[i] + " ");
		}
		System.out.println();
		_operationCounts = new int[opCounts.size()];
		System.out.print("Operation counts: ");
		for (int i = 0; i < opCounts.size(); i++)
		{
			_operationCounts[i] = opCounts.get(i).intValue();
			System.out.print(_operationCounts[i] + " ");
		}
		System.out.println();
		_writerPercents = new int[percents.size()];
		System.out.print("Percentages: ");
		for (int i = 0; i < percents.size(); i++)
		{
			_writerPercents[i] = percents.get(i).intValue();
			System.out.print(_writerPercents[i] + " ");
		}
		System.out.println();
	}

	public static void writeArray(String[][] array, PrintStream out)
	{
		for (int i = 0; i < array.length; ++i)
		{
			String[] row = array[i];
			for (int j = 0; j < row.length - 1; ++j)
			{
				out.print(array[i][j] + ",");
			}
			out.println(array[i][row.length - 1]);
		}
	}

	public static void performTests() throws IOException
	{
		long timeInMilliseconds = 0;
		String fileExtension = _csv ? ".csv" : ".txt";
		File outdir = new File(_outputDirectory);
		if (!outdir.exists())
		{
			outdir.mkdir();
		}
		DecimalFormat format = new DecimalFormat("0.000");
		long startTime = System.nanoTime();
		for (int r = 0; r < _reps; r++)
		{
			for (int p = 0; p < _writerPercents.length; p++)
			{
				int percentage = _writerPercents[p];
				String[][] tpArray = new String[_threadCounts.length + 1][_operationCounts.length + 1];
				String[][] rtArray = new String[_threadCounts.length + 1][_operationCounts.length + 1];

				// add the titles
				tpArray[0][0] = "#Threads";
				rtArray[0][0] = "#Threads";
				for (int i = 0; i < _operationCounts.length; ++i)
				{
					tpArray[0][i + 1] = _operationCounts[i] + " ops/thread";
					rtArray[0][i + 1] = _operationCounts[i] + " ops/thread";
				}
				for (int i = 0; i < _threadCounts.length; ++i)
				{
					rtArray[i + 1][0] = "" + _threadCounts[i];
					tpArray[i + 1][0] = "" + _threadCounts[i];
				}

				// start the test
				for (int o = 0; o < _operationCounts.length; o++)
				{
					int opCount = _operationCounts[o];
					// run a stress test for each number of threads and record
					// results in throughput and responsetime files
					for (int t = 0; t < _threadCounts.length; ++t)
					{
						AbstractStressTest st;
						if (_bLocal)
						{
							throw new UnsupportedOperationException("Local stress test not yet implemented");
							//st = new LocalStressTest(_threadCounts[t], opCount, percentage, _csv);
						}
						else
						{
							st = new RemoteStressTest(_threadCounts[t], opCount, percentage, _csv);
						}
						st.doit();
						timeInMilliseconds += (st.getTotalElapsedTime() / 1000000);
						// store results for printing after all thread sets are complete:
						tpArray[t + 1][o + 1] = format.format(st.getThroughput());
						rtArray[t + 1][o + 1] = format.format(st.getAverageResponseTime());
					}
				}
				String filePrefix = (_bLocal ? "local_" : "remote_");
				File tpFile = new File(outdir, filePrefix + "run" + r + "_wp"
					+ percentage + "_throughput" + fileExtension);
				File rtFile = new File(outdir, filePrefix + "run" + r + "_wp"
					+ percentage + "_responsetime" + fileExtension);
				try (
					PrintStream tpOut = new PrintStream(tpFile, "UTF-8");
					PrintStream rtOut = new PrintStream(rtFile, "UTF-8");
				) {
					writeArray(tpArray, tpOut);
					writeArray(rtArray, rtOut);
				}
			}
		}
		long actualTime = System.nanoTime() - startTime;
		System.out.println();
		System.out.println("Total elapsed time: " + timeInMilliseconds + "ms");
		System.out
		.println("Actual time passed: " + (actualTime / 1000000) + "ms");
	}

	public static void main(String[] args)
	{
		parseArgs(args);
		try
		{
			performTests();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
