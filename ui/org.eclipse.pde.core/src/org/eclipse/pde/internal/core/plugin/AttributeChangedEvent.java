/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.ModelChangedEvent;

public class AttributeChangedEvent extends ModelChangedEvent {
	public static final String P_ATTRIBUTE_VALUE = "att_value"; //$NON-NLS-1$

	private Object attribute;

	public AttributeChangedEvent(IModelChangeProvider provider, Object element, Object attribute, String oldValue, String newValue) {
		super(provider, element, P_ATTRIBUTE_VALUE, oldValue, newValue);
		this.attribute = attribute;
	}

	public Object getChangedAttribute() {
		return attribute;
	}
}
