package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class ReferencePropertySource extends GrammarPropertySource {

	public ReferencePropertySource(SchemaElementReference obj) {
		super(obj);
	}

	protected String isMinOccursValid(int ivalue) {
		String status = super.isMinOccursValid(ivalue);
		if (status==null && isInAll()) {
			if (ivalue!=0 && ivalue!=1) {
				return PDEPlugin.getResourceString("ReferencePropertySource.minOccurs.value"); //$NON-NLS-1$
			}
		}
		return status;
	}

	protected String isMaxOccursValid(int ivalue) {
		String status = super.isMaxOccursValid(ivalue);
		if (status==null && isInAll()) {
			if (ivalue!=1) {
				return PDEPlugin.getResourceString("ReferencePropertySource.maxOccurs.value"); //$NON-NLS-1$
			}
		}
		return status;
	}
	
	private boolean isInAll() {
		ISchemaCompositor compositor = getReference().getCompositor();
		return (compositor!=null && compositor.getKind()==ISchemaCompositor.ALL);
	}
	
	protected SchemaElementReference getReference() {
		return (SchemaElementReference)getSourceObject();
	}
}
