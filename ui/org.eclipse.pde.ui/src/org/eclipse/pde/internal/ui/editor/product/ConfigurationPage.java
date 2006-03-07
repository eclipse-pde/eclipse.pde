/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;


public class ConfigurationPage extends PDEFormPage {
	
	public static final String PLUGIN_ID = "plugin-configuration"; //$NON-NLS-1$
	public static final String FEATURE_ID = "feature-configuration"; //$NON-NLS-1$

	private boolean fUseFeatures;

	public ConfigurationPage(FormEditor editor, boolean useFeatures) {
		super(editor, useFeatures ? FEATURE_ID : PLUGIN_ID, PDEUIMessages.Product_ConfigurationPage_title); 
		fUseFeatures = useFeatures;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(PDEUIMessages.Product_ConfigurationPage_title);  
		fillBody(managedForm, toolkit);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.CONFIGURATION_PAGE);
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		GridLayout layout = new GridLayout();
		layout.marginWidth = 10;
		layout.verticalSpacing = 15;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);

		// sections
		if (fUseFeatures)
			managedForm.addPart(new FeatureSection(this, body));
		else
			managedForm.addPart(new PluginSection(this, body));	
		managedForm.addPart(new ConfigurationSection(this, body));
	}
	

}
