// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.output.DeferredFileOutputStream;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.shared.JenaException;

/** @author sallen */
public class FileBackedResultSet {
	/** UID used in unique file name generation. */
	private static final String UID = new java.rmi.server.UID().toString().replace(':', '_').replace('-', '_');

	/** Counter used in unique identifier generation. */
	private static final AtomicLong COUNTER = new AtomicLong(0);

	private DeferredFileOutputStream dfos = null;
	private ResultSet resultSet;
	private InputStream underlyingInputStream;
	private ZipInputStream input;

	/**
	 * Create a file-backed result set from any ResultSet object.
	 * This operation destroys (uses up) a ResultSet object that
	 * is not an in-memory one.
	 */
	@SuppressWarnings("resource")
	public FileBackedResultSet(ResultSet rs, File tmpDir, int threshold) {
		String tmpFileName = "resultset_" + UID + "_" + getUniqueId() + ".zip";
		File tmpFile = new File(tmpDir, tmpFileName);

		dfos = DeferredFileOutputStream.builder()
			.setThreshold(threshold)
			.setFile(tmpFile)
			.get();

		try (ZipOutputStream zout = new ZipOutputStream(dfos)) {
			zout.putNextEntry(new ZipEntry("ResultSet.xml"));
			ResultSetFormatter.outputAsXML(zout, rs);
			zout.closeEntry();
		} catch (IOException ex) {
			throw new JenaException("Error writing to FileBackedResultSet OutputStream", ex);
		}

		try {
			underlyingInputStream = dfos.isInMemory()
				? new ByteArrayInputStream(dfos.getData())
				: new FileInputStream(dfos.getFile());
			input = new ZipInputStream(underlyingInputStream);
			input.getNextEntry();
			resultSet = ResultSetFactory.fromXML(input);
		} catch (FileNotFoundException ex) {
			throw new JenaException("Error opening the backing file for FileBackedResultSet", ex);
		} catch (IOException ex) {
			throw new JenaException("Error reading the backing file for FileBackedResultSet", ex);
		}
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	/** Remove the temporary file (if it exists) */
	public void delete() {
		try {
			if (null != input) {
				input.closeEntry();
				input.close();
			}

			if (null != underlyingInputStream) {
				underlyingInputStream.close();
			}
		} catch (IOException ex) {
			throw new JenaException("Error closing FileBackedResultSet InputStream", ex);
		}

		File tmpFile = (dfos == null) ? null : dfos.getFile();
		if (tmpFile != null && tmpFile.exists()) {
			tmpFile.delete();
		}
	}

	/**
	 * Returns an identifier that is unique within the class loader used to
	 * load this class, but does not have random-like appearance.
	 *
	 * @return A String with the non-random looking instance identifier.
	 */
	private static String getUniqueId() {
		return "%1$08d".formatted(COUNTER.getAndIncrement());
	}
}
