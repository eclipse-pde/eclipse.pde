/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core;

/**
 * This event will be delivered to all model provider listeners when a model
 * managed by the model provider changes in some way.
 * 
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IModelProviderEvent {
	/**
	 * Event is sent after the models have been added.
	 */
	int MODELS_ADDED = 0x1;
	/**
	 * Event is sent before the models will be removed.
	 */
	int MODELS_REMOVED = 0x2;
	/**
	 * Event is sent after the models have been changed.
	 */
	int MODELS_CHANGED = 0x4;

	/**
	 * Event is sent when the target platform changes
	 * 
	 * @since 3.2
	 */
	int TARGET_CHANGED = 0x8;

	/**
	 * Event is sent when the target environment changes
	 * 
	 * @since 3.5
	 */
	int ENVIRONMENT_CHANGED = 0x10;

	/**
	 * Returns the models that are added
	 * 
	 * @return the models that have been added or an empty array
	 */
	IModel[] getAddedModels();

	/**
	 * Returns the models that are removed
	 * 
	 * @return the models that have been removed or an empty array
	 */
	IModel[] getRemovedModels();

	/**
	 * Returns the models that has changed
	 * 
	 * @return the models that has changed or an empty array
	 */
	IModel[] getChangedModels();

	/**
	 * Returns the combination of flags indicating type of event. In case of
	 * multiple changes, flags are ORed together. (a combination of
	 * MODEL_CHANGED, MODEL_ADDED, MODEL_REMOVED)
	 * 
	 * @return the model change type
	 */
	int getEventTypes();

	/**
	 * Returns the object that fired this event.
	 * @return The object that fired this event
	 */
	Object getEventSource();
}
