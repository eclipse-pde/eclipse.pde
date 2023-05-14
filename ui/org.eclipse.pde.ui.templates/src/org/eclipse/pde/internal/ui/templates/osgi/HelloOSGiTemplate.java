/*******************************************************************************
 *  Copyright (c) 2005, 2007 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.osgi;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.templates.IHelpContextIds;
import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.internal.ui.templates.PDETemplateSection;

public class HelloOSGiTemplate extends PDETemplateSection {

	public static final String KEY_START_MESSAGE = "startMessage"; //$NON-NLS-1$
	public static final String KEY_STOP_MESSAGE = "stopMessage"; //$NON-NLS-1$
	public static final String KEY_APPLICATION_CLASS = "applicationClass"; //$NON-NLS-1$

	public HelloOSGiTemplate() {
		setPageCount(1);
		addOption(KEY_START_MESSAGE, PDETemplateMessages.HelloOSGiTemplate_startMessage, PDETemplateMessages.HelloOSGiTemplate_hello, 0);
		addOption(KEY_STOP_MESSAGE, PDETemplateMessages.HelloOSGiTemplate_stopMessage, PDETemplateMessages.HelloOSGiTemplate_goodbye, 0);
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_RCP_MAIL);
		page.setTitle(PDETemplateMessages.HelloOSGiTemplate_pageTitle);
		page.setDescription(PDETemplateMessages.HelloOSGiTemplate_pageDescription);
		wizard.addPage(page);
		markPagesAdded();
	}

	@Override
	public String getSectionId() {
		return "helloOSGi"; //$NON-NLS-1$
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) { // do nothing

	}

	@Override
	public String getUsedExtensionPoint() {
		return null;
	}

	@Override
	public boolean isDependentOnParentWizard() {
		return true;
	}

	@Override
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		return new IPluginReference[0];
	}
}
