/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin.rows;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 */
public class BooleanAttributeRow extends ChoiceAttributeRow {
	/**
	 * @param att
	 */
	public BooleanAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ExtensionElementEditor#createContents(org.eclipse.swt.widgets.Composite,
	 *      org.eclipse.ui.forms.widgets.FormToolkit, int)
	 */
	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		super.createContents(parent, toolkit, span);
		if (getUse()!=ISchemaAttribute.REQUIRED)
			combo.add(""); //$NON-NLS-1$
		combo.add("true"); //$NON-NLS-1$
		combo.add("false"); //$NON-NLS-1$
	}
}