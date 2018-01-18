package com.bbn.parliament.jena.graph.index;

/**
 * Thrown when an error occurs in an {@link Index}.
 *
 * @author rbattle
 */
public class IndexException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/** Constructs a new exception with <code>null</code> as its detail message. */
	public IndexException(Index<?> index) {
		super(index.toString());
	}

	/** Constructs a new exception with the specified detail message */
	public IndexException(Index<?> index, String message) {
		super(String.format("%s: %s", index.toString(), message));
	}

	/**
	 * Constructs a new exception with the specified cause and a detail message of
	 * <code>(cause==null ? null : cause.toString())</code> (which typically contains the
	 * class and detail message of <code>cause</code>).
	 */
	public IndexException(Index<?> index, Throwable cause) {
		super(index.toString(), cause);
	}

	/** Constructs a new exception with the specified detail message and cause. */
	public IndexException(Index<?> index, String message, Throwable cause) {
		super(String.format("%s: %s", index.toString(), message), cause);
	}
}
