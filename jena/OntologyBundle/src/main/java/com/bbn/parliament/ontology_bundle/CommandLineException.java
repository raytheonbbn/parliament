package com.bbn.parliament.ontology_bundle;

public class CommandLineException extends Exception {
	private static final long serialVersionUID = 1L;

	public CommandLineException() {
	}

	public CommandLineException(String format, Object... args) {
		super(String.format(format, args));
	}

	public CommandLineException(Throwable cause, String format, Object... args) {
		super(String.format(format, args), cause);
	}
}
