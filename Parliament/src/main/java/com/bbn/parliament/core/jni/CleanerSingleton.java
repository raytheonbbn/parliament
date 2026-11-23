package com.bbn.parliament.core.jni;

import java.lang.ref.Cleaner;

public class CleanerSingleton {
	private static class CleanerHolder {
		private static final Cleaner INSTANCE = Cleaner.create();
	}

	/**
	 * Get the singleton instance of the model manager. This follows the "lazy
	 * initialization holder class" idiom for lazy initialization of a static field.
	 * See Item 83 of Effective Java, Third Edition, by Joshua Bloch for details.
	 *
	 * @return the instance
	 */
	public static Cleaner inst() {
		return CleanerHolder.INSTANCE;
	}

	private CleanerSingleton() {}// prevents instantiation
}
