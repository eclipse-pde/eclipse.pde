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
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSRegisterCSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
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
		fRegisterCSArea = new CSRegisterCSDetails(this, fCheatSheet.getModel());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		FormToolkit toolkit = getManagedForm().getToolkit();
		//Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		GridData data = null;
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
		
		// Create main section
		fMainSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		fMainSection.marginHeight = 5;
		fMainSection.marginWidth = 5; 
		fMainSection.setText(PDEUIMessages.SimpleCSDetails_3);
		fMainSection.setDescription(PDEUIMessages.SimpleCSDetails_2);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);
		
		// Create container for main section
		Composite mainSectionClient = toolkit.createComposite(fMainSection);	
		GridLayout layout = new GridLayout(2, false);
		if (paintedBorder) {
			layout.verticalSpacing = 7;
		}
		mainSectionClient.setLayout(layout);		

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

}
