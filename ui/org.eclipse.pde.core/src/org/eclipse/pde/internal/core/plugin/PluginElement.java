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
import org.xml.sax.*;

public class PluginElement extends PluginParent implements IPluginElement {
	private transient ISchemaElement elementInfo;
	private String text;
	static final String ATTRIBUTE_SHIFT = "      "; //$NON-NLS-1$
	static final String ELEMENT_SHIFT = "   "; //$NON-NLS-1$
	private Hashtable attributes = new Hashtable();

	public PluginElement() {
	}
	PluginElement(PluginElement element) {
		setModel(element.getModel());
		setParent(element.getParent());
		this.name = element.getName();
		IPluginAttribute[] atts = element.getAttributes();
		for (int i = 0; i < atts.length; i++) {
			PluginAttribute att = (PluginAttribute) atts[i];
			attributes.put(att.getName(), att.clone());
		}
		this.text = element.getText();
		this.elementInfo = (ISchemaElement)element.getElementInfo();
	}
	
	public boolean equals(Object obj) {
		if (obj==this) return true;
		if (obj==null) return false;
		if (obj instanceof IPluginElement) {
			IPluginElement target = (IPluginElement)obj;
			if (target.getModel().equals(getModel()))
				return false;
			if (target.getAttributeCount()!=getAttributeCount())
				return false;
			IPluginAttribute tatts [] = target.getAttributes();
			for (int i=0; i<tatts.length; i++) {
				IPluginAttribute tatt = tatts[i];
				if (tatt.equals(attributes.get(tatt.getName()))==false)
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
		return (IPluginAttribute) attributes.get(name);
	}
	public IPluginAttribute[] getAttributes() {
		Collection values = attributes.values();
		IPluginAttribute[] result = new IPluginAttribute[values.size()];
		return (IPluginAttribute[]) values.toArray(result);
	}
	public int getAttributeCount() {
		return attributes.size();
	}
	public Object getElementInfo() {
		if (elementInfo != null) {
			ISchema schema = elementInfo.getSchema();
			if (schema.isDisposed()) {
				elementInfo = null;
			}
		}
		if (elementInfo == null) {
			IPluginObject parent = getParent();
			while (parent != null && !(parent instanceof IPluginExtension)) {
				parent = parent.getParent();
			}
			if (parent != null) {
				PluginExtension extension = (PluginExtension) parent;
				ISchema schema = (ISchema)extension.getSchema();
				if (schema != null) {
					elementInfo = schema.findElement(getName());
				}
			}
		}
		return elementInfo;
	}
	public String getText() {
		return text;
	}
	void load(String tagName, Attributes attributes) {
		this.name = tagName;
		for (int i = 0; i < attributes.getLength(); i++) {
			IPluginAttribute att = getModel().getFactory().createAttribute(this);
			((PluginAttribute) att).load(attributes.getQName(i), attributes.getValue(i));
			this.attributes.put(attributes.getQName(i), att);
		}		
	}
	
	public void reconnect() {
		super.reconnect();
		reconnectAttributes();
	}
	private void reconnectAttributes() {
		for (Enumeration enum=attributes.elements(); enum.hasMoreElements();) {
			PluginAttribute att = (PluginAttribute)enum.nextElement();
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
			IPluginAttribute att = getModel().getFactory().createAttribute(this);
			((PluginAttribute) att).load(attribute, lineTable);
			((PluginAttribute) att).setInTheModel(true);
			this.attributes.put(attribute.getNodeName(), att);
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
			} else if (
				child.getNodeType() == Node.TEXT_NODE && child.getNodeValue() != null) {
				String text = child.getNodeValue();
				text = text.trim();
				if (isNotEmpty(text))
					this.text = text;
			}
		}
	}
	public void removeAttribute(String name) throws CoreException {
		ensureModelEditable();
		PluginAttribute att = (PluginAttribute) attributes.remove(name);
		String oldValue = att.getValue();
		if (att != null) {
			att.setInTheModel(false);
		}
		firePropertyChanged(P_ATTRIBUTE, oldValue, null);
	}
	public void replaceAttributes(Hashtable newAttributes) throws CoreException {
		ensureModelEditable();
		Object oldValue = attributes;
		setAttributesInTheModel(attributes, false);
		attributes = newAttributes;
		setAttributesInTheModel(newAttributes, true);
		firePropertyChanged(P_ATTRIBUTES, oldValue, attributes);
	}

	private void setAttributesInTheModel(Hashtable table, boolean value) {
		if (table != null) {
			for (Enumeration enum = table.elements(); enum.hasMoreElements();) {
				PluginAttribute att = (PluginAttribute) enum.nextElement();
				att.setInTheModel(value);
			}
		}
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
			attributes.put(name, attribute);
			((PluginAttribute) attribute).setInTheModel(true);
		}
		attribute.setValue(value);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		super.restoreProperty(name, oldValue, newValue);
	}

	public void setElementInfo(ISchemaElement newElementInfo) {
		elementInfo = newElementInfo;
		if (elementInfo==null) {
			for (Enumeration atts = attributes.elements(); atts.hasMoreElements();) {
				PluginAttribute att = (PluginAttribute)atts.nextElement();
				att.setAttributeInfo(null);
			}
		}
	}
	public void setText(String newText) throws CoreException {
		ensureModelEditable();
		String oldValue = text;
		text = newText;
		firePropertyChanged(P_TEXT, oldValue, text);

	}
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<" + getName()); //$NON-NLS-1$
		String newIndent = indent + ATTRIBUTE_SHIFT;
		if (attributes.isEmpty() == false) {
			writer.println();
			for (Iterator iter = attributes.values().iterator(); iter.hasNext();) {
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
