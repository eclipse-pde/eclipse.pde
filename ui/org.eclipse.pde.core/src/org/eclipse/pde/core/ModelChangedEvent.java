/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core;


/**
 * @see IModelChangedEvent
 * <p>
 * <b>Note:</b> This class is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public class ModelChangedEvent implements IModelChangedEvent {
	private int type;
	private IModelChangeProvider provider;
	private Object[] changedObjects;
	private Object oldValue, newValue;
	private String changedProperty;
	/**
	 * The constructor of the event.
	 * @param provider the change provider
	 * @param type the event type
	 * @param objects the changed objects
	 * @param changedProperty or <samp>null</samp> if not applicable
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public ModelChangedEvent(IModelChangeProvider provider, int type, Object[] objects, String changedProperty) {
		this.type = type;
		this.provider = provider;
		this.changedObjects = objects;
		this.changedProperty = changedProperty;
	}

	/**
	 * A costructor that should be used for changes of object properties.
	 * @param provider the event provider
	 * @param object affected object
	 * @param changedProperty changed property of the affected object
	 * @param oldValue the value before the change
	 * @param newValue the value after the change
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */

	public ModelChangedEvent(
		IModelChangeProvider provider,
		Object object,
		String changedProperty,
		Object oldValue,
		Object newValue) {
		this.type = CHANGE;
		this.provider = provider;
		this.changedObjects = new Object[] { object };
		this.changedProperty = changedProperty;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	/**
	 * @see IModelChangedEvent#getChangeProvider
	 */
	public IModelChangeProvider getChangeProvider() {
		return provider;
	}
	/**
	 * @see IModelChangedEvent#getChangedObjects
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public Object[] getChangedObjects() {
		return changedObjects;
	}
	/**
	 * @see IModelChangedEvent#getChangedProperty
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public String getChangedProperty() {
		return changedProperty;
	}

	/**
	 * The old property value.
	 * @return the value before the change
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public Object getOldValue() {
		return oldValue;
	}

	/**
	 * The new property value.
	 * @return the value after the change
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public Object getNewValue() {
		return newValue;
	}
	/**
	 * @see IModelChangedEvent#getChangedType
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public int getChangeType() {
		return type;
	}
}
