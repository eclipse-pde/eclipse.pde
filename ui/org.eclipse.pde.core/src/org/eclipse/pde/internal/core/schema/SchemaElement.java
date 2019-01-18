/*******************************************************************************
 *  Copyright (c) 2000, 2014 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Carver - STAR - bug 213255
 *     Brian de Alwis (MTI) - bug 429420
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaRepeatable;
import org.eclipse.pde.internal.core.ischema.ISchemaType;
import org.eclipse.pde.internal.core.util.SchemaUtil;
import org.eclipse.pde.internal.core.util.XMLComponentRegistry;

public class SchemaElement extends RepeatableSchemaObject implements ISchemaElement {

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

	private String calculateChildRepresentation(ISchemaObject object, boolean addLinks) {
		String child = ""; //$NON-NLS-1$
		if (object instanceof ISchemaCompositor) {
			child = calculateCompositorRepresentation((ISchemaCompositor) object, addLinks);
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
			if (maxOccurs == 1) {
				child += "?"; //$NON-NLS-1$
			} else {
				child += "*"; //$NON-NLS-1$
			}
		} else if (minOccurs == 1) {
			if (maxOccurs > 1)
			 {
				child += "+"; //$NON-NLS-1$
			}
		}
		return child;
	}

	private String calculateCompositorRepresentation(ISchemaCompositor compositor, boolean addLinks) {
		int kind = compositor.getKind();
		ISchemaObject[] children = compositor.getChildren();
		if (children.length == 0) {
			return "EMPTY"; //$NON-NLS-1$
		}
		String text = kind == ISchemaCompositor.GROUP ? "(" : ""; //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < children.length; i++) {
			ISchemaObject object = children[i];
			String child = calculateChildRepresentation(object, addLinks);

			text += child;
			if (i < children.length - 1) {
				if (kind == ISchemaCompositor.SEQUENCE) {
					text += " , "; //$NON-NLS-1$
				} else if (kind == ISchemaCompositor.CHOICE) {
					text += " | "; //$NON-NLS-1$
				}
			}
		}
		if (kind == ISchemaCompositor.GROUP) {
			text += ")"; //$NON-NLS-1$
		}
		return text;
	}

	@Override
	public ISchemaAttribute getAttribute(String name) {
		if (type != null && type instanceof ISchemaComplexType) {
			return ((ISchemaComplexType) type).getAttribute(name);
		}
		return null;
	}

	@Override
	public int getAttributeCount() {
		if (type != null && type instanceof ISchemaComplexType) {
			return ((ISchemaComplexType) type).getAttributeCount();
		}
		return 0;
	}

	@Override
	public ISchemaAttribute[] getAttributes() {
		if (type != null && type instanceof ISchemaComplexType) {
			return ((ISchemaComplexType) type).getAttributes();
		}
		return new ISchemaAttribute[0];
	}

	@Override
	public String[] getAttributeNames() {
		ISchemaAttribute[] attributes = getAttributes();
		String[] names = new String[attributes.length];
		for (int i = 0; i < attributes.length; i++) {
			names[i] = attributes[i].getName();
		}
		return names;
	}

	@Override
	public String getDTDRepresentation(boolean addLinks) {
		String text = ""; //$NON-NLS-1$
		if (type == null) {
			text += "EMPTY"; //$NON-NLS-1$
		} else {
			if (type instanceof ISchemaComplexType) {
				ISchemaComplexType complexType = (ISchemaComplexType) type;
				ISchemaCompositor compositor = complexType.getCompositor();
				if (compositor != null) {
					text += calculateChildRepresentation(compositor, addLinks);
				} else if (getAttributeCount() != 0) {
					text += "EMPTY"; //$NON-NLS-1$
				}
			}
			if (text.length() == 0) {
				text += "(#PCDATA)"; //$NON-NLS-1$
			}
		}
		if (text.length() > 0) {
			if (!text.equals("EMPTY") && text.charAt(0) != '(') { //$NON-NLS-1$
				text = "(" + text + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return text;
	}

	@Override
	public String getIconProperty() {
		if (iconName != null) {
			return iconName;
		}
		ISchemaAttribute[] attributes = getAttributes();
		for (ISchemaAttribute attribute : attributes) {
			if (isValidIconProperty(attribute)) {
				return attribute.getName();
			}
		}
		return null;
	}

	@Override
	public String getLabelProperty() {
		if (labelProperty != null) {
			return labelProperty;
		}
		ISchemaAttribute[] attributes = getAttributes();
		for (ISchemaAttribute attribute : attributes) {
			if (isValidLabelProperty(attribute)) {
				return attribute.getName();
			}
		}
		return null;
	}

	private boolean isValidLabelProperty(ISchemaAttribute a) {
		return a.getKind() == IMetaAttribute.STRING && a.getType().getName().equals(ISchemaAttribute.TYPES[ISchemaAttribute.STR_IND]) && a.isTranslatable();
	}

	private boolean isValidIconProperty(ISchemaAttribute a) {
		return a.getKind() == IMetaAttribute.RESOURCE;
	}

	@Override
	public ISchemaType getType() {
		return type;
	}

	@Override
	public void setParent(ISchemaObject parent) {
		super.setParent(parent);
		if (type != null) {
			type.setSchema(getSchema());
			if (type instanceof ISchemaComplexType) {
				ISchemaComplexType ctype = (ISchemaComplexType) type;
				ISchemaCompositor comp = ctype.getCompositor();
				if (comp != null) {
					comp.setParent(this);
				}
			}
		}
		if (getAttributeCount() > 0) {
			ISchemaAttribute[] atts = getAttributes();
			for (ISchemaAttribute attribute : atts) {
				attribute.setParent(this);
			}
		}
	}

	public void setIconProperty(String newIconName) {
		String oldValue = iconName;
		iconName = newIconName;
		getSchema().fireModelObjectChanged(this, P_ICON_NAME, oldValue, iconName);
	}

	public void setTranslatableProperty(boolean translatable) {
		boolean oldValue = fTranslatable;
		fTranslatable = translatable;
		getSchema().fireModelObjectChanged(this, P_TRANSLATABLE, Boolean.valueOf(oldValue), Boolean.valueOf(translatable));
	}

	public void setDeprecatedProperty(boolean deprecated) {
		boolean oldValue = fDeprecated;
		fDeprecated = deprecated;
		getSchema().fireModelObjectChanged(this, P_DEPRECATED, Boolean.valueOf(oldValue), Boolean.valueOf(deprecated));
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

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<element name=\"" + getName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		ISchemaType type = getType();
		if (type instanceof SchemaSimpleType) {
			writer.print(" type=\"" + type.getName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}

		writer.println(">"); //$NON-NLS-1$
		String indent2 = indent + Schema.INDENT;
		String realDescription = getWritableDescription();
		if (realDescription.length() == 0) {
			realDescription = null;
		}

		String extendedProperties = getExtendedAttributes();

		if (realDescription != null || iconName != null || labelProperty != null || extendedProperties != null || isDeprecated() || hasTranslatableContent()) {
			String indent3 = indent2 + Schema.INDENT;
			String indent4 = indent3 + Schema.INDENT;
			writer.println(indent2 + "<annotation>"); //$NON-NLS-1$
			if (iconName != null || labelProperty != null || extendedProperties != null || isDeprecated() || hasTranslatableContent()) {
				writer.println(indent3 + (getSchema().getSchemaVersion() >= 3.4 ? "<appinfo>" : "<appInfo>")); //$NON-NLS-1$ //$NON-NLS-2$
				writer.print(indent4 + "<meta.element"); //$NON-NLS-1$
				if (labelProperty != null) {
					writer.print(" labelAttribute=\"" + labelProperty + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (iconName != null) {
					writer.print(" icon=\"" + iconName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (hasTranslatableContent()) {
					writer.print(" translatable=\"true\""); //$NON-NLS-1$
				}
				if (isDeprecated()) {
					writer.print(" deprecated=\"true\""); //$NON-NLS-1$
				}
				if (extendedProperties != null) {
					writer.print(extendedProperties);
				}
				writer.println("/>"); //$NON-NLS-1$
				writer.println(indent3 + (getSchema().getSchemaVersion() >= 3.4 ? "</appinfo>" : "</appInfo>")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (realDescription != null) {
				writer.println(indent3 + "<documentation>"); //$NON-NLS-1$
				if (getDescription() != null) {
					writer.println(indent4 + realDescription);
				}
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

	@Override
	public boolean hasTranslatableContent() {
		return fTranslatable;
	}

	@Override
	public boolean isDeprecated() {
		return fDeprecated;
	}

	@Override
	public boolean hasDeprecatedAttributes() {
		for (ISchemaAttribute att : getAttributes()) {
			if (att.isDeprecated()) {
				return true;
			}
		}
		return false;
	}

	public String getExtendedAttributes() {
		return null;
	}

	@Override
	public String getDescription() {
		if (super.getDescription() != null) {
			return super.getDescription();
		}
		ISchema schema = getSchema();
		if ((schema == null) || (schema.getURL() == null)) {
			// This can happen when creating a new extension point schema
			return null;
		}
		String hashkey = schema.getURL().hashCode() + "_" + getName(); //$NON-NLS-1$
		String description = XMLComponentRegistry.Instance().getDescription(hashkey, XMLComponentRegistry.F_ELEMENT_COMPONENT);
		if (description == null) {
			SchemaElementHandler handler = new SchemaElementHandler(getName());
			SchemaUtil.parseURL(schema.getURL(), handler);
			description = handler.getDescription();
			XMLComponentRegistry.Instance().putDescription(hashkey, description, XMLComponentRegistry.F_ELEMENT_COMPONENT);
		}

		return description;
	}

	@Override
	public int compareTo(Object arg0) {
		if (arg0 instanceof ISchemaElement) {
			return getName().compareToIgnoreCase(((ISchemaElement) arg0).getName());
		}
		return -1;
	}
}
