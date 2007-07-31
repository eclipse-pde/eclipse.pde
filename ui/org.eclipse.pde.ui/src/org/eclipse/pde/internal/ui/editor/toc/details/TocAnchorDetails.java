/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.toc.details;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.core.text.toc.TocAnchor;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.toc.TocInputContext;
import org.eclipse.pde.internal.ui.editor.toc.TocTreeSection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;

public class TocAnchorDetails extends TocAbstractDetails {

	private TocAnchor fDataTOCAnchor;
	
	private FormEntry fAnchorIdEntry;
	
	/**
	 * @param masterSection
	 */
	public TocAnchorDetails(TocTreeSection masterSection) {
		super(masterSection, TocInputContext.CONTEXT_ID);
		fDataTOCAnchor = null;

		fAnchorIdEntry = null;
	}
	
	/**
	 * @param object
	 */
	public void setData(TocAnchor object) {
		// Set data
		fDataTOCAnchor = object;
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createFields(Composite parent) {
		createAnchorIdWidget(parent);
	}
	
	/**
	 * @param parent
	 */
	private void createAnchorIdWidget(Composite parent) {
		createLabel(parent, getManagedForm().getToolkit(), PDEUIMessages.TocAnchorDetails_id_desc);

		fAnchorIdEntry = new FormEntry(parent, getManagedForm().getToolkit(), 
				PDEUIMessages.TocAnchorDetails_id, SWT.NONE);
	}

	protected String getDetailsTitle()
	{	return PDEUIMessages.TocAnchorDetails_title;
	}
	
	protected String getDetailsDescription()
	{	return PDEUIMessages.TocAnchorDetails_sectionDescription;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		createAnchorIdEntryListeners();
	}

	/**
	 * 
	 */
	private void createAnchorIdEntryListeners() {
		fAnchorIdEntry.setFormEntryListener(new FormEntryAdapter(this) {
				public void textValueChanged(FormEntry entry) {
					// Ensure data object is defined
					if (fDataTOCAnchor != null) {
					{	fDataTOCAnchor.setFieldAnchorId(fAnchorIdEntry.getValue());
					}
				}
			}
		});			
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#updateFields()
	 */
	public void updateFields() {
		// Ensure data object is defined
		if (fDataTOCAnchor != null)
		{	// Update name entry
			updateAnchorIdEntry(isEditableElement());
		}
	}

	/**
	 * @param editable
	 */
	private void updateAnchorIdEntry(boolean editable) {
		fAnchorIdEntry.setValue(fDataTOCAnchor.getFieldAnchorId(), true);
		fAnchorIdEntry.setEditable(editable);			
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fAnchorIdEntry.commit();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IPartSelectionListener#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if (object != null && object instanceof TocAnchor) {
			// Set data
			setData((TocAnchor)object);
			// Update the UI given the new data
			updateFields();
		}
	}
}
