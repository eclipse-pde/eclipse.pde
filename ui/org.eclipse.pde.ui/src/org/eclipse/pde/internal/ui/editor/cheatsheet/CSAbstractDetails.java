/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * CSAbstractDetails
 *
 */
public abstract class CSAbstractDetails extends PDEDetails implements
		ICSDetailsSurrogate, ICSDetails {

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
	public final void createContents(Composite parent) {
		configureParentLayout(parent);
		createDetails(parent);
		updateFields();
		hookListeners();
	}

	/**
	 * @param parent
	 */
	private void configureParentLayout(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		parent.setLayout(layout);		
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
		return (PDEFormPage)getManagedForm().getContainer();
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
	
}
