// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.recovery;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author dkolas */
public class RecoveryManager
{
	public static final String JOURNAL_FILE_NAME   = "recovery.journal";
	public static final long   AUTO_FLUSH_INTERVAL = 5000;

	private static Logger      _logger             = LoggerFactory.getLogger(RecoveryManager.class);

	private String             _directoryPath;
	private boolean            _needsRecovery;
	BufferedWriter             _writer;
	private File               _journalFile;
	private AutoFlush          _autoFlush;

	public RecoveryManager(String directoryPath, Recoverable recoverable)
	{
		_directoryPath = directoryPath;
		_journalFile = new File(_directoryPath, JOURNAL_FILE_NAME);

		_needsRecovery = _journalFile.exists() && _journalFile.length() > 0;
		if (_needsRecovery)
		{
			recover(recoverable);
		}
		_autoFlush = new AutoFlush();
		new Thread(_autoFlush, "RecoveryManagerAutoFlusher").start();
	}

	public synchronized void recordAdd(String subject, String predicate,
		String object) throws IOException
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append('+');
		buffer.append(triple(subject, predicate, object));
		buffer.append('\n');
		getWriter().write(buffer.toString());
	}

	public synchronized void recordDelete(String subject, String predicate,
		String object) throws IOException
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append('-');
		buffer.append(triple(subject, predicate, object));
		buffer.append('\n');
		getWriter().write(buffer.toString());
	}

	private static String triple(String subject, String predicate, String object)
	{
		StringBuilder result = new StringBuilder();
		result.append(subject);
		result.append(' ');
		result.append(predicate);
		result.append(' ');
		result.append(object);
		return result.toString();
	}

	public void startBlock() throws IOException
	{
		getWriter().write("~start~");
	}

	public void endBlock() throws IOException
	{
		getWriter().write("~end~");
	}

	private BufferedWriter getWriter() throws IOException
	{
		if (_writer == null)
		{
			_writer = new BufferedWriter(new FileWriter(_journalFile));
		}
		return _writer;
	}

	public void instanceFlushed() throws IOException
	{
		if (_writer != null)
		{
			_writer.close();
		}
		_writer = null;
		_journalFile.delete();
	}

	private void recover(Recoverable recoverable)
	{
		_logger.warn("Needed to recover unflushed changes!");
		boolean finishedRecovery = false;
		try (BufferedReader rdr = Files.newBufferedReader(_journalFile.toPath(), StandardCharsets.UTF_8)) {
			String line = null;
			while ((line = rdr.readLine()) != null)
			{
				int firstSpace = line.indexOf(' ');
				int secondSpace = line.indexOf(' ', firstSpace + 1);
				String subject = line.substring(1, firstSpace);
				String predicate = line.substring(firstSpace + 1, secondSpace);
				String object = line.substring(secondSpace + 1);

				if (line.charAt(0) == '+')
				{
					_logger.debug("Recovery: adding statement: {} {} {}",
						new Object[] { subject, predicate, object });
					recoverable.recoverAdd(subject, predicate, object);
				}
				else if (line.charAt(0) == '-')
				{
					_logger.debug("Recovery: deleting statement: {} {} {}",
						new Object[] { subject, predicate, object });
					recoverable.recoverDelete(subject, predicate, object);
				}
			}
			recoverable.recoverFlush();
			finishedRecovery = true;
			_logger.warn("Recovered successfully!");
		}
		catch (IOException e)
		{
			throw new RuntimeException("Error during recovery!", e);
		}
		if (finishedRecovery)
		{
			_journalFile.delete();
		}
	}

	private class AutoFlush implements Runnable
	{
		public AutoFlush() {
		}

		@Override
		public void run()
		{
			while (true)
			{
				try
				{
					Thread.sleep(AUTO_FLUSH_INTERVAL);
				}
				catch (InterruptedException e)
				{
					// Who cares?
				}
				if (_writer != null)
				{
					try
					{
						_writer.flush();
					}
					catch (IOException e)
					{
						throw new RuntimeException("Can't flush journal!", e);
					}
				}
			}
		}
	}
}
