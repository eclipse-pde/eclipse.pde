package org.eclipse.pde.internal.editor.manifest;

import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.views.properties.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.base.model.plugin.*;

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
}
