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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSSubItemDetails
 *
 */
public class SimpleCSSubItemDetails extends CSAbstractDetails {

	private ISimpleCSSubItem fSubItem;
	
	private FormEntry fLabel;
	
	private Button fSkip;
	
	private Section fMainSection;

	private ICSDetails fCommandSection;

	private ICSDetails fRegisterCSArea;
	
	// Not supporting when at this moment; since, we are not supporting
	// conditional-subitem
	//private FormEntry fWhen;	
	
	/**
	 * @param elementSection
	 */
	public SimpleCSSubItemDetails(ISimpleCSSubItem subItem, ICSMaster masterTreeSection) {
		super(masterTreeSection, SimpleCSInputContext.CONTEXT_ID);
		fSubItem = subItem;

		fLabel = null;
		fSkip = null;
		// Not supporting when at this moment; since, we are not supporting
		// conditional-subitem
		//fWhen = null;
		fMainSection = null;
		fCommandSection = new SimpleCSCommandDetails(fSubItem, 
				masterTreeSection);		
		fRegisterCSArea = new CSRegisterCSDetails(fSubItem.getModel(), 
				masterTreeSection, SimpleCSInputContext.CONTEXT_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	public void initialize(IManagedForm form) {
		super.initialize(form);
		// Unfortunately this has to be explicitly called for sub detail
		// sections through its main section parent; since, it never is 
		// registered directly.
		// Initialize managed form for command section
		if (fCommandSection instanceof IFormPart) {
			((IFormPart)fCommandSection).initialize(form);
		}
		// Initialize managed form for register area
		if (fRegisterCSArea instanceof IFormPart) {
			((IFormPart)fRegisterCSArea).initialize(form);
		}
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		GridData data = null;
		
		// Create main section
		fMainSection = getToolkit().createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(PDEUIMessages.SimpleCSSubItemDetails_10);
		fMainSection.setDescription(PDEUIMessages.SimpleCSSubItemDetails_11);
		fMainSection.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);
		
		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), 
				fMainSection);	
		
		// Create container for main section
		Composite mainSectionClient = getToolkit().createComposite(fMainSection);	
		mainSectionClient.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		
		// Attribute: label
		fLabel = new FormEntry(mainSectionClient, getToolkit(), PDEUIMessages.SimpleCSSubItemDetails_0, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 50;
		fLabel.getText().setLayoutData(data);
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fLabel.getLabel().setLayoutData(data);		

		// Attribute: skip
		fSkip = getToolkit().createButton(mainSectionClient, PDEUIMessages.SimpleCSSubItemDetails_3, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fSkip.setLayoutData(data);

		// Bind widgets
		getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);

		fCommandSection.createDetails(parent);
		// Create the register cheat sheet area
		fRegisterCSArea.createDetails(parent);
		// Attribute: when
		// Not supporting when at this moment; since, we are not supporting
		// conditional-subitem
		//fWhen = new FormEntry(optionalSectionClient, toolkit, PDEUIMessages.SimpleCSSubItemDetails_2, SWT.NONE);
		
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// Attribute: label
		fLabel.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// TODO: MP: Can when ever be null?
				fSubItem.setLabel(fLabel.getValue());
			}
		});	
		// Attribute: skip
		fSkip.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fSubItem.setSkip(fSkip.getSelection());
			}
		});
		// Attribute: when
		// Not supporting when at this moment; since, we are not supporting
		// conditional-subitem
//		fWhen.setFormEntryListener(new FormEntryAdapter(this) {
//			public void textValueChanged(FormEntry entry) {
//				fSubItem.setWhen(fWhen.getValue());
//			}
//		});			
		
		fCommandSection.hookListeners();
		// Create the listeners within the register cheat sheet area
		fRegisterCSArea.hookListeners();		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = isEditableElement();
		
		if (fSubItem == null) {
			return;
		}
		// Attribute: label
		fLabel.setValue(fSubItem.getLabel(), true);
		fLabel.setEditable(editable);
		
		// Attribute: skip
		fSkip.setSelection(fSubItem.getSkip());
		fSkip.setEnabled(editable);
		
		// Attribute: when
		// Not supporting when at this moment; since, we are not supporting
		// conditional-subitem
//		fWhen.setValue(fSubItem.getWhen(), true);
//		fWhen.setEditable(editable);
		
		fCommandSection.updateFields();
		// Update the fields within the register cheat sheet area
		fRegisterCSArea.updateFields();			
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fLabel.commit();
		// No need to call for sub details, because they contain no form entries
	}
	
}
