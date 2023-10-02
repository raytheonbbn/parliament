// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.rmi.server.UID;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletRequest;

import org.apache.commons.io.output.DeferredFileOutputStream;
import org.springframework.boot.system.ApplicationTemp;

import com.bbn.parliament.server.ParliamentBridge;

/** @author sallen */
public class FileBackedRequestBody implements AutoCloseable {
	private static class BodyInputStream extends FilterInputStream {
		private final FileBackedRequestBody fileBackedRequestBody;

		@SuppressWarnings("resource")
		protected BodyInputStream(FileBackedRequestBody fileBackedRequestBody, InputStream in) {
			super(Objects.requireNonNull(in, "in"));
			this.fileBackedRequestBody = Objects.requireNonNull(fileBackedRequestBody, "fileBackedRequestBody");
		}

		@Override
		public void close() throws IOException {
			super.close();
			fileBackedRequestBody.markBodyInputStreamClosed();
		}
	}

	private static final String UID = new UID()
		.toString().replace(':', '_').replace('-', '_');				// for unique id generation
	private static final AtomicLong COUNTER = new AtomicLong(0);	// for unique id generation
	private static final String ZIP_ENTRY_NAME = "requestBodyZipEntry";

	private DeferredFileOutputStream dfos;
	private BodyInputStream bodyInputStream;

	/**
	 * Create a file-backed request body from a servlet request. This operation
	 * destroys (uses up) the input stream of the servlet request.
	 *
	 * @throws IOException
	 */
	public FileBackedRequestBody(ServletRequest request) throws IOException {
		int threshold = ParliamentBridge.getInstance().getConfiguration().getDeferredFileOutputStreamThreshold();
		File tmpFile = new File(new ApplicationTemp().getDir(), getTmpFileName());
		dfos = DeferredFileOutputStream.builder()
			.setThreshold(threshold)
			.setFile(tmpFile)
			.get();

		try (ZipOutputStream zout = new ZipOutputStream(dfos)) {
			zout.putNextEntry(new ZipEntry(ZIP_ENTRY_NAME));
			try (InputStream is = request.getInputStream()) {
				for (var buffer = new byte[16 * 1024];;) {
					int bytesRead = is.read(buffer);
					if (bytesRead == -1) {
						break;
					}
					zout.write(buffer, 0, bytesRead);
				}
			}
			zout.closeEntry();
		}

		bodyInputStream = null;
	}

	private static String getTmpFileName() {
		return "requestBody_%1$s_%2$08d.zip".formatted(UID, COUNTER.getAndIncrement());
	}

	public InputStream openBody() {
		if (bodyInputStream != null) {
			throw new IllegalStateException("Resource body input stream is already open");
		}
		try {
			var zipInputStream = new ZipInputStream(dfos.isInMemory()
				? new ByteArrayInputStream(dfos.getData())
				: new FileInputStream(dfos.getFile()));
			zipInputStream.getNextEntry();
			bodyInputStream = new BodyInputStream(this, zipInputStream);
			return bodyInputStream;
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/** may only be called from BodyInputStream.close(): */
	private void markBodyInputStreamClosed() {
		bodyInputStream = null;
	}

	@Override
	public void close() throws IOException {
		if (bodyInputStream != null) {
			bodyInputStream.close();
		}
		if (dfos != null) {
			File tmpFile = dfos.getFile();
			dfos.close();
			dfos = null;
			if (tmpFile != null && tmpFile.exists()) {
				tmpFile.delete();
			}
		}
	}
}
