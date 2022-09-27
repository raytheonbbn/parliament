package com.bbn.parliament.jena.bridge.tracker;

public class TrackableException extends Exception {
	private static final long serialVersionUID = 1L;

	public TrackableException(String message) {
		super(message);
	}

	public TrackableException(Throwable cause) {
		super(cause);
	}

	public TrackableException(String message, Throwable cause) {
		super(message, cause);
	}
}
