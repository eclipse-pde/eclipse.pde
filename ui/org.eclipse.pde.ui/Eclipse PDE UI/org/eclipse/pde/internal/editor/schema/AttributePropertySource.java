package org.eclipse.pde.internal.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.schema.*;
import org.eclipse.ui.views.properties.*;
import java.util.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.*;

public class AttributePropertySource extends SchemaObjectPropertySource implements ICloneablePropertySource {
	public static final String P_USE = "use";
	public static final String P_KIND = "kind";
	public static final String P_VALUE = "value";
	public static final String P_BASED_ON = "basedOn";
	public static final String P_TYPE = "type";
	public static final String KEY_COPY_OF = "SchemaEditor.AttributePR.attributeCopy";
	public static final String KEY_USE = "SchemaEditor.AttributePR.use";
	public static final String KEY_KIND = "SchemaEditor.AttributePR.kind";
	public static final String KEY_TYPE = "SchemaEditor.AttributePR.type";
	public static final String KEY_RESTRICTION = "SchemaEditor.AttributePR.restriction";
	public static final String KEY_VALUE = "SchemaEditor.AttributePR.value";
	public static final String KEY_BASED_ON = "SchemaEditor.AttributePR.basedOn";
	public static final String KEY_NAME = "SchemaEditor.AttributePR.name";  
	public static final String P_RESTRICTION = "restriction";
	public static final String P_NAME = "name";
	private Vector descriptors;

	private static final String [] typeTable = { "string", "boolean" };

	class ValueValidator implements ICellEditorValidator {
		public String isValid(Object value) {
			String svalue = value.toString();
			ISchemaAttribute att = (ISchemaAttribute)getSourceObject();
			ISchemaSimpleType type = att.getType();
			if (type.getName().equals("boolean")) {
				if (!svalue.equals("true") && !svalue.equals("false"))
				return "Boolean value must either be \"true\" or \"false\"";
			}
			else if (type.getName().equals("string") && type.getRestriction()!=null) {
				ISchemaRestriction restriction = type.getRestriction();
				if (restriction.isValueValid(svalue)==false) {
					return "Value \""+svalue+"\" is not valid for the specified restriction";
				}
			}
			return null;
		}
	}


public AttributePropertySource(org.eclipse.pde.internal.base.schema.ISchemaAttribute att) {
	super(att);
}
public Object doClone() {
	ISchemaAttribute att = (ISchemaAttribute)getSourceObject();
	SchemaElement element = (SchemaElement)att.getParent();
	String value = PDEPlugin.getFormattedMessage(KEY_COPY_OF, att.getName());
	SchemaAttribute att2 = new SchemaAttribute(att, value);
	((SchemaComplexType)element.getType()).addAttribute(att2);
	return att2;
}
public Object getEditableValue() {
	return null;
}
private int getIndexOf(String value, String[] table) {
	for (int i=0; i<table.length; i++) {
		if (value.equals(table[i])) return i;
	}
	return 0;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	if (descriptors == null) {
		descriptors = new Vector();
		PropertyDescriptor cdesc =
			createComboBoxPropertyDescriptor(P_USE, PDEPlugin.getResourceString(KEY_USE), ISchemaAttribute.useTable);
		if (cdesc instanceof ComboBoxPropertyDescriptor)
			((ComboBoxPropertyDescriptor) cdesc).setLabelProvider(
				new ComboProvider(P_USE, ISchemaAttribute.useTable));
		descriptors.addElement(cdesc);
		cdesc =
			createComboBoxPropertyDescriptor(P_KIND, PDEPlugin.getResourceString(KEY_KIND), ISchemaAttribute.kindTable);
		if (cdesc instanceof ComboBoxPropertyDescriptor)
			((ComboBoxPropertyDescriptor) cdesc).setLabelProvider(
				new ComboProvider(P_KIND, ISchemaAttribute.kindTable));
		descriptors.addElement(cdesc);
		cdesc = createComboBoxPropertyDescriptor(P_TYPE, PDEPlugin.getResourceString(KEY_TYPE), typeTable);
		if (cdesc instanceof ComboBoxPropertyDescriptor)
			((ComboBoxPropertyDescriptor) cdesc).setLabelProvider(
				new ComboProvider(P_TYPE, typeTable));
		descriptors.addElement(cdesc);
		cdesc = new TypeRestrictionDescriptor(P_RESTRICTION, PDEPlugin.getResourceString(KEY_RESTRICTION), !isEditable());
		descriptors.addElement(cdesc);
		cdesc = createTextPropertyDescriptor(P_VALUE, PDEPlugin.getResourceString(KEY_VALUE));
		cdesc.setValidator(new ValueValidator());
		descriptors.addElement(cdesc);
		PropertyDescriptor desc = createTextPropertyDescriptor(P_BASED_ON, PDEPlugin.getResourceString(KEY_BASED_ON));
		descriptors.addElement(desc);
		desc = createTextPropertyDescriptor(P_NAME, PDEPlugin.getResourceString(KEY_NAME));
		descriptors.addElement(desc);
	}
	return toDescriptorArray(descriptors);
}
public Object getPropertyValue(Object name) {
	ISchemaAttribute att = (ISchemaAttribute) getSourceObject();
	if (name.equals(P_USE)) return new Integer(att.getUse());
	if (name.equals(P_KIND)) return new Integer(att.getKind());
	if (name.equals(P_TYPE)) return new Integer(getIndexOf(att.getType().getName(), typeTable));
	if (name.equals(P_RESTRICTION)) return ((ISchemaSimpleType)att.getType()).getRestriction();
	if (name.equals(P_VALUE)) return getNonzeroValue(att.getValue());
	if (name.equals(P_BASED_ON)) return getNonzeroValue(att.getBasedOn());
	if (name.equals(P_NAME)) return getNonzeroValue(att.getName());
	return "";
}
public boolean isCloneable() {
	ISchemaAttribute att = (ISchemaAttribute)getSourceObject();
	if (att.getParent().getName().equals("extension")) return false;
	return true;
}
public boolean isPropertySet(Object property) {
	return false;
}
public void resetPropertyValue(Object property) {}
public void setPropertyValue(Object name, Object value) {
	SchemaAttribute att = (SchemaAttribute) getSourceObject();
	if (value instanceof Integer) {
		int index = ((Integer) value).intValue();
		if (name.equals(P_USE))
			att.setUse(index);
		else
			if (name.equals(P_KIND))
				att.setKind(index);
			else
				if (name.equals(P_TYPE)) {
					att.setType(new SchemaSimpleType(att.getSchema(), typeTable[index]));
					if (att.getValue()!=null) att.setValue(null);
				}
	} else
		if (name.equals(P_RESTRICTION)) {
			ISchemaRestriction restriction = (ISchemaRestriction) value;
			if (restriction != null && restriction.getChildren().length==0)
				restriction = null;
			if (att.getType() instanceof SchemaSimpleType) {
				SchemaSimpleType type = (SchemaSimpleType) att.getType();
				type.setRestriction(restriction);
				att.setType(type);
			}
		} else
			if (value instanceof String) {
				String svalue = (String) value;
				if (name.equals(P_VALUE))
					att.setValue(svalue);
				else
					if (name.equals(P_BASED_ON))
						att.setBasedOn(svalue);
					else
						if (name.equals(P_NAME))
							att.setName(svalue);
			}
}
}
