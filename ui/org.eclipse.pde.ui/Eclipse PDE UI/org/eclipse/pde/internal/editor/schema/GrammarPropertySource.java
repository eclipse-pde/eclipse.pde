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

public class GrammarPropertySource extends SchemaObjectPropertySource {
	public static final String P_MIN_OCCURS = "minOccurs";
	public static final String P_MAX_OCCURS = "maxOccurs";
	protected Vector descriptors;

	class MinValidator implements ICellEditorValidator {
		public String isValid(Object value) {
			String svalue = value.toString();
			try {
				int ivalue = Integer.parseInt(svalue);
				if (ivalue < 0)
					return "minOccurs must be an integer that is greater or equal 0";
			} catch (NumberFormatException e) {
				return "minOccurs must be an integer that is greater or equal 0";
			}
			return null;
		}
	}
	class MaxValidator implements ICellEditorValidator {
		public String isValid(Object value) {
			String svalue = value.toString();
			if (svalue.equals("unbounded")) return null;
			try {
				int ivalue = Integer.parseInt(svalue);
				if (ivalue < 0)
					return "maxOccurs must be either \"unbounded\" or an integer that is greater or equal 0";
			} catch (NumberFormatException e) {
				return "maxOccurs must be either \"unbounded\" or an integer that is greater or equal 0";
			}
			return null;
		}
	}

public GrammarPropertySource(ISchemaRepeatable obj) {
	super(obj);
}
public Object getEditableValue() {
	return null;
}
protected String getMaxOccurs(ISchemaRepeatable obj) {
	if (obj.getMaxOccurs()==Integer.MAX_VALUE) return "unbounded";
	return Integer.toString(obj.getMaxOccurs());
}
protected String getMinOccurs(ISchemaRepeatable obj) {
	return Integer.toString(obj.getMinOccurs());
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	if (descriptors == null) {
		descriptors = getPropertyDescriptorsVector();
	}
	return toDescriptorArray(descriptors);
}
protected Vector getPropertyDescriptorsVector() {
	Vector result = new Vector();
	PropertyDescriptor desc =
		createTextPropertyDescriptor(P_MIN_OCCURS, "minOccurs");
	desc.setValidator(new MinValidator());
	result.addElement(desc);
	desc = createTextPropertyDescriptor(P_MAX_OCCURS, "maxOccurs");
	desc.setValidator(new MaxValidator());
	result.addElement(desc);
	return result;
}
public Object getPropertyValue(Object name) {
	ISchemaRepeatable obj= (ISchemaRepeatable) getSourceObject();
	if (name.equals(P_MIN_OCCURS)) return getMinOccurs(obj);
	if (name.equals(P_MAX_OCCURS)) return getMaxOccurs(obj);
	return null;
}
public boolean isPropertySet(Object property) {
	return false;
}
public int parseValue(Object value) {
	String svalue = (String) value;
	if (svalue.equals("unbounded"))
		return Integer.MAX_VALUE;
	try {
		return Integer.parseInt(svalue.toString());

	} catch (NumberFormatException e) {
		PDEPlugin.logException(e);
	}
	return 1;
}
public void resetPropertyValue(Object property) {}
public void setPropertyValue(Object name, Object value) {
	ISchemaRepeatable obj = (ISchemaRepeatable) getSourceObject();
	
	if (name.equals(P_MIN_OCCURS)) {
		int ivalue = parseValue(value);
		if (obj instanceof RepeatableSchemaObject) {
			((RepeatableSchemaObject)obj).setMinOccurs(ivalue);
		}
		else if (obj instanceof SchemaElementReference) {
			((SchemaElementReference)obj).setMinOccurs(ivalue);
		}
	}
	else if (name.equals(P_MAX_OCCURS)) {
		int ivalue = parseValue(value);
		if (obj instanceof RepeatableSchemaObject) {
			((RepeatableSchemaObject)obj).setMaxOccurs(ivalue);
		}
		else if (obj instanceof SchemaElementReference) {
			((SchemaElementReference)obj).setMaxOccurs(ivalue);
		}
	}
}
}
