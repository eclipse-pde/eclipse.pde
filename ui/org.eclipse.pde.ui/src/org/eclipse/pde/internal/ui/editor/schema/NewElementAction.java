/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;

public class NewElementAction extends Action {
	private Schema schema;

	public NewElementAction() {
		setText(PDEUIMessages.SchemaEditor_NewElement_label);
		setImageDescriptor(PDEPluginImages.DESC_GEL_SC_OBJ);
		setToolTipText(PDEUIMessages.SchemaEditor_NewElement_tooltip);
	}

	private String getInitialName() {
		return PDELabelUtility.generateName(schema.getElementNames(), PDEUIMessages.SchemaEditor_NewElement_initialName, false);
	}

	public org.eclipse.pde.internal.core.schema.Schema getSchema() {
		return schema;
	}

	@Override
	public void run() {
		String name = getInitialName();
		SchemaElement element;
		if (name.equals("extension")) //$NON-NLS-1$
			element = new SchemaRootElement(schema, name);
		else
			element = new SchemaElement(schema, name);
		element.setType(new SchemaSimpleType(schema, "string")); //$NON-NLS-1$
		schema.addElement(element);
		schema.updateReferencesFor(element, ISchema.REFRESH_ADD);
	}

	public void setSchema(Schema newSchema) {
		schema = newSchema;
	}
}
