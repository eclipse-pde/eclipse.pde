package org.eclipse.pde.internal.core.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.*;
import org.w3c.dom.Node;

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
		ISiteObject object,
		String property,
		Object oldValue,
		Object newValue) {
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}
	protected void fireStructureChanged(ISiteObject child, int changeType) {
		fireStructureChanged(new ISiteObject[] { child }, changeType);
	}
	protected void fireStructureChanged(
		ISiteObject[] children,
		int changeType) {
		ISiteModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelChanged(new ModelChangedEvent(changeType, children, null));
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
			return "";
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
			return value.equalsIgnoreCase("true");
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
		label = getNodeAttribute(node, "label");
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