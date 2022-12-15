// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2022, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jni;

public class LibraryLoader {
	private static final String PATH_FIX_LIB_NAME = "FixupParliamentPath";
	private static final String MAIN_LIB_NAME = "Parliament";
	private static final String OS_NAME_PROP = "os.name";

	private LibraryLoader() {}	// prevents instantiation

	public static void loadLibraries() {
		if (isWindows()) {
			System.loadLibrary(PATH_FIX_LIB_NAME);
			addDirToDllPath();
			System.loadLibrary(MAIN_LIB_NAME);
			resetDllPath();
		} else {
			System.loadLibrary(MAIN_LIB_NAME);
		}
	}

	private static boolean isWindows() {
		return System.getProperty(OS_NAME_PROP).toLowerCase().contains("windows");
	}

	private static native void addDirToDllPath();
	private static native void resetDllPath();
}
