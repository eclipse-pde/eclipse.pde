/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.ctxhelp.details;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.CtxHelpTreeSection;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Abstract details class that acts as the base for the details section of the context
 * help editor.  The details section will display a form specific to the current tree
 * selection, allowing the user to edit the fields.
 * @since 3.4
 */
public abstract class CtxHelpAbstractDetails extends PDEDetails {

	/**
	 * Number of columns the detail section will have
	 */
	private static final int NUM_COLUMNS = 3;

	private CtxHelpTreeSection fMasterSection;
	private Section fMainSection;
	private String fContextID;

	public CtxHelpAbstractDetails(CtxHelpTreeSection masterSection, String contextID) {
		fMasterSection = masterSection;
		fContextID = contextID;
		fMainSection = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		parent.setLayout(FormLayoutFactory.createDetailsGridLayout(false, 1));
		createDetails(parent);
		hookListeners();
	}

	/**
	 * Create the UI elements for the detail section
	 * @param parent parent composite to create the details in
	 */
	public void createDetails(Composite parent) { // Create the main section
		int style = ExpandableComposite.TITLE_BAR;

		if (getDetailsDescription() != null)
			style |= Section.DESCRIPTION;

		fMainSection = getPage().createUISection(parent, getDetailsTitle(), getDetailsDescription(), style);
		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), fMainSection);
		// Create the container for the main section
		Composite sectionClient = getPage().createUISectionContainer(fMainSection, NUM_COLUMNS);
		GridData data = new GridData(GridData.FILL_BOTH);
		fMainSection.setLayoutData(data);
		createFields(sectionClient);

		// Bind widgets
		getManagedForm().getToolkit().paintBordersFor(sectionClient);
		fMainSection.setClient(sectionClient);
		markDetailsPart(fMainSection);
	}

	/**
	 * Subclasses must use this method to create the fields that allow user input.
	 * @param parent parent composite
	 */
	protected abstract void createFields(Composite parent);

	/**
	 * @return the title to display at the top of the details section
	 */
	protected abstract String getDetailsTitle();

	/**
	 * @return the description to display at the top of the details section
	 */
	protected abstract String getDetailsDescription();

	/**
	 * Subclasses should add there listeners by overriding this method.
	 */
	public abstract void hookListeners();

	/**
	 * Subclasses should update the form entries using their CtxHelpObject.  This method
	 * should be called from subclasses implementation of {@link #selectionChanged(IFormPart, ISelection)}
	 */
	public abstract void updateFields();

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
	 * @return whether the section is editable
	 */
	public boolean isEditableElement() {
		return fMasterSection.isEditable();
	}

	/**
	 * @return the toolkit to use to create the form elements
	 */
	public FormToolkit getToolkit() {
		return getManagedForm().getToolkit();
	}

	/**
	 * @return the tree section that controls this details section
	 */
	public CtxHelpTreeSection getMasterSection() {
		return fMasterSection;
	}

	/**
	 * Create a new label in the given composite using the appropriate style
	 * as defined by the toolkit.
	 * @param parent composite to create the label in
	 * @param toolkit toolkit that will define the style of the label
	 * @param text label text
	 */
	protected void createLabel(Composite parent, FormToolkit toolkit, String text) {
		Label label = toolkit.createLabel(parent, text, SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = NUM_COLUMNS;
		label.setLayoutData(gd);
	}

	/**
	 * Creates an empty label generating empty space in the parent composite.
	 * @param parent composite to create the space in
	 */
	protected void createSpace(Composite parent) {
		createLabel(parent, getManagedForm().getToolkit(), ""); //$NON-NLS-1$
	}

	/**
	 * Returns the first object from the selection or <code>null</code>.
	 * @param selection selection to get the object from
	 * @return first object in the selection or <code>null</code>
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
