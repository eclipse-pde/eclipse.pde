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
/**
 * @see IModelChangedEvent#getChangedType
 */
public int getChangeType() {
	return type;
}
}
