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

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class PluginExtensionPoint
	extends IdentifiablePluginObject
	implements IPluginExtensionPoint {
	private String schema;
	static final String ID_SEPARATOR = "."; //$NON-NLS-1$

	public PluginExtensionPoint() {
	}

	public boolean isValid() {
		return id != null;
	}

	public String getFullId() {
		IPluginBase pluginBase = getPluginModel().getPluginBase();
		String id = pluginBase.getId();
		if (pluginBase instanceof IFragment)
			id = ((IFragment) pluginBase).getPluginId();
		return id + ID_SEPARATOR + getId();
	}
	public String getSchema() {
		return schema;
	}
	
	boolean load(Attributes attributes, int line) {
		String id = attributes.getValue("id"); //$NON-NLS-1$
		if (id == null || id.length() == 0)
			return false;
		this.id = id;
		
		String name = attributes.getValue("name"); //$NON-NLS-1$
		if (name == null || name.length() == 0)
			return false;
		this.name = name;
		
		this.schema = attributes.getValue("schema"); //$NON-NLS-1$
		this.range = new int[] {line, line};
		return true;
	}
	void load(Node node, Hashtable lineTable) {
		this.id = getNodeAttribute(node, "id"); //$NON-NLS-1$
		this.name = getNodeAttribute(node, "name"); //$NON-NLS-1$
		this.schema = getNodeAttribute(node, "schema"); //$NON-NLS-1$
		bindSourceLocation(node, lineTable);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof IPluginExtensionPoint) {
			IPluginExtensionPoint target = (IPluginExtensionPoint) obj;
			// Objects from the same model must be
			// binary equal
			if (target.getModel().equals(getModel()))
				return false;
			if (stringEqualWithNull(target.getId(), getId())
				&& stringEqualWithNull(target.getName(), getName())
				&& stringEqualWithNull(target.getSchema(), getSchema()))
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

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_SCHEMA)) {
			setSchema(newValue != null ? newValue.toString() : null);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<extension-point"); //$NON-NLS-1$
		if (getId() != null)
			writer.print(" id=\"" + getWritableString(getId()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (getName() != null)
			writer.print(" name=\"" + getWritableString(getName()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (getSchema() != null)
			writer.print(" schema=\"" + getSchema() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("/>"); //$NON-NLS-1$
	}
}
