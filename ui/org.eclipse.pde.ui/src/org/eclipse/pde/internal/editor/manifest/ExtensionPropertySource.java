package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.internal.base.model.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;

public class ExtensionPropertySource extends ManifestPropertySource {
	private Vector descriptors;
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
public String getNonzeroValue(String value) {
	if (value!=null) return value;
	return "";
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	if (descriptors == null) {
		descriptors = new Vector();
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		PropertyDescriptor desc = createTextPropertyDescriptor(P_POINT, "point");
		descriptors.addElement(desc);
		desc.setLabelProvider(
			new PropertyLabelProvider(
				P_POINT,
				provider.get(PDEPluginImages.DESC_ATT_REQ_OBJ)));
		desc = createTextPropertyDescriptor(P_ID, "id");
		descriptors.addElement(desc);
		desc = createTextPropertyDescriptor(P_NAME, "name");
		descriptors.addElement(desc);
	}
	return toDescriptorArray(descriptors);
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
