package org.eclipse.pde.internal.base.model;
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
 * Event is sent after the model has been added.
 */
	int MODEL_ADDED = 1;
/**
 * Event is sent before the model will be removed.
 */
	int MODEL_REMOVED = 2;
/**
 * Event is sent after the model has been changed.
 */
	int MODEL_CHANGED = 3;
/**
 * Returns the model that is affected by this change.
 *
 * @return the model that has changed
 */
IModel getAffectedModel();
/**
 * Returns the type of the model change 
 * (one of MODEL_CHANGED, MODEL_ADDED, MODEL_REMOVED)
 *
 * @return the model change type
 */
int getEventType();
}
