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
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;
import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.model.ExtensionPointModel;
import org.eclipse.pde.core.plugin.*;
import org.w3c.dom.Node;

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
	int line = extensionPointModel.getStartLine();
	this.range = new int[] { line, line };
	
}
void load(Node node, Hashtable lineTable) {
	this.id = getNodeAttribute(node, "id");
	this.name = getNodeAttribute(node, "name");
	this.schema = getNodeAttribute(node, "schema");
	addComments(node);
	bindSourceLocation(node, lineTable);
}

public boolean equals(Object obj) {
	if (obj==this) return true;
	if (obj instanceof IPluginExtensionPoint) {
		IPluginExtensionPoint target = (IPluginExtensionPoint)obj;
		// Objects from the same model must be
		// binary equal
		if (target.getModel().equals(getModel()))
			return false;
		if (stringEqualWithNull(target.getId(), getId()) &&
			stringEqualWithNull(target.getName(), getName()) &&
			stringEqualWithNull(target.getSchema(), getSchema()))
			return true;
	}
	return false;
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
