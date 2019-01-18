/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public void setDeprecatedSuggestion(String value) {
		Object oldValue = fDeperecatedReplacement;
		fDeperecatedReplacement = value;
		getSchema().fireModelObjectChanged(this, P_DEP_REPLACEMENT, oldValue, fDeperecatedReplacement);
	}

	@Override
	public String getDeprecatedSuggestion() {
		return fDeperecatedReplacement;
	}

	@Override
	public String getExtendedAttributes() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(" "); //$NON-NLS-1$
		if (fDeperecatedReplacement != null) {
			buffer.append(P_DEP_REPLACEMENT + "=\"" + fDeperecatedReplacement + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (fInternal == true) {
			buffer.append(P_INTERNAL + "=\"" + "true" + "\" "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		return buffer.toString();
	}

	@Override
	public boolean isInternal() {
		return fInternal;
	}

	@Override
	public void setInternal(boolean value) {
		boolean oldValue = fInternal;
		fInternal = value;
		getSchema().fireModelObjectChanged(this, P_INTERNAL, Boolean.valueOf(oldValue), Boolean.valueOf(fInternal));
	}
}
