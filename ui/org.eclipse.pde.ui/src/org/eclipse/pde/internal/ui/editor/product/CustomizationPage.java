/*******************************************************************************
 * Copyright (c) 2014, 2015 Rapicorp Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class CustomizationPage extends PDEFormPage {

	public static final String PAGE_ID = "plugin-customization"; //$NON-NLS-1$

	public CustomizationPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.CustomizationPage_title);
	}

	@Override
	protected String getHelpResource() {
		return IHelpContextIds.CUSTOMIZATION_PAGE;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ));
		form.setText(PDEUIMessages.CustomizationPage_title);
		fillBody(managedForm, toolkit);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.CUSTOMIZATION_PAGE);
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormTableWrapLayout(false, 1));
		managedForm.addPart(new PreferencesSection(this, body));
		if (TargetPlatformHelper.getTargetVersion() > 3.5) {
			managedForm.addPart(new CSSSection(this, body));
		}
	}
}
