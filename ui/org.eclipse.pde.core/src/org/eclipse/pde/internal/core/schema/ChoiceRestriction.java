/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.PrintWriter;
import java.util.Vector;

import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaEnumeration;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaRestriction;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;

public class ChoiceRestriction extends SchemaObject implements ISchemaRestriction {

	private static final long serialVersionUID = 1L;
	private ISchemaSimpleType baseType;
	private Vector children;
	public static final String P_CHOICES = "choices"; //$NON-NLS-1$

	public ChoiceRestriction(ISchema schema) {
		super(schema, "__choice__"); //$NON-NLS-1$

	}

	public ChoiceRestriction(ChoiceRestriction source) {
		this(source.getSchema());
		children = new Vector();
		Object[] choices = source.getChildren();
		for (int i = 0; i < choices.length; i++) {
			children.add(new SchemaEnumeration(this, ((ISchemaEnumeration) choices[i]).getName()));
		}
	}

	public ISchemaSimpleType getBaseType() {
		return baseType;
	}

	public Object[] getChildren() {
		return (children != null) ? children.toArray() : new Object[0];
	}

	public String[] getChoicesAsStrings() {
		if (children == null)
			return new String[0];
		Vector result = new Vector();
		for (int i = 0; i < children.size(); i++) {
			ISchemaEnumeration enumeration = (ISchemaEnumeration) children.get(i);
			result.addElement(enumeration.getName());
		}
		String[] choices = new String[result.size()];
		result.copyInto(choices);
		return choices;
	}

	public ISchemaObject getParent() {
		if (baseType != null)
			return baseType.getSchema();
		return super.getParent();
	}

	public boolean isValueValid(java.lang.Object value) {
		if (children == null)
			return false;
		String svalue = value.toString();

		for (int i = 0; i < children.size(); i++) {
			ISchemaEnumeration enumeration = (ISchemaEnumeration) children.get(i);
			if (enumeration.getName().equals(svalue))
				return true;
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
			return ""; //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < children.size(); i++) {
			Object child = children.get(i);
			if (child instanceof ISchemaEnumeration) {
				ISchemaEnumeration enumeration = (ISchemaEnumeration) child;
				if (i > 0)
					buffer.append(", "); //$NON-NLS-1$
				buffer.append(enumeration.getName());
			}
		}
		return buffer.toString();
	}

	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<restriction base=\"" + baseType.getName() + "\">"); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < children.size(); i++) {
			Object child = children.get(i);
			if (child instanceof ISchemaEnumeration) {
				ISchemaEnumeration enumeration = (ISchemaEnumeration) child;
				enumeration.write(indent + Schema.INDENT, writer);
			}
		}
		writer.println(indent + "</restriction>"); //$NON-NLS-1$
	}
}
