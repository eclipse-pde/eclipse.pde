/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class EnvironmentPage extends AbstractTargetPage {
	
	private EnvironmentSection fEnvSection;
	
	public static final String PAGE_ID = "environment"; //$NON-NLS-1$

	public EnvironmentPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.EnvironmentPage_title); 
	}
	
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setText(PDEUIMessages.EnvironmentPage_title);
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_TARGET_ENVIRONMENT));
		FormToolkit toolkit = managedForm.getToolkit();
		fillBody(managedForm, toolkit);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.ENVIRONMENT_PAGE);
	}
	
	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(false, 2));
		
		managedForm.addPart(fEnvSection = new EnvironmentSection(this, body));
		managedForm.addPart(new JRESection(this, body));
		managedForm.addPart(new ArgumentsSection(this, body));
		managedForm.addPart(new ImplicitDependenciesSection(this, body));
	}
	
	protected void updateChoices() {
		fEnvSection.updateChoices();
	}

	protected String getHelpResource() {
		return "/org.eclipse.pde.doc.user/guide/tools/editors/target_definition_editor/environment.htm"; //$NON-NLS-1$
	}
	
}
