package com.bbn.parliament.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

public class UrlUtil {
	public static URL create(String urlStr) throws MalformedURLException {
		try {
			return new URI(urlStr).toURL();
		} catch (URISyntaxException ex) {
			throw new MalformedURLException("URISyntaxException: %1$s".formatted(ex.getMessage()));
		}
	}

	public static URLConnection openConnection(String urlStr) throws IOException {
		return create(urlStr).openConnection();
	}
}
