package org.eclipse.pde.internal.core.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Vector;
import java.io.*;
import org.eclipse.pde.internal.core.ischema.*;
import java.util.List;
import java.util.Iterator;

public class ChoiceRestriction extends SchemaObject implements ISchemaRestriction {
	private ISchemaSimpleType baseType;
	private Vector children;
	public static final String P_CHOICES = "choices";
	private Iterator emptyIterator = new Iterator () {
		public boolean hasNext() { return false; }
		public Object next() { return null; }
		public void remove() {}
	};

public ChoiceRestriction(ISchema schema) {
	super(schema, "__choice__");
	
}
public ChoiceRestriction(ChoiceRestriction source) {
	this(source.getSchema());
	children = new Vector();
	Object [] choices = source.getChildren(); 
	for (int i=0; i<choices.length; i++) {
		children.add(new SchemaEnumeration(this, ((ISchemaEnumeration)choices[i]).getName()));
	}
}
public ISchemaSimpleType getBaseType() {
	return baseType;
}
public Object[] getChildren() {
	if (children != null) {
		Object[] result = new Object[children.size()];
		children.copyInto(result);
		return result;
	} else
		return new Object[0];
}
public String[] getChoicesAsStrings() {
	if (children==null) return new String [0];
	Vector result = new Vector();
	for (int i=0; i<children.size(); i++) {
		ISchemaEnumeration enum = (ISchemaEnumeration)children.get(i);
		result.addElement(enum.getName());
	}
	String [] choices = new String [ result.size() ];
	result.copyInto(choices);
	return choices;
}
public ISchemaObject getParent() {
	if (baseType != null)
		return baseType.getSchema();
	return super.getParent();
}
public boolean isValueValid(java.lang.Object value) {
	if (children==null) return false;
	String svalue = value.toString();

	for (int i=0; i<children.size(); i++) {
		ISchemaEnumeration enum = (ISchemaEnumeration)children.get(i);
		if (enum.getName().equals(svalue)) return true;
	}
	return false;
}
public void setBaseType(ISchemaSimpleType baseType) {
	this.baseType = baseType;
}
public void setChildren(Vector children) {
	Vector oldValue = this.children;
	this.children = children;
	if (getParent() != null)
		getSchema().fireModelObjectChanged(this, P_CHOICES, oldValue, children);
}
public String toString() {
	if (children == null)
		return "";
	StringBuffer buffer = new StringBuffer();

	for (int i = 0; i < children.size(); i++) {
		Object child = children.get(i);
		if (child instanceof ISchemaEnumeration) {
			ISchemaEnumeration enum = (ISchemaEnumeration) child;
			if (i > 0)
				buffer.append(", ");
			buffer.append(enum.getName());
		}
	}
	return buffer.toString();
}
public void write(String indent, PrintWriter writer) {
	writeComments(writer);
	writer.println(indent+"<restriction base=\""+baseType.getName()+"\">");
	for (int i=0; i<children.size(); i++) {
		Object child = children.get(i);
		if (child instanceof ISchemaEnumeration) {
			ISchemaEnumeration enum = (ISchemaEnumeration)child;
			enum.write(indent + Schema.INDENT, writer);
		}
	}
	writer.println(indent+"</restriction>");
}
}
