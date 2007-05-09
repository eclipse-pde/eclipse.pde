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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSIntroDetails
 *
 */
public class SimpleCSIntroDetails extends CSAbstractDetails {

	private ISimpleCSIntro fIntro;
	
	private FormEntry fContent;
	
	private Section fMainSection;	
	
	private SimpleCSHelpDetails fHelpSection;
	
	/**
	 * @param elementSection
	 */
	public SimpleCSIntroDetails(ICSMaster elementSection) {
		super(elementSection, SimpleCSInputContext.CONTEXT_ID);
		fIntro = null;
		
		fContent = null;
		fMainSection = null;
		fHelpSection = new SimpleCSHelpDetails(elementSection);
	}

	/**
	 * @param object
	 */
	public void setData(ISimpleCSIntro object) {
		// Set data
		fIntro = object;
		// Set data on help section
		fHelpSection.setData(object);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) ||
				(object instanceof ISimpleCSIntro) == false) {
			return;
		}
		// Set data
		setData((ISimpleCSIntro)object);
		// Update the UI given the new data
		updateFields();	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	public void initialize(IManagedForm form) {
		super.initialize(form);
		// Unfortunately this has to be explicitly called for sub detail
		// sections through its main section parent; since, it never is 
		// registered directly.
		// Initialize managed form for help section
		fHelpSection.initialize(form);
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		GridData data = null;
		
		// Create main section
		fMainSection = getToolkit().createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(PDEUIMessages.SimpleCSIntroDetails_2);
		fMainSection.setDescription(PDEUIMessages.SimpleCSIntroDetails_3);
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
	
		// description:  Content (Element)
		fContent = new FormEntry(mainSectionClient, getToolkit(), PDEUIMessages.SimpleCSDescriptionDetails_0, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 90;
		fContent.getText().setLayoutData(data);		
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fContent.getLabel().setLayoutData(data);				

		// Bind widgets
		getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);
		
		fHelpSection.createDetails(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// description: Content (Element)
		fContent.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fIntro == null) {
					return;
				}

				if (fIntro.getDescription() != null) {
					fIntro.getDescription().setContent(fContent.getValue());
				}
			}
		});
		
		fHelpSection.hookListeners();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {
		// Ensure data object is defined
		if (fIntro == null) {
			return;
		}
		
		fHelpSection.updateFields();
		
		boolean editable = isEditableElement();
		
		if (fIntro.getDescription() == null) {
			return;
		}

		// description:  Content (Element)
		fContent.setValue(fIntro.getDescription().getContent(), true);
		fContent.setEditable(editable);		

	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fContent.commit();
		// No need to call for sub details, because they contain no form entries
	}
	
}
