package org.eclipse.pde.internal.core.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.pde.internal.core.ischema.*;

public class SchemaAttribute extends SchemaObject implements ISchemaAttribute {
	private int kind = STRING;
	private int use = OPTIONAL;
	private String valueFilter;
	private ISchemaSimpleType type;
	private String basedOn;
	private Object value;
	public static final String P_USE = "useProperty";
	public static final String P_VALUE_FILTER = "valueFilterProperty";
	public static final String P_VALUE = "value";
	public static final String P_KIND = "kindProperty";
	public static final String P_TYPE = "typeProperty";
	public static final String P_BASED_ON = "basedOnProperty";
	private boolean verifying;

public SchemaAttribute(ISchemaAttribute att, String newName) {
	super(att.getParent(), newName);
	kind = att.getKind();
	use = att.getUse();
	value = att.getValue();
	type = new SchemaSimpleType(att.getType());
	basedOn = att.getBasedOn();
}
public SchemaAttribute(ISchemaObject parent, String name) {
	super(parent, name);
}
public String getBasedOn() {
	return basedOn;
}
public int getKind() {
	return kind;
}
public ISchemaSimpleType getType() {
	return type;
}
public int getUse() {
	return use;
}
public Object getValue() {
	return value;
}
public String getValueFilter() {
	return valueFilter;
}
public void setBasedOn(String newBasedOn) {
	String oldValue = basedOn;
	basedOn = newBasedOn;
	getSchema().fireModelObjectChanged(this, P_BASED_ON, oldValue, basedOn);
	verifyProperties();
}
public void setKind(int newKind) {
	Integer oldValue= new Integer(kind);
	kind = newKind;
	getSchema().fireModelObjectChanged(this, P_KIND, oldValue, new Integer(kind));
	verifyProperties();
}
public void setType(ISchemaSimpleType newType) {
	Object oldValue = type;
	type = newType;
	getSchema().fireModelObjectChanged(this, P_TYPE, oldValue, type);
	verifyProperties();
}
public void setUse(int newUse) {
	Integer oldValue = new Integer(use);
	use = newUse;
	getSchema().fireModelObjectChanged(this, P_USE, oldValue, new Integer(use));
	verifyProperties();
}
public void setValue(String value) {
	String oldValue = value;
	this.value = value;
	getSchema().fireModelObjectChanged(this, P_VALUE, oldValue, value);
	verifyProperties();
}
public void setValueFilter(String valueFilter) {
	String oldValue = this.valueFilter;
	this.valueFilter = valueFilter;
	getSchema().fireModelObjectChanged(this, P_VALUE_FILTER, oldValue, valueFilter);
}

private void verifyProperties() {
	if (verifying) return;
	verifying=true;
// check if the current combination of properties
// make sense.
	if (kind != STRING) {
		// type must be 'string' and no restriction
		ensureStringType();
		ensureNoRestriction();
	}
	if (kind != JAVA) {
		// basedOn makes no sense
		setBasedOn(null);
	}
	if (type!=null && type.getName().equals("boolean"))
		// no restriction for boolean
		ensureNoRestriction();
	if (use != DEFAULT)
		// value makes no sense without 'default' use
		setValue(null);
	verifying = false;
}

private void ensureStringType() {
	if (type==null || type.getName().equals("boolean")) 
	   setType(new SchemaSimpleType(getSchema(), "string"));
}

private void ensureNoRestriction() {
	if (type instanceof SchemaSimpleType &&
		((SchemaSimpleType)type).getRestriction()!=null) {
		SchemaSimpleType simpleType = (SchemaSimpleType)type;
		simpleType.setRestriction(null);
		setType(simpleType);
	}
}

public void write(String indent, PrintWriter writer) {
	writeComments(writer);
	boolean annotation=false;
	ISchemaSimpleType type = (ISchemaSimpleType)getType();
	String typeName = type.getName();
	writer.print(indent);
	writer.print("<attribute name=\""+getName()+"\"");
	if (type.getRestriction()==null) writer.print(" type=\""+typeName+"\"");
	String useString=null;
	switch (getUse()) {
		case OPTIONAL:
		// don't write default setting
		//useString="optional";
		break;
		case DEFAULT:
		useString="default";
		break;
		case REQUIRED:
		useString="required";
		break;
	}
	if (useString!=null) {
		writer.print(" use=\""+useString+"\"");
	}
	if (value!=null) {
		writer.print(" value=\""+value+"\"");
	}
	String documentation = getWritableDescription();
	if (documentation!=null || this.getBasedOn()!=null || getKind()!=STRING) {
		// Add annotation
		annotation = true;
		writer.println(">");
		String annIndent = indent + Schema.INDENT;
		String indent2 = annIndent + Schema.INDENT;
		String indent3 = indent2 + Schema.INDENT;
		writer.print(annIndent);
		writer.println("<annotation>");
		if (documentation!=null) {
			writer.println(indent2+"<documentation>");
			writer.println(indent3+documentation);
			writer.println(indent2+"</documentation>");
		}
		if (getBasedOn()!=null || getKind()!=STRING) {
			writer.println(indent2+"<appInfo>");
			writer.print(indent3+"<meta.attribute");
			String kindValue = null;
			switch (getKind()) {
				case JAVA: kindValue = "java";
				break;
				case RESOURCE: kindValue = "resource";
				break;
			}
			if (kindValue!=null) writer.print(" kind=\""+kindValue+"\"");
			if (getBasedOn()!=null) writer.print(" basedOn=\""+getBasedOn()+"\"");
			writer.println("/>");
			writer.println(indent2+"</appInfo>");
		}
		writer.println(annIndent+"</annotation>");
	}
	if (type.getRestriction()!=null) {
		type.write(indent+Schema.INDENT, writer);
	}
	if (annotation || type.getRestriction()!=null) {
		writer.println(indent+"</attribute>");
	}
	else {
		writer.println("/>");
	}
}
}
