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
package org.eclipse.pde.internal.runtime.registry;

import java.util.Vector;

import org.eclipse.ui.views.properties.*;

public abstract class RegistryPropertySource implements IPropertySource {

public Object getEditableValue() {
	return null;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	return null;
}
public boolean isPropertySet(Object id) {
	return false;
}
public void resetPropertyValue(Object id) {}
public void setPropertyValue(Object id, Object value) {}
protected IPropertyDescriptor[] toDescriptorArray(Vector result) {
	IPropertyDescriptor [] array = new IPropertyDescriptor[result.size()];
	result.copyInto(array);
	return array;
}
}
