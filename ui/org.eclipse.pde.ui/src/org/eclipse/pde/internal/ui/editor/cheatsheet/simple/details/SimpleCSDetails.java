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

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSRegisterCSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSDetails
 *
 */
public class SimpleCSDetails extends CSAbstractDetails {

	private ISimpleCS fCheatSheet;
	
	private FormEntry fTitle;
	
	private Section fMainSection;

	private ICSDetails fRegisterCSArea;
	
	/**
	 * 
	 */
	public SimpleCSDetails(ISimpleCS cheatsheet, ICSMaster section) {
		super(section, SimpleCSInputContext.CONTEXT_ID);
		fCheatSheet = cheatsheet;
		
		fTitle = null;
		fMainSection = null;
		fRegisterCSArea = new CSRegisterCSDetails(fCheatSheet.getModel(), 
				section, SimpleCSInputContext.CONTEXT_ID);
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
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		FormToolkit toolkit = getManagedForm().getToolkit();
		GridData data = null;
		
		// Create main section
		fMainSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(PDEUIMessages.SimpleCSDetails_3);
		fMainSection.setDescription(PDEUIMessages.SimpleCSDetails_2);
		fMainSection.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);
		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), 
				fMainSection);	
		
		// Create container for main section
		Composite mainSectionClient = toolkit.createComposite(fMainSection);
		mainSectionClient.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));		

		// Attribute: title
		fTitle = new FormEntry(mainSectionClient, toolkit, PDEUIMessages.SimpleCSDetails_0, SWT.NONE);

		// Bind widgets
		toolkit.paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);		
		
		// Create the register cheat sheet area
		fRegisterCSArea.createDetails(parent);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// Attribute: title
		fTitle.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fCheatSheet.setTitle(fTitle.getValue());
			}
		});		
		// Create the listeners within the register cheat sheet area
		fRegisterCSArea.hookListeners();		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = isEditableElement();
		// Attribute: title
		fTitle.setValue(fCheatSheet.getTitle(), true);
		fTitle.setEditable(editable);
		// Update the fields within the register cheat sheet area
		fRegisterCSArea.updateFields();			
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fTitle.commit();
		// No need to call for sub details, because they contain no form entries
	}
	
}
