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

import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.resource.*;

public class NewReferenceAction extends Action {
	private ISchemaElement source;
	private Object object;
	private ISchemaElement referencedElement;

public NewReferenceAction(
	ISchemaElement source,
	Object object,
	ISchemaElement referencedElement) {
	this.source = source;
	this.object = object;
	this.referencedElement = referencedElement;
	setText(referencedElement.getName());
	ImageDescriptor desc = PDEPluginImages.DESC_ELREF_SC_OBJ;
	setImageDescriptor(desc);
	setEnabled(source.getSchema().isEditable());
}
public void run() {
	if (object != null && object instanceof SchemaCompositor) {
		SchemaCompositor parent = (SchemaCompositor) object;
		SchemaElementReference reference =
			new SchemaElementReference(parent, referencedElement.getName());
		reference.setReferencedObject(referencedElement);
		parent.addChild(reference);
	}
}
}
