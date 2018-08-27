/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * SplashPage
 *
 */
public class SplashPage extends PDEFormPage {

	public static final String PAGE_ID = "splash"; //$NON-NLS-1$

	public SplashPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.SplashPage_splashName);
	}

	@Override
	protected String getHelpResource() {
		return IHelpContextIds.SPLASH_PAGE;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_IMAGE_APPLICATION));
		form.setText(PDEUIMessages.SplashPage_splashName);
		fillBody(managedForm, toolkit);
		// TODO: MP: SPLASH: Update help context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.SPLASH_PAGE);
	}

	/**
	 * @param managedForm
	 * @param toolkit
	 */
	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(false, 1));
		// Sections
		managedForm.addPart(new SplashLocationSection(this, body));
		managedForm.addPart(new SplashConfigurationSection(this, body));
	}

}
