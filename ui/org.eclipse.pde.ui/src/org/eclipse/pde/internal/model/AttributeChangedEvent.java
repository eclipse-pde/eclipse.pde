/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.model;

import org.eclipse.pde.internal.base.model.IModelChangedEvent;
import org.eclipse.pde.internal.base.model.ModelChangedEvent;

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
