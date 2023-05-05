// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.core.util;

import java.util.Calendar;

/**
 * Timer represents a simple start-stop timer.
 *
 * @author Paul Neves Created on Nov 5, 2002.
 */
public class Timer
{
	private boolean _running;
	private long    _start;
	private long    _end;
	private long    _total;

	public Timer()
	{
		init();
	}

	private synchronized void init()
	{
		_running = false;
		_start = 0L;
		_end = 0L;
		_total = 0L;
	}

	public synchronized void reset()
	{
		if (_running)
		{
			throw new IllegalStateException("Timer is still running");
		}

		init();
	}

	public synchronized void start()
	{
		if (_running)
		{
			throw new IllegalStateException("Timer is already started");
		}

		_running = true;
		_start = Calendar.getInstance().getTimeInMillis();
	}

	public synchronized void stop()
	{
		if (!_running)
		{
			throw new IllegalStateException("Timer has not been started");
		}

		_end = Calendar.getInstance().getTimeInMillis();
		_total = _end - _start;
		_running = false;
	}

	public synchronized TimeValue getElapsedTime()
	{
		if (_running)
		{
			throw new IllegalStateException("Timer is still running");
		}

		TimeValue retval = new TimeValue();
		retval.setMsec(_total);
		return retval;
	}

	public synchronized boolean isRunning()
	{
		return _running;
	}

	public static void main(String[] args)
	{
		Timer timer = new Timer();

		timer.start();

		for (int i = 0; i < 100000000; i++)
		{
			timer.isRunning();
		}

		timer.stop();

		TimeValue tv = timer.getElapsedTime();

		System.out.println("Elapsed time = " + tv + " milliseconds or "
			+ tv.getSec() + "s" + tv.getUsec() + "usecs");
	}
}
