/*******************************************************************************
 *  Copyright (c) 2003, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.plugin.rows;

import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class BooleanAttributeRow extends ChoiceAttributeRow {
	/**
	 * @param att
	 */
	public BooleanAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}

	@Override
	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		super.createContents(parent, toolkit, span);
		if (getUse() != ISchemaAttribute.REQUIRED)
			combo.add(""); //$NON-NLS-1$
		combo.add("true"); //$NON-NLS-1$
		combo.add("false"); //$NON-NLS-1$
	}

	@Override
	protected boolean isValid(String value) {
		if (getUse() == ISchemaAttribute.REQUIRED)
			return (value.equals("true") || value.equals("false")); //$NON-NLS-1$ //$NON-NLS-2$
		return (value.equals("true") || value.equals("false") || value.equals("")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	protected String getValidValue() {
		return "true"; //$NON-NLS-1$
	}
}
