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

import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTask;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * CompCSTaskDetails
 *
 */
public class CompCSTaskDetails extends CSAbstractDetails {

	private Section fDefinitionSection;
	
	private FormEntry fNameEntry;

	private FormEntry fPathEntry;
	
	private Button fSkip;	
	
	private ICompCSTask fDataTask;
	
	private ICSDetails fEnclosingTextSection;		
	
	
	/**
	 * @param masterSection
	 * @param contextID
	 */
	public CompCSTaskDetails(ICompCSTask task, ICSMaster section) {
		super(section, SimpleCSInputContext.CONTEXT_ID);

		fDataTask = task;
		fNameEntry = null;
		fPathEntry = null;
		fSkip = null;

		fDefinitionSection = null;
		fEnclosingTextSection = new CompCSEnclosingTextDetails(fDataTask, this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		// Create the main section
		int style = Section.DESCRIPTION | ExpandableComposite.TITLE_BAR;
		fDefinitionSection = createUISection(parent, PDEUIMessages.SimpleCSDetails_3, 
			PDEUIMessages.CompCSTaskDetails_SectionDescription, style);
		// Create the container for the main section
		Composite sectionClient = createUISectionContainer(fDefinitionSection, 3);		
		// Create the name entry
		createUINameEntry(sectionClient);
		// Create the kind combo
		createUIPathEntry(sectionClient);
		// Create the skip button
		createUISkipButton(sectionClient);
		// Create the enclosing text section
		fEnclosingTextSection.createDetails(parent);
		// Bind widgets
		getManagedForm().getToolkit().paintBordersFor(sectionClient);
		fDefinitionSection.setClient(sectionClient);
		markDetailsPart(fDefinitionSection);			
	}

	/**
	 * @param parent
	 */
	private void createUINameEntry(Composite parent) {
		fNameEntry = new FormEntry(parent, getManagedForm().getToolkit(), 
				PDEUIMessages.CompCSTaskDetails_Name, SWT.NONE);		
	}	

	/**
	 * @param parent
	 */
	private void createUIPathEntry(Composite parent) {
		fPathEntry = new FormEntry(parent, getManagedForm().getToolkit(), 
				PDEUIMessages.CompCSTaskDetails_Path, PDEUIMessages.GeneralInfoSection_browse, isEditable());	
	}	
	
	/**
	 * @param parent
	 */
	private void createUISkipButton(Composite parent) {
		Color foreground = getToolkit().getColors().getColor(FormColors.TITLE);		
		fSkip = getToolkit().createButton(parent, PDEUIMessages.CompCSTaskDetails_SkipLabel, SWT.CHECK);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		fSkip.setLayoutData(data);
		fSkip.setForeground(foreground);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// TODO: MP: MED: Current: Auto-generated method stub

		fEnclosingTextSection.hookListeners();
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#updateFields()
	 */
	public void updateFields() {
		// TODO: MP: MED: Current: Auto-generated method stub
		if (fPathEntry == null) {}
		if (fNameEntry == null) {}
		
		fEnclosingTextSection.updateFields();

	}

}
