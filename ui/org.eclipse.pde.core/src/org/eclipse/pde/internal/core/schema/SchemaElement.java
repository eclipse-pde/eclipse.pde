package org.eclipse.pde.internal.core.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.ischema.*;

public class SchemaElement extends RepeatableSchemaObject implements ISchemaElement {
	public static final String P_ICON_NAME = "iconName";
	public static final String P_LABEL_PROPERTY = "labelProperty";
	public static final String P_TYPE = "type";
	private String labelProperty;
	private ISchemaType type;
	private String iconName;

public SchemaElement(ISchemaObject parent, String name) {
	super(parent, name);
}
private String calculateChildRepresentation(ISchemaObject object) {
	String child = "";
	if (object instanceof ISchemaCompositor) {
		child = calculateCompositorRepresentation((ISchemaCompositor) object);
		if (!child.equals("EMPTY") && child.length()>0) {
			child = "("+child+")";
		}
	} else
		child = object.getName();
	int minOccurs = 1;
	int maxOccurs = 1;
	if (object instanceof ISchemaRepeatable) {
		minOccurs = ((ISchemaRepeatable) object).getMinOccurs();
		maxOccurs = ((ISchemaRepeatable) object).getMaxOccurs();
	}
	if (minOccurs == 0) {
		if (maxOccurs == 1)
			child += "?";
		else
			child += "*";
	} else
		if (minOccurs == 1) {
			if (maxOccurs > 1)
				child += "+";
		}
	return child;
}
private String calculateCompositorRepresentation(ISchemaCompositor compositor) {
	int kind = compositor.getKind();
	ISchemaObject[] children = compositor.getChildren();
	if (children.length==0) return "EMPTY";
	String text = kind == ISchemaCompositor.GROUP ? "(" : "";
	for (int i=0; i<children.length; i++) {
		ISchemaObject object = (ISchemaObject) children[i];
		String child = calculateChildRepresentation(object);

		text += child;
		if (i< children.length -1) {
			if (kind == ISchemaCompositor.SEQUENCE)
				text += " , ";
			else
				if (kind == ISchemaCompositor.CHOICE)
					text += " | ";
		}
	}
	if (kind == ISchemaCompositor.GROUP)
		text += ")";
	return text;
}
public ISchemaAttribute getAttribute(String name) {
	if (type != null && type instanceof ISchemaComplexType) {
		return ((ISchemaComplexType) type).getAttribute(name);
	}
	return null;
}
public int getAttributeCount() {
	if (type!=null && type instanceof ISchemaComplexType) {
		return ((ISchemaComplexType)type).getAttributeCount();
	}
	return 0;
}
public ISchemaAttribute[] getAttributes() {
	if (type!=null && type instanceof ISchemaComplexType) {
		return ((ISchemaComplexType)type).getAttributes();
	}
	return new ISchemaAttribute[0];
}
public String getDTDRepresentation() {
	String text = "";
	if (type == null)
		text += "EMPTY";
	else {
		if (type instanceof ISchemaComplexType) {
			ISchemaComplexType complexType = (ISchemaComplexType) type;
			ISchemaCompositor compositor = complexType.getCompositor();
			if (compositor != null)
				text += calculateChildRepresentation(compositor);
			else
				text += "EMPTY";

		} else
			text += "(#CDATA)";
	}
	if (text.length() > 0) {
		if (!text.equals("EMPTY") && text.charAt(0) != '(')
			text = "(" + text + ")";
	}
	return text;
}
public String getIconProperty() {
	return iconName;
}
public String getLabelProperty() {
	return labelProperty;
}
public ISchemaType getType() {
	return type;
}
public void setIconProperty(String newIconName) {
	String oldValue = iconName;
	iconName = newIconName;
	getSchema().fireModelObjectChanged(this, P_ICON_NAME, oldValue, iconName);
}
public void setLabelProperty(String labelProperty) {
	String oldValue = this.labelProperty;
	this.labelProperty = labelProperty;
	getSchema().fireModelObjectChanged(this, P_LABEL_PROPERTY, oldValue, labelProperty);
}
public void setType(ISchemaType newType) {
	Object oldValue = type;
	type = newType;
	getSchema().fireModelObjectChanged(this, P_TYPE, oldValue, type);
}
public void write(String indent, PrintWriter writer) {
	writeComments(writer);
	writer.print(indent + "<element name=\"" + getName() + "\"");
	ISchemaType type = getType();
	if (type instanceof SchemaSimpleType) {
		writer.print(" type=\"" + type.getName() + "\"");
	}
	writer.println(">");
	String indent2 = indent + Schema.INDENT;
	String realDescription = getWritableDescription();
	if (realDescription.length() == 0)
		realDescription = null;
	if (realDescription != null || iconName != null || labelProperty != null) {
		String indent3 = indent2 + Schema.INDENT;
		String indent4 = indent3 + Schema.INDENT;
		writer.println(indent2 + "<annotation>");
		if (iconName != null || labelProperty != null) {
			writer.println(indent3 + "<appInfo>");
			writer.print(indent4 + "<meta.element");
			if (labelProperty != null)
				writer.print(" labelAttribute=\"" + labelProperty + "\"");
			if (iconName != null)
				writer.print(" icon=\"" + iconName + "\"");
			writer.println("/>");
			writer.println(indent3 + "</appInfo>");
		}
		if (realDescription != null) {
			writer.println(indent3 + "<documentation>");
			if (getDescription() != null)
				writer.println(indent4 + getDescription());
			writer.println(indent3 + "</documentation>");
		}
		writer.println(indent2 + "</annotation>");
	}

	if (type instanceof SchemaComplexType) {
		SchemaComplexType complexType = (SchemaComplexType) type;
		complexType.write(indent2, writer);
	}
	writer.println(indent + "</element>");
}
}
