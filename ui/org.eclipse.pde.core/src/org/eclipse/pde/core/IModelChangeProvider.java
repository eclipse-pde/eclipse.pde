/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core;

/**
 * Classes that implement this interface are
 * capable of notifying listeners about
 * model changes. Interested parties
 * should implement <samp>IModelChangedListener</samp>
 * and add as listeners to be able to receive
 * change notification.
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IModelChangeProvider {
	/**
	 * Adds the listener to the list of listeners that will be
	 * notified on model changes.
	 * @param listener a model change listener to be added
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void addModelChangedListener(IModelChangedListener listener);
	/**
	 * Delivers change event to all the registered listeners.
	 * @param event a change event that will be passed to all the listeners
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
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
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void fireModelObjectChanged(
		Object object,
		String property,
		Object oldValue,
		Object newValue);
	/**
	 * Takes the listener off the list of registered change listeners.
	 *
	 * @param listener a model change listener to be removed
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void removeModelChangedListener(IModelChangedListener listener);
}
