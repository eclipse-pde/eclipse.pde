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
package org.eclipse.pde.internal.ui.editor.manifest;

import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.views.properties.*;

public class UnknownElementPropertySource extends ManifestPropertySource {
	private Vector descriptors;
	private String TAG_NAME = PDEPlugin.getResourceString("ManifestEditor.PropertyPage.tagName");

public UnknownElementPropertySource(IPluginElement element) {
	super(element);
}
public void addAttribute(String name, String initialValue) {
	IPluginElement element = (IPluginElement) object;
	try {
		element.setAttribute(name, initialValue);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
public void createPropertyDescriptors() {
	descriptors = new Vector();
	IPluginElement element = (IPluginElement)object;

	PropertyDescriptor nameDesc = createTextPropertyDescriptor(TAG_NAME, TAG_NAME);
	descriptors.addElement(nameDesc);

	IPluginAttribute [] attributes = element.getAttributes();

	for (int i=0; i<attributes.length; i++) {
		IPluginAttribute att = attributes[i];
		PropertyDescriptor desc = createTextPropertyDescriptor(att.getName(), att.getName());
		descriptors.addElement(desc);
	}
}
public Object getEditableValue() {
	return null;
}
public IPluginElement getElement() {
	return (IPluginElement)object;
}
public IPropertyDescriptor [] getPropertyDescriptors() {
	if (descriptors == null)
		createPropertyDescriptors();
	return toDescriptorArray(descriptors);
}
public Object getPropertyValue(Object name) {
	if (name.equals(TAG_NAME)) return getElement().getName();
	IPluginAttribute att = getElement().getAttribute(name.toString());
	if (att!=null) return att.getValue();
	return null;
}
public boolean isPropertySet(Object property) {
	return false;
}
public void removeAttribute(String name) {
	IPluginElement element = (IPluginElement) object;
	try {
		element.setAttribute(name, null);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
	descriptors = null;
}
public void resetPropertyValue(Object property) {
}
public void setElement(IPluginElement newElement) {
	object = newElement;
	descriptors  = null;
}
public void setPropertyValue(Object name, Object value) {
	IPluginElement ee = (IPluginElement) object;

	String valueString = value.toString();
	try {
		if (name.equals(TAG_NAME)) {
			ee.setName(valueString);
		} else {
			ee.setAttribute(
				name.toString(),
				(valueString == null | valueString.length() == 0) ? null : valueString);
		}
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
}
