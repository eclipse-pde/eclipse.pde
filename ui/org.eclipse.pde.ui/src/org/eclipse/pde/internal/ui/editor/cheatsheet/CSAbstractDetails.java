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
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetailsSurrogate#createUISectionContainer(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createUISectionContainer(Composite parent, int columns) {
		Composite container = getManagedForm().getToolkit().createComposite(parent);
		GridLayout layout = new GridLayout(columns, false);
		container.setLayout(layout);
		return container;		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetailsSurrogate#createUISection(org.eclipse.swt.widgets.Composite, java.lang.String, java.lang.String, int)
	 */
	public Section createUISection(Composite parent, String text,
			String description, int style) {
		Section section = getManagedForm().getToolkit().createSection(parent, style);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		section.marginHeight = 5;
		section.marginWidth = 5; 
		section.setText(text);
		section.setDescription(description);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		section.setLayoutData(data);
		return section;
	}
	
}
