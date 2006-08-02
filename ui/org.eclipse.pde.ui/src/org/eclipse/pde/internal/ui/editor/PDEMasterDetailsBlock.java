/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
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
	
	protected void createMasterPart(final IManagedForm managedForm,
			Composite parent) {
		fSection = createMasterSection(managedForm, parent);
		managedForm.addPart(fSection);
		Section sc = fSection.getSection();
		sc.marginWidth = 10;
		sc.marginHeight = 5;
	}
	
	protected void createToolBarActions(IManagedForm managedForm) {}
	
	protected abstract PDESection createMasterSection(IManagedForm managedForm, Composite parent);
	
}
