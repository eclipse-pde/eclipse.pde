/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class CSAbstractDetails extends PDEDetails implements ICSDetails {

	private ICSMaster fMasterSection;

	private String fContextID;

	public CSAbstractDetails(ICSMaster masterSection, String contextID) {
		fMasterSection = masterSection;
		fContextID = contextID;
	}

	@Override
	public void createContents(Composite parent) {
		configureParentLayout(parent);
		createDetails(parent);
		hookListeners();
	}

	private void configureParentLayout(Composite parent) {
		parent.setLayout(FormLayoutFactory.createDetailsGridLayout(false, 1));
	}

	@Override
	public abstract void createDetails(Composite parent);

	@Override
	public abstract void updateFields();

	@Override
	public abstract void hookListeners();

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		// NO-OP
		// Children to override
	}

	@Override
	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}

	@Override
	public String getContextId() {
		return fContextID;
	}

	@Override
	public PDEFormPage getPage() {
		return (PDEFormPage) getManagedForm().getContainer();
	}

	@Override
	public boolean isEditable() {
		return fMasterSection.isEditable();
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		// NO-OP
	}

	public boolean isEditableElement() {
		return fMasterSection.isEditable();
	}

	public FormToolkit getToolkit() {
		return getManagedForm().getToolkit();
	}

	public ICSMaster getMasterSection() {
		return fMasterSection;
	}

	protected Object getFirstSelectedObject(ISelection selection) {
		// Get the structured selection (obtained from the master tree viewer)
		IStructuredSelection structuredSel = ((IStructuredSelection) selection);
		// Ensure we have a selection
		if (structuredSel == null) {
			return null;
		}
		return structuredSel.getFirstElement();
	}

}
