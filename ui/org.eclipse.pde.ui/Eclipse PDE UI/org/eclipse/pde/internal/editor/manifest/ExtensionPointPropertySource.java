package org.eclipse.pde.internal.editor.manifest;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.ui.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.pde.internal.base.model.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.editor.*;

public class ExtensionPointPropertySource extends ManifestPropertySource implements IOpenablePropertySource {
	private Vector descriptors;
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
		descriptors = new Vector();
		PropertyDescriptor desc = createTextPropertyDescriptor(P_ID, "id");
		desc.setLabelProvider(
			new PropertyLabelProvider(P_ID, PDEPluginImages.get(PDEPluginImages.IMG_ATT_REQ_OBJ)));
		descriptors.addElement(desc);
		desc = createTextPropertyDescriptor(P_NAME, "name");
		descriptors.addElement(desc);
		desc = createTextPropertyDescriptor(P_SCHEMA, "schema");
		descriptors.addElement(desc);
	}
	return toDescriptorArray(descriptors);
}
public Object getPropertyValue(Object name) {
	if (name.equals(P_ID)) {
		return getPoint().getId();
	}
	if (name.equals(P_NAME)) {
		return getPoint().getName();
	}
	if (name.equals(P_SCHEMA)) {
		return getPoint().getSchema();
	}
	return null;
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
	String name = entry.getDisplayName();
	String value = entry.getValueAsString();
	if (value == null || value.length() == 0)
		return;
	IPluginModelBase model = getPoint().getModel();
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
			page.openEditor(file);
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
