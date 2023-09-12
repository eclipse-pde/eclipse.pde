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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.wizards.WizardElement;
import org.eclipse.swt.widgets.Composite;

/**
 * Modifies the default new project creation page, disabling UI elements based on a pre-selected template.
 */
public class NewProjectCreationFromTemplatePage extends NewProjectCreationPage {

	private final WizardElement fTemplateWizard;

	public NewProjectCreationFromTemplatePage(String pageName, AbstractFieldData data, IStructuredSelection selection, WizardElement templateWizard) {
		super(pageName, data, false, selection);
		fTemplateWizard = templateWizard;
	}

	@Override
	protected void createFormatGroup(Composite container) {
		super.createFormatGroup(container);
		Boolean osgiFlag = TemplateWizardHelper.getFlag(fTemplateWizard, TemplateWizardHelper.FLAG_OSGI);
		if (osgiFlag != null) {
			boolean isOSGi = osgiFlag.booleanValue();
			fEclipseButton.setSelection(!isOSGi);
			fEclipseButton.setEnabled(!isOSGi);
			fOSGIButton.setSelection(isOSGi);
			fOSGIButton.setEnabled(isOSGi);
			fOSGiCombo.setEnabled(isOSGi);
		}
	}

	@Override
	protected void createProjectTypeGroup(Composite container) {
		super.createProjectTypeGroup(container);
		Boolean javaFlag = TemplateWizardHelper.getFlag(fTemplateWizard, TemplateWizardHelper.FLAG_JAVA);
		if (javaFlag != null) {
			boolean isJava = javaFlag.booleanValue();
			fJavaButton.setSelection(isJava);
			fJavaButton.setEnabled(false);
			fSourceLabel.setEnabled(isJava);
			fSourceText.setEnabled(isJava);
			fOutputlabel.setEnabled(isJava);
			fOutputText.setEnabled(isJava);
		}
	}
}
