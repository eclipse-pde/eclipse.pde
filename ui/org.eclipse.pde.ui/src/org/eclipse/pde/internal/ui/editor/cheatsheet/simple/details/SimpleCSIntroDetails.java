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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSElementSection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSIntroDetails
 *
 */
public class SimpleCSIntroDetails extends SimpleCSAbstractDetails {

	private ISimpleCSIntro fIntro;
	
	private FormEntry fContent;
	
	private Section fMainSection;	
	
	private ISimpleCSDetails fHelpSection;
	
	/**
	 * @param elementSection
	 */
	public SimpleCSIntroDetails(ISimpleCSIntro intro, SimpleCSElementSection elementSection) {
		super(elementSection);
		fIntro = intro;
		
		fContent = null;
		fMainSection = null;
		fHelpSection = new SimpleCSHelpDetails(fIntro, this);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		// TODO: MP: Probably can refactor this back into super class as utility
		// Creation of section and composite
		// TODO: MP: make the toolkit a member var for all details classes for consistency
		fToolkit = getManagedForm().getToolkit();
		//Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		GridData data = null;
		boolean paintedBorder = fToolkit.getBorderStyle() != SWT.BORDER;
		
		// Set parent layout
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		parent.setLayout(layout);
		
		// Create main section
		fMainSection = fToolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		fMainSection.marginHeight = 5;
		fMainSection.marginWidth = 5; 
		fMainSection.setText(PDEUIMessages.SimpleCSIntroDetails_2);
		fMainSection.setDescription(PDEUIMessages.SimpleCSIntroDetails_3);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);
		
		// Create container for main section
		Composite mainSectionClient = fToolkit.createComposite(fMainSection);	
		layout = new GridLayout(2, false);
		if (paintedBorder) {
			layout.verticalSpacing = 7;
		}
		mainSectionClient.setLayout(layout);				

	
		// description:  Content (Element)
		fContent = new FormEntry(mainSectionClient, fToolkit, PDEUIMessages.SimpleCSDescriptionDetails_0, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 90;
		fContent.getText().setLayoutData(data);		
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fContent.getLabel().setLayoutData(data);				

		// Bind widgets
		fToolkit.paintBordersFor(mainSectionClient);
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

		fHelpSection.updateFields();
		
		boolean editable = isEditableElement();
		
		if (fIntro.getDescription() == null) {
			return;
		}

		// description:  Content (Element)
		fContent.setValue(fIntro.getDescription().getContent(), true);
		fContent.setEditable(editable);		

	}
	

	
}
