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
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.w3c.dom.*;

public class PluginElement extends PluginParent implements IPluginElement {
	static final String ATTRIBUTE_SHIFT = "      "; //$NON-NLS-1$

	static final String ELEMENT_SHIFT = "   "; //$NON-NLS-1$

	private transient ISchemaElement fElementInfo;

	private String fText;

	private Hashtable fAttributes = new Hashtable();

	public PluginElement() {
	}

	PluginElement(PluginElement element) {
		setModel(element.getModel());
		setParent(element.getParent());
		this.name = element.getName();
		IPluginAttribute[] atts = element.getAttributes();
		for (int i = 0; i < atts.length; i++) {
			PluginAttribute att = (PluginAttribute) atts[i];
			fAttributes.put(att.getName(), att.clone());
		}
		fText = element.getText();
		fElementInfo = (ISchemaElement) element.getElementInfo();
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof IPluginElement) {
			IPluginElement target = (IPluginElement) obj;
			if (target.getModel().equals(getModel()))
				return false;
			if (target.getAttributeCount() != getAttributeCount())
				return false;
			IPluginAttribute tatts[] = target.getAttributes();
			for (int i = 0; i < tatts.length; i++) {
				IPluginAttribute tatt = tatts[i];
				if (tatt.equals(fAttributes.get(tatt.getName())) == false)
					return false;
			}
			return super.equals(obj);
		}
		return false;
	}

	public IPluginElement createCopy() {
		return new PluginElement(this);
	}

	public IPluginAttribute getAttribute(String name) {
		return (IPluginAttribute) fAttributes.get(name);
	}

	public IPluginAttribute[] getAttributes() {
		Collection values = fAttributes.values();
		IPluginAttribute[] result = new IPluginAttribute[values.size()];
		return (IPluginAttribute[]) values.toArray(result);
	}

	public int getAttributeCount() {
		return fAttributes.size();
	}

	public Object getElementInfo() {
		if (fElementInfo != null) {
			ISchema schema = fElementInfo.getSchema();
			if (schema.isDisposed()) {
				fElementInfo = null;
			}
		}
		if (fElementInfo == null) {
			IPluginObject parent = getParent();
			while (parent != null && !(parent instanceof IPluginExtension)) {
				parent = parent.getParent();
			}
			if (parent != null) {
				PluginExtension extension = (PluginExtension) parent;
				ISchema schema = (ISchema) extension.getSchema();
				if (schema != null) {
					fElementInfo = schema.findElement(getName());
				}
			}
		}
		return fElementInfo;
	}

	public String getText() {
		return fText;
	}

	void load(Element element) {
		this.name = element.getTagName();
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			PluginAttribute att = (PluginAttribute) getModel().getFactory()
					.createAttribute(this);
			Attr attr = (Attr)attributes.item(i);
			att.name = attr.getName();
			att.value = attr.getValue();
			fAttributes.put(att.getName(), att);
		}
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				PluginElement childElement = new PluginElement();
				childElement.setModel(getModel());
				childElement.setInTheModel(true);
				childElement.setParent(this);
				this.children.add(childElement);
				childElement.load((Element)child);
			}
		}
	}

	public void reconnect() {
		super.reconnect();
		reconnectAttributes();
	}

	private void reconnectAttributes() {
		for (Enumeration elements = fAttributes.elements(); elements
				.hasMoreElements();) {
			PluginAttribute att = (PluginAttribute) elements.nextElement();
			att.setModel(getModel());
			att.setParent(this);
			att.setInTheModel(true);
		}
	}

	void load(Node node, Hashtable lineTable) {
		this.name = node.getNodeName();
		NamedNodeMap attributes = node.getAttributes();
		bindSourceLocation(node, lineTable);
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attribute = attributes.item(i);
			IPluginAttribute att = getModel().getFactory()
					.createAttribute(this);
			((PluginAttribute) att).load(attribute, lineTable);
			((PluginAttribute) att).setInTheModel(true);
			this.fAttributes.put(attribute.getNodeName(), att);
		}
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				PluginElement childElement = new PluginElement();
				childElement.setModel(getModel());
				childElement.setInTheModel(true);
				this.children.add(childElement);
				childElement.setParent(this);
				childElement.load(child, lineTable);
			} else if (child.getNodeType() == Node.TEXT_NODE
					&& child.getNodeValue() != null) {
				String text = child.getNodeValue();
				text = text.trim();
				if (isNotEmpty(text))
					this.fText = text;
			}
		}
	}

	public void removeAttribute(String name) throws CoreException {
		ensureModelEditable();
		PluginAttribute att = (PluginAttribute) fAttributes.remove(name);
		String oldValue = att.getValue();
		if (att != null) {
			att.setInTheModel(false);
		}
		firePropertyChanged(P_ATTRIBUTE, oldValue, null);
	}

	public void setAttribute(String name, String value) throws CoreException {
		ensureModelEditable();
		if (value == null) {
			removeAttribute(name);
			return;
		}
		IPluginAttribute attribute = (IPluginAttribute) getAttribute(name);
		if (attribute == null) {
			attribute = getModel().getFactory().createAttribute(this);
			attribute.setName(name);
			fAttributes.put(name, attribute);
			((PluginAttribute) attribute).setInTheModel(true);
		}
		attribute.setValue(value);
	}

	public void setElementInfo(ISchemaElement newElementInfo) {
		fElementInfo = newElementInfo;
		if (fElementInfo == null) {
			for (Enumeration atts = fAttributes.elements(); atts
					.hasMoreElements();) {
				PluginAttribute att = (PluginAttribute) atts.nextElement();
				att.setAttributeInfo(null);
			}
		}
	}

	public void setText(String newText) throws CoreException {
		ensureModelEditable();
		String oldValue = fText;
		fText = newText;
		firePropertyChanged(P_TEXT, oldValue, fText);

	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<" + getName()); //$NON-NLS-1$
		String newIndent = indent + ATTRIBUTE_SHIFT;
		if (fAttributes.isEmpty() == false) {
			writer.println();
			for (Iterator iter = fAttributes.values().iterator(); iter
					.hasNext();) {
				IPluginAttribute attribute = (IPluginAttribute) iter.next();
				attribute.write(newIndent, writer);
				if (iter.hasNext())
					writer.println();
			}
		}
		writer.println(">"); //$NON-NLS-1$
		newIndent = indent + ELEMENT_SHIFT;
		IPluginObject[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement element = (IPluginElement) children[i];
			element.write(newIndent, writer);
		}
		if (getText() != null) {
			writer.println(newIndent + getWritableString(getText()));
		}
		writer.println(indent + "</" + getName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
