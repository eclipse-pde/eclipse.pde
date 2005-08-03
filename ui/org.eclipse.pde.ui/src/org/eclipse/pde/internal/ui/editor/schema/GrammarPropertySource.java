/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.ui.views.properties.*;
import java.util.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;

public class GrammarPropertySource extends SchemaObjectPropertySource {
	public static final String P_MIN_OCCURS = "minOccurs"; //$NON-NLS-1$
	public static final String P_MAX_OCCURS = "maxOccurs"; //$NON-NLS-1$
	protected Vector descriptors;

	class MinValidator implements ICellEditorValidator {
		public String isValid(Object value) {
			String svalue = value.toString();
			try {
				int ivalue = Integer.parseInt(svalue);
				return isMinOccursValid(ivalue);
			} catch (NumberFormatException e) {
				return PDEUIMessages.GrammarPropertySource_minOccursFormat; 
			}
		}
	}
	class MaxValidator implements ICellEditorValidator {
		public String isValid(Object value) {
			String svalue = value.toString();
			if (svalue.equals("unbounded")) //$NON-NLS-1$
				return isMaxOccursValid(Integer.MAX_VALUE);
			try {
				int ivalue = Integer.parseInt(svalue);
				return isMaxOccursValid(ivalue);
			} catch (NumberFormatException e) {
				return PDEUIMessages.GrammarPropertySource_maxOccursFormat; 
			}
		}
	}
	
	protected String isMinOccursValid(int ivalue) {
		if (ivalue < 0)
			return PDEUIMessages.GrammarPropertySource_minOccursValue; 
		return null;
	}
	
	protected String isMaxOccursValid(int ivalue) {
		if (ivalue < 0)
			return PDEUIMessages.GrammarPropertySource_maxOccursValue; 
		return null;
	}

	public GrammarPropertySource(ISchemaRepeatable obj) {
		super(obj);
	}
	public Object getEditableValue() {
		return null;
	}
	protected String getMaxOccurs(ISchemaRepeatable obj) {
		if (obj.getMaxOccurs() == Integer.MAX_VALUE)
			return "unbounded"; //$NON-NLS-1$
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
			createTextPropertyDescriptor(P_MIN_OCCURS, "minOccurs"); //$NON-NLS-1$
		desc.setValidator(new MinValidator());
		result.addElement(desc);
		desc = createTextPropertyDescriptor(P_MAX_OCCURS, "maxOccurs"); //$NON-NLS-1$
		desc.setValidator(new MaxValidator());
		result.addElement(desc);
		return result;
	}
	public Object getPropertyValue(Object name) {
		ISchemaRepeatable obj = (ISchemaRepeatable) getSourceObject();
		if (name.equals(P_MIN_OCCURS))
			return getMinOccurs(obj);
		if (name.equals(P_MAX_OCCURS))
			return getMaxOccurs(obj);
		return null;
	}
	public boolean isPropertySet(Object property) {
		return false;
	}
	public int parseValue(Object value) {
		String svalue = (String) value;
		if (svalue.equals("unbounded")) //$NON-NLS-1$
			return Integer.MAX_VALUE;
		try {
			return Integer.parseInt(svalue.toString());

		} catch (NumberFormatException e) {
			PDEPlugin.logException(e);
		}
		return 1;
	}
	public void resetPropertyValue(Object property) {
	}
	public void setPropertyValue(Object name, Object value) {
		ISchemaRepeatable obj = (ISchemaRepeatable) getSourceObject();

		if (name.equals(P_MIN_OCCURS)) {
			int ivalue = parseValue(value);
			if (obj instanceof RepeatableSchemaObject) {
				((RepeatableSchemaObject) obj).setMinOccurs(ivalue);
			} else if (obj instanceof SchemaElementReference) {
				((SchemaElementReference) obj).setMinOccurs(ivalue);
			}
		} else if (name.equals(P_MAX_OCCURS)) {
			int ivalue = parseValue(value);
			if (obj instanceof RepeatableSchemaObject) {
				((RepeatableSchemaObject) obj).setMaxOccurs(ivalue);
			} else if (obj instanceof SchemaElementReference) {
				((SchemaElementReference) obj).setMaxOccurs(ivalue);
			}
		}
	}
}
