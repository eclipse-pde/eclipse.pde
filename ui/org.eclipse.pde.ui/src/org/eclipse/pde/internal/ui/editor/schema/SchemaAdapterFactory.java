/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.ui.views.properties.IPropertySource;

public class SchemaAdapterFactory implements IAdapterFactory {
	private ElementPropertySource elementPropertySource;
	private AttributePropertySource attributePropertySource;

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType.equals(IPropertySource.class))
			return getProperties(adaptableObject);
		return null;
	}
	public java.lang.Class[] getAdapterList() {
		return new Class[] { IPropertySource.class };
	}
	protected AttributePropertySource getAttributeProperties(ISchemaAttribute att) {
		if (attributePropertySource == null)
			attributePropertySource = new AttributePropertySource(att);
		else
			attributePropertySource.setSourceObject(att);
		return attributePropertySource;
	}
	protected ElementPropertySource getElementProperties(ISchemaElement element) {
		if (elementPropertySource == null)
			elementPropertySource = new ElementPropertySource(element);
		else
			elementPropertySource.setSourceObject(element);
		return elementPropertySource;
	}
	private IPropertySource getProperties(Object object) {
		if (object instanceof ISchemaElement
			&& !(object instanceof ISchemaObjectReference))
			return getElementProperties((ISchemaElement) object);
		if (object instanceof ISchemaAttribute)
			return getAttributeProperties((ISchemaAttribute) object);
		if (object instanceof ISchemaRepeatable)
			return getRepeatableProperties((ISchemaRepeatable) object);
		return null;
	}
	protected GrammarPropertySource getRepeatableProperties(ISchemaRepeatable obj) {
		if (obj instanceof ISchemaCompositor)
			return new CompositorPropertySource((ISchemaCompositor) obj);
		if (obj instanceof SchemaElementReference)
			return new ReferencePropertySource((SchemaElementReference)obj);
		return null;
	}
}
