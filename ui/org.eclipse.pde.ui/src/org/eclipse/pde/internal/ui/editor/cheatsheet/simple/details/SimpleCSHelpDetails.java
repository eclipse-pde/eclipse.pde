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
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractSubDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSHelpDetailsSection
 *
 */
public class SimpleCSHelpDetails extends CSAbstractSubDetails {

	private Text fHelpText;
	
	private ComboPart fHelpCombo;	
	
	private Label fHelpLabel;
	
	private ISimpleCSHelpObject fHelpObject;
	
	private Section fHelpSection;
	
	private boolean fBlockListeners;

	private static final String F_NO_HELP = PDEUIMessages.SimpleCSCommandDetails_6;
	
	private static final String F_HELP_CONTEXT_ID = PDEUIMessages.SimpleCSHelpDetails_HelpContextID;

	private static final String F_HELP_DOCUMENT_LINK = PDEUIMessages.SimpleCSHelpDetails_HelpDocumentLink;
	
	/**
	 * @param section
	 */
	public SimpleCSHelpDetails(ICSMaster section) {
		super(section, SimpleCSInputContext.CONTEXT_ID);
		fHelpObject = null;
		fBlockListeners = false;
		
		fHelpText = null;
		fHelpCombo = null;
		fHelpLabel = null;
		
		fHelpSection = null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#setData(java.lang.Object)
	 */
	public void setData(ISimpleCSHelpObject object) {
		// Set data
		fHelpObject = object;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		int columnSpan = 2;
		FormToolkit toolkit = getToolkit();
		
		GridData data = null;
		Label label = null;
		Color foreground = toolkit.getColors().getColor(IFormColors.TITLE);
		
		// Create help section
		fHelpSection = toolkit.createSection(parent, Section.DESCRIPTION
				| ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE);
		fHelpSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fHelpSection.setText(PDEUIMessages.SimpleCSSharedUIFactory_1);
		fHelpSection.setDescription(PDEUIMessages.SimpleCSSharedUIFactory_2);
		fHelpSection.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		data = new GridData(GridData.FILL_HORIZONTAL);
		fHelpSection.setLayoutData(data);
		
		// Create container for help section		
		Composite helpSectionClient = toolkit.createComposite(fHelpSection);	
		helpSectionClient.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, columnSpan));		

		// Attribute: href		
		// Attribute: contextId
		label = toolkit.createLabel(helpSectionClient, 
				PDEUIMessages.SimpleCSHelpDetails_Type, SWT.WRAP);
		label.setForeground(foreground);

		// Attribute: href		
		// Attribute: contextId
		fHelpCombo = new ComboPart();
		fHelpCombo.createControl(helpSectionClient, toolkit, SWT.READ_ONLY);
		fHelpCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fHelpCombo.add(F_NO_HELP);
		fHelpCombo.add(F_HELP_CONTEXT_ID);
		fHelpCombo.add(F_HELP_DOCUMENT_LINK);
		fHelpCombo.setText(F_NO_HELP);
		
		// Attribute: href		
		// Attribute: contextId
		fHelpLabel = toolkit.createLabel(helpSectionClient, 
				PDEUIMessages.SimpleCSHelpDetails_Value, SWT.WRAP);
		fHelpLabel.setForeground(foreground);
		
		// Attribute: href		
		// Attribute: contextId
		fHelpText = toolkit.createText(helpSectionClient, null);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fHelpText.setLayoutData(data);
	
		// Bind widgets
		toolkit.paintBordersFor(helpSectionClient);
		fHelpSection.setClient(helpSectionClient);
		// Mark as a details part to enable cut, copy, paste, etc.
		markDetailsPart(fHelpSection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails#hookListeners()
	 */
	public void hookListeners() {

		// Attribute: href		
		// Attribute: contextId		
		fHelpCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fHelpObject == null) {
					return;
				}	
				String selection = fHelpCombo.getSelection();
				if (selection.equals(F_NO_HELP) ==  false) {
					// Help was selected
					if (selection.equals(F_HELP_CONTEXT_ID)) {
						// Help context ID was selected, clear the help 
						// document link value
						fHelpObject.setHref(null);
					} else {
						// Help document link was selected, clear the help 
						// context ID value
						fHelpObject.setContextId(null);
					}
					// Make the label and text field visible
					fHelpLabel.setVisible(true);
					fHelpText.setVisible(true);
					// Set the focus on the text field
					fHelpText.setFocus();
					// Clear the previous contents of the text field
					// (Will cause field to become dirty)
					fHelpText.setText(""); //$NON-NLS-1$
				} else {
					// No help was selected
					// Make the label and text field invisible
					fHelpLabel.setVisible(false);
					fHelpText.setVisible(false);
					// Clear values for help in model
					fHelpObject.setContextId(null);
					fHelpObject.setHref(null);
				}
			}		
		});		
		// Attribute: href		
		// Attribute: contextId		
		fHelpText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				// Block UI updates
				if (fBlockListeners) {
					return;
				}
				// Ensure data object is defined
				if (fHelpObject == null) {
					return;
				}	
				String selection = fHelpCombo.getSelection();
				if (selection.equals(F_HELP_CONTEXT_ID)) {
					// Help context ID was selected, save the field contents
					// as such
					fHelpObject.setContextId(fHelpText.getText());
				} else {
					// Help document link was selected, save the field contents
					// as such
					fHelpObject.setHref(fHelpText.getText());
				}
			}
		});		
	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails#updateFields()
	 */
	public void updateFields() {
		// Ensure data object is defined
		if (fHelpObject == null) {
			return;
		}			

		boolean editable = isEditableElement();
		boolean expanded = false;
		
		// Block model updates
		fBlockListeners = true;
		// Attribute: contextId
		// Attribute: href		
		if (PDETextHelper.isDefined(fHelpObject.getContextId())) {
			fHelpText.setText(fHelpObject.getContextId());
			fHelpCombo.setText(F_HELP_CONTEXT_ID);
			expanded = true;
		} else if (PDETextHelper.isDefined(fHelpObject.getHref())) {
			fHelpText.setText(fHelpObject.getHref());
			fHelpCombo.setText(F_HELP_DOCUMENT_LINK);
			expanded = true;
		} else {
			fHelpCombo.setText(F_NO_HELP);
		}
		// Unblock model updates
		fBlockListeners = false;

		fHelpSection.setExpanded(expanded);
		fHelpText.setEnabled(editable);
		fHelpText.setVisible(expanded);
		fHelpLabel.setVisible(expanded);
		fHelpCombo.setEnabled(editable);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// NO-OP
		// No form entries
	}	
	
}
