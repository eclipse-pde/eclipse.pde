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
package org.eclipse.pde.internal.core.schema;

import java.io.*;

import org.eclipse.pde.internal.core.ischema.*;

public class SchemaElement extends RepeatableSchemaObject implements
		ISchemaElement {

	private static final long serialVersionUID = 1L;

	public static final String P_ICON_NAME = "iconName"; //$NON-NLS-1$

	public static final String P_LABEL_PROPERTY = "labelProperty"; //$NON-NLS-1$

	public static final String P_TYPE = "type"; //$NON-NLS-1$
	
	private String labelProperty;

	private ISchemaType type;

	private String iconName;

	private boolean fTranslatable;

	private boolean fDeprecated;

	public SchemaElement(ISchemaObject parent, String name) {
		super(parent, name);
	}

	private String calculateChildRepresentation(ISchemaObject object,
			boolean addLinks) {
		String child = ""; //$NON-NLS-1$
		if (object instanceof ISchemaCompositor) {
			child = calculateCompositorRepresentation(
					(ISchemaCompositor) object, addLinks);
			if (!child.equals("EMPTY") && child.length() > 0) { //$NON-NLS-1$
				child = "(" + child + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			child = object.getName();
			if (addLinks) {
				child = "<a href=\"#e." + child + "\">" + child + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		int minOccurs = 1;
		int maxOccurs = 1;
		if (object instanceof ISchemaRepeatable) {
			minOccurs = ((ISchemaRepeatable) object).getMinOccurs();
			maxOccurs = ((ISchemaRepeatable) object).getMaxOccurs();
		}
		if (minOccurs == 0) {
			if (maxOccurs == 1)
				child += "?"; //$NON-NLS-1$
			else
				child += "*"; //$NON-NLS-1$
		} else if (minOccurs == 1) {
			if (maxOccurs > 1)
				child += "+"; //$NON-NLS-1$
		}
		return child;
	}

	private String calculateCompositorRepresentation(
			ISchemaCompositor compositor, boolean addLinks) {
		int kind = compositor.getKind();
		ISchemaObject[] children = compositor.getChildren();
		if (children.length == 0)
			return "EMPTY"; //$NON-NLS-1$
		String text = kind == ISchemaCompositor.GROUP ? "(" : ""; //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < children.length; i++) {
			ISchemaObject object = children[i];
			String child = calculateChildRepresentation(object, addLinks);

			text += child;
			if (i < children.length - 1) {
				if (kind == ISchemaCompositor.SEQUENCE)
					text += " , "; //$NON-NLS-1$
				else if (kind == ISchemaCompositor.CHOICE)
					text += " | "; //$NON-NLS-1$
			}
		}
		if (kind == ISchemaCompositor.GROUP)
			text += ")"; //$NON-NLS-1$
		return text;
	}

	public ISchemaAttribute getAttribute(String name) {
		if (type != null && type instanceof ISchemaComplexType) {
			return ((ISchemaComplexType) type).getAttribute(name);
		}
		return null;
	}

	public int getAttributeCount() {
		if (type != null && type instanceof ISchemaComplexType) {
			return ((ISchemaComplexType) type).getAttributeCount();
		}
		return 0;
	}

	public ISchemaAttribute[] getAttributes() {
		if (type != null && type instanceof ISchemaComplexType) {
			return ((ISchemaComplexType) type).getAttributes();
		}
		return new ISchemaAttribute[0];
	}

	public String getDTDRepresentation(boolean addLinks) {
		String text = ""; //$NON-NLS-1$
		if (type == null)
			text += "EMPTY"; //$NON-NLS-1$
		else {
			if (type instanceof ISchemaComplexType) {
				ISchemaComplexType complexType = (ISchemaComplexType) type;
				ISchemaCompositor compositor = complexType.getCompositor();
				if (compositor != null)
					text += calculateChildRepresentation(compositor, addLinks);
				else
					text += "EMPTY"; //$NON-NLS-1$

			} else
				text += "(#PCDATA)"; //$NON-NLS-1$
		}
		if (text.length() > 0) {
			if (!text.equals("EMPTY") && text.charAt(0) != '(') //$NON-NLS-1$
				text = "(" + text + ")"; //$NON-NLS-1$ //$NON-NLS-2$
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

	public void setParent(ISchemaObject parent) {
		super.setParent(parent);
		if (type != null) {
			type.setSchema(getSchema());
			if (type instanceof ISchemaComplexType) {
				ISchemaComplexType ctype = (ISchemaComplexType) type;
				ISchemaCompositor comp = ctype.getCompositor();
				if (comp != null)
					comp.setParent(this);
			}
		}
		if (getAttributeCount() > 0) {
			ISchemaAttribute[] atts = getAttributes();
			for (int i = 0; i < atts.length; i++) {
				ISchemaAttribute att = atts[i];
				att.setParent(this);
			}
		}
	}

	public void setIconProperty(String newIconName) {
		String oldValue = iconName;
		iconName = newIconName;
		getSchema().fireModelObjectChanged(this, P_ICON_NAME, oldValue,
				iconName);
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

	public void setLabelProperty(String labelProperty) {
		String oldValue = this.labelProperty;
		this.labelProperty = labelProperty;
		getSchema().fireModelObjectChanged(this, P_LABEL_PROPERTY, oldValue,
				labelProperty);
	}

	public void setType(ISchemaType newType) {
		Object oldValue = type;
		type = newType;
		getSchema().fireModelObjectChanged(this, P_TYPE, oldValue, type);
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<element name=\"" + getName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		ISchemaType type = getType();
		if (type instanceof SchemaSimpleType) {
			writer.print(" type=\"" + type.getName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(">"); //$NON-NLS-1$
		String indent2 = indent + Schema.INDENT;
		String realDescription = getWritableDescription();
		if (realDescription.length() == 0)
			realDescription = null;
		if (realDescription != null || iconName != null
				|| labelProperty != null || isDeprecated() || hasTranslatableContent()) {
			String indent3 = indent2 + Schema.INDENT;
			String indent4 = indent3 + Schema.INDENT;
			writer.println(indent2 + "<annotation>"); //$NON-NLS-1$
			if (iconName != null || labelProperty != null || isDeprecated() || hasTranslatableContent()) {
				writer.println(indent3 + "<appInfo>"); //$NON-NLS-1$
				writer.print(indent4 + "<meta.element"); //$NON-NLS-1$
				if (labelProperty != null)
					writer.print(" labelAttribute=\"" + labelProperty + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				if (iconName != null)
					writer.print(" icon=\"" + iconName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				if (hasTranslatableContent())
					writer.print(" translatable=\"true\""); //$NON-NLS-1$
				if (isDeprecated())
					writer.print(" deprecated=\"true\""); //$NON-NLS-1$
				writer.println("/>"); //$NON-NLS-1$
				writer.println(indent3 + "</appInfo>"); //$NON-NLS-1$
			}
			if (realDescription != null) {
				writer.println(indent3 + "<documentation>"); //$NON-NLS-1$
				if (getDescription() != null)
					writer.println(indent4 + realDescription);
				writer.println(indent3 + "</documentation>"); //$NON-NLS-1$
			}
			writer.println(indent2 + "</annotation>"); //$NON-NLS-1$
		}

		if (type instanceof SchemaComplexType) {
			SchemaComplexType complexType = (SchemaComplexType) type;
			complexType.write(indent2, writer);
		}
		writer.println(indent + "</element>"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.ischema.IMetaElement#isTranslatable()
	 */
	public boolean hasTranslatableContent() {
		return fTranslatable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.IMetaElement#isDeprecated()
	 */
	public boolean isDeprecated() {
		return fDeprecated;
	}
}
