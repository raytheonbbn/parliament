// Copyright (c) 2019, 2020 RTX BBN Technologies Corp.

package com.bbn.parliament.client;

import java.io.InputStream;
import java.util.MissingResourceException;

/**
 * A utility class for loading a Java resource into memory.
 *
 * Since Java resources are usually considered part of the code base in which
 * they reside, exceptions generally only happen as the result of programming
 * errors, and should therefore be detected during implementation and testing.
 * Thus, we convert them to runtime exceptions (UncheckedIOException) under the
 * assumption that they won't happen in practice in a real deployment.
 *
 * @author iemmons
 */
public class ResourceUtil {
	private ResourceUtil() {} // Prevent instantiation

	/**
	 * Opens an InputStream on the resource of the given name in the package of the
	 * given class.
	 *
	 * @param resourceName The name of the resource to open
	 * @param cls          A class whose package is the resource's location
	 * @return An InputStream for the indicated resource. If the resource cannot be
	 *         found, the method throws.
	 */
	@SuppressWarnings("resource")
	public static InputStream getAsStream(String resourceName, Class<?> cls) {
		InputStream result = cls.getResourceAsStream(resourceName);	// NOPMD - CloseResource
		return requireNonNull(result, cls, resourceName);
	}

	/**
	 * Opens an InputStream on the resource of the given name.
	 *
	 * @param resourceName The name of the resource to open
	 * @return An InputStream for the indicated resource. If the resource cannot be
	 *         found, the method throws.
	 */
	@SuppressWarnings("resource")
	public static InputStream getAsStream(String resourceName) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream result = cl.getResourceAsStream(resourceName);	// NOPMD - CloseResource
		return requireNonNull(result, null, resourceName);
	}

	private static InputStream requireNonNull(InputStream stream, Class<?> cls, String resourceName) {
		if (stream == null) {
			String message = String.format("Unable to locate resource '%1$s'", resourceName);
			String clsName = (cls == null) ? null : cls.getCanonicalName();
			throw new MissingResourceException(message, clsName, resourceName);
		}
		return stream;
	}
}
