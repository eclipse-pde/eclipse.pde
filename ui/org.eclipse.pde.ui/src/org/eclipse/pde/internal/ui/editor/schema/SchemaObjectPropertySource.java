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

import java.util.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.jface.viewers.*;

public abstract class SchemaObjectPropertySource implements IPropertySource {
    private Object sourceObject;
    protected boolean isSchemaObject = false;
    class ComboProvider extends LabelProvider {
        private String property;
        private String[] table;

        public ComboProvider(String property, String[] table) {
            this.property = property;
            this.table = table;
        }

        public String getText(Object obj) {
        	isSchemaObject = true;
        	Integer index = (Integer) getPropertyValue(property);
        	return table[index.intValue()];
        }
    }

    public SchemaObjectPropertySource(Object object) {
        this.sourceObject = object;
    }

    protected PropertyDescriptor createComboBoxPropertyDescriptor(String id, String name,
            String[] choices) {
        if (isEditable())
            return new ComboBoxPropertyDescriptor(id, name, choices);
        else
            return new PropertyDescriptor(id, name);
    }

    protected PropertyDescriptor createTextPropertyDescriptor(String id, String name) {
        if (isEditable())
            return new ModifiedTextPropertyDescriptor(id, name);
        else
            return new PropertyDescriptor(id, name);
    }

    protected Object getNonzeroValue(Object value) {
        if (value != null)
            return value;
        return ""; //$NON-NLS-1$
    }

    public java.lang.Object getSourceObject() {
        return sourceObject;
    }

    public boolean isEditable() {
        ISchemaObject schemaObject = (ISchemaObject) getSourceObject();
        ISchema schema = schemaObject.getSchema();
        return schema != null ? schema.isEditable() : false;
    }

    public void setSourceObject(java.lang.Object newSourceObject) {
        sourceObject = newSourceObject;
    }

    protected IPropertyDescriptor[] toDescriptorArray(Vector result) {
        IPropertyDescriptor[] array = new IPropertyDescriptor[result.size()];
        result.copyInto(array);
        return array;
    }
}