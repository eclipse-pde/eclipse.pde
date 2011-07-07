/*******************************************************************************
 *  Copyright (c) 2003, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.Section;

public abstract class PDEMasterDetailsBlock extends MasterDetailsBlock {
	private PDEFormPage fPage;
	private PDESection fSection;

	public PDEMasterDetailsBlock(PDEFormPage page) {
		fPage = page;
	}

	public PDEFormPage getPage() {
		return fPage;
	}

	protected void createMasterPart(final IManagedForm managedForm, Composite parent) {
		Composite container = managedForm.getToolkit().createComposite(parent);
		container.setLayout(FormLayoutFactory.createMasterGridLayout(false, 1));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		fSection = createMasterSection(managedForm, container);
		managedForm.addPart(fSection);
		Section section = fSection.getSection();
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	protected void createToolBarActions(IManagedForm managedForm) {
	}

	protected abstract PDESection createMasterSection(IManagedForm managedForm, Composite parent);

	public void createContent(IManagedForm managedForm) {
		super.createContent(managedForm);
		managedForm.getForm().getBody().setLayout(FormLayoutFactory.createFormGridLayout(false, 1));
	}

	public DetailsPart getDetailsPart() {
		return detailsPart;
	}

}
