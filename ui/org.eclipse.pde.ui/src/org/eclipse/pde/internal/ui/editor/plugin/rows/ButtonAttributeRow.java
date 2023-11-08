/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * ButtonAttributeRow
 *
 */
public abstract class ButtonAttributeRow extends ReferenceAttributeRow {

	/**
	 * @param part
	 * @param att
	 */
	public ButtonAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}

	@Override
	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		super.createContents(parent, toolkit, span);
		if (part.isEditable()) {
			Button button = toolkit.createButton(parent, PDEUIMessages.ReferenceAttributeRow_browse, SWT.PUSH);
			button.addSelectionListener(widgetSelectedAdapter(e -> {
				if (!isReferenceModel())
					browse();
			}));
			//button.setEnabled(part.isEditable());
		}
	}

	@Override
	protected GridData createGridData(int span) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 20;
		gd.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		return gd;
	}

	protected abstract void browse();

}
