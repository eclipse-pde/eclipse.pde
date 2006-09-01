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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * SimpleCSDescriptionDetails
 *
 */
public class SimpleCSDescriptionDetails extends SimpleCSAbstractDetails {

	private ISimpleCSDescription fDescription;
	
	private Text fContent;
	
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
		
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		Label label = null;

		// Content (Element)		
		label = toolkit.createLabel(parent, PDEUIMessages.SimpleCSDescriptionDetails_0);
		label.setForeground(foreground);
		
		fContent = toolkit.createText(parent, "", SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);//$NON-NLS-1$
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 90;
		fContent.setLayoutData(gd);		

		setText(PDEUIMessages.SimpleCSDescriptionDetails_1);
		setDecription(NLS.bind(PDEUIMessages.SimpleCSDescriptionDetails_2,
				fDescription.getName()));		
		
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
		
		if (fDescription == null) {
			return;
		}
		
		// Content (Element)
		fContent.setText(fDescription.getContent());
		fContent.setEditable(editable);
		// TODO: MP: Should strip existing newlines?  Where to do it?
	}

}
