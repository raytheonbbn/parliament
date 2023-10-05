package org.semwebcentral.parliament.odda;

public class OddaException extends Exception {
	private static final long serialVersionUID = 1L;

	public OddaException() {
	}

	public OddaException(String format, Object... args) {
		super(String.format(format, args));
	}

	public OddaException(Throwable cause) {
		super(cause);
	}

	public OddaException(Throwable cause, String format, Object... args) {
		super(String.format(format, args), cause);
	}

	public OddaException(Throwable cause, boolean enableSuppression, boolean writableStackTrace, String format, Object... args) {
		super(String.format(format, args), cause, enableSuppression, writableStackTrace);
	}
}
