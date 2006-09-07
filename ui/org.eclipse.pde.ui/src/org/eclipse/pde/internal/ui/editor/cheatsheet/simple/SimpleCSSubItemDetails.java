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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * SimpleCSSubItemDetails
 *
 */
public class SimpleCSSubItemDetails extends SimpleCSAbstractDetails {

	private ISimpleCSSubItem fSubItem;
	
	private FormEntry fLabel;
	
	private Button fSkipTrue;
	
	private Button fSkipFalse;
	
	private FormEntry fWhen;
	
	/**
	 * @param elementSection
	 */
	public SimpleCSSubItemDetails(ISimpleCSSubItem subItem, SimpleCSElementSection elementSection) {
		super(elementSection);
		fSubItem = subItem;
		// TODO: MP: Set fields to null
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
		
		// Attribute: label
		fLabel = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSSubItemDetails_0, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 50;
		gd.horizontalSpan = 2;		
		fLabel.getText().setLayoutData(gd);
		
		// Attribute: skip
		label = toolkit.createLabel(parent, PDEUIMessages.SimpleCSSubItemDetails_1);
		label.setForeground(foreground);
		Button[] SkipButtons = createTrueFalseButtons(parent, toolkit, 2);
		fSkipTrue = SkipButtons[0];
		fSkipFalse = SkipButtons[1];		

		// Attribute: when
		fWhen = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSSubItemDetails_2, SWT.NONE);
		
		setText(PDEUIMessages.SimpleCSSubItemDetails_3);
		setDecription(NLS.bind(PDEUIMessages.SimpleCSSubItemDetails_4,
				fSubItem.getName()));		
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
		fSkipTrue.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fSubItem.setSkip(fSkipTrue.getSelection());
			}
		});			
		// Attribute: when
		fWhen.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// TODO: MP: Can when ever be null?
				fSubItem.setWhen(fWhen.getValue());
			}
		});			
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
		fLabel.setValue(fSubItem.getLabel());
		fLabel.setEditable(editable);
		
		// Attribute: skip
		fSkipTrue.setSelection(fSubItem.getSkip());
		fSkipTrue.setEnabled(editable);
		fSkipFalse.setSelection(!fSubItem.getSkip());
		fSkipFalse.setEnabled(editable);
		
		// Attribute: when
		fWhen.setValue(fSubItem.getWhen(), true);
		fWhen.setEditable(editable);

	}

}
