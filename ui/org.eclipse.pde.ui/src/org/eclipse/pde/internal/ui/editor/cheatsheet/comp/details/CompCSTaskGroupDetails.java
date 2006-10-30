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

import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
	
	private ICSDetails fEnclosingTextSection;
	
	//private ICSDetails fRegisterCSArea;
	
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
		//fRegisterCSArea = new CSRegisterCSDetails(this, fDataTaskGroup.getModel());
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
		// Create the register cheat sheet area
		//fRegisterCSArea.createDetails(parent);		
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
		fKindCombo.getControl().setToolTipText(
				PDEUIMessages.CompCSTaskGroupDetails_KindToolTip); 
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
		// Create listeners for the name entry
		createListenersNameEntry();
		// Create listeners for the kind combo
		createListenersKindCombo();
		// Create listeners for the skip button
		createListenersSkipButton();
		// Create listeners within the enclosing text section
		fEnclosingTextSection.hookListeners();
		// Create the listeners within the register cheat sheet area
		//fRegisterCSArea.hookListeners();		
	}

	/**
	 * 
	 */
	private void createListenersNameEntry() {
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fDataTaskGroup.setFieldName(fNameEntry.getValue());
			}
		});			
	}	

	/**
	 * 
	 */
	private void createListenersKindCombo() {
		fKindCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String selection = fKindCombo.getSelection();
				if (selection.equals(F_KIND_VALUE_CHOICE)) {
					fDataTaskGroup.setFieldKind(
							ICompCSConstants.ATTRIBUTE_VALUE_CHOICE);
				} else if (selection.equals(F_KIND_VALUE_SEQUENCE)) {
					fDataTaskGroup.setFieldKind(
							ICompCSConstants.ATTRIBUTE_VALUE_SEQUENCE);
				} else if (selection.equals(F_KIND_VALUE_SET)) {
					fDataTaskGroup.setFieldKind(
							ICompCSConstants.ATTRIBUTE_VALUE_SET);
				}
			}
		});
	}	

	/**
	 * 
	 */
	private void createListenersSkipButton() {
		fSkip.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fDataTaskGroup.setFieldSkip(fSkip.getSelection());
			}
		});		
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = isEditableElement();
		// Update name entry
		updateNameEntry(editable);
		// Update kind combo
		updateKindCombo(editable);
		// Update skip button
		updateSkipButton(editable);
		// Update fields within enclosing text section
		fEnclosingTextSection.updateFields();
		// Update the fields within the register cheat sheet area
		//fRegisterCSArea.updateFields();		
	}

	/**
	 * @param editable
	 */
	private void updateNameEntry(boolean editable) {
		fNameEntry.setValue(fDataTaskGroup.getFieldName(), true);
		fNameEntry.setEditable(editable);			
	}	

	/**
	 * @param editable
	 */
	private void updateKindCombo(boolean editable) {
		String kind = fDataTaskGroup.getFieldKind();
		
		if (kind == null) {
			// NO-OP
		} else if (kind.compareTo(ICompCSConstants.ATTRIBUTE_VALUE_SEQUENCE) == 0) {
			fKindCombo.setText(F_KIND_VALUE_SEQUENCE);
		} else if (kind.compareTo(ICompCSConstants.ATTRIBUTE_VALUE_CHOICE) == 0) {
			fKindCombo.setText(F_KIND_VALUE_CHOICE);
		} else {
			fKindCombo.setText(F_KIND_VALUE_SET);
		}
		
		fKindCombo.setEnabled(editable);
	}	

	/**
	 * @param editable
	 */
	private void updateSkipButton(boolean editable) {
		fSkip.setSelection(fDataTaskGroup.getFieldSkip());
		fSkip.setEnabled(editable);	
	}	
	
}
