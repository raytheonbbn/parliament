// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.recovery;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author dkolas */
public class RecoveryManager {
	public static final String JOURNAL_FILE_NAME = "recovery.journal";
	public static final long AUTO_FLUSH_INTERVAL = 5000;
	private static final Logger LOG = LoggerFactory.getLogger(RecoveryManager.class);

	private final File _journalFile;
	private volatile BufferedWriter _writer;

	public RecoveryManager(String directoryPath, Recoverable recoverable) {
		_journalFile = new File(directoryPath, JOURNAL_FILE_NAME);

		if (_journalFile.exists() && _journalFile.length() > 0) {
			recover(recoverable);
		}

		Thread autoFlusher = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(AUTO_FLUSH_INTERVAL);
				} catch (InterruptedException e) {
					// Who cares?
				}
				try {
					getWriter().flush();
				} catch (IOException ex) {
					throw new RuntimeException("Can't flush journal!", ex);
				}
			}
		});
		autoFlusher.setName("RecoveryManagerAutoFlusher");
		autoFlusher.setDaemon(true);
		autoFlusher.start();
	}

	public void startBlock() throws IOException {
		getWriter().write("~start~");
	}

	public void endBlock() throws IOException {
		getWriter().write("~end~");
	}

	public void recordAdd(String subject, String predicate, String object) throws IOException {
		getWriter().write(appendTriple('+', subject, predicate, object));
	}

	public void recordDelete(String subject, String predicate, String object) throws IOException {
		getWriter().write(appendTriple('-', subject, predicate, object));
	}

	private static String appendTriple(char operation, String subject, String predicate, String object) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(operation);
		buffer.append(subject);
		buffer.append(' ');
		buffer.append(predicate);
		buffer.append(' ');
		buffer.append(object);
		buffer.append('\n');
		return buffer.toString();
	}

	/**
	 * Get the writer, with lazy initialization. This follows the double-check idiom
	 * for lazy initialization of an instance field. See Item 83 of Effective Java,
	 * Third Edition, by Joshua Bloch for details.
	 *
	 * @return the writer
	 */
	private BufferedWriter getWriter() throws IOException {
		BufferedWriter result = _writer;
		if (result == null) {
			synchronized (this) {
				if (_writer == null) {
					_writer = result = new BufferedWriter(
						new OutputStreamWriter(
							new FileOutputStream(_journalFile), StandardCharsets.UTF_8));
				}
			}
		}
		return result;
	}

	public void instanceFlushed() throws IOException {
		if (_writer != null) {
			_writer.close();
		}
		_writer = null;
		_journalFile.delete();
	}

	private void recover(Recoverable recoverable) {
		LOG.warn("Needed to recover unflushed changes!");
		boolean finishedRecovery = false;
		try (BufferedReader rdr = Files.newBufferedReader(_journalFile.toPath(), StandardCharsets.UTF_8)) {
			String line = null;
			while ((line = rdr.readLine()) != null) {
				int firstSpace = line.indexOf(' ');
				int secondSpace = line.indexOf(' ', firstSpace + 1);
				String subject = line.substring(1, firstSpace);
				String predicate = line.substring(firstSpace + 1, secondSpace);
				String object = line.substring(secondSpace + 1);

				if (line.charAt(0) == '+') {
					LOG.debug("Recovery: adding statement: {} {} {}", subject, predicate, object);
					recoverable.recoverAdd(subject, predicate, object);
				} else if (line.charAt(0) == '-') {
					LOG.debug("Recovery: deleting statement: {} {} {}", subject, predicate, object);
					recoverable.recoverDelete(subject, predicate, object);
				}
			}
			recoverable.recoverFlush();
			finishedRecovery = true;
			LOG.warn("Recovered successfully!");
		} catch (IOException e) {
			throw new RuntimeException("Error during recovery!", e);
		}
		if (finishedRecovery) {
			_journalFile.delete();
		}
	}
}
