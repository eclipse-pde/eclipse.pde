package org.eclipse.pde.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * Classes that need to be notified on model
 * changes should implement this interface
 * and add themselves as listeners to
 * the model they want to listen to.
 */
public interface IModelChangedListener {
	/**
	 * Called when there is a change in the model
	 * this listener is registered with.
	 *
	 * @param event a change event that describes
	 * the kind of the model change
	 */
	public void modelChanged(IModelChangedEvent event);
}