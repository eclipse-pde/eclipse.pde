package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.model.ConfigurationPropertyModel;
import org.w3c.dom.Node;
import org.eclipse.core.runtime.CoreException;
import java.io.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;

public class PluginAttribute extends PluginObject implements IPluginAttribute {
	private String value;
	private ISchemaAttribute attributeInfo;

public PluginAttribute() {
}
PluginAttribute(IPluginAttribute attribute) {
	setModel(attribute.getModel());
	setParent(attribute.getParent());
	this.name = attribute.getName();
	this.value = attribute.getValue();
	this.attributeInfo = attribute.getAttributeInfo();
}
public Object clone() {
	return new PluginAttribute(this);
}
public ISchemaAttribute getAttributeInfo() {
	if (attributeInfo!=null) {
		ISchema schema = attributeInfo.getSchema();
		if (schema.isDisposed()) {
			attributeInfo = null;
		}
	}
	if (attributeInfo==null) {
		IPluginElement element = (IPluginElement)getParent();
		ISchemaElement elementInfo = element.getElementInfo();
		if (elementInfo!=null) {
			attributeInfo = elementInfo.getAttribute(getName());
		}
	}
	return attributeInfo;
}
public String getValue() {
	return value;
}
void load(ConfigurationPropertyModel attributeModel) {
	this.name = attributeModel.getName();
	this.value = attributeModel.getValue();
}
void load(Node node) {
	this.name = node.getNodeName();
	this.value = node.getNodeValue();
}
public void setAttributeInfo(ISchemaAttribute newAttributeInfo) {
	attributeInfo = newAttributeInfo;
}
public void setValue(String newValue) throws CoreException {
	ensureModelEditable();
	this.value = newValue;
	firePropertyChanged(getParent(), P_VALUE);
}
public void write(String indent, PrintWriter writer) {
	if (value==null) return;
	writer.print(indent);
	writer.print(getName()+"=\""+getWritableString(value)+"\"");
}
}
