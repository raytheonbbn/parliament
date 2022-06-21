// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jni;

/**
 * @author iemmons
 *
 * This class tests various aspects of the JNI interface, with two goals:
 * (1) To determine how to code a JNI interface in a cross-platform manner, and
 * (2) To understand how to boost the performance of a JNI interface.
 */
public class JniAssessments
{
	private static final String SHORT_TEST_STR = "Hi!";
	private static final int NUM_TRIALS = 5;
	private static final int NUM_WARM_UP_ITERS = 300 * 1000;
	private static final int NUM_STRING_TEST_ITERS = 10 * 1000 * 1000;
	private static final int NUM_METHOD_CALL_ITERS = 10 * 1000 * 1000;
	private static final double NANOS_PER_MILLI = 1000.0 * 1000.0;

	private long _pObj;

	static
	{
		System.loadLibrary("JniAssessments");
	}

	public static native void printJniStringAsHex(String str);

	/** Returns whether the JNI API copies the string */
	public static native boolean testJniStringEncoding(String str,
			boolean testCriticalStrFunctions, boolean testWideCharEncoding);

	public static native void testJniStringCreation(int numIters, boolean useUtf16Chars);

	public static native JniAssessments create();

	/** Intended only to be called by create(). */
	private JniAssessments(long pObj)
	{
		_pObj = pObj;
	}

	@Override
	public void finalize()
	{
		if (_pObj != 0)
		{
			try
			{
				dispose();
			}
			catch (Throwable ex)
			{
				// Do nothing
			}

			_pObj = 0;
		}
	}

	/** Intended only to be called by finalize(). */
	private native void dispose();

	public native double testMethod1(double d);

	public double testMethod2(double d)
	{
		return internalTestMethod2(_pObj, d);
	}

	private native double internalTestMethod2(long pObj, double d);

	public native double testMethod3(double d);

	public static void main(String[] args)
	{
		System.out.format("%nShow Java's string representations forced into C++"
				+ " char types for the string \"%1$s\":%n", SHORT_TEST_STR);
		printJniStringAsHex(SHORT_TEST_STR);

		System.out.format("%nTest memory management characteristics of JNI string API:%n");
		testStringMemMgmt("GetStringUTFChars", false, false);
		testStringMemMgmt("GetStringChars", false, true);
		testStringMemMgmt("GetStringCritical", true, true);

		System.out.format("%nString Performance Tests (Iterations per Millisecond)%n%n"
				+ "Trial,Passing UTF-8 String,Passing UTF-16 String"
				+ ",Passing UTF-16 String (Critical Section),Creating UTF-8 String"
				+ ",Creating UTF-16 String%n");
		runStringPerformanceTests("Warm-up", NUM_WARM_UP_ITERS);
		for (int trial = 0; trial < NUM_TRIALS; ++trial)
		{
			String trialLabel = String.format("Run %1$d", trial);
			runStringPerformanceTests(trialLabel, NUM_STRING_TEST_ITERS);
		}

		System.out.format("%nMethod Call Performance Tests (Iterations per Millisecond)%n%n"
				+ "Trial,Pull Pointer from Java Field,Pass Pointer via Helper Method"
				+ ",Pointer via C++ Lookup Table%n");
		runMethodCallPerformanceTests("Warm-up", NUM_WARM_UP_ITERS);
		for (int trial = 0; trial < NUM_TRIALS; ++trial)
		{
			String trialLabel = String.format("Run %1$d", trial);
			runMethodCallPerformanceTests(trialLabel, NUM_METHOD_CALL_ITERS);
		}
	}

	private static void testStringMemMgmt(String apiName, boolean testCriticalStrFunctions, boolean testWideCharEncoding)
	{
		String fmt = testJniStringEncoding("Hello World!", testCriticalStrFunctions, testWideCharEncoding)
				? "   %1$s returns a copy of the string%n"
						: "   %1$s does not copy the string%n";
		System.out.format(fmt, apiName);
	}

	private static void runStringPerformanceTests(String trialLabel, int numIters)
	{
		JniAssessments tester = JniAssessments.create();
		try
		{
			System.out.format("%1$s", trialLabel);

			long start = System.nanoTime();
			for (int i = 0; i < numIters; ++i)
			{
				testJniStringEncoding("Hello World!", false, false);
			}
			long duration = System.nanoTime() - start;
			double tput = NANOS_PER_MILLI * numIters / duration;
			System.out.format(",%1$f", tput);

			start = System.nanoTime();
			for (int i = 0; i < numIters; ++i)
			{
				testJniStringEncoding("Hello World!", false, true);
			}
			duration = System.nanoTime() - start;
			tput = NANOS_PER_MILLI * numIters / duration;
			System.out.format(",%1$f", tput);

			start = System.nanoTime();
			for (int i = 0; i < numIters; ++i)
			{
				testJniStringEncoding("Hello World!", true, true);
			}
			duration = System.nanoTime() - start;
			tput = NANOS_PER_MILLI * numIters / duration;
			System.out.format(",%1$f", tput);

			start = System.nanoTime();
			testJniStringCreation(numIters, false);
			duration = System.nanoTime() - start;
			tput = NANOS_PER_MILLI * numIters / duration;
			System.out.format(",%1$f", tput);

			start = System.nanoTime();
			testJniStringCreation(numIters, true);
			duration = System.nanoTime() - start;
			tput = NANOS_PER_MILLI * numIters / duration;
			System.out.format(",%1$f", tput);
		}
		finally
		{
			tester.finalize();
		}
		System.out.format("%n");
	}

	private static void runMethodCallPerformanceTests(String runLabel, int numIters)
	{
		JniAssessments tester = JniAssessments.create();
		try
		{
			System.out.format("%1$s", runLabel);

			@SuppressWarnings("unused")
			double result = 0.0;
			long start = System.nanoTime();
			for (int i = 0; i < numIters; ++i)
			{
				result = tester.testMethod1(i);
			}
			long duration = System.nanoTime() - start;
			double tput = NANOS_PER_MILLI * numIters / duration;
			System.out.format(",%1$f", tput);

			start = System.nanoTime();
			for (int i = 0; i < numIters; ++i)
			{
				result = tester.testMethod2(i);
			}
			duration = System.nanoTime() - start;
			tput = NANOS_PER_MILLI * numIters / duration;
			System.out.format(",%1$f", tput);

			start = System.nanoTime();
			for (int i = 0; i < numIters; ++i)
			{
				result = tester.testMethod3(i);
			}
			duration = System.nanoTime() - start;
			tput = NANOS_PER_MILLI * numIters / duration;
			System.out.format(",%1$f", tput);
		}
		finally
		{
			tester.finalize();
		}
		System.out.format("%n");
	}
}
