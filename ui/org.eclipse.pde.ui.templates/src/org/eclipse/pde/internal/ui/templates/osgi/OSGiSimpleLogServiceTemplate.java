/*******************************************************************************
 *  Copyright (c) 2006, 2007 IBM Corporation and others.
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
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.ui.IFieldData;

public class OSGiSimpleLogServiceTemplate extends PDETemplateSection {

	public static final String START_LOG_MESSAGE = "startLogMessage"; //$NON-NLS-1$
	public static final String STOP_LOG_MESSAGE = "stopLogMessage"; //$NON-NLS-1$

	private String packageName;

	public OSGiSimpleLogServiceTemplate() {
		setPageCount(1);
		addOption(START_LOG_MESSAGE, PDETemplateMessages.OSGiSimpleLogServiceTemplate_startLogMessage, PDETemplateMessages.OSGiSimpleLogServiceTemplate_logMessage, 0);
		addOption(STOP_LOG_MESSAGE, PDETemplateMessages.OSGiSimpleLogServiceTemplate_stopLogMessage, PDETemplateMessages.OSGiSimpleLogServiceTemplate_logMessage, 0);
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_RCP_MAIL);
		page.setTitle(PDETemplateMessages.OSGiSimpleLogServiceTemplate_pageTitle);
		page.setDescription(PDETemplateMessages.OSGiSimpleLogServiceTemplate_pageDescription);
		wizard.addPage(page);
		markPagesAdded();
	}

	@Override
	public String getSectionId() {
		return "OSGiSimpleLogService"; //$NON-NLS-1$
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
		String id = data.getId();
		this.packageName = getFormattedPackageName(id);
		initializeOption(KEY_PACKAGE_NAME, packageName);
	}

	@Override
	public void initializeFields(IPluginModelBase model) {
		String id = model.getPluginBase().getId();
		this.packageName = getFormattedPackageName(id);
		initializeOption(KEY_PACKAGE_NAME, packageName);
	}

	@Override
	public String getStringOption(String name) {
		if (name.equals(KEY_PACKAGE_NAME)) {
			return packageName;
		}
		return super.getStringOption(name);
	}
}
