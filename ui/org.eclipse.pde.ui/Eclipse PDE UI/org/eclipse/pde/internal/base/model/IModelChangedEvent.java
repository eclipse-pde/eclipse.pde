package org.eclipse.pde.internal.base.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * Model change events are fired by the model
 * when it is changed from the last clean state.
 * Model change listeners can use these events
 * to update accordingly.
 */
public interface IModelChangedEvent {
/**
 * Indicates a change where one or more objects are added to the model.
 */
	int INSERT = 1;
/**
 * Indicates a change where one or more objects are removed from the model.
 */
	int REMOVE = 2;
/**
 * Indicates that the model has been reloaded and that listeners
 * should perform full refresh.
 */
	int WORLD_CHANGED = 99;
/**
 * indicates that a model object's property has been changed.
 */
	int CHANGE = 3;
/**
 * Returns an array of model objects that are affected
 * by the change.
 *
 * @return array of affected objects
 */
public Object [] getChangedObjects();
/**
 * Returns a name of the object's property that
 * has been changed if change type is CHANGE.
 *
 * @return property that has been changed
 * in the model object, or <samp>null</samp>
 * if type is not CHANGE or if more than
 * one property has been changed.
 */
public String getChangedProperty();
/**
 * Returns the type of change that occured in the model
 * (one of INSERT, REMOVE, CHANGE or WORLD_CHANGED).
 * @return type of change
 */
public int getChangeType();
}
