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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * SimpleCSIntroDetails
 *
 */
public class SimpleCSIntroDetails extends SimpleCSAbstractDetails {

	private ISimpleCSIntro fIntro;
	
	private FormEntry fContextId;
	
	private FormEntry fHref;	
	
	/**
	 * @param elementSection
	 */
	public SimpleCSIntroDetails(ISimpleCSIntro intro, SimpleCSElementSection elementSection) {
		super(elementSection);
		fIntro = intro;
		// TODO: MP: Set rest to null
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

		// Attribute: contextId
		fContextId = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSIntroDetails_0, SWT.NONE);
		
		// Attribute: href
		fHref = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSIntroDetails_1, SWT.NONE);
		
		
		setText(PDEUIMessages.SimpleCSIntroDetails_2);
		setDecription(NLS.bind(PDEUIMessages.SimpleCSIntroDetails_3,
				fIntro.getName()));		
		
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
		
		if (fIntro == null) {
			return;
		}		

		// Attribute: contextId
		fContextId.setValue(fIntro.getContextId(), true);
		fContextId.setEditable(editable);
		
		// Attribute: href
		fHref.setValue(fIntro.getHref(), true);
		fHref.setEditable(editable);		

	}

}
