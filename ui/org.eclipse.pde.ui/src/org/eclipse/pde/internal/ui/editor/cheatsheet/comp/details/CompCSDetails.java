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

package org.eclipse.pde.internal.ui.editor.cheatsheet.comp.details;

import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCS;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSRegisterCSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ui.editor.cheatsheet.comp.CompCSInputContext;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * CompCSDetails
 *
 */
public class CompCSDetails extends CSAbstractDetails {

	private ICompCS fDataCheatSheet;
	
	private Section fMainSection;
	
	private FormEntry fNameEntry;
	
	private ICSDetails fRegisterCSArea;
	
	/**
	 * @param masterSection
	 * @param contextID
	 */
	public CompCSDetails(ICompCS dataCheatSheet, ICSMaster masterSection) {
		super(masterSection, CompCSInputContext.CONTEXT_ID);
		
		fDataCheatSheet = dataCheatSheet;
		fNameEntry = null;
		fMainSection = null;		
		
		fRegisterCSArea = 
			new CSRegisterCSDetails(fDataCheatSheet.getModel(), masterSection,
					CompCSInputContext.CONTEXT_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	public void initialize(IManagedForm form) {
		super.initialize(form);
		// Unfortunately this has to be explicitly called for sub detail
		// sections through its main section parent; since, it never is 
		// registered directly.
		// Initialize managed form for register area
		if (fRegisterCSArea instanceof IFormPart) {
			((IFormPart)fRegisterCSArea).initialize(form);
		}
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {
		// Create the main section
		int style = Section.DESCRIPTION | ExpandableComposite.TITLE_BAR;
		fMainSection = getPage().createUISection(parent, PDEUIMessages.SimpleCSDetails_3, 
			PDEUIMessages.CompCSDetails_sectionDescription, style);
		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), 
				fMainSection);
		// Create the container for the main section
		Composite sectionClient = getPage().createUISectionContainer(fMainSection, 2);		
		// Create the name widget
		createUINameEntry(sectionClient);
		// Create the register cheat sheet area
		fRegisterCSArea.createDetails(parent);
		// Bind widgets
		getManagedForm().getToolkit().paintBordersFor(sectionClient);
		fMainSection.setClient(sectionClient);
		markDetailsPart(fMainSection);				
	}
	
	/**
	 * @param parent
	 */
	private void createUINameEntry(Composite parent) {
		fNameEntry = new FormEntry(parent, getManagedForm().getToolkit(), 
				PDEUIMessages.CompCSDetails_Name, SWT.NONE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// Create the listeners for the name entry
		createListenersNameEntry();
		// Create the listeners within the register cheat sheet area
		fRegisterCSArea.hookListeners();
	}

	/**
	 * 
	 */
	private void createListenersNameEntry() {
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fDataCheatSheet.setFieldName(fNameEntry.getValue());
			}
		});			
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#updateFields()
	 */
	public void updateFields() {
		// Update name entry
		updateNameEntry(isEditableElement());
		// Update the fields within the register cheat sheet area
		fRegisterCSArea.updateFields();
	}

	/**
	 * @param editable
	 */
	private void updateNameEntry(boolean editable) {
		fNameEntry.setValue(fDataCheatSheet.getFieldName(), true);
		fNameEntry.setEditable(editable);			
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fNameEntry.commit();
		// No need to call for sub details, because they contain no form entries
	}

}
