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

import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * CompCSTaskGroupDetails
 *
 */
public class CompCSTaskGroupDetails extends CSAbstractDetails {

	private Section fDefinitionSection;
	
	private FormEntry fNameEntry;
	
	private ComboPart fKindCombo;
	
	private Button fSkip;	
	
	private ICompCSTaskGroup fDataTaskGroup;
	
	private ICSDetails fDependenciesSection;	
	
	private ICSDetails fEnclosingTextSection;

	private static final String F_KIND_VALUE_SET = PDEUIMessages.CompCSTaskGroupDetails_Set;
	
	private static final String F_KIND_VALUE_CHOICE = PDEUIMessages.CompCSTaskGroupDetails_Choice;

	private static final String F_KIND_VALUE_SEQUENCE = PDEUIMessages.CompCSTaskGroupDetails_Sequence;
	
	/**
	 * @param masterSection
	 * @param contextID
	 */
	public CompCSTaskGroupDetails(ICompCSTaskGroup taskGroup, ICSMaster section) {
		super(section, SimpleCSInputContext.CONTEXT_ID);
		
		fDataTaskGroup = taskGroup;
		fNameEntry = null;
		fKindCombo = null;
		fSkip = null;

		fDefinitionSection = null;
		fEnclosingTextSection = new CompCSEnclosingTextDetails(fDataTaskGroup,
				this);
		fDependenciesSection = new CompCSDependenciesDetails(fDataTaskGroup,
				this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {
		
		// Create the main section
		int style = Section.DESCRIPTION | ExpandableComposite.TITLE_BAR;
		fDefinitionSection = createUISection(parent, PDEUIMessages.SimpleCSDetails_3, 
			PDEUIMessages.CompCSTaskGroupDetails_SectionDescription, style);
		// Create the container for the main section
		Composite sectionClient = createUISectionContainer(fDefinitionSection, 2);		
		// Create the name entry
		createUINameEntry(sectionClient);
		// Create the kind label
		createUIKindLabel(sectionClient);		
		// Create the kind combo
		createUIKindCombo(sectionClient);
		// Create the skip button
		createUISkipButton(sectionClient);
		// Create the enclosing text section
		fEnclosingTextSection.createDetails(parent);
		// Create the dependencies section
		fDependenciesSection.createDetails(parent);
		// Bind widgets
		getManagedForm().getToolkit().paintBordersFor(sectionClient);
		fDefinitionSection.setClient(sectionClient);
		markDetailsPart(fDefinitionSection);			

	}

	/**
	 * @param parent
	 */
	private void createUISkipButton(Composite parent) {
		Color foreground = getToolkit().getColors().getColor(FormColors.TITLE);		
		fSkip = getToolkit().createButton(parent, PDEUIMessages.CompCSTaskGroupDetails_SkipLabel, SWT.CHECK);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fSkip.setLayoutData(data);
		fSkip.setForeground(foreground);
	}

	/**
	 * @param parent
	 */
	private void createUIKindLabel(Composite parent) {
		Color foreground = getToolkit().getColors().getColor(FormColors.TITLE);		
		Label label = getToolkit().createLabel(parent, 
				PDEUIMessages.CompCSTaskGroupDetails_Type, SWT.WRAP);
		label.setForeground(foreground);
		// TODO: MP: MED: CompCS: Update kind tooltip text
		label.setToolTipText(PDEUIMessages.CompCSTaskGroupDetails_KindToolTip);
	}
	
	/**
	 * @param parent
	 */
	private void createUIKindCombo(Composite parent) {
		fKindCombo = new ComboPart();
		fKindCombo.createControl(parent, getToolkit(), SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fKindCombo.getControl().setLayoutData(data);
		fKindCombo.add(F_KIND_VALUE_SET);
		fKindCombo.add(F_KIND_VALUE_SEQUENCE);
		fKindCombo.add(F_KIND_VALUE_CHOICE);
		fKindCombo.setText(F_KIND_VALUE_SET);	
		// TODO: MP: MED: CompCS: Update kind tooltip text
		fKindCombo.getControl().setToolTipText(PDEUIMessages.CompCSTaskGroupDetails_KindToolTip); 
	}

	/**
	 * @param parent
	 */
	private void createUINameEntry(Composite parent) {
		fNameEntry = new FormEntry(parent, getManagedForm().getToolkit(), 
				PDEUIMessages.CompCSTaskGroupDetails_Name, SWT.NONE);		
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
		if (fNameEntry == null) {}

		fEnclosingTextSection.updateFields();
		
	}

}
