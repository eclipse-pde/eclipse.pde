/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;


public class OverviewPage extends PDEFormPage {
	
	public static final String PAGE_ID = "overview"; //$NON-NLS-1$

	public OverviewPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.OverviewPage_title); 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(PDEUIMessages.OverviewPage_title);  
		fillBody(managedForm, toolkit);
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		GridLayout layout = new GridLayout();
		layout.marginBottom = 5;
		layout.marginTop = 5;
		layout.marginLeft = 10;
		layout.marginRight = 10;
		layout.verticalSpacing = 5;
		body.setLayout(layout);
		
		managedForm.addPart(new TargetDefinitionSection(this, body));
		managedForm.addPart(new LocationsSection(this, body));
		managedForm.addPart(new ContentSection(this, body));
	}

}
