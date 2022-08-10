package com.bbn.parliament.jena.joseki.client;

public class CmdLineException extends Exception {
	private static final long serialVersionUID = 1L;

	public CmdLineException() {
	}

	public CmdLineException(String message) {
		super(message);
	}

	public CmdLineException(String format, Object... args) {
		super(format.formatted(args));
	}

	public CmdLineException(Throwable cause, String message) {
		super(message, cause);
	}

	public CmdLineException(Throwable cause, String format, Object... args) {
		super(format.formatted(args), cause);
	}
}
