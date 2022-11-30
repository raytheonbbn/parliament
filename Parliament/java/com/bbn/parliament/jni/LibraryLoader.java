package com.bbn.parliament.jni;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryLoader {
	private static final String PATH_FIX_LIB_NAME = "ParliamentPathFixup";
	private static final String MAIN_LIB_NAME = "Parliament";
	private static final String MAIN_LIB_FNAME = System.mapLibraryName(MAIN_LIB_NAME);
	private static final String OS_NAME_PROP = "os.name";
	private static final String LIB_PATH_PROP = "java.library.path";
	private static final String PATH_SEP_PROP = "path.separator";
	private static Logger LOG = LoggerFactory.getLogger(LibraryLoader.class);

	private LibraryLoader() {}	// prevents instantiation

	public static void loadLibraries() {
		try {
			if (isWindows()) {
				System.loadLibrary(PATH_FIX_LIB_NAME);
				File libDir = findLibDir(MAIN_LIB_FNAME).getCanonicalFile();
				LOG.info("Found native library {} in {}", MAIN_LIB_NAME, libDir.getPath());
				addDirToDllPath();
				System.loadLibrary(MAIN_LIB_NAME);
				resetDllPath();
			} else {
				System.loadLibrary(MAIN_LIB_NAME);
			}
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static boolean isWindows() {
		LOG.info("os.name = '{}'", System.getProperty(OS_NAME_PROP));
		return true;
		//return System.getProperty(OS_NAME_PROP).toLowerCase().contains("win");
	}

	private static File findLibDir(String fileName) {
		String libPath = System.getProperty(LIB_PATH_PROP);
		String pathSep = System.getProperty(PATH_SEP_PROP);
		return Stream.of(libPath.split(Pattern.quote(pathSep)))
			.map(File::new)
			.filter(dir -> new File(dir, fileName).isFile())
			.findFirst()
			.orElseThrow(() -> new IllegalStateException(
				"Unable to find native library %1$s (%2$s) on %3$s (%4$s)"
					.formatted(MAIN_LIB_NAME, MAIN_LIB_FNAME, LIB_PATH_PROP, libPath)));
	}

	private static native void addDirToDllPath();
	private static native void resetDllPath();
}
