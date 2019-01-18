/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
	private final int type;
	private final IModelChangeProvider provider;
	private final Object[] changedObjects;
	private Object oldValue, newValue;
	private final String changedProperty;

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

	@Override
	public IModelChangeProvider getChangeProvider() {
		return provider;
	}

	@Override
	public Object[] getChangedObjects() {
		return (changedObjects == null) ? new Object[0] : changedObjects;
	}

	@Override
	public String getChangedProperty() {
		return changedProperty;
	}

	@Override
	public Object getOldValue() {
		return oldValue;
	}

	@Override
	public Object getNewValue() {
		return newValue;
	}

	@Override
	public int getChangeType() {
		return type;
	}
}
