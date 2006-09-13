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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSHelpObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSHelpDetailsSection
 *
 */
public class SimpleCSHelpDetails implements ISimpleCSDetails {

	private Text fContextId;
	
	private Text fHref;	
	
	private Button fContextIdRadio;	

	private Button fHrefRadio;
	
	private ISimpleCSHelpObject fHelpObject;
	
	private SimpleCSAbstractDetails fDetails;

	
	public SimpleCSHelpDetails(ISimpleCSHelpObject helpObject,
			SimpleCSAbstractDetails details) {
		fHelpObject = helpObject;
		fDetails = details;
		
		fContextId = null;
		fHref = null;
		fContextIdRadio = null;
		fHrefRadio = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		int columnSpan = 1;
		Section helpSection = null;
		FormToolkit toolkit = fDetails.getToolkit();
		
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
		GridData data = null;
		GridLayout layout = null;
		
		// Create help section
		helpSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		// | ExpandableComposite.TWISTIE
		helpSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		helpSection.marginHeight = 5;
		helpSection.marginWidth = 5;
		helpSection.setText(PDEUIMessages.SimpleCSSharedUIFactory_1);
		helpSection.setDescription(PDEUIMessages.SimpleCSSharedUIFactory_2);
		data = new GridData(GridData.FILL_HORIZONTAL);
		helpSection.setLayoutData(data);
		
		// Create container for help section		
		Composite helpSectionClient = toolkit.createComposite(helpSection);	
		layout = new GridLayout(columnSpan, false);
		if (paintedBorder) {
			layout.verticalSpacing = 7;
		}
		helpSectionClient.setLayout(layout);		
		
		// Create the radio button for the contextId
		fContextIdRadio = toolkit.createButton(helpSectionClient, PDEUIMessages.SimpleCSSharedUIFactory_3, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fContextIdRadio.setLayoutData(data);
		//data.horizontalSpan = 2;
		
		// Attribute: contextId
		//contextId = new FormEntry(helpSectionClient, toolkit, null, 20, 0);
		fContextId = toolkit.createText(helpSectionClient, null);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalIndent = 20;
		fContextId.setLayoutData(data);
		// TODO: MP: Delete
		//PDEUIMessages.SimpleCSIntroDetails_0

		// Create the radio button for the href
		fHrefRadio = toolkit.createButton(helpSectionClient, PDEUIMessages.SimpleCSSharedUIFactory_4, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fHrefRadio.setLayoutData(data);
		//data.horizontalSpan = 2;
		
		// Attribute: href
		//href = new FormEntry(helpSectionClient, toolkit, null, 20, 0);
		fHref = toolkit.createText(helpSectionClient, null);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalIndent = 20;
		fHref.setLayoutData(data);		
		// TODO: MP: Delete
		//PDEUIMessages.SimpleCSIntroDetails_1
		
		//createSpacer(helpSectionClient, toolkit, columnSpan);
		
		// Bind widgets
		toolkit.paintBordersFor(helpSectionClient);
		helpSection.setClient(helpSectionClient);
		// TODO: MP: What does this do?
		//markDetailsPart(fHelpSection);		
		
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails#hookListeners()
	 */
	public void hookListeners() {
		// Attribute: contextId
		fContextId.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fHelpObject.setContextId(fContextId.getText());
			}
		});		
		// Attribute: href
		fHref.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fHelpObject.setHref(fHref.getText());
			}
		});	
		// Radio button for contextId
		fContextIdRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fContextIdRadio.getSelection();
				fContextId.setEnabled(selected);
				fHref.setEnabled(!selected);				
			}
		});		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = fDetails.isEditableElement();
		
		if (fHelpObject == null) {
			return;
		}			
		
		// Attribute: contextId
		// Attribute: href		
		// Radio button for contextId
		// Radio button for contextId		
		if (PDETextHelper.isDefined(fHelpObject.getContextId())) {
			fContextId.setText(fHelpObject.getContextId());
			fContextId.setEnabled(true && editable);
			fContextIdRadio.setSelection(true && editable);
			fHref.setEnabled(false);	
			fHrefRadio.setSelection(false);			
		} else if (PDETextHelper.isDefined(fHelpObject.getHref())) {
			fHref.setText(fHelpObject.getHref());
			fContextId.setEnabled(false);
			fContextIdRadio.setSelection(false);			
			fHref.setEnabled(true && editable);			
			fHrefRadio.setSelection(true && editable);
		} else {
			fContextId.setEnabled(true && editable);
			fContextIdRadio.setSelection(true && editable);
			fHref.setEnabled(false);	
			fHrefRadio.setSelection(false);					
		}

	}
	
	
}
