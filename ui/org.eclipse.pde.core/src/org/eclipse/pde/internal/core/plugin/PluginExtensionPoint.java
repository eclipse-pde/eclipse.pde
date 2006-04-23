/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.w3c.dom.Node;

public class PluginExtensionPoint extends IdentifiablePluginObject
	implements IPluginExtensionPoint {

	private static final long serialVersionUID = 1L;
	
	protected String fSchema;
	
	public boolean isValid() {
		return id != null && fName != null;
	}

	public String getFullId() {
		IPluginModelBase modelBase = getPluginModel();
		IPluginBase pluginBase = modelBase.getPluginBase();
		if ("3.2".equals(pluginBase.getSchemaVersion())) { //$NON-NLS-1$
			String pointId = getId();
			if (pointId.indexOf('.') > 0)
				return pointId;
		}
		String id = pluginBase.getId();
		if (pluginBase instanceof IFragment)
			id = ((IFragment) pluginBase).getPluginId();
		return id + "." + getId(); //$NON-NLS-1$
	}
	
	public String getSchema() {
		return fSchema;
	}
	
	void load(Node node) {
		this.id = getNodeAttribute(node, "id"); //$NON-NLS-1$
		fName = getNodeAttribute(node, "name"); //$NON-NLS-1$
		fSchema = getNodeAttribute(node, "schema"); //$NON-NLS-1$
		fStartLine = Integer.parseInt(getNodeAttribute(node, "line")); //$NON-NLS-1$
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
		String oldValue = fSchema;
		fSchema = newSchema;
		firePropertyChanged(P_SCHEMA, oldValue, fSchema);
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
