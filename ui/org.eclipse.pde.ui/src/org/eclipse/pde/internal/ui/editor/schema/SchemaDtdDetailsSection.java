/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.widgets.*;

public class SchemaDtdDetailsSection extends AbstractFormPart {
	private Text fDtdText;
	private Section fSection;

	public void createContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();

		fSection = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		fSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fSection.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		fSection.setLayoutData(gd);

		Composite client = toolkit.createComposite(fSection);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));

		fDtdText = toolkit.createText(client, "", SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);//$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 60;
		fDtdText.setLayoutData(gd);
		fDtdText.setEditable(false);
		// remove pop-up menu
		fDtdText.setMenu(new Menu(client));

		toolkit.paintBordersFor(client);
		fSection.setClient(client);

		fSection.setText(PDEUIMessages.SchemaDtdDetailsSection_title);
	}

	protected void updateDTDLabel(Object changeObject) {
		if ((fDtdText == null) || (fDtdText.isDisposed())) {
			return;
		}
		if (changeObject instanceof ISchemaAttribute) {
			changeObject = ((ISchemaAttribute) changeObject).getParent();
		} else if (changeObject instanceof ISchemaCompositor) {
			while (changeObject != null) {
				if (changeObject instanceof ISchemaElement)
					break;
				changeObject = ((ISchemaCompositor) changeObject).getParent();
			}
		}
		if (changeObject instanceof ISchemaElement)
			fDtdText.setText(((ISchemaElement) changeObject).getDTDRepresentation(false));
	}
}
