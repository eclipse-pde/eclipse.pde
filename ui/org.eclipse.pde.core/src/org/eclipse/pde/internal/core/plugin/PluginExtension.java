/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PluginExtension extends PluginParent implements IPluginExtension {

	private static final long serialVersionUID = 1L;
	protected String fPoint;
	private transient ISchema schema;

	public PluginExtension() {
	}
	public String getPoint() {
		return fPoint;
	}
	
	public boolean isValid() {
		return fPoint != null;
	}
	
	public Object getSchema() {
		if (schema == null) {
			SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
			schema = registry.getSchema(fPoint);
		} else if (schema.isDisposed()) {
			schema = null;
		}
		return schema;
	}
	
	void load(Node node) {
		this.id = getNodeAttribute(node, "id"); //$NON-NLS-1$
		fName = getNodeAttribute(node, "name"); //$NON-NLS-1$
		fPoint = getNodeAttribute(node, "point"); //$NON-NLS-1$
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				PluginElement childElement = new PluginElement();
				childElement.setModel(getModel());
				childElement.setInTheModel(true);
				childElement.setParent(this);
				this.fChildren.add(childElement);
				childElement.load(child);
			}
		}
		fStartLine = Integer.parseInt(getNodeAttribute(node, "line")); //$NON-NLS-1$
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof IPluginExtension) {
			IPluginExtension target = (IPluginExtension) obj;
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
		String oldValue = fPoint;
		fPoint = point;
		firePropertyChanged(P_POINT, oldValue, point);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_POINT)) {
			setPoint(newValue != null ? newValue.toString() : null);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	public String toString() {
		if (getName() != null)
			return getName();
		return getPoint();
	}
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<extension"); //$NON-NLS-1$
		String attIndent = indent + PluginElement.ATTRIBUTE_SHIFT;
		if (getId() != null) {
			writer.println();
			writer.print(attIndent + "id=\"" + getId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getName() != null) {
			writer.println();
			writer.print(
				attIndent + "name=\"" + getWritableString(getName()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getPoint() != null) {
			writer.println();
			writer.print(attIndent + "point=\"" + getPoint() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(">"); //$NON-NLS-1$
		IPluginObject[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement) children[i];
			child.write(indent + PluginElement.ELEMENT_SHIFT, writer);
		}
		writer.println(indent + "</extension>"); //$NON-NLS-1$
	}
}
