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
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * SimpleCSSubItemDetails
 *
 */
public class SimpleCSSubItemDetails extends SimpleCSAbstractDetails {

	private ISimpleCSSubItem fSubItem;
	
	private Text fLabel;
	
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
		GridData gd = null;
		
		// Attribute: label
		label = toolkit.createLabel(parent, PDEUIMessages.SimpleCSSubItemDetails_0);
		label.setForeground(foreground);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		
		fLabel = toolkit.createText(parent, "", SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);//$NON-NLS-1$
		
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 50;
		gd.horizontalSpan = 2;
		fLabel.setLayoutData(gd);		
		
		// Attribute: skip
		label = toolkit.createLabel(parent, PDEUIMessages.SimpleCSSubItemDetails_1);
		label.setForeground(foreground);
		Button[] SkipButtons = createTrueFalseButtons(parent, toolkit, 2);
		fSkipTrue = SkipButtons[0];
		fSkipFalse = SkipButtons[1];		

		// Attribute: href
		fWhen = new FormEntry(parent, toolkit, PDEUIMessages.SimpleCSSubItemDetails_2, SWT.NONE);
		
		setText(PDEUIMessages.SimpleCSSubItemDetails_3);
		setDecription(NLS.bind(PDEUIMessages.SimpleCSSubItemDetails_4,
				fSubItem.getName()));		
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
		
		if (fSubItem == null) {
			return;
		}
		// Attribute: label
		fLabel.setText(fSubItem.getLabel());
		fLabel.setEditable(editable);
		
		// Attribute: skip
		fSkipTrue.setSelection(fSubItem.getSkip());
		fSkipTrue.setEnabled(editable);
		fSkipFalse.setSelection(!fSubItem.getSkip());
		fSkipFalse.setEnabled(editable);
		
		// Attribute: href
		fWhen.setValue(fSubItem.getWhen(), true);
		fWhen.setEditable(editable);

	}

}
