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
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IModelChangedListener {
	/**
	 * Called when there is a change in the model
	 * this listener is registered with.
	 *
	 * @param event a change event that describes
	 * the kind of the model change
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void modelChanged(IModelChangedEvent event);
}