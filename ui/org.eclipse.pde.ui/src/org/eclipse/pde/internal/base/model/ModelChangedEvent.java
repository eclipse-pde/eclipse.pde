package org.eclipse.pde.internal.base.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.*;
/**
 * @see IModelChangedEvent
 */
public class ModelChangedEvent implements IModelChangedEvent {
	private int type;
	private Object [] changedObjects;
	private Object oldValue, newValue;
	private String changedProperty;
/**
 * The constructor of the event.
 * @param event type
 * @param changed objects
 * @param changedProperty or <samp>null</samp> if not applicable
 */
public ModelChangedEvent(int type, Object [] objects, String changedProperty) {
	this.type = type;
	this.changedObjects = objects;
	this.changedProperty = changedProperty;
}

/**
 * A costructor that should be used for changes of object properties.
 * 
 */

public ModelChangedEvent(Object object, String changedProperty, Object oldValue, Object newValue) {
	this.type = CHANGE;
	this.changedObjects = new Object[] { object };
	this.changedProperty = changedProperty;
	this.oldValue = oldValue;
	this.newValue = newValue;
}
/**
 * @see IModelChangedEvent#getChangedObjects
 */
public Object[] getChangedObjects() {
	return changedObjects;
}
/**
 * @see IModelChangedEvent#getChangedProperty
 */
public String getChangedProperty() {
	return changedProperty;
}

public Object getOldValue() {
	return oldValue;
}

public Object getNewValue() {
	return newValue;
}
/**
 * @see IModelChangedEvent#getChangedType
 */
public int getChangeType() {
	return type;
}
}
