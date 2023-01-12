package com.bbn.parliament.test_util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.MissingResourceException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.jena.rdf.model.Model;

import com.bbn.parliament.client.jena.RDFFormat;

public class RdfResourceLoader {
	@FunctionalInterface
	public static interface SampleDataConsumer {
		void accept(RDFFormat rdfFormat, InputStream input);
	}

	// This class is used to circumvent Jena closing the zip input stream prematurely:
	private static class NonClosingInputStream extends FilterInputStream {
		public NonClosingInputStream(InputStream in) {
			super(in);
		}

		@Override
		public int read() throws IOException {
			return super.read();
		}

		@Override
		public int read(byte[] b) throws IOException {
			return super.read(b);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return super.read(b, off, len);
		}

		@Override
		public long skip(long n) throws IOException {
			return super.skip(n);
		}

		@Override
		public int available() throws IOException {
			return super.available();
		}

		@Override
		public void close() throws IOException {
			// Do nothing:  Prevents closing of the wrapped stream
		}

		@Override
		public synchronized void mark(int readlimit) {
			super.mark(readlimit);
		}

		@Override
		public synchronized void reset() throws IOException {
			super.reset();
		}

		@Override
		public boolean markSupported() {
			return super.markSupported();
		}
	}

	private RdfResourceLoader() {}	//prevents instantiation

	public static void load(String rsrcName, Model model) {
		load(rsrcName,
			(rdfFormat, inStream) -> model.read(inStream, null, rdfFormat.toString()));
	}

	public static void load(File rsrcFile, Model model) {
		load(rsrcFile,
			(rdfFormat, inStream) -> model.read(inStream, null, rdfFormat.toString()));
	}

	private static void load(String rsrcName, SampleDataConsumer consumer) {
		try (InputStream is = getResourceAsStream(rsrcName)) {
			RDFFormat rdfFmt = RDFFormat.parseFilename(rsrcName);
			load(is, rdfFmt, consumer);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static void load(File rsrcFile, SampleDataConsumer consumer) {
		try (InputStream is = new FileInputStream(rsrcFile)) {
			RDFFormat rdfFmt = RDFFormat.parseFilename(rsrcFile);
			load(is, rdfFmt, consumer);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static void load(InputStream is, RDFFormat rdfFmt, SampleDataConsumer consumer) throws IOException {
		if (rdfFmt.isJenaReadable()) {
			consumer.accept(rdfFmt, is);
		} else if (rdfFmt == RDFFormat.ZIP) {
			try (
				ZipInputStream zis = new ZipInputStream(is, StandardCharsets.UTF_8);
				NonClosingInputStream ncis = new NonClosingInputStream(zis);
			) {
				for (ZipEntry ze = null; (ze = zis.getNextEntry()) != null;) {
					RDFFormat rdfZeFmt = RDFFormat.parseFilename(ze.getName());
					if (!ze.isDirectory() && rdfZeFmt.isJenaReadable()) {
						consumer.accept(rdfZeFmt, ncis);
					}
				}
			}
		}
	}

	public static InputStream getResourceAsStream(String rsrcName) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream is = cl.getResourceAsStream(rsrcName);
		if (is == null) {
			throw new MissingResourceException("Unable to find resource", null, rsrcName);
		}
		return is;
	}

	public static String readResourceAsString(String rsrcName) throws IOException {
		try (InputStream is = getResourceAsStream(rsrcName)) {
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
