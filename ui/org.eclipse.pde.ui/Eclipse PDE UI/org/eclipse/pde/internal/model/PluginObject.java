package org.eclipse.pde.internal.model;

import org.w3c.dom.Node;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.*;

public abstract class PluginObject extends PlatformObject implements IPluginObject {
	protected String name;
	private IPluginObject parent;
	private IPluginModelBase model;

public PluginObject() {
}
protected void ensureModelEditable() throws CoreException {
	if (!model.isEditable()) {
		throwCoreException("Illegal attempt to change read-only plug-in manifest model");
	}
}
protected void firePropertyChanged(String property) {
	firePropertyChanged(this, property);
}
protected void firePropertyChanged(IPluginObject object, String property) {
	if (model.isEditable() && model instanceof IModelChangeProvider) {
		IModelChangeProvider provider = (IModelChangeProvider) model;
		provider.fireModelObjectChanged(object, property);
	}
}
protected void fireStructureChanged(IPluginObject child, int changeType) {
	IPluginModelBase model = getModel();
	if (model.isEditable() && model instanceof IModelChangeProvider) {
		IModelChangeProvider provider = (IModelChangeProvider) model;
		provider.fireModelChanged(
			new ModelChangedEvent(changeType, new Object[] { child }, null));
	}
}
private String getAttribute(Node node, String name) {
	Node attribute = node.getAttributes().getNamedItem(name);
	if (attribute!=null) return attribute.getNodeValue();
	return null;
}
public IPluginModelBase getModel() {
	return model;
}
public String getName() {
	return name;
}
String getNodeAttribute(Node node, String name) {
	Node attribute = node.getAttributes().getNamedItem(name);
	if (attribute != null)
		return attribute.getNodeValue();
	return null;
}
public IPluginObject getParent() {
	return parent;
}
public IPluginBase getPluginBase() {
	return model != null ? model.getPluginBase() : null;
}
public String getResourceString(String key) {
	return model.getResourceString(key);
}
static boolean isNotEmpty(String text) {
	for (int i = 0; i < text.length(); i++) {
		if (Character.isWhitespace(text.charAt(i)) == false)
			return true;
	}
	return false;
}
abstract void load(Node node);
void setModel(IPluginModelBase model) {
	this.model = model;
}
public void setName(String name) throws CoreException {
	ensureModelEditable();
	this.name = name;
	firePropertyChanged(P_NAME);
}
void setParent(IPluginObject parent) {
	this.parent = parent;
}
protected void throwCoreException(String message) throws CoreException {
	Status status =
		new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, message, null);
	throw new CoreException(status);
}
public String toString() {
	if (name!=null) return name;
	return super.toString();
}
}
