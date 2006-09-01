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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * SimpleCSPage
 *
 */
public class SimpleCSPage extends PDEFormPage {

	public static final String PAGE_ID = "main"; //$NON-NLS-1$
	
	private SimpleCSBlock fBlock;
	
	private IColorManager fColorManager;
	
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public SimpleCSPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.SimpleCSPage_0);
		fBlock = new SimpleCSBlock(this);
		fColorManager = ColorManager.getDefault();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		// Set page title
		ISimpleCSModel simpleCSModel = (ISimpleCSModel)getModel();
		// TODO: MP: This probably a very bad idea
		if (!simpleCSModel.isLoaded()) {
			throw new RuntimeException(PDEUIMessages.SimpleCSPage_1);
		}
		
		String title = simpleCSModel.getSimpleCS().getTitle();
		// TODO: MP: Check if model is null?
		if ((title != null) &&
				(title.length() > 0)) {
			form.setText(title);
		} else {
			// TODO: MP: Set on model?
			form.setText(PDEUIMessages.SimpleCSPage_0);
		}
		
		fBlock.createContent(managedForm);
		// TODO: MP: model change listener
		//schema.addModelChangedListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#dispose()
	 */
	public void dispose() {
		
		ISimpleCSModel simpleCSModel = (ISimpleCSModel)getModel();
		if (simpleCSModel != null) {
			// TODO: MP: model change listener
			//schema.removeModelChangedListener(this);
		}
		fColorManager.dispose();
		super.dispose();
	}
	
}
