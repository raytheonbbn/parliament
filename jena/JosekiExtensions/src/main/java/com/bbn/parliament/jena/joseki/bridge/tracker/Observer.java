package com.bbn.parliament.jena.joseki.bridge.tracker;

/**
 * A class can implement the {@code Observer} interface when it wants to be
 * informed of changes in observable objects.
 * <p>
 * This interface and the Observer class were copied from the java.util
 * implementations of the same names to avoid their removal after deprecation.
 */
public interface Observer {
	/**
	 * This method is called whenever the observed object is changed. An application
	 * calls an {@code Observable} object's {@code notifyObservers} method to have
	 * all the object's observers notified of the change.
	 *
	 * @param o   the observable object.
	 * @param arg an argument passed to the {@code notifyObservers} method.
	 */
	void update(Observable o, Object arg);
}
