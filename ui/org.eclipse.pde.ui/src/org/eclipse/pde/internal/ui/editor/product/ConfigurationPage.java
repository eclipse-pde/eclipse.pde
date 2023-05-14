/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others
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
 *     Code 9 Corporation - additional enhancements
 *     Bartosz Michalik (bartosz.michalik@gmail.com)
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Configuration page of the product editor
 *
 * @see ProductEditor
 * @see ConfigurationSection
 * @see PluginConfigurationSection
 * @see PropertiesSection
 */
public class ConfigurationPage extends PDEFormPage {
	public static final String PLUGIN_ID = "plugin-configuration"; //$NON-NLS-1$

	public ConfigurationPage(ProductEditor editor, boolean useFeatures) {
		super(editor, PLUGIN_ID, PDEUIMessages.ConfigurationPageMock_pageTitle);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ));
		form.setText(PDEUIMessages.ConfigurationPageMock_pageTitle);
		fillBody(managedForm, toolkit);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.CONFIGURATION_PAGE);
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(false, 2));
		managedForm.addPart(new ConfigurationSection(this, body));
		if (TargetPlatformHelper.getTargetVersion() > 3.4) {
			managedForm.addPart(new PluginConfigurationSection(this, body));
		}
		managedForm.addPart(new PropertiesSection(this, body));
	}
}
