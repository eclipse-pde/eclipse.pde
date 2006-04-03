/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaRootElement;

public class SchemaRootElement extends SchemaElement implements
		ISchemaRootElement {

	private static final long serialVersionUID = 1L;
	public static final String P_DEP_REPLACEMENT = "replacement"; //$NON-NLS-1$
	private String fDeperecatedReplacement;
	
	public SchemaRootElement(ISchemaObject parent, String name) {
		super(parent, name);
	}

	public void setDeprecatedSuggestion(String value) {
		Object oldValue = fDeperecatedReplacement;
		fDeperecatedReplacement = value;
		getSchema().fireModelObjectChanged(this, P_DEP_REPLACEMENT, oldValue, fDeperecatedReplacement);
	}

	public String getDeprecatedSuggestion() {
		return fDeperecatedReplacement;
	}

	public String getExtendedAttributes() {
		if (fDeperecatedReplacement == null)
			return null;
		return " " + P_DEP_REPLACEMENT + "=\"" + fDeperecatedReplacement + "\" "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
