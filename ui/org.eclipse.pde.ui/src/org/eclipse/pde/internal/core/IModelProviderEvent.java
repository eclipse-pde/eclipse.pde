package org.eclipse.pde.internal.core;

import org.eclipse.pde.core.*;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * This event will be delivered to all model provider
 * listeners when a model managed by the model provider
 * changes in some way.
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
 * Returns the models that are added
 *
 * @return the models that have been added or an empty array
 */
IModel [] getAddedModels();

/**
 * Returns the models that are removed
 *
 * @return the models that have been removed or an empty array
 */
IModel [] getRemovedModels();

/**
 * Returns the models that has changed
 *
 * @return the models that has changed or an empty array
 */
IModel [] getChangedModels();

/**
 * Returns the combination of flags indicating type of
 * event. In case of multiple changes, flags are ORed together.
 * (a combination of MODEL_CHANGED, MODEL_ADDED, MODEL_REMOVED)
 *
 * @return the model change type
 */
int getEventTypes();

/**
 * Returns the object that fired this event. 
 */
Object getEventSource();
}
