package com.bbn.parliament.jena.joseki.bridge.tracker;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This class represents an observable object, or "data" in the model-view
 * paradigm. It can be subclassed to represent an object that the application
 * wants to have observed.
 * <p>
 * An observable object can have one or more observers. An observer may be any
 * object that implements interface {@code Observer}. After an observable
 * instance changes, an application calling the {@code Observable}'s
 * {@code notifyObservers} method causes all of its observers to be notified of
 * the change by a call to their {@code update} method.
 * <p>
 * The order in which notifications will be delivered is unspecified. The
 * default implementation provided in the Observable class will notify Observers
 * in the order in which they registered interest, but subclasses may change
 * this order, use no guaranteed order, deliver notifications on separate
 * threads, or may guarantee that their subclass follows this order, as they
 * choose.
 * <p>
 * Note that this notification mechanism has nothing to do with threads and is
 * completely separate from the {@code wait} and {@code notify} mechanism of
 * class {@code Object}.
 * <p>
 * When an observable object is newly created, its set of observers is empty.
 * Two observers are considered the same if and only if the {@code equals}
 * method returns true for them.
 * <p>
 * This class and the Observable interface were copied from the java.util
 * implementations of the same names to avoid their removal after deprecation.
 */
public class Observable {
	private boolean changed;
	private final Set<Observer> observers;

	/** Construct an Observable with zero Observers. */
	public Observable() {
		changed = false;
		observers = new HashSet<>();
	}

	/**
	 * Adds an observer to the set of observers for this object, provided that it is
	 * not the same as some observer already in the set. The order in which
	 * notifications will be delivered to multiple observers is not specified. See
	 * the class comment.
	 *
	 * @param o an observer to be added.
	 * @throws NullPointerException if the parameter o is null.
	 */
	public synchronized void addObserver(Observer o) {
		observers.add(Objects.requireNonNull(o, "o"));
	}

	/**
	 * If this object has changed, as indicated by the {@code hasChanged} method,
	 * then notify all of its observers and then call the {@code clearChanged}
	 * method to indicate that this object has no longer changed.
	 * <p>
	 * Each observer has its {@code update} method called with two arguments: this
	 * observable object and the {@code arg} argument.
	 *
	 * @param arg any object.
	 */
	public void notifyObservers(Object arg) {
		Observer[] arrLocal;	// a temporary snapshot of current Observers

		synchronized (this) {
			/*
			 * We don't want the Observer doing callbacks into arbitrary code while holding
			 * its own Monitor. The code where we extract each Observable from the Vector
			 * and store the state of the Observer needs synchronization, but notifying
			 * observers does not (should not). The worst result of any potential
			 * race-condition here is that:
			 * 1) Newly added Observers may miss a notification in progress
			 * 2) Newly unregistered Observers may be wrongly notified when they don't care
			 */
			if (!changed) {
				return;
			}
			arrLocal = observers.toArray(length -> new Observer[length]);
			changed = false;
		}

		for (int i = arrLocal.length-1; i>=0; i--) {
			arrLocal[i].update(this, arg);
		}
	}

	/**
	 * Marks this {@code Observable} object as having been changed.
	 */
	protected synchronized void setChanged() {
		changed = true;
	}
}
