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
package org.eclipse.pde.internal.core.schema;

import java.io.*;

import org.eclipse.pde.internal.core.ischema.*;

public class SchemaAttribute extends SchemaObject implements ISchemaAttribute {

	private static final long serialVersionUID = 1L;

	private int kind = STRING;

	private int use = OPTIONAL;

	private String valueFilter;

	private ISchemaSimpleType type;

	private String basedOn;

	private Object value;

	public static final String P_USE = "useProperty"; //$NON-NLS-1$

	public static final String P_VALUE_FILTER = "valueFilterProperty"; //$NON-NLS-1$

	public static final String P_VALUE = "value"; //$NON-NLS-1$

	public static final String P_KIND = "kindProperty"; //$NON-NLS-1$

	public static final String P_TYPE = "typeProperty"; //$NON-NLS-1$

	public static final String P_BASED_ON = "basedOnProperty"; //$NON-NLS-1$

	private boolean verifying;

	private boolean fTranslatable;

	private boolean fDeprecated;

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
		Integer oldValue = new Integer(kind);
		kind = newKind;
		getSchema().fireModelObjectChanged(this, P_KIND, oldValue,
				new Integer(kind));
		verifyProperties();
	}
	
	public void setTranslatableProperty(boolean translatable) {
		boolean oldValue = fTranslatable;
		fTranslatable = translatable;
		getSchema().fireModelObjectChanged(this, P_TRANSLATABLE,
				Boolean.valueOf(oldValue), Boolean.valueOf(translatable));
	}
	
	public void setDeprecatedProperty(boolean deprecated) {
		boolean oldValue = fDeprecated;
		fDeprecated = deprecated;
		getSchema().fireModelObjectChanged(this, P_DEPRECATED,
				Boolean.valueOf(oldValue), Boolean.valueOf(deprecated));		
	}

	public void setType(ISchemaSimpleType newType) {
		Object oldValue = type;
		type = newType;
		getSchema().fireModelObjectChanged(this, P_TYPE, oldValue, type);
		verifyProperties();
	}

	public void setParent(ISchemaObject obj) {
		super.setParent(obj);
		if (type != null)
			type.setSchema(getSchema());
	}

	public void setUse(int newUse) {
		Integer oldValue = new Integer(use);
		use = newUse;
		getSchema().fireModelObjectChanged(this, P_USE, oldValue,
				new Integer(use));
		verifyProperties();
	}

	public void setValue(String value) {
		String oldValue = (String) this.value;
		this.value = value;
		getSchema().fireModelObjectChanged(this, P_VALUE, oldValue, value);
		verifyProperties();
	}

	public void setValueFilter(String valueFilter) {
		String oldValue = this.valueFilter;
		this.valueFilter = valueFilter;
		getSchema().fireModelObjectChanged(this, P_VALUE_FILTER, oldValue,
				valueFilter);
	}

	private void verifyProperties() {
		if (verifying)
			return;
		verifying = true;
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
		if (type != null && type.getName().equals("boolean")) //$NON-NLS-1$
			// no restriction for boolean
			ensureNoRestriction();
		if (use != DEFAULT)
			// value makes no sense without 'default' use
			setValue(null);
		verifying = false;
	}

	private void ensureStringType() {
		if (type == null || type.getName().equals("boolean")) //$NON-NLS-1$
			setType(new SchemaSimpleType(getSchema(), "string")); //$NON-NLS-1$
	}

	private void ensureNoRestriction() {
		if (type instanceof SchemaSimpleType
				&& ((SchemaSimpleType) type).getRestriction() != null) {
			SchemaSimpleType simpleType = (SchemaSimpleType) type;
			simpleType.setRestriction(null);
			setType(simpleType);
		}
	}

	public void write(String indent, PrintWriter writer) {
		boolean annotation = false;
		ISchemaSimpleType type = (ISchemaSimpleType) getType();
		String typeName = type.getName();
		writer.print(indent);
		writer.print("<attribute name=\"" + getName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (type.getRestriction() == null)
			writer.print(" type=\"" + typeName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		String useString = null;
		switch (getUse()) {
		case OPTIONAL:
			// don't write default setting
			// useString="optional";
			break;
		case DEFAULT:
			useString = "default"; //$NON-NLS-1$
			break;
		case REQUIRED:
			useString = "required"; //$NON-NLS-1$
			break;
		}
		if (useString != null) {
			writer.print(" use=\"" + useString + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (value != null) {
			writer.print(" value=\"" + value + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		String documentation = getWritableDescription();
		if (documentation != null || this.getBasedOn() != null
				|| getKind() != STRING) {
			// Add annotation
			annotation = true;
			writer.println(">"); //$NON-NLS-1$
			String annIndent = indent + Schema.INDENT;
			String indent2 = annIndent + Schema.INDENT;
			String indent3 = indent2 + Schema.INDENT;
			writer.print(annIndent);
			writer.println("<annotation>"); //$NON-NLS-1$
			if (documentation != null) {
				writer.println(indent2 + "<documentation>"); //$NON-NLS-1$
				writer.println(indent3 + documentation);
				writer.println(indent2 + "</documentation>"); //$NON-NLS-1$
			}
			if (getBasedOn() != null || getKind() != STRING || isDeprecated() || isTranslatable()) {
				writer.println(indent2 + "<appInfo>"); //$NON-NLS-1$
				writer.print(indent3 + "<meta.attribute"); //$NON-NLS-1$
				String kindValue = null;
				switch (getKind()) {
				case JAVA:
					kindValue = "java"; //$NON-NLS-1$
					break;
				case RESOURCE:
					kindValue = "resource"; //$NON-NLS-1$
					break;
				}
				if (kindValue != null)
					writer.print(" kind=\"" + kindValue + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				if (getBasedOn() != null)
					writer.print(" basedOn=\"" + getBasedOn() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				if (isTranslatable())
					writer.print(" translatable=\"true\""); //$NON-NLS-1$
				if (isDeprecated())
					writer.print(" deprecated=\"true\""); //$NON-NLS-1$
				writer.println("/>"); //$NON-NLS-1$
				writer.println(indent2 + "</appInfo>"); //$NON-NLS-1$
			}
			writer.println(annIndent + "</annotation>"); //$NON-NLS-1$
		}
		if (type.getRestriction() != null) {
			type.write(indent + Schema.INDENT, writer);
		}
		if (annotation || type.getRestriction() != null) {
			writer.println(indent + "</attribute>"); //$NON-NLS-1$
		} else {
			writer.println("/>"); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaAttribute#isTranslatable()
	 */
	public boolean isTranslatable() {
		return fTranslatable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.ischema.IMetaAttribute#isDeprecated()
	 */
	public boolean isDeprecated() {
		return fDeprecated;
	}
}
