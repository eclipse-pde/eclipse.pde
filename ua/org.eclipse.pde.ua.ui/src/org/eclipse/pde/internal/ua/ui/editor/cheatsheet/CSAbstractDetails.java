/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

/**
 * CSAbstractDetails
 */
public abstract class CSAbstractDetails extends PDEDetails implements ICSDetails {

	private ICSMaster fMasterSection;

	private String fContextID;

	/**
	 * 
	 */
	public CSAbstractDetails(ICSMaster masterSection, String contextID) {
		fMasterSection = masterSection;
		fContextID = contextID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		configureParentLayout(parent);
		createDetails(parent);
		hookListeners();
	}

	/**
	 * @param parent
	 */
	private void configureParentLayout(Composite parent) {
		parent.setLayout(FormLayoutFactory.createDetailsGridLayout(false, 1));
	}

	/**
	 * @param parent
	 */
	public abstract void createDetails(Composite parent);

	/**
	 * 
	 */
	public abstract void updateFields();

	/**
	 * 
	 */
	public abstract void hookListeners();

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IPartSelectionListener#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		// NO-OP
		// Children to override
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#fireSaveNeeded()
	 */
	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#getContextId()
	 */
	public String getContextId() {
		return fContextID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#getPage()
	 */
	public PDEFormPage getPage() {
		return (PDEFormPage) getManagedForm().getContainer();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#isEditable()
	 */
	public boolean isEditable() {
		return fMasterSection.isEditable();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		// NO-OP
	}

	/**
	 * @return
	 */
	public boolean isEditableElement() {
		return fMasterSection.isEditable();
	}

	/**
	 * @return
	 */
	public FormToolkit getToolkit() {
		return getManagedForm().getToolkit();
	}

	/**
	 * @return
	 */
	public ICSMaster getMasterSection() {
		return fMasterSection;
	}

	/**
	 * @param selection
	 * @return
	 */
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
