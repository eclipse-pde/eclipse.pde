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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.*;

public class ExtensionPropertySource extends ManifestPropertySource {
	private PropertyDescriptor [] descriptors;
	private final static String P_POINT = "point";
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

public ExtensionPropertySource(IPluginExtension extension) {
	super(extension);
}
public Object getEditableValue() {
	return null;
}
public IPluginExtension getExtension() {
	return (IPluginExtension)object;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	if (descriptors == null) {
		descriptors = new PropertyDescriptor[3];

		descriptors[0] = createTextPropertyDescriptor(P_POINT, "point");
		descriptors[0].setLabelProvider(
		new PropertyLabelProvider(
			P_POINT,
			PDEPluginImages.get(PDEPluginImages.IMG_ATT_REQ_OBJ)));
		descriptors[1] = createTextPropertyDescriptor(P_ID, "id");
		descriptors[2] = createTextPropertyDescriptor(P_NAME, "name");
	}
	return descriptors;
}

public Object getPropertyValue(Object name) {
	if (name.equals(P_ID)) {
		return getNonzeroValue(getExtension().getId());
	}
	if (name.equals(P_NAME)) {
		return getNonzeroValue(getExtension().getName());
	}
	if (name.equals(P_POINT)) {
		return getNonzeroValue(getExtension().getPoint());
	}
	return "";
}
public boolean isPropertySet(Object property) {
	return false;
}
public void resetPropertyValue(Object property) {
}
public void setExtension(IPluginExtension newExtension) {
	object = newExtension;
}
public void setPropertyValue(Object name, Object value) {
	String svalue = value.toString();
	IPluginExtension ex = (IPluginExtension) object;
	if (svalue.length()==0) svalue = null;
	try {
		if (name.equals(P_ID)) {
			ex.setId(svalue);
		} else
			if (name.equals(P_NAME)) {
				ex.setName(svalue);
			} else
				if (name.equals(P_POINT)) {
					ex.setPoint(svalue);
				}
	} catch (CoreException e) {
		System.out.println(e);
	}
}
}
