package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.model.ConfigurationElementModel;
import org.eclipse.core.runtime.model.ConfigurationPropertyModel;
import org.w3c.dom.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;
import java.util.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;

public class PluginElement extends PluginParent implements IPluginElement {
	private ISchemaElement elementInfo;
	private String text;
	static final String ATTRIBUTE_SHIFT = "      ";
	static final String ELEMENT_SHIFT = "   ";
	private Hashtable attributes=new Hashtable();

public PluginElement() {
}
PluginElement(PluginElement element) {
	setModel(element.getModel());
	setParent(element.getParent());
	this.name=element.getName();
	IPluginAttribute [] atts = element.getAttributes();
	for (int i=0; i<atts.length; i++) {
		PluginAttribute att = (PluginAttribute)atts[i];
		attributes.put(att.getName(), att.clone());
	}
	this.text = element.getText();
	this.elementInfo = element.getElementInfo();
}
public IPluginElement createCopy() {
	return new PluginElement(this);
}
public IPluginAttribute getAttribute(String name) {
	return (IPluginAttribute) attributes.get(name);
}
public IPluginAttribute[] getAttributes() {
	Collection values = attributes.values();
	IPluginAttribute [] result = new IPluginAttribute[values.size()];
	return (IPluginAttribute[])values.toArray(result);
}
public ISchemaElement getElementInfo() {
	if (elementInfo!=null) {
		ISchema schema = elementInfo.getSchema();
		if (schema.isDisposed()) {
			elementInfo = null;
		}
	}
	if (elementInfo==null) {
		IPluginObject parent = getParent();
		while (parent != null && !(parent instanceof IPluginExtension)) {
			parent = parent.getParent();
		}
		if (parent!=null) {
			IPluginExtension extension = (IPluginExtension)parent;
			ISchema schema = extension.getSchema();
			if (schema!=null) {
				elementInfo = schema.findElement(getName());
			}
		}
	}
	return elementInfo;
}
public java.lang.String getText() {
	return text;
}
void load(ConfigurationElementModel elementModel) {
	this.name = elementModel.getName();
	ConfigurationPropertyModel[] attributes = elementModel.getProperties();
	if (attributes != null) {
		for (int i = 0; i < attributes.length; i++) {
			ConfigurationPropertyModel attribute = attributes[i];
			IPluginAttribute att = getModel().getFactory().createAttribute(this);
			((PluginAttribute) att).load(attribute);
			this.attributes.put(attribute.getName(), att);
		}
	}
	this.text = elementModel.getValue();
	ConfigurationElementModel[] children = elementModel.getSubElements();
	if (children != null) {
		for (int i = 0; i < children.length; i++) {
			ConfigurationElementModel child = children[i];
			PluginElement childElement = new PluginElement();
			childElement.setModel(getModel());
			this.children.add(childElement);
			childElement.setParent(this);
			childElement.load(child);
		}
	}
}
void load(Node node, Hashtable lineTable) {
	this.name = node.getNodeName();
	NamedNodeMap attributes = node.getAttributes();
	for (int i = 0; i < attributes.getLength(); i++) {
		Node attribute = attributes.item(i);
		IPluginAttribute att = getModel().getFactory().createAttribute(this);
		((PluginAttribute)att).load(attribute, null);
		this.attributes.put(attribute.getNodeName(), att);
	}
	NodeList children = node.getChildNodes();
	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			PluginElement childElement = new PluginElement();
			childElement.setModel(getModel());
			this.children.add(childElement);
			childElement.setParent(this);
			childElement.load(child, lineTable);
		} else
			if (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue() != null) {
				String text = child.getNodeValue();
				text = text.trim();
				if (isNotEmpty(text))
					this.text = text;
			}
	}
	addComments(node);
	bindSourceLocation(node, lineTable);
}
public void removeAttribute(String name) throws CoreException {
	ensureModelEditable();
	attributes.remove(name);
	firePropertyChanged(P_ATTRIBUTES);
}
public void replaceAttributes(Hashtable newAttributes) throws CoreException {
	ensureModelEditable();
	attributes = newAttributes;
	firePropertyChanged(P_ATTRIBUTES);
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
	}
	attribute.setValue(value);
}
public void setElementInfo(ISchemaElement newElementInfo) {
	elementInfo = newElementInfo;
}
public void setText(String newText) throws CoreException {
	ensureModelEditable();
	text = newText;
	firePropertyChanged(P_TEXT);

}
public void write(String indent, PrintWriter writer) {
	writeComments(writer);
	writer.print(indent);
	writer.print("<" + getName());
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
	writer.println(">");
	newIndent = indent + ELEMENT_SHIFT;
	IPluginObject [] children = getChildren();
	for (int i=0; i<children.length; i++) {
		IPluginElement element = (IPluginElement) children[i];
		element.write(newIndent, writer);
	}
	if (getText()!=null) {
		writer.println(newIndent+getWritableString(getText()));
	}
	writer.println(indent + "</" + getName() + ">");
}
}
