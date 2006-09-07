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

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * SimpleCSItemDetails
 *
 */
public class SimpleCSItemDetails extends SimpleCSAbstractDetails {

	private ISimpleCSItem fItem;
	
	private FormEntry fTitle;
	
	private Button fDialogTrue;
	
	private Button fDialogFalse;
	
	private Button fSkipTrue;
	
	private Button fSkipFalse;
	
	private FormEntry fContextId;
	
	private FormEntry fHref;
	
	/**
	 * 
	 */
	public SimpleCSItemDetails(ISimpleCSItem item, SimpleCSElementSection section) {
		super(section);
		fItem = item;
		fTitle = null;
		// TODO: MP: Set rest to null
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		FormToolkit toolkit = getManagedForm().getToolkit();
		// Configure layout
		GridLayout glayout = new GridLayout(3, false);
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
		if (paintedBorder) {
			glayout.verticalSpacing = 7;
		}
		parent.setLayout(glayout);		
		
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		Label label = null;
		
		// Attribute: title
		fTitle = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSItemDetails_0, SWT.NONE);

		// Attribute: dialog
		label = toolkit.createLabel(parent, PDEUIMessages.SimpleCSItemDetails_1);
		label.setForeground(foreground);
		Button[] dialogButtons = createTrueFalseButtons(parent, toolkit, 2);
		fDialogTrue = dialogButtons[0];
		fDialogFalse = dialogButtons[1];

		// Attribute: skip
		label = toolkit.createLabel(parent, PDEUIMessages.SimpleCSItemDetails_2);
		label.setForeground(foreground);
		Button[] SkipButtons = createTrueFalseButtons(parent, toolkit, 2);
		fSkipTrue = SkipButtons[0];
		fSkipFalse = SkipButtons[1];		

		// Attribute: contextId
		fContextId = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSItemDetails_3, SWT.NONE);
		
		// Attribute: href
		fHref = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSItemDetails_4, SWT.NONE);
		
		
		setText(PDEUIMessages.SimpleCSItemDetails_5);
		setDecription(NLS.bind(PDEUIMessages.SimpleCSItemDetails_6,
				fItem.getName()));

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		
		// Attribute: title
		fTitle.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fItem.setTitle(fTitle.getValue());
			}
		});
		// Attribute: dialog
		fDialogTrue.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fItem.setDialog(fDialogTrue.getSelection());
			}
		});	
		// Attribute: skip
		fSkipTrue.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fItem.setSkip(fSkipTrue.getSelection());
			}
		});			
		// Attribute: contextId
		fContextId.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fItem.setContextId(fContextId.getValue());
			}
		});		
		// Attribute: href
		fHref.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fItem.setHref(fHref.getValue());
			}
		});	
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = isEditableElement();
		
		if (fItem == null) {
			return;
		}
		// Attribute: title
		fTitle.setValue(fItem.getTitle(), true);
		fTitle.setEditable(editable);

		// Attribute: dialog
		fDialogTrue.setSelection(fItem.getDialog());
		fDialogTrue.setEnabled(editable);
		fDialogFalse.setSelection(!fItem.getDialog());
		fDialogFalse.setEnabled(editable);
		
		// Attribute: skip
		fSkipTrue.setSelection(fItem.getSkip());
		fSkipTrue.setEnabled(editable);
		fSkipFalse.setSelection(!fItem.getSkip());
		fSkipFalse.setEnabled(editable);
		
		// Attribute: contextId
		fContextId.setValue(fItem.getContextId(), true);
		fContextId.setEditable(editable);
		
		// Attribute: href
		fHref.setValue(fItem.getHref(), true);
		fHref.setEditable(editable);
		
	}
}
