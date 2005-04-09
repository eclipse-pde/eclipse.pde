/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.schema.*;
import java.util.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;

public class ElementPropertySource extends SchemaObjectPropertySource {
	public static final String P_LABEL_ATTRIBUTE = "labelAttribute"; //$NON-NLS-1$

	public static final String P_ICON = "icon"; //$NON-NLS-1$

	public static final String P_NAME = "name"; //$NON-NLS-1$
    public static final String P_TRANSLATABLE = "translatable"; //$NON-NLS-1$
    public static final String P_DEPRECATED = "deprecated"; //$NON-NLS-1$

	private Vector descriptors;
	
    private static final String[] booleanTable = { "false", "true" }; //$NON-NLS-1$ //$NON-NLS-2$

	class LabelAttributeValidator implements ICellEditorValidator {
		public String isValid(Object value) {
			String svalue = value.toString();
			if (isValidAttribute(svalue) == false) {
				return NLS.bind(PDEUIMessages.SchemaEditor_ElementPR_invalid, svalue);
			}
			return null;
		}
	}

	public ElementPropertySource(ISchemaElement extension) {
		super(extension);
	}

	private void fixReferences(SchemaElement element) {
		((Schema) element.getSchema()).updateReferencesFor(element);
	}

	public Object getEditableValue() {
		return null;
	}

	public IPropertyDescriptor[] getPropertyDescriptors() {
		if (descriptors == null) {
			descriptors = new Vector();
			PropertyDescriptor desc = createTextPropertyDescriptor(
					P_LABEL_ATTRIBUTE, PDEUIMessages.SchemaEditor_ElementPR_labelAttribute);
			desc.setValidator(new LabelAttributeValidator());
			descriptors.addElement(desc);
			
			desc = createTextPropertyDescriptor(P_ICON, PDEUIMessages.SchemaEditor_ElementPR_icon);
			descriptors.addElement(desc);
			
			desc = createTextPropertyDescriptor(P_NAME, PDEUIMessages.SchemaEditor_ElementPR_name);
			descriptors.addElement(desc);
			
            desc = createComboBoxPropertyDescriptor(P_TRANSLATABLE, PDEUIMessages.ElementPropertySource_translatable, booleanTable); //$NON-NLS-1$
            if (desc instanceof ComboBoxPropertyDescriptor)
                ((ComboBoxPropertyDescriptor) desc).setLabelProvider(new ComboProvider(
                        P_TRANSLATABLE, booleanTable));
            descriptors.addElement(desc);

            desc = createComboBoxPropertyDescriptor(P_DEPRECATED, PDEUIMessages.ElementPropertySource_deprecated, booleanTable); //$NON-NLS-1$
            if (desc instanceof ComboBoxPropertyDescriptor)
                ((ComboBoxPropertyDescriptor) desc).setLabelProvider(new ComboProvider(
                        P_DEPRECATED, booleanTable));
            descriptors.addElement(desc);

 		}
		return toDescriptorArray(descriptors);
	}

	public Object getPropertyValue(Object name) {
		ISchemaElement element = (ISchemaElement) getSourceObject();
        if (name.equals(P_DEPRECATED))
        	return element.isDeprecated() ? new Integer(1) : new Integer(0);
        	
        if (name.equals(P_TRANSLATABLE))
        	return element.hasTranslatableContent() ? new Integer(1) : new Integer(0);
        	
		if (name.equals(P_LABEL_ATTRIBUTE))
			return getNonzeroValue(element.getLabelProperty());
		if (name.equals(P_ICON))
			return getNonzeroValue(element.getIconProperty());
		if (name.equals(P_NAME))
			return getNonzeroValue(element.getName());
		return ""; //$NON-NLS-1$
	}

	public boolean isPropertySet(Object property) {
		return false;
	}

	private boolean isValidAttribute(String name) {
		if (name == null || name.length() == 0)
			return true;
		ISchemaElement element = (ISchemaElement) getSourceObject();
		return element.getAttribute(name) != null;
	}

	public void resetPropertyValue(Object property) {
	}

	public void setPropertyValue(Object name, Object value) {
		SchemaElement element = (SchemaElement) getSourceObject();
	    if (value instanceof Integer) {
            int index = ((Integer) value).intValue();
            if (name.equals(P_TRANSLATABLE)) {
            	element.setTranslatableProperty(index == 1);
            } else if (name.equals(P_DEPRECATED)) {
            	element.setDeprecatedProperty(index == 1);
            }	    
        } else {
			String svalue = (String) value;
			if (name.equals(P_LABEL_ATTRIBUTE))
				element.setLabelProperty(svalue);
			else if (name.equals(P_ICON))
				element.setIconProperty(svalue);
			else if (name.equals(P_NAME)) {
				element.setName(svalue);
				fixReferences(element);
			}
	    }
	}
}
