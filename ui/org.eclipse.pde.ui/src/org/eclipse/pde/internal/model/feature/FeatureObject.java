package org.eclipse.pde.internal.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.Node;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.runtime.*;
import java.io.*;
import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.internal.*;

public abstract class FeatureObject extends PlatformObject implements IFeatureObject {
	IFeatureModel model;
	IFeatureObject parent;
	protected String label;

protected void ensureModelEditable() throws CoreException {
	if (!model.isEditable()) {
		throwCoreException("Illegal attempt to change read-only component manifest model");
	}
}
protected void firePropertyChanged(String property, Object oldValue, Object newValue) {
	firePropertyChanged(this, property, oldValue, newValue);
}
protected void firePropertyChanged(IFeatureObject object, String property, Object oldValue, Object newValue) {
	if (model.isEditable() && model instanceof IModelChangeProvider) {
		IModelChangeProvider provider = (IModelChangeProvider) model;
		provider.fireModelObjectChanged(object, property, oldValue, newValue);
	}
}
protected void fireStructureChanged(IFeatureObject child, int changeType) {
	IFeatureModel model = getModel();
	if (model.isEditable() && model instanceof IModelChangeProvider) {
		IModelChangeProvider provider = (IModelChangeProvider) model;
		provider.fireModelChanged(
			new ModelChangedEvent(changeType, new Object[] { child }, null));
	}
}
public IFeature getComponent() {
	return model.getFeature();
}
public String getLabel() {
	return label;
}
public IFeatureModel getModel() {
	return model;
}
String getNodeAttribute(Node node, String name) {
	Node attribute = node.getAttributes().getNamedItem(name);
	if (attribute != null)
		return attribute.getNodeValue();
	return null;
}

int getIntegerAttribute(Node node, String name) {
	String value = getNodeAttribute(node, name);
	if (value!=null) {
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
		}
	}
	return 0;
}

protected String getNormalizedText(String source) {
	String result = source.replace('\t', ' ');
	result = result.trim();
	boolean skip=false;

	return result;
	/*
	
	StringBuffer buff = new StringBuffer();
	for (int i=0; i<result.length(); i++) {
		char c = result.charAt(i);
		if (c=='\n') {
			skip = true;
		}
		else if (c==' ') {
			if (skip) continue;
		}
		else skip = false;
		
		buff.append(c);
	}
	return buff.toString();
	*/
}

public IFeatureObject getParent() {
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
		new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, message, null);
	throw new CoreException(status);
}

public static String getWritableString(String source) {
	if (source==null) return "";
	StringBuffer buf = new StringBuffer();
	for (int i=0; i<source.length(); i++) {
		char c = source.charAt(i);
		switch (c) {
			case '&': 
				buf.append("&amp;");
				break;
			case '<':
				buf.append("&lt;");
				break;
			case '>':
				buf.append("&gt;");
				break;
			case '\'':
				buf.append("&apos;");
				break;
			case '\"':
				buf.append("&quot;");
				break;
			default:
			buf.append(c);
			break;
		}
	}
	return buf.toString();
}

public void write(String indent, PrintWriter writer) {
}
}
