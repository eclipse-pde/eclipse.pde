/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 Corporation and others.
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
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 244558
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.osgi;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.ui.IFieldData;

public class HelloServiceComponentTemplate extends PDETemplateSection {

	public static final String COMMAND = "command"; //$NON-NLS-1$
	public static final String WORD1 = "word1"; //$NON-NLS-1$
	public static final String WORD2 = "word2"; //$NON-NLS-1$
	public static final String WORD3 = "word3"; //$NON-NLS-1$
	private String packageName = null;

	public HelloServiceComponentTemplate() {
		setPageCount(1);
		addOption(COMMAND, PDETemplateMessages.HelloServiceComponentTemplate_commandTitle, PDETemplateMessages.HelloServiceComponentTemplate_command, 0);
		addOption(WORD1, PDETemplateMessages.HelloOSGiServiceTemplate_word1, "osgi", 0); //$NON-NLS-1$
		addOption(WORD2, PDETemplateMessages.HelloOSGiServiceTemplate_word2, "eclipse", 0); //$NON-NLS-1$
		addOption(WORD3, PDETemplateMessages.HelloOSGiServiceTemplate_word3, "equinox", 0); //$NON-NLS-1$
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_RCP_MAIL);
		page.setTitle(PDETemplateMessages.DSTemplate_pageTitle);
		page.setDescription(PDETemplateMessages.DSTemplate_pageDescription);
		wizard.addPage(page);
		markPagesAdded();
	}

	@Override
	public String getSectionId() {
		return "helloOSGiServiceComponent"; //$NON-NLS-1$
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) {
		setManifestHeader("Export-Package", getStringOption(KEY_PACKAGE_NAME)); //$NON-NLS-1$
		setManifestHeader("Service-Component", "OSGI-INF/*.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		setManifestHeader("Bundle-ActivationPolicy", "lazy"); //$NON-NLS-1$ //$NON-NLS-2$
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

	@Override
	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String packageName = getFormattedPackageName(data.getId());
		initializeOption(KEY_PACKAGE_NAME, packageName);
		this.packageName = getFormattedPackageName(data.getId());
	}

	@Override
	public void initializeFields(IPluginModelBase model) {
		String id = model.getPluginBase().getId();
		String packageName = getFormattedPackageName(id);
		initializeOption(KEY_PACKAGE_NAME, packageName);
		this.packageName = getFormattedPackageName(id);
	}

	@Override
	public String getStringOption(String name) {
		if (name.equals(KEY_PACKAGE_NAME)) {
			return packageName;
		}
		return super.getStringOption(name);
	}
}
