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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.IOpenablePropertySource;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.views.properties.*;

public class ExtensionPointPropertySource extends ManifestPropertySource implements IOpenablePropertySource {
	private PropertyDescriptor [] descriptors;
	private final static String P_SCHEMA = "schema";
	private final static String P_ID = "id";
	private final static String P_NAME = "name";

	class PropertyLabelProvider extends LabelProvider {
		private Image image;
		private String name;
		public PropertyLabelProvider(String name, Image image) {
			this.image = image;
			this.name = name;
		}
		public String getText(Object obj) {
			Object value = getPropertyValue(name);
			return value!=null?value.toString():"";
		}
		public Image getImage(Object obj) {
			return image;
		}
	}

public ExtensionPointPropertySource(IPluginExtensionPoint point) {
	super(point);
}
public Object getEditableValue() {
	return null;
}
public IPluginExtensionPoint getPoint() {
	return (IPluginExtensionPoint)object;
}
public IPropertyDescriptor [] getPropertyDescriptors() {
	if (descriptors == null) {
		descriptors = new PropertyDescriptor[3];
		descriptors[0] = createTextPropertyDescriptor(P_ID, "id");
		descriptors[0].setLabelProvider(
			new PropertyLabelProvider(P_ID, PDEPluginImages.get(PDEPluginImages.IMG_ATT_REQ_OBJ)));
		descriptors[1] = createTextPropertyDescriptor(P_NAME, "name");
		descriptors[2] = createTextPropertyDescriptor(P_SCHEMA, "schema");
	}
	return descriptors;
}
public Object getPropertyValue(Object name) {
	if (name.equals(P_ID)) {
		return getNonzeroValue(getPoint().getId());
	}
	if (name.equals(P_NAME)) {
		return getNonzeroValue(getPoint().getName());
	}
	if (name.equals(P_SCHEMA)) {
		return getNonzeroValue(getPoint().getSchema());
	}
	return "";
}
public boolean isOpenable(IPropertySheetEntry entry) {
	String value = entry.getValueAsString();
	if (value != null
		&& value.length() > 0
		&& entry.getDisplayName().equals(P_SCHEMA)) {
		return true;
	}
	return false;
}
public boolean isPropertySet(Object property) {
	return false;
}
public void openInEditor(IPropertySheetEntry entry) {
	String value = entry.getValueAsString();
	if (value == null || value.length() == 0)
		return;
	IPluginModelBase model = getPoint().getPluginModel();
	IResource pluginFile = model.getUnderlyingResource();
	if (pluginFile != null) {
		IProject project = pluginFile.getProject();
		IPath fullPath = project.getFullPath();
		fullPath = fullPath.append(value);
		IWorkspace workspace = project.getWorkspace();
		IFile file = workspace.getRoot().getFile(fullPath);
		if (file.exists() == false)
			return;
		IWorkbenchPage page = PDEPlugin.getActivePage();
		try {
			IDE.openEditor(page, file, true);
			return;
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
}
public void resetPropertyValue(Object property) {
}
public void setPoint(IPluginExtensionPoint newPoint) {
	object = newPoint;
}
public void setPropertyValue(Object name, Object value) {
	String svalue = value.toString();
	String realValue = svalue == null | svalue.length() == 0 ? null : svalue;
	IPluginExtensionPoint exp = (IPluginExtensionPoint) object;
	try {
		if (name.equals(P_ID)) {
			exp.setId(realValue);
		} else
			if (name.equals(P_NAME)) {
				exp.setName(realValue);
			} else
				if (name.equals(P_SCHEMA)) {
					exp.setSchema(realValue);
				}
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
}
