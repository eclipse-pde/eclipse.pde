package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.plugin.*;
import org.eclipse.jdt.internal.ui.util.*;

import java.lang.reflect.*;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.operation.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.*;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.internal.ui.ischema.*;
import org.eclipse.pde.internal.base.model.*;

import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.core.runtime.IProgressMonitor;

public class UnknownElementPropertySource extends ManifestPropertySource {
	private Vector descriptors;
	private static final String TAG_NAME = "Tag name";

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

	PDEProblemFinder.fixMe("Need 1G9NVYL to separate id and name");
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
