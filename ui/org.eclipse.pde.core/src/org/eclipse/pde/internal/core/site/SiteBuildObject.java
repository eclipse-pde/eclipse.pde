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
package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.*;
import org.w3c.dom.Node;

public abstract class SiteBuildObject
	extends PlatformObject
	implements ISiteBuildObject {
	transient ISiteBuildModel model;
	transient ISiteBuildObject parent;
	boolean inTheModel;

	void setInTheModel(boolean value) {
		inTheModel = value;
	}

	public boolean isInTheModel() {
		return inTheModel;
	}

	protected void ensureModelEditable() throws CoreException {
		if (!model.isEditable()) {
			throwCoreException("Illegal attempt to change read-only site manifest model");
		}
	}
	protected void firePropertyChanged(
		String property,
		Object oldValue,
		Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}
	protected void firePropertyChanged(
		ISiteBuildObject object,
		String property,
		Object oldValue,
		Object newValue) {
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}
	protected void fireStructureChanged(ISiteBuildObject child, int changeType) {
		fireStructureChanged(new ISiteBuildObject[] { child }, changeType);
	}
	protected void fireStructureChanged(
		ISiteBuildObject[] children,
		int changeType) {
		ISiteBuildModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelChanged(new ModelChangedEvent(changeType, children, null));
		}
	}
	public ISiteBuild getSiteBuild() {
		return model.getSiteBuild();
	}

	public ISiteBuildModel getModel() {
		return model;
	}
	
	String getNodeAttribute(Node node, String name) {
		NamedNodeMap atts = node.getAttributes();
		Node attribute = null;
		if (atts != null)
		   attribute = atts.getNamedItem(name);
		if (attribute != null)
			return attribute.getNodeValue();
		return null;
	}

	int getIntegerAttribute(Node node, String name) {
		String value = getNodeAttribute(node, name);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
			}
		}
		return 0;
	}
	
	boolean getBooleanAttribute(Node node, String name) {
		String value = getNodeAttribute(node, name);
		if (value != null) {
			return value.equalsIgnoreCase("true");
		}
		return false;
	}
	
	public ISiteBuildObject getParent() {
		return parent;
	}

	protected void reset() {
	}

	protected void throwCoreException(String message) throws CoreException {
		Status status =
			new Status(IStatus.ERROR, PDECore.getPluginId(), IStatus.OK, message, null);
		throw new CoreException(status);
	}

	public static String getWritableString(String source) {
		if (source == null)
			return "";
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;");
					break;
				case '<' :
					buf.append("&lt;");
					break;
				case '>' :
					buf.append("&gt;");
					break;
				case '\'' :
					buf.append("&apos;");
					break;
				case '\"' :
					buf.append("&quot;");
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
	}

	public void write(String indent, PrintWriter writer) {
	}

	public void setModel(ISiteBuildModel model) {
		this.model = model;
	}
	
	public void setParent(ISiteBuildObject parent) {
		this.parent = parent;
	}
}
