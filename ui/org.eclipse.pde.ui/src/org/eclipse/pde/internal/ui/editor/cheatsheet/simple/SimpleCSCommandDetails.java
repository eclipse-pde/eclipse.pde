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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * SimpleCSCommandDetails
 *
 */
public class SimpleCSCommandDetails extends SimpleCSAbstractDetails {

	private ISimpleCSCommand fCommand;

	private FormEntry fSerialization;	

	private FormEntry fReturns;	

	private Button fConfirmTrue;
	
	private Button fConfirmFalse;	
	
	private FormEntry fWhen;	
	
	/**
	 * @param elementSection
	 */
	public SimpleCSCommandDetails(ISimpleCSCommand command, SimpleCSElementSection elementSection) {
		super(elementSection);
		fCommand = command;
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
		
		// Attribute: serialization
		fSerialization = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSCommandDetails_0, SWT.NONE);

		// Attribute: serialization
		fReturns = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSCommandDetails_1, SWT.NONE);
		
		// Attribute: dialog
		label = toolkit.createLabel(parent, PDEUIMessages.SimpleCSCommandDetails_2);
		label.setForeground(foreground);
		Button[] dialogButtons = createTrueFalseButtons(parent, toolkit, 2);
		fConfirmTrue = dialogButtons[0];
		fConfirmFalse = dialogButtons[1];		

		// Attribute: serialization
		fWhen = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSCommandDetails_3, SWT.NONE);
		
		setText(PDEUIMessages.SimpleCSCommandDetails_4);
		setDecription(NLS.bind(PDEUIMessages.SimpleCSCommandDetails_5,
				fCommand.getName()));		
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = isEditableElement();
		
		if (fCommand == null) {
			return;
		}
		// Attribute: serialization
		fSerialization.setValue(fCommand.getSerialization(), true);
		fSerialization.setEditable(editable);		

		// Attribute: returns
		fReturns.setValue(fCommand.getReturns(), true);
		fReturns.setEditable(editable);		

		// Attribute: skip
		fConfirmTrue.setSelection(fCommand.getConfirm());
		fConfirmTrue.setEnabled(editable);
		fConfirmFalse.setSelection(!fCommand.getConfirm());
		fConfirmFalse.setEnabled(editable);	

		// Attribute: when
		fWhen.setValue(fCommand.getWhen(), true);
		fWhen.setEditable(editable);				
	}

}
