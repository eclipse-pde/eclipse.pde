/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.w3c.dom.*;

public class PluginAttribute extends PluginObject implements IPluginAttribute {
	private String value;
	private transient ISchemaAttribute attributeInfo;

public PluginAttribute() {
}
PluginAttribute(IPluginAttribute attribute) {
	setModel(attribute.getModel());
	setParent(attribute.getParent());
	this.name = attribute.getName();
	this.value = attribute.getValue();
	this.attributeInfo = ((PluginAttribute)attribute).getAttributeInfo();
}
public Object clone() {
	return new PluginAttribute(this);
}

public boolean equals(Object obj) {
	if (obj==this) return true;
	if (obj==null) return false;
	if (obj instanceof IPluginAttribute) {
		IPluginAttribute target = (IPluginAttribute)obj;
		if (target.getModel().equals(getModel()))
			return false;
		if (stringEqualWithNull(getName(), target.getName()) &&
			stringEqualWithNull(getValue(), target.getValue()))
			return true;
	}
	return false;
}

public ISchemaAttribute getAttributeInfo() {
	if (attributeInfo!=null) {
		ISchema schema = attributeInfo.getSchema();
		if (schema.isDisposed()) {
			attributeInfo = null;
		}
	}
	if (attributeInfo==null) {
		PluginElement element = (PluginElement)getParent();
		ISchemaElement elementInfo = (ISchemaElement)element.getElementInfo();
		if (elementInfo!=null) {
			attributeInfo = elementInfo.getAttribute(getName());
		}
	}
	return attributeInfo;
}
public String getValue() {
	return value;
}

void load (String name, String value) {
	this.name = name;
	this.value = value;
}
void load(Node node, Hashtable lineTable) {
	this.name = node.getNodeName();
	this.value = node.getNodeValue();
	if (getParent() instanceof ISourceObject) {
		ISourceObject pobj = (ISourceObject)getParent();
		int start = pobj.getStartLine();
		int stop = pobj.getStopLine();
		if (start!= -1 && stop!= -1) {
			range = new int[] { start, stop };
		}
	}
}

public void setAttributeInfo(ISchemaAttribute newAttributeInfo) {
	attributeInfo = newAttributeInfo;
}
public void setValue(String newValue) throws CoreException {
	ensureModelEditable();
	String oldValue = this.value;
	this.value = newValue;
	AttributeChangedEvent e = new AttributeChangedEvent(getModel(), getParent(), this, oldValue, newValue);
	fireModelChanged(e);
}

public void write(String indent, PrintWriter writer) {
	if (value==null) return;
	writer.print(indent);
	writer.print(getName()+"=\""+getWritableString(value)+"\""); //$NON-NLS-1$ //$NON-NLS-2$
}
}
