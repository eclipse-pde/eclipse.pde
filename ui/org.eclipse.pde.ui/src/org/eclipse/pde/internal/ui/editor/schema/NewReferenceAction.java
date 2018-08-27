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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaCompositor;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.ui.PDEPluginImages;

public class NewReferenceAction extends Action {
	private Object object;

	private ISchemaElement referencedElement;

	public NewReferenceAction(ISchemaElement source, Object object, ISchemaElement referencedElement) {
		this.object = object;
		this.referencedElement = referencedElement;
		setText(referencedElement.getName());
		ImageDescriptor desc = PDEPluginImages.DESC_ELREF_SC_OBJ;
		setImageDescriptor(desc);
		setEnabled(source.getSchema().isEditable());
	}

	@Override
	public void run() {
		if (object != null && object instanceof SchemaCompositor) {
			SchemaCompositor parent = (SchemaCompositor) object;
			SchemaElementReference reference = new SchemaElementReference(parent, referencedElement.getName());
			reference.setReferencedObject(referencedElement);
			parent.addChild(reference);
		}
	}
}
