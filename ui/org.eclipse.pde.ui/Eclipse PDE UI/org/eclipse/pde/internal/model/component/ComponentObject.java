package org.eclipse.pde.internal.model.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.Node;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.runtime.*;
import java.io.*;
import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.pde.internal.*;

public abstract class ComponentObject extends PlatformObject implements IComponentObject {
	IComponentModel model;
	IComponentObject parent;
	protected String label;

protected void ensureModelEditable() throws CoreException {
	if (!model.isEditable()) {
		throwCoreException("Illegal attempt to change read-only component manifest model");
	}
}
protected void firePropertyChanged(String property) {
	firePropertyChanged(this, property);
}
protected void firePropertyChanged(IComponentObject object, String property) {
	if (model.isEditable() && model instanceof IModelChangeProvider) {
		IModelChangeProvider provider = (IModelChangeProvider) model;
		provider.fireModelObjectChanged(object, property);
	}
}
protected void fireStructureChanged(IComponentObject child, int changeType) {
	IComponentModel model = getModel();
	if (model.isEditable() && model instanceof IModelChangeProvider) {
		IModelChangeProvider provider = (IModelChangeProvider) model;
		provider.fireModelChanged(
			new ModelChangedEvent(changeType, new Object[] { child }, null));
	}
}
public IComponent getComponent() {
	return model.getComponent();
}
public String getLabel() {
	return label;
}
public IComponentModel getModel() {
	return model;
}
String getNodeAttribute(Node node, String name) {
	Node attribute = node.getAttributes().getNamedItem(name);
	if (attribute != null)
		return attribute.getNodeValue();
	return null;
}
public IComponentObject getParent() {
	return parent;
}
abstract void parse(Node node);
public void setLabel(java.lang.String newLabel) throws CoreException {
	ensureModelEditable();
	label = newLabel;
	firePropertyChanged(P_LABEL);
}
protected void throwCoreException(String message) throws CoreException {
	Status status =
		new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, message, null);
	throw new CoreException(status);
}
public void write(String indent, PrintWriter writer) {
}
}
