/*******************************************************************************
 * Copyright (c) 2008, 2022 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     IBM Corporation - Bug 265935: Product editor opens on the wrong page
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class DependenciesPage extends PDEFormPage {

	static final String PLUGIN_ID = "plugin-dependencies"; //$NON-NLS-1$
	static final String FEATURE_ID = "feature-dependencies"; //$NON-NLS-1$

	private boolean fUseFeatures;
	private PluginSection fPluginSection = null;

	public DependenciesPage(FormEditor editor, boolean useFeatures) {
		super(editor, useFeatures ? FEATURE_ID : PLUGIN_ID, PDEUIMessages.Product_DependenciesPage_title);
		fUseFeatures = useFeatures;
	}

	@Override
	protected String getHelpResource() {
		return IHelpContextIds.CONFIGURATION_PAGE;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REQ_PLUGINS_OBJ));
		form.setText(PDEUIMessages.Product_DependenciesPage_title);
		fillBody(managedForm);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.CONFIGURATION_PAGE);
	}

	private void fillBody(IManagedForm managedForm) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(false, 1));

		// sections
		if (fUseFeatures)
			managedForm.addPart(new FeatureSection(this, body));
		else
			managedForm.addPart(fPluginSection = new PluginSection(this, body));
	}

	public boolean includeOptionalDependencies() {
		return fPluginSection != null && fPluginSection.includeOptionalDependencies();
	}
}
