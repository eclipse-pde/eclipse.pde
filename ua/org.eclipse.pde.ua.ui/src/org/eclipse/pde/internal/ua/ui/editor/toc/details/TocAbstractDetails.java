/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.toc.details;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ua.core.toc.text.TocObject;
import org.eclipse.pde.internal.ua.ui.editor.toc.TocTreeSection;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TocAbstractDetails
 */
public abstract class TocAbstractDetails extends PDEDetails {

	private static final int NUM_COLUMNS = 3;

	private TocTreeSection fMasterSection;

	private Section fMainSection;

	private String fContextID;

	/**
	 * 
	 */
	public TocAbstractDetails(TocTreeSection masterSection, String contextID) {
		fMasterSection = masterSection;
		fContextID = contextID;
		fMainSection = null;
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
	 * 
	 */
	protected abstract void createFields(Composite parent);

	/**
	 * 
	 */
	protected abstract String getDetailsTitle();

	/**
	 * 
	 */
	protected abstract String getDetailsDescription();

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

	protected void setPathEntry(IFile file) {
		IPath path = file.getFullPath();
		if (file.getProject().equals(getDataObject().getModel().getUnderlyingResource().getProject())) {
			getPathEntryField().setValue(path.removeFirstSegments(1).toString()); //$NON-NLS-1$
		} else {
			getPathEntryField().setValue(".." + path.toString()); //$NON-NLS-1$
		}
	}

	protected void handleOpen() {
		IFile file = getMasterSection().openFile(getPathEntryField().getValue(), isTocPath());
		if (file != null) {
			setPathEntry(file);
		}
	}

	protected boolean isTocPath() {
		return false;
	}

	protected abstract TocObject getDataObject();

	protected abstract FormEntry getPathEntryField();

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
	public TocTreeSection getMasterSection() {
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

	protected void createLabel(Composite client, FormToolkit toolkit, String text) {
		Label label = toolkit.createLabel(client, text, SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = NUM_COLUMNS;
		label.setLayoutData(gd);
	}

	protected void createSpace(Composite parent) {
		createLabel(parent, getManagedForm().getToolkit(), ""); //$NON-NLS-1$
	}

}
