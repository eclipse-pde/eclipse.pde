package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.schema.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.base.model.plugin.*;
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
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.base.model.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.*;

public class ExtensionElementPropertySource
	extends ManifestPropertySource
	implements IOpenablePropertySource {
	public static final String KEY_FINISH =
		"ManifestEditor.ExtensionElementPR.finish";
	private Vector descriptors;

	class PropertyLabelProvider extends LabelProvider {
		private Image image;
		private String name;
		public PropertyLabelProvider(String name, Image image) {
			this.image = image;
			this.name = name;
		}
		public String getText(Object obj) {
			IPluginAttribute att = getElement().getAttribute(name);
			Object value = att != null ? att.getValue() : null;
			return value != null ? value.toString() : "";
		}
		public Image getImage(Object obj) {
			return image;
		}
	}

public ExtensionElementPropertySource(IPluginElement element) {
	super(element);
}
public boolean canGenerate(IPropertySheetEntry entry) {
	if (isEditable()==false) return false;
	String name = entry.getDisplayName();
	IPluginAttribute att = getElement().getAttribute(name);
	if (att == null)
		return false;
	ISchemaAttribute info = att.getAttributeInfo();
	if (info == null)
		return false;
	String baseType = info.getBasedOn();
	if (info.getKind() != ISchemaAttribute.JAVA
		|| baseType == null
		|| baseType.length() == 0)
		return false;
	String value = entry.getValueAsString();
	if (value == null || value.length() == 0)
		return false;
	return true;
}
private String createDescription(ISchemaAttribute att) {
	String fullText = att.getDescription();
	StringBuffer buff = new StringBuffer();
	boolean tag = false;
	for (int i=0; i<fullText.length(); i++) {
		char c = fullText.charAt(i);
		if (c=='<') tag=true;
		else if (c=='>') tag=false;
		else if (tag) continue;
		else if (c=='\r' || c=='\n') {
			buff.append(' ');
		}
		else if (c=='.') {
			break;
		}
		else buff.append(c);
	}
	return buff.toString();
}
protected PropertyDescriptor createPropertyDescriptor(ISchemaAttribute att) {
	String description = createDescription(att);
	if (!isEditable()) {
		PropertyDescriptor desc = new PropertyDescriptor(att.getName(), att.getName());
		desc.setDescription(description);
		return desc;
	}
	ISchemaSimpleType type = att.getType();
	PropertyDescriptor desc = null;
	if (type.getName().equals("boolean")) {
		desc =
			new ComboBoxPropertyDescriptor(
				att.getName(),
				att.getName(),
				new String[] { "false", "true" });
	} else {
		ISchemaRestriction restriction = type.getRestriction();
		if (restriction instanceof ChoiceRestriction) {
			String[] choices = ((ChoiceRestriction) restriction).getChoicesAsStrings();
			desc = new ComboBoxPropertyDescriptor(att.getName(), att.getName(), choices);
		}
	}
	if (desc == null)
		desc = new ModifiedTextPropertyDescriptor(att.getName(), att.getName());
	if (desc instanceof ComboBoxPropertyDescriptor)
		desc.setLabelProvider(new PropertyLabelProvider(att.getName(), null));
	desc.setDescription(description);
	return desc;
}
public void createPropertyDescriptors() {
	descriptors = new Vector();
	Image reqImage = PDEPluginImages.get(PDEPluginImages.IMG_ATT_REQ_OBJ);
	Image classImage = PDEPluginImages.get(PDEPluginImages.IMG_ATT_CLASS_OBJ);
	Image resourceImage = PDEPluginImages.get(PDEPluginImages.IMG_ATT_FILE_OBJ);
	ISchemaElement info = getElement().getElementInfo();
	ISchemaAttribute[] attributes = info.getAttributes();
	for (int i = 0; i < attributes.length; i++) {
		ISchemaAttribute att = attributes[i];
		PropertyDescriptor desc;

		if (att.getKind() == att.JAVA) {
			desc = new JavaAttributeDescriptor(att.getName(), att.getName(), !isEditable());
			desc.setLabelProvider(new PropertyLabelProvider(att.getName(), classImage));
		} else
			if (att.getKind() == att.RESOURCE) {
				desc =
					new ResourceAttributeDescriptor(att.getName(), att.getName(), !isEditable());
				desc.setLabelProvider(new PropertyLabelProvider(att.getName(), resourceImage));
			} else {
				desc = createPropertyDescriptor(att);
				if (att.getUse() == att.REQUIRED) {
					desc.setLabelProvider(new PropertyLabelProvider(att.getName(), reqImage));
				} else {
				}
			}
		descriptors.addElement(desc);
	}
}
private void createTask(IFile file) {
	String message = PDEPlugin.getFormattedMessage(KEY_FINISH, file.getName());
	try {
		IMarker marker = file.createMarker(IMarker.TASK);
		marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_LOW);
		marker.setAttribute(IMarker.MESSAGE, message);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
public Object getEditableValue() {
	return null;
}
public IPluginElement getElement() {
	return (IPluginElement)object;
}
private Object getIndexUsingType(ISchemaSimpleType type, Object value) {
	String svalue = value != null ? value.toString().toLowerCase() : "";
	if (type.getName().equals("boolean")) {
		if (svalue.equals("true"))
			return new Integer(1);
		else
			return new Integer(0);
	} else {
		ISchemaRestriction restriction = type.getRestriction();
		if (restriction instanceof ChoiceRestriction) {
			String[] choices = ((ChoiceRestriction) restriction).getChoicesAsStrings();
			for (int i = 0; i < choices.length; i++) {
				if (choices[i].equals(svalue))
					return new Integer(i);
			}
			return new Integer(0);
		}
	}
	return value!=null?value:"";
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	ISchemaElement element = getElement().getElementInfo();
	if (element!=null) {
		ISchema schema = element.getSchema();
		if (schema.isEditable()) {
			descriptors = null;
		}
	}
	if (descriptors == null)
		createPropertyDescriptors();
	return toDescriptorArray(descriptors);
}
public Object getPropertyValue(Object name) {
	IPluginAttribute att = getElement().getAttribute(name.toString());
	ISchemaElement elementInfo = getElement().getElementInfo();
	ISchemaAttribute attInfo =
		elementInfo != null ? elementInfo.getAttribute(name.toString()) : null;
	IResource resource = getElement().getModel().getUnderlyingResource();
	IProject project = resource != null ? resource.getProject() : null;

	if (att == null) {
		// Make sure we still return special values
		if (isEditable() && attInfo != null) {
			if (attInfo.getKind() == ISchemaAttribute.JAVA) {
				IPluginModelBase model = getElement().getModel();
				return new JavaAttributeValue(project, model, attInfo, "");
			} else
				if (attInfo.getKind() == ISchemaAttribute.RESOURCE) {
					return new ResourceAttributeValue(project, "");
				} else {
					// check the type
					ISchemaSimpleType type = attInfo.getType();
					return getIndexUsingType(type, attInfo.getValue());
				}
		}
	} else {
		Object value = att.getValue();
		if (attInfo != null) {
			if (attInfo.getKind() == ISchemaAttribute.JAVA) {
				IPluginModelBase model = att.getModel();
				return new JavaAttributeValue(project, model, attInfo, value.toString());
			} else
				if (attInfo.getKind() == ISchemaAttribute.RESOURCE) {
					return new ResourceAttributeValue(project, value.toString());
				} else if (isEditable()) {
					// check the type
					ISchemaSimpleType type = attInfo.getType();
					return getIndexUsingType(type, value);
				}
		}
		return value;
	}
	return "";
}
private Object getStringUsingType(ISchemaSimpleType type, Object value) {
	if (value instanceof Integer) {
		int index = ((Integer) value).intValue();
		if (type.getName().equals("boolean")) {
			return index == 1 ? "true" : "false";
		}
		ISchemaRestriction restriction = type.getRestriction();
		if (restriction instanceof ChoiceRestriction) {
			String[] choices = ((ChoiceRestriction) restriction).getChoicesAsStrings();
			if (index >= 0 && index < choices.length)
				return choices[index];
		}
	}
	return "";
}
public boolean isAlreadyCreated(String fullName) {
	int nameloc = fullName.lastIndexOf('.');
	String packageName = fullName.substring(0, nameloc);
	String className = fullName.substring(nameloc + 1);

	String javaFileName = className + ".java";
	IProject project = getProject();
	IWorkspace workspace = project.getWorkspace();
	IPath path = project.getFullPath().append(packageName.replace('.', '/'));
	IPath filePath = path.append(javaFileName);
	return workspace.getRoot().exists(filePath);
	
}
public boolean isOpenable(IPropertySheetEntry entry) {
	if (isEditable()==false) return false;
	String name = entry.getDisplayName();
	IPluginAttribute att = getElement().getAttribute(name);
	if (att==null) return false;
	ISchemaAttribute info = att.getAttributeInfo();
	if (info==null) return false;
	if (info.getKind()!=ISchemaAttribute.JAVA && info.getKind()!=ISchemaAttribute.RESOURCE) return false;
	String value = entry.getValueAsString();
	if (value==null || value.length()==0) return false;
	return true;
}
public boolean isPropertySet(Object property) {
	return false;
}
public void openInEditor(IPropertySheetEntry entry) {
	String name = entry.getDisplayName();
	IPluginAttribute att = getElement().getAttribute(name);
	if (att == null)
		return;
	ISchemaAttribute info = att.getAttributeInfo();
	if (info == null)
		return;
	String value = entry.getValueAsString();
	if (value == null || value.length() == 0)
		return;
	if (info.getKind() == ISchemaAttribute.JAVA) {
		openJavaFile(value);
	} else
		if (info.getKind() == ISchemaAttribute.RESOURCE) {
			openResourceFile(value);
		};
}
private void openJavaFile(String name) {
	IJavaProject jproject = getJavaProject();

	String path = name.replace('.', '/') + ".java";
	try {
		IJavaElement result = jproject.findElement(new Path(path));
		if (result != null) {
			JavaUI.openInEditor(result);
		}
	} catch (PartInitException e) {
		Display.getCurrent().beep();
	} catch (JavaModelException e) {
		// nothing
		Display.getCurrent().beep();
	}
}
private void openResourceFile(String name) {
	IPath path = getProject().getFullPath().append(name);
	IFile file = getProject().getWorkspace().getRoot().getFile(path);
	if (file.exists()) {
		try {
			PDEPlugin.getActivePage().openEditor(file);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	} else {
		Display.getCurrent().beep();
	}
}
public void resetPropertyValue(Object property) {
}
public void setElement(IPluginElement newElement) {
	object = newElement;
}
public void setPropertyValue(Object name, Object value) {
	IPluginElement ee = (IPluginElement) object;
	ISchemaElement elementInfo = ee.getElementInfo();

	if (value instanceof Integer && elementInfo != null) {
		ISchemaAttribute attInfo = elementInfo.getAttribute(name.toString());
		if (attInfo != null) {
			value = getStringUsingType(attInfo.getType(), value);
		}
	}

	String valueString = value.toString();

	try {
		ee.setAttribute(
			name.toString(),
			(valueString == null | valueString.length() == 0) ? null : valueString);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
}
