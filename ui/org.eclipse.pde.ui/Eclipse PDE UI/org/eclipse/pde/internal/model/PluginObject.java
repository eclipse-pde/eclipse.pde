package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.*;
import java.util.*;
import java.io.PrintWriter;

public abstract class PluginObject extends PlatformObject implements IPluginObject {
	protected String name;
	private IPluginObject parent;
	private IPluginModelBase model;
	private Vector comments;

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

public void addComments(Node node) {
	comments = addComments(node, comments);
}

public Vector addComments(Node node, Vector result) {
	for (Node prev=node.getPreviousSibling(); 
				prev!=null; prev=prev.getPreviousSibling()) {
		if (prev.getNodeType()==Node.TEXT_NODE) continue;
		if (prev instanceof Comment) {
			String comment = prev.getNodeValue();
			if (result==null) result = new Vector();
			result.add(comment);
		}
		else break;
	}
	return result;
}

void writeComments(PrintWriter writer) {
	writeComments(writer, comments);
}

void writeComments(PrintWriter writer, Vector source) {
	if (source==null) return;
	for (int i=0; i<source.size(); i++) {
		String comment = (String)source.elementAt(i);
		writer.println("<!--"+comment+"-->");
	}
}
}