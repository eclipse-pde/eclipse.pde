/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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

public class SchemaRootElement extends SchemaElement implements ISchemaRootElement {

	private static final long serialVersionUID = 1L;

	private String fDeperecatedReplacement;
	private boolean fInternal;

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
		StringBuffer buffer = new StringBuffer();
		buffer.append(" "); //$NON-NLS-1$
		if (fDeperecatedReplacement != null)
			buffer.append(P_DEP_REPLACEMENT + "=\"" + fDeperecatedReplacement + "\" "); //$NON-NLS-1$ //$NON-NLS-2$

		if (fInternal == true)
			buffer.append(P_INTERNAL + "=\"" + "true" + "\" "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		return buffer.toString();
	}

	public boolean isInternal() {
		return fInternal;
	}

	public void setInternal(boolean value) {
		boolean oldValue = fInternal;
		fInternal = value;
		getSchema().fireModelObjectChanged(this, P_INTERNAL, Boolean.valueOf(oldValue), Boolean.valueOf(fInternal));
	}
}
