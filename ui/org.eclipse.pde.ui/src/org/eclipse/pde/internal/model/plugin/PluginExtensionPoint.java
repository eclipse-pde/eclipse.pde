package org.eclipse.pde.internal.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.model.ExtensionPointModel;
import org.w3c.dom.Node;
import org.eclipse.core.runtime.CoreException;
import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.model.plugin.*;
import java.util.Hashtable;

public class PluginExtensionPoint extends IdentifiablePluginObject implements IPluginExtensionPoint {
	private String schema;
	static final String ID_SEPARATOR=".";

public PluginExtensionPoint() {
}
public String getFullId() {
	IPluginBase pluginBase = getModel().getPluginBase();
	String id = pluginBase.getId();
	if (pluginBase instanceof IFragment)
		id = ((IFragment) pluginBase).getPluginId();
	return id + ID_SEPARATOR + getId();
}
public String getSchema() {
	return schema;
}
void load(ExtensionPointModel extensionPointModel) {
	this.id = extensionPointModel.getId();
	this.name = extensionPointModel.getName();
	this.schema = extensionPointModel.getSchema();
}
void load(Node node, Hashtable lineTable) {
	this.id = getNodeAttribute(node, "id");
	this.name = getNodeAttribute(node, "name");
	this.schema = getNodeAttribute(node, "schema");
	addComments(node);
	bindSourceLocation(node, lineTable);
}
public void setSchema(String newSchema) throws CoreException {
	ensureModelEditable();
	String oldValue = this.schema;
	schema = newSchema;
	firePropertyChanged(P_SCHEMA, oldValue, schema);
}

public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
	if (name.equals(P_SCHEMA)) {
		setSchema(newValue!=null ? newValue.toString():null);
		return;
	}
	super.restoreProperty(name, oldValue, newValue);
}

public void write(String indent, PrintWriter writer) {
	writeComments(writer);
	writer.print(indent);
	writer.print("<extension-point");
	if (getId()!=null) writer.print(" id=\""+getWritableString(getId())+"\"");
	if (getName()!=null) writer.print(" name=\""+getWritableString(getName())+"\"");
	if (getSchema()!=null) writer.print(" schema=\""+getSchema()+"\"");
	writer.println("/>");
}
}
