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
package org.eclipse.pde.internal.core.site;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.*;

public abstract class SiteObject
	extends PlatformObject
	implements ISiteObject {
	transient ISiteModel model;
	transient ISiteObject parent;
	protected String label;
	boolean inTheModel;

	void setInTheModel(boolean value) {
		inTheModel = value;
	}

	public boolean isInTheModel() {
		return inTheModel;
	}

	protected void ensureModelEditable() throws CoreException {
		if (!model.isEditable()) {
			throwCoreException(PDECoreMessages.SiteObject_readOnlyChange); 
		}
	}
	protected void firePropertyChanged(
		String property,
		Object oldValue,
		Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}
	protected void firePropertyChanged(
		ISiteObject object,
		String property,
		Object oldValue,
		Object newValue) {
		if (model.isEditable()) {
			model.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}
	protected void fireStructureChanged(ISiteObject child, int changeType) {
		fireStructureChanged(new ISiteObject[] { child }, changeType);
	}
	protected void fireStructureChanged(
		ISiteObject[] children,
		int changeType) {
		ISiteModel model = getModel();
		if (model.isEditable()) {
			model.fireModelChanged(new ModelChangedEvent(model, changeType, children, null));
		}
	}
	public ISite getSite() {
		return model.getSite();
	}
	public String getLabel() {
		return label;
	}

	public String getTranslatableLabel() {
		if (label == null)
			return ""; //$NON-NLS-1$
		return model.getResourceString(label);
	}
	public ISiteModel getModel() {
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
			return value.equalsIgnoreCase("true"); //$NON-NLS-1$
		}
		return false;
	}
	
	protected String getNormalizedText(String source) {
		String result = source.replace('\t', ' ');
		result = result.trim();

		return result;
	}

	public ISiteObject getParent() {
		return parent;
	}

	protected void parse(Node node) {
		label = getNodeAttribute(node, "label"); //$NON-NLS-1$
	}

	protected void reset() {
		label = null;
	}

	public void setLabel(String newLabel) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.label;
		label = newLabel;
		firePropertyChanged(P_LABEL, oldValue, newLabel);
	}
	protected void throwCoreException(String message) throws CoreException {
		Status status =
			new Status(IStatus.ERROR, PDECore.getPluginId(), IStatus.OK, message, null);
		CoreException ce= new CoreException(status);
		ce.fillInStackTrace();
		throw ce;
	}

	public static String getWritableString(String source) {
		if (source == null)
			return ""; //$NON-NLS-1$
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
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
		if (name.equals(P_LABEL)) {
			setLabel(newValue != null ? newValue.toString() : null);
		}
	}

	public void write(String indent, PrintWriter writer) {
	}
	public void setModel(ISiteModel model) {
		this.model = model;
	}
	
	public void setParent(ISiteObject parent) {
		this.parent = parent;
	}
}
