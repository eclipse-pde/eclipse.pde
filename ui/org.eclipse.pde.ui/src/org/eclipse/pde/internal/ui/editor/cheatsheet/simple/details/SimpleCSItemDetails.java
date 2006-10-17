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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSItemDetails
 *
 */
public class SimpleCSItemDetails extends CSAbstractDetails {

	private ISimpleCSItem fItem;
	
	private FormEntry fTitle;
	
	private Button fDialog;
	
	private Button fSkip;	
	
	private FormEntry fContent;

	private Section fMainSection;	

	private ICSDetails fHelpSection;	
	
	private ICSDetails fCommandSection;
	
	/**
	 * 
	 */
	public SimpleCSItemDetails(ISimpleCSItem item, ICSMaster section) {
		super(section, SimpleCSInputContext.CONTEXT_ID);
		fItem = item;
		
		fTitle = null;
		fDialog = null;
		fSkip = null;
		fContent = null;
		fMainSection = null;
		
		fHelpSection = new SimpleCSHelpDetails(fItem, this);
		fCommandSection = new SimpleCSCommandDetails(fItem, this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		Color foreground = getToolkit().getColors().getColor(FormColors.TITLE);
		GridData data = null;
		boolean paintedBorder = getToolkit().getBorderStyle() != SWT.BORDER;
		
		// Create main section
		// TODO: MP: Do make section scrollable
		fMainSection = getToolkit().createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		fMainSection.marginHeight = 5;
		fMainSection.marginWidth = 5; 
		fMainSection.setText(PDEUIMessages.SimpleCSItemDetails_11);
		fMainSection.setDescription(PDEUIMessages.SimpleCSItemDetails_12);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);
		
		// Create container for main section
		Composite mainSectionClient = getToolkit().createComposite(fMainSection);	
		GridLayout layout = new GridLayout(2, false);
		if (paintedBorder) {
			layout.verticalSpacing = 7;
		}
		mainSectionClient.setLayout(layout);				

		// Attribute: title
		fTitle = new FormEntry(mainSectionClient, getToolkit(), PDEUIMessages.SimpleCSItemDetails_0, SWT.NONE);

		// description: Content (Element)
		fContent = new FormEntry(mainSectionClient, getToolkit(), PDEUIMessages.SimpleCSDescriptionDetails_0, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 90;
		//data.horizontalSpan = 2;
		fContent.getText().setLayoutData(data);	
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fContent.getLabel().setLayoutData(data);

		// Attribute: dialog
		fDialog = getToolkit().createButton(mainSectionClient, PDEUIMessages.SimpleCSItemDetails_13, SWT.CHECK);
														
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fDialog.setLayoutData(data);
		fDialog.setForeground(foreground);
		
		// Attribute: skip
		fSkip = getToolkit().createButton(mainSectionClient, PDEUIMessages.SimpleCSItemDetails_14, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fSkip.setLayoutData(data);
		fSkip.setForeground(foreground);
		
		// Bind widgets
		getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);		
		
		fCommandSection.createDetails(parent);
		
		fHelpSection.createDetails(parent);
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {

		// description: Content (Element)
		// TODO: MP: HIGH:  Figure out why form entry is marking things dirty on lost focus
		fContent.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fItem.getDescription() != null) {
					fItem.getDescription().setContent(fContent.getValue());
				}
			}
		});		
		// Attribute: title
		fTitle.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fItem.setTitle(fTitle.getValue());
			}
		});
		// Attribute: dialog
		fDialog.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fItem.setDialog(fDialog.getSelection());
			}
		});	
		// Attribute: skip
		fSkip.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fItem.setSkip(fSkip.getSelection());
			}
		});	

		fHelpSection.hookListeners();
		
		fCommandSection.hookListeners();
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = isEditableElement();
		
		if (fItem == null) {
			return;
		}
		// TODO: MP: Check isdefined for all parameters in updateFields methods
		// Attribute: title
		if (PDETextHelper.isDefined(fItem.getTitle())) {
			fTitle.setValue(fItem.getTitle(), true);
		}
		fTitle.setEditable(editable);

		// Attribute: dialog
		fDialog.setSelection(fItem.getDialog());
		fDialog.setEnabled(editable);
		
		// Attribute: skip
		fSkip.setSelection(fItem.getSkip());
		updateSkipEnablement();
		
		fHelpSection.updateFields();

		fCommandSection.updateFields();
		
		// TODO: MP: Important: revist all parameters and check we are simply
		// looking for null - okay for non-String types
		// TODO: MP: Reevaluate write methods and make sure not writing empty string
		
		
		if (fItem.getDescription() == null) {
			return;
		}

		// description:  Content (Element)
		if (PDETextHelper.isDefined(fItem.getDescription().getContent())) {
			fContent.setValue(fItem.getDescription().getContent(), true);
		}
		fContent.setEditable(editable);			

	}
	
	/**
	 * 
	 */
	private void updateSkipEnablement() {

		boolean editable = isEditableElement();
		// Preserve cheat sheet validity
		// Semantic Rule:  Specifying whether an item can be skipped or not has
		// no effect when subitems are present (because the item delegates the
		// control to the subitem to skip).
		if (fItem.hasSubItems()) {
			editable = false;
		}		
		fSkip.setEnabled(editable);		
	}
	
}
