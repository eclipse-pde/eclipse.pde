/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ReferencePropertySource extends GrammarPropertySource {

	public ReferencePropertySource(SchemaElementReference obj) {
		super(obj);
	}

	protected String isMinOccursValid(int ivalue) {
		String status = super.isMinOccursValid(ivalue);
		if (status==null && isInAll()) {
			if (ivalue!=0 && ivalue!=1) {
				return PDEUIMessages.ReferencePropertySource_minOccurs_value; 
			}
		}
		return status;
	}

	protected String isMaxOccursValid(int ivalue) {
		String status = super.isMaxOccursValid(ivalue);
		if (status==null && isInAll()) {
			if (ivalue!=1) {
				return PDEUIMessages.ReferencePropertySource_maxOccurs_value; 
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
