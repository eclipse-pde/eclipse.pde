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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * SimpleCSDescriptionDetails
 *
 */
public class SimpleCSDescriptionDetails extends SimpleCSAbstractDetails {

	private ISimpleCSDescription fDescription;
	
	private FormEntry fContent;
	
	/**
	 * @param elementSection
	 */
	public SimpleCSDescriptionDetails(ISimpleCSDescription description, SimpleCSElementSection elementSection) {
		super(elementSection);
		fDescription = description;
		// TODO: MP: Set fields to null
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		FormToolkit toolkit = getManagedForm().getToolkit();
		// Configure layout
		GridLayout glayout = new GridLayout(1, false);
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
		if (paintedBorder) {
			glayout.verticalSpacing = 7;
		}
		parent.setLayout(glayout);		
		
		// Content (Element)
		fContent = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSDescriptionDetails_0, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 90;
		fContent.getText().setLayoutData(gd);
		

		setText(PDEUIMessages.SimpleCSDescriptionDetails_1);
		setDecription(NLS.bind(PDEUIMessages.SimpleCSDescriptionDetails_2,
				fDescription.getName()));		
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// Content (Element)
		fContent.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fDescription.setContent(fContent.getValue());
			}
		});

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = isEditableElement();
		
		if (fDescription == null) {
			return;
		}
		
		// Content (Element)
		fContent.setValue(fDescription.getContent());
		fContent.setEditable(editable);
		// TODO: MP: Should strip existing newlines?  Where to do it?
	}

}
