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
import org.eclipse.core.runtime.model.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.w3c.dom.*;

public class PluginExtension extends PluginParent implements IPluginExtension {
	private String point;
	private transient ISchema schema;


public PluginExtension() {
}
public String getPoint() {
	return point;
}
public ISchema getSchema() {
	if (schema == null) {
		SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
		schema = registry.getSchema(point);
	} else
		if (schema.isDisposed()) {
			schema = null;
		}
	return schema;
}
void load(ExtensionModel extensionModel) {
	this.id = extensionModel.getId();
	this.name = extensionModel.getName();
	this.point = extensionModel.getExtensionPoint();

	ConfigurationElementModel[] childModels = extensionModel.getSubElements();
	if (childModels != null) {
		for (int i = 0; i < childModels.length; i++) {
			ConfigurationElementModel childModel = childModels[i];
			PluginElement childElement = new PluginElement();
			childElement.setModel(getModel());
			childElement.setParent(this);
			this.children.add(childElement);
			childElement.load(childModel);
		}
	}
	int line = extensionModel.getStartLine();
	range = new int [] { line, line };
}
void load(Node node, Hashtable lineTable) {
	this.id = getNodeAttribute(node, "id");
	this.name = getNodeAttribute(node, "name");
	this.point = getNodeAttribute(node, "point");
	NodeList children = node.getChildNodes();
	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			PluginElement childElement = new PluginElement();
			childElement.setModel(getModel());
			childElement.setInTheModel(true);
			childElement.setParent(this);
			this.children.add(childElement);
			childElement.load(child, lineTable);
		}
	}
	addComments(node);
	bindSourceLocation(node, lineTable);
}

public boolean equals(Object obj) {
	if (obj==this) return true;
	if (obj==null) return false;
	if (obj instanceof IPluginExtension) {
		IPluginExtension target = (IPluginExtension)obj;
		// Objects from the same model must be
		// binary equal
		if (target.getModel().equals(getModel()))
			return false;
		if (!stringEqualWithNull(target.getId(), getId()))
			return false;
		if (!stringEqualWithNull(target.getName(), getName()))
			return false;
		if (!stringEqualWithNull(target.getPoint(), getPoint()))
			return false;
		// Children
		return super.equals(obj);
	}
	return false;
}

public void setPoint(String point) throws CoreException {
	ensureModelEditable();
	String oldValue = this.point;
	this.point = point;
	firePropertyChanged(P_POINT, oldValue, point);
}

public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
	if (name.equals(P_POINT)) {
		setPoint(newValue!=null ? newValue.toString():null);
		return;
	}
	super.restoreProperty(name, oldValue, newValue);
}

public String toString() {
	if (getName()!=null) return getName();
	return getPoint();
}
public void write(String indent, PrintWriter writer) {
	writeComments(writer);
	writer.print(indent);
	writer.print("<extension");
	String attIndent = indent + PluginElement.ATTRIBUTE_SHIFT;
	if (getId() != null) {
		writer.println();
		writer.print(attIndent + "id=\"" + getId() + "\"");
	}
	if (getName() != null) {
		writer.println();
		writer.print(attIndent + "name=\"" + getWritableString(getName()) + "\"");
	}
	if (getPoint() != null) {
		writer.println();
		writer.print(attIndent + "point=\"" + getPoint() + "\"");
	}
	writer.println(">");
	IPluginObject [] children = getChildren();
	for (int i=0; i<children.length; i++) {
		IPluginElement child = (IPluginElement) children[i];
		child.write(indent + PluginElement.ELEMENT_SHIFT, writer);
	}
	writer.println(indent + "</extension>");
}
}
