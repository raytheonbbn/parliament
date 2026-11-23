package com.bbn.parliament.server.test_util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFLanguages;

public class RdfFileLoader {
	// This class is used to circumvent Jena closing the zip input stream prematurely:
	private static class NonClosingInputStream extends FilterInputStream {
		public NonClosingInputStream(InputStream in) {
			super(in);
		}

		@Override
		public void close() throws IOException {
			// Do nothing:  Prevents closing of the wrapped stream
		}
	}

	private RdfFileLoader() {}	//prevents instantiation

	public static void load(File file, Model model) {
		var lang = RDFLanguages.resourceNameToLang(file.getPath());
		try (InputStream is = new FileInputStream(file)) {
			if (lang != null) {
				model.read(is, null, lang.getName());
			} else if (file.getName().endsWith(".zip")) {
				try (
					ZipInputStream zis = new ZipInputStream(is, StandardCharsets.UTF_8);
					NonClosingInputStream ncis = new NonClosingInputStream(zis);
				) {
					for (ZipEntry ze = null; (ze = zis.getNextEntry()) != null;) {
						var zeLang = RDFLanguages.pathnameToLang(ze.getName());
						if (!ze.isDirectory() && zeLang != null) {
							model.read(ncis, null, zeLang.getName());
						}
					}
				}
			}
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
