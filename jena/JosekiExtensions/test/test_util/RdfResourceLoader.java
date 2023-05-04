package test_util;

import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.jena.rdf.model.Model;

import com.bbn.parliament.client.RDFFormat;

public class RdfResourceLoader {
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

	public static void load(String rsrcName, Model model) throws IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try (InputStream is = cl.getResourceAsStream(rsrcName)) {
			if (is == null) {
				throw new FileNotFoundException("Unable to find resource '%1$s'"
					.formatted(rsrcName));
			}
			RDFFormat rdfFmt = RDFFormat.parseFilename(rsrcName);
			if (rdfFmt.isJenaReadable()) {
				model.read(is, null, rdfFmt.toString());
			} else if (rdfFmt == RDFFormat.ZIP) {
				try (
					ZipInputStream zis = new ZipInputStream(is, StandardCharsets.UTF_8);
					NonClosingInputStream ncis = new NonClosingInputStream(zis);
				) {
					for (ZipEntry ze = null; (ze = zis.getNextEntry()) != null;) {
						RDFFormat rdfZeFmt = RDFFormat.parseFilename(ze.getName());
						if (!ze.isDirectory() && rdfZeFmt.isJenaReadable()) {
							model.read(ncis, null, rdfZeFmt.toString());
						}
					}
				}
			}
		}
	}
}
