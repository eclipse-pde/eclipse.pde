/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.internal.ui.wizards.WizardElement;
import org.eclipse.swt.widgets.Composite;

/**
 * Modifies the default new project content page, disabling UI elements based on a pre-selected template.
 */
public class PluginContentFromTemplatePage extends PluginContentPage {

	private WizardElement fTemplateWizard;

	public PluginContentFromTemplatePage(String pageName, IProjectProvider provider, NewProjectCreationPage page, AbstractFieldData data, WizardElement templateWizard) {
		super(pageName, provider, page, data);
		fTemplateWizard = templateWizard;
	}

	@Override
	protected void createRCPQuestion(Composite parent, int horizontalSpan) {
		super.createRCPQuestion(parent, horizontalSpan);
		Boolean rcpFlag = TemplateWizardHelper.getFlag(fTemplateWizard, TemplateWizardHelper.FLAG_RCP);
		if (rcpFlag != null) {
			boolean isRCP = rcpFlag.booleanValue();
			fYesButton.setSelection(isRCP);
			fYesButton.setEnabled(false);
			fNoButton.setSelection(!isRCP);
			fNoButton.setEnabled(false);
		}
	}

	@Override
	protected void createPluginClassGroup(Composite container) {
		super.createPluginClassGroup(container);
		Boolean activatorFlag = TemplateWizardHelper.getFlag(fTemplateWizard, TemplateWizardHelper.FLAG_ACTIVATOR);
		if (activatorFlag != null) {
			fGenerateActivator.setSelection(activatorFlag.booleanValue());
			fGenerateActivator.setEnabled(false);
			fNameText.setEnabled(activatorFlag.booleanValue());
		}
		Boolean uiFlag = TemplateWizardHelper.getFlag(fTemplateWizard, TemplateWizardHelper.FLAG_UI);
		if (uiFlag != null) {
			fUIPlugin.setSelection(uiFlag.booleanValue());
			fUIPlugin.setEnabled(false);
		}
	}
}
