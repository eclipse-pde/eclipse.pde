package org.eclipse.pde.internal.base.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * Classes that implement this interface are
 * capable of notifying listeners about
 * model changes. Interested parties
 * should implement <samp>IModelChangedListener</samp>
 * and add as listeners to be able to receive
 * change notification.
 */
public interface IModelChangeProvider {
/**
 * Adds the listener to the list of listeners that will be
 * notified on model changes.
 */
public void addModelChangedListener(IModelChangedListener listener);
/**
 * Delivers change event to all the registered listeners.
 * @param event a change event that will be passed to all the listeners
 */
public void fireModelChanged(IModelChangedEvent event);
/**
 * Notifies listeners that a property of a model object changed.
 * This is a utility method that will create a model
 * event and fire it.
 *
 * @param object an affected model object
 * @param property name of the property that has changed
 * @param oldValue the old value of the property
 * @param newValue the new value of the property
 */
public void fireModelObjectChanged(Object object, String property, Object oldValue, Object newValue);
/**
 * Takes the listener off the list of registered change listeners.
 *
 * @param listener the listener to be removed
 */
public void removeModelChangedListener(IModelChangedListener listener);
}
