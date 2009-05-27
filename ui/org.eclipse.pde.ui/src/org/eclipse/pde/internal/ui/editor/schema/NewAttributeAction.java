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
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaType;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;

public class NewAttributeAction extends Action {
	private SchemaElement element;

	public NewAttributeAction() {
		setText(PDEUIMessages.SchemaEditor_NewAttribute_label);
		setImageDescriptor(PDEPluginImages.DESC_ATTRIBUTE_OBJ);
		setToolTipText(PDEUIMessages.SchemaEditor_NewAttribute_tooltip);
	}

	public org.eclipse.pde.internal.core.schema.SchemaElement getElement() {
		return element;
	}

	private String getInitialName() {
		return PDELabelUtility.generateName(element.getAttributeNames(), PDEUIMessages.SchemaEditor_NewAttribute_initialName, false);
	}

	public void run() {
		String name = getInitialName();
		SchemaAttribute att = new SchemaAttribute(element, name);
		att.setType(new SchemaSimpleType(element.getSchema(), "string")); //$NON-NLS-1$
		ISchemaType type = element.getType();
		SchemaComplexType complexType = null;
		if (!(type instanceof ISchemaComplexType)) {
			complexType = new SchemaComplexType(element.getSchema());
			element.setType(complexType);
		} else {
			complexType = (SchemaComplexType) type;
		}
		complexType.addAttribute(att);
		// Any element that defines attributes cannot be translatable
		if (element.hasTranslatableContent()) {
			element.setTranslatableProperty(false);
		}
	}

	public void setElement(org.eclipse.pde.internal.core.schema.SchemaElement newElement) {
		element = newElement;
	}
}
