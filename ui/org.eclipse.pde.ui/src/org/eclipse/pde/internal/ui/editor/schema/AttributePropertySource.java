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
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.ui.views.properties.*;
import java.util.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;

public class AttributePropertySource extends SchemaObjectPropertySource implements
        ICloneablePropertySource {
    public static final String P_USE = "use"; //$NON-NLS-1$
    public static final String P_KIND = "kind"; //$NON-NLS-1$
    public static final String P_VALUE = "value"; //$NON-NLS-1$
    public static final String P_BASED_ON = "basedOn"; //$NON-NLS-1$
    public static final String P_TYPE = "type"; //$NON-NLS-1$
    public static final String P_TRANSLATABLE = "translatable"; //$NON-NLS-1$
    public static final String P_DEPRECATED = "deprecated"; //$NON-NLS-1$
    
    public static final String KEY_COPY_OF = "SchemaEditor.AttributePR.attributeCopy"; //$NON-NLS-1$
    public static final String KEY_USE = "SchemaEditor.AttributePR.use"; //$NON-NLS-1$
    public static final String KEY_KIND = "SchemaEditor.AttributePR.kind"; //$NON-NLS-1$
    public static final String KEY_TYPE = "SchemaEditor.AttributePR.type"; //$NON-NLS-1$
    public static final String KEY_RESTRICTION = "SchemaEditor.AttributePR.restriction"; //$NON-NLS-1$
    public static final String KEY_VALUE = "SchemaEditor.AttributePR.value"; //$NON-NLS-1$
    public static final String KEY_BASED_ON = "SchemaEditor.AttributePR.basedOn"; //$NON-NLS-1$
    public static final String KEY_NAME = "SchemaEditor.AttributePR.name"; //$NON-NLS-1$
    public static final String P_RESTRICTION = "restriction"; //$NON-NLS-1$
    public static final String P_NAME = "name"; //$NON-NLS-1$
    private Vector descriptors;
    
    private static final String[] typeTable = { "string", "boolean" }; //$NON-NLS-1$ //$NON-NLS-2$
    private static final String[] booleanTable = { "false", "true" }; //$NON-NLS-1$ //$NON-NLS-2$
    
    class ValueValidator implements ICellEditorValidator {
        public String isValid(Object value) {
            String svalue = value.toString();
            ISchemaAttribute att = (ISchemaAttribute) getSourceObject();
            ISchemaSimpleType type = att.getType();
            if (type.getName().equals("boolean")) { //$NON-NLS-1$
                if (!svalue.equals("true") && !svalue.equals("false")) //$NON-NLS-1$ //$NON-NLS-2$
                    return PDEPlugin
                            .getResourceString("AttributePropertySource.assertBoolean"); //$NON-NLS-1$
            } else if (type.getName().equals("string") //$NON-NLS-1$
                    && type.getRestriction() != null) {
                ISchemaRestriction restriction = type.getRestriction();
                if (restriction.isValueValid(svalue) == false) {
                    return PDEPlugin.getFormattedMessage(
                            "AttributePropertySource.invalidRestriction", svalue); //$NON-NLS-1$
                }
            }
            return null;
        }
    }

    public AttributePropertySource(
            org.eclipse.pde.internal.core.ischema.ISchemaAttribute att) {
        super(att);
    }

    public Object doClone() {
        ISchemaAttribute att = (ISchemaAttribute) getSourceObject();
        SchemaElement element = (SchemaElement) att.getParent();
        String value = PDEPlugin.getFormattedMessage(KEY_COPY_OF, att.getName());
        SchemaAttribute att2 = new SchemaAttribute(att, value);
        ((SchemaComplexType) element.getType()).addAttribute(att2);
        return att2;
    }

    public Object getEditableValue() {
        return null;
    }

    private int getIndexOf(String value, String[] table) {
        for (int i = 0; i < table.length; i++) {
            if (value.equals(table[i]))
                return i;
        }
        return 0;
    }

    public IPropertyDescriptor[] getPropertyDescriptors() {
        descriptors = new Vector();
        PropertyDescriptor cdesc = createComboBoxPropertyDescriptor(P_USE, PDEPlugin
                .getResourceString(KEY_USE), ISchemaAttribute.useTable);
        cdesc.setLabelProvider(new ComboProvider(P_USE, ISchemaAttribute.useTable));
        descriptors.addElement(cdesc);
        
        cdesc = createComboBoxPropertyDescriptor(P_KIND, PDEPlugin
                .getResourceString(KEY_KIND), ISchemaAttribute.kindTable);
        cdesc.setLabelProvider(new ComboProvider(P_KIND, ISchemaAttribute.kindTable));
        descriptors.addElement(cdesc);
        
        cdesc = createComboBoxPropertyDescriptor(P_TYPE, PDEPlugin
                .getResourceString(KEY_TYPE), typeTable);
        cdesc.setLabelProvider(new ComboProvider(P_TYPE, typeTable));
        descriptors.addElement(cdesc);
        
        cdesc = createComboBoxPropertyDescriptor(P_TRANSLATABLE, PDEPlugin.getResourceString("AttributePropertySource.translatable"), booleanTable); //$NON-NLS-1$
        cdesc.setLabelProvider(new ComboProvider(P_TRANSLATABLE, booleanTable));
        descriptors.addElement(cdesc);

        cdesc = createComboBoxPropertyDescriptor(P_DEPRECATED, PDEPlugin.getResourceString("AttributePropertySource.deprecated"), booleanTable); //$NON-NLS-1$
        cdesc.setLabelProvider(new ComboProvider(P_DEPRECATED, booleanTable));
        descriptors.addElement(cdesc);

        cdesc = new TypeRestrictionDescriptor(P_RESTRICTION, PDEPlugin
                .getResourceString(KEY_RESTRICTION), !isEditable());
        descriptors.addElement(cdesc);
        cdesc = createTextPropertyDescriptor(P_VALUE, PDEPlugin
                .getResourceString(KEY_VALUE));
        cdesc.setValidator(new ValueValidator());
        descriptors.addElement(cdesc);
        
        PropertyDescriptor desc = createTextPropertyDescriptor(P_BASED_ON, PDEPlugin
                .getResourceString(KEY_BASED_ON));
        descriptors.addElement(desc);
        
        desc = createTextPropertyDescriptor(P_NAME, PDEPlugin
                .getResourceString(KEY_NAME));
        descriptors.addElement(desc);

        return toDescriptorArray(descriptors);
    }

    public Object getPropertyValue(Object name) {
        ISchemaAttribute att = (ISchemaAttribute) getSourceObject();
        if (name.equals(P_DEPRECATED))
        	return att.isDeprecated() ? new Integer(1) : new Integer(0);
        	
        if (name.equals(P_TRANSLATABLE))
        	return att.isTranslatable() ? new Integer(1) : new Integer(0);
        	
        if (name.equals(P_RESTRICTION))
            return att.getType().getRestriction();
        if (name.equals(P_VALUE))
            return getNonzeroValue(att.getValue());
        if (name.equals(P_BASED_ON))
            return getNonzeroValue(att.getBasedOn());
        if (name.equals(P_NAME))
            return getNonzeroValue(att.getName());
        if (name.equals(P_USE)) {
            if (isSchemaObject)
                return new Integer(att.getUse());
            return ISchemaAttribute.useTable[att.getUse()];
        }
        if (name.equals(P_KIND)) {
            if (isSchemaObject)
                return new Integer(att.getKind());
            return ISchemaAttribute.kindTable[att.getKind()];
        }
        if (name.equals(P_TYPE)) {
            if (isSchemaObject)
                return new Integer(getIndexOf(att.getType().getName(), typeTable));
            return att.getType().getName();
        }
        return ""; //$NON-NLS-1$
    }

    public boolean isCloneable() {
        ISchemaAttribute att = (ISchemaAttribute) getSourceObject();
        if (att.getParent().getName().equals("extension")) //$NON-NLS-1$
            return false;
        return true;
    }

    public boolean isPropertySet(Object property) {
        return false;
    }

    public void resetPropertyValue(Object property) {
    }

    public void setPropertyValue(Object name, Object value) {
        SchemaAttribute att = (SchemaAttribute) getSourceObject();
        if (value instanceof Integer) {
            int index = ((Integer) value).intValue();
            if (name.equals(P_USE))
                att.setUse(index);
            else if (name.equals(P_KIND))
                att.setKind(index);
            else if (name.equals(P_TYPE)) {
                att.setType(new SchemaSimpleType(att.getSchema(), typeTable[index]));
                if (att.getValue() != null)
                    att.setValue(null);
            } else if (name.equals(P_TRANSLATABLE)) {
            	att.setTranslatableProperty(index == 1);
            } else if (name.equals(P_DEPRECATED)) {
            	att.setDeprecatedProperty(index == 1);
            }
        } else if (name.equals(P_RESTRICTION)) {
            ISchemaRestriction restriction = (ISchemaRestriction) value;
            if (restriction != null && restriction.getChildren().length == 0)
                restriction = null;
            if (att.getType() instanceof SchemaSimpleType) {
                SchemaSimpleType type = (SchemaSimpleType) att.getType();
                type.setRestriction(restriction);
                att.setType(type);
            }
        } else if (value instanceof String) {
            String svalue = (String) value;
            if (name.equals(P_VALUE))
                att.setValue(svalue);
            else if (name.equals(P_BASED_ON))
                att.setBasedOn(svalue);
            else if (name.equals(P_NAME))
                att.setName(svalue);
        }
    }
}