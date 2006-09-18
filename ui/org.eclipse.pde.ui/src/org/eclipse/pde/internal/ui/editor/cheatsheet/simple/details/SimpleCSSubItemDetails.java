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
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSElementSection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSSubItemDetails
 *
 */
public class SimpleCSSubItemDetails extends SimpleCSAbstractDetails {

	private ISimpleCSSubItem fSubItem;
	
	private FormEntry fLabel;
	
	private Button fSkip;
	
	private Section fMainSection;

	private ISimpleCSDetails fCommandSection;
	
	// Not supporting when at this moment; since, we are not supporting
	// conditional-subitem
	//private FormEntry fWhen;	
	
	/**
	 * @param elementSection
	 */
	public SimpleCSSubItemDetails(ISimpleCSSubItem subItem, SimpleCSElementSection elementSection) {
		super(elementSection);
		fSubItem = subItem;

		fLabel = null;
		fSkip = null;
		// Not supporting when at this moment; since, we are not supporting
		// conditional-subitem
		//fWhen = null;
		fMainSection = null;
		fCommandSection = new SimpleCSCommandDetails(fSubItem, this);		
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

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
		fMainSection.setText(PDEUIMessages.SimpleCSSubItemDetails_10);
		fMainSection.setDescription(PDEUIMessages.SimpleCSSubItemDetails_11);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);
		
		// Create container for main section
		Composite mainSectionClient = fToolkit.createComposite(fMainSection);	
		layout = new GridLayout(2, false);
		if (paintedBorder) {
			layout.verticalSpacing = 7;
		}
		mainSectionClient.setLayout(layout);
		
		// Attribute: label
		fLabel = new FormEntry(mainSectionClient, fToolkit, PDEUIMessages.SimpleCSSubItemDetails_0, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 50;
		fLabel.getText().setLayoutData(data);
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fLabel.getLabel().setLayoutData(data);		

		// Attribute: skip
		fSkip = fToolkit.createButton(mainSectionClient, PDEUIMessages.SimpleCSSubItemDetails_3, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fSkip.setLayoutData(data);

		// Bind widgets
		fToolkit.paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);

		fCommandSection.createDetails(parent);

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
//				// TODO: MP: Can when ever be null?
//				fSubItem.setWhen(fWhen.getValue());
//			}
//		});			
		
		fCommandSection.hookListeners();
		
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
		
		// TODO: MP: Add update function for table
		// TODO: MP: remove qualifyer from qualified parameter names

		
	}

}
