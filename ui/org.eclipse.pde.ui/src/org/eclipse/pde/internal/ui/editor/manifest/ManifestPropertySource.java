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

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.editor.ModifiedTextPropertyDescriptor;
import org.eclipse.ui.views.properties.*;

public abstract class ManifestPropertySource implements IPropertySource {
	protected IPluginObject object;

public ManifestPropertySource(IPluginObject object) {
	this.object = object;
}
protected PropertyDescriptor createTextPropertyDescriptor(String name, String displayName) {
	if (isEditable()) return new ModifiedTextPropertyDescriptor(name, displayName);
	else return new PropertyDescriptor(name, displayName);
}
protected IJavaProject getJavaProject() {
	IProject project = getProject();
	return JavaCore.create(project);
}
protected IProject getProject() {
	IPluginModelBase model = object.getModel();
	IResource file = model.getUnderlyingResource();
	if (file != null) {
		return file.getProject();
	}
	return null;
}
public boolean isEditable() {
	return object.getModel().isEditable();
}
protected IPropertyDescriptor[] toDescriptorArray(Vector result) {
	IPropertyDescriptor[] array = new IPropertyDescriptor[result.size()];
	result.copyInto(array);
	return array;
}

public String getNonzeroValue(String value) {
	if (value!=null) return value;
	return "";
}
}
