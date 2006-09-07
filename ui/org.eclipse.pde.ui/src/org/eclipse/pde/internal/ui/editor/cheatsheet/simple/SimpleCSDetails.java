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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * SimpleCSDetails
 *
 */
public class SimpleCSDetails extends SimpleCSAbstractDetails {

	private ISimpleCS fCheatSheet;
	
	private FormEntry fTitle;
	
	/**
	 * 
	 */
	public SimpleCSDetails(ISimpleCS cheatsheet, SimpleCSElementSection section) {
		super(section);
		fCheatSheet = cheatsheet;
		fTitle = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {
		
		FormToolkit toolkit = getManagedForm().getToolkit();
		// Configure layout
		GridLayout glayout = new GridLayout(2, false);
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
		if (paintedBorder) {
			glayout.verticalSpacing = 7;
		}
		parent.setLayout(glayout);
		
		//Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		// Attribute: title
		fTitle = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSDetails_0, SWT.NONE);
	
		setText(PDEUIMessages.SimpleCSDetails_1);
		setDecription(NLS.bind(PDEUIMessages.SimpleCSDetails_2,
				fCheatSheet.getName()));

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// Attribute: title
		fTitle.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// TODO: MP: Can cheat sheet ever be null?
				fCheatSheet.setTitle(fTitle.getValue());
			}
		});		
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = isEditableElement();
		// TODO: MP: Can this ever happen ?
		if (fCheatSheet == null) {
			return;
		}

		// Attribute: title
		fTitle.setValue(fCheatSheet.getTitle(), true);
		fTitle.setEditable(editable);
		

	}

}
