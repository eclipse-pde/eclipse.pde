package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.resource.*;

public class NewReferenceAction extends Action {
	private ISchemaElement source;
	private Object object;
	private int kind;
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
