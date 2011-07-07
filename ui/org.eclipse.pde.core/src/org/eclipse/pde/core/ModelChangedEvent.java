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
 * @see IModelChangedEvent
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public class ModelChangedEvent implements IModelChangedEvent {
	private int type;
	private IModelChangeProvider provider;
	private Object[] changedObjects;
	private Object oldValue, newValue;
	private String changedProperty;

	/**
	 * The constructor of the event.
	 * 
	 * @param provider
	 *            the change provider
	 * @param type
	 *            the event type
	 * @param objects
	 *            the changed objects
	 * @param changedProperty
	 *            or <samp>null </samp> if not applicable
	 */
	public ModelChangedEvent(IModelChangeProvider provider, int type, Object[] objects, String changedProperty) {
		this.type = type;
		this.provider = provider;
		this.changedObjects = objects;
		this.changedProperty = changedProperty;
	}

	/**
	 * A costructor that should be used for changes of object properties.
	 * 
	 * @param provider
	 *            the event provider
	 * @param object
	 *            affected object
	 * @param changedProperty
	 *            changed property of the affected object
	 * @param oldValue
	 *            the value before the change
	 * @param newValue
	 *            the value after the change
	 */
	public ModelChangedEvent(IModelChangeProvider provider, Object object, String changedProperty, Object oldValue, Object newValue) {
		this.type = CHANGE;
		this.provider = provider;
		this.changedObjects = new Object[] {object};
		this.changedProperty = changedProperty;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedEvent#getChangeProvider()
	 */
	public IModelChangeProvider getChangeProvider() {
		return provider;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedEvent#getChangedObjects()
	 */
	public Object[] getChangedObjects() {
		return (changedObjects == null) ? new Object[0] : changedObjects;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedEvent#getChangedProperty()
	 */
	public String getChangedProperty() {
		return changedProperty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedEvent#getOldValue()
	 */
	public Object getOldValue() {
		return oldValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedEvent#getNewValue()
	 */
	public Object getNewValue() {
		return newValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedEvent#getChangeType()
	 */
	public int getChangeType() {
		return type;
	}
}
