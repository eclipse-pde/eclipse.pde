/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.core.plugin;

import org.eclipse.pde.core.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;

/**
 * @version 	1.0
 * @author
 */
public class AttributeChangedEvent extends ModelChangedEvent {
	public static final String P_ATTRIBUTE_VALUE = "att_value";
	private Object attribute;
	public AttributeChangedEvent(Object element, Object attribute, String oldValue, String newValue) {
		super(element, P_ATTRIBUTE_VALUE, oldValue, newValue);
		this.attribute = attribute;
	}
	
	public Object getChagedAttribute() {
		return attribute;
	}
}
