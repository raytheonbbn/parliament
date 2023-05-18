package com.bbn.parliament.jena.jetty;

public class ServerInitException extends Exception {
	private static final long serialVersionUID = 1L;

	public ServerInitException(String fmt, Object... args) {
		super(fmt.formatted(args));
	}

	public ServerInitException(Throwable cause, String fmt, Object... args) {
		super(fmt.formatted(args), cause);
	}
}
