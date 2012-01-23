/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.ide;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.PluginReference;

public class PreferencePageTemplate extends PDETemplateSection {
	private static final String KEY_PAGE_NAME = "pageName"; //$NON-NLS-1$
	private static final String KEY_PAGE_CLASS_NAME = "pageClassName"; //$NON-NLS-1$

	public PreferencePageTemplate() {
		setPageCount(1);
		createOptions();
	}

	public String getSectionId() {
		return "preferences"; //$NON-NLS-1$
	}

	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	private void createOptions() {
		// first page
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.PreferencePageTemplate_packageName, (String) null, 0);
		addOption(KEY_PAGE_CLASS_NAME, PDETemplateMessages.PreferencePageTemplate_className, "SamplePreferencePage", //$NON-NLS-1$
				0);
		addOption(KEY_PAGE_NAME, PDETemplateMessages.PreferencePageTemplate_pageName, PDETemplateMessages.PreferencePageTemplate_defaultPageName, 0);
	}

	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id));
	}

	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId));
	}

	protected String getTemplateDirectory() {
		String schemaVersion = model.getPluginBase().getSchemaVersion();
		return "templates_" + (schemaVersion == null ? "3.0" : schemaVersion); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean isDependentOnParentWizard() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		if (schemaVersion == null)
			return super.getDependencies(schemaVersion);
		PluginReference[] deps = new PluginReference[2];
		deps[0] = new PluginReference("org.eclipse.core.runtime", null, 0); //$NON-NLS-1$
		deps[1] = new PluginReference("org.eclipse.ui", null, 0); //$NON-NLS-1$
		return deps;
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_PREFERENCE_PAGE);
		page.setTitle(PDETemplateMessages.PreferencePageTemplate_title);
		page.setDescription(PDETemplateMessages.PreferencePageTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.preferencePages"; //$NON-NLS-1$
	}

	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getPluginFactory();

		String fullClassName = getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_PAGE_CLASS_NAME); //$NON-NLS-1$

		IPluginElement pageElement = factory.createElement(extension);
		pageElement.setName("page"); //$NON-NLS-1$
		pageElement.setAttribute("id", fullClassName); //$NON-NLS-1$
		pageElement.setAttribute("name", getStringOption(KEY_PAGE_NAME)); //$NON-NLS-1$
		pageElement.setAttribute("class", fullClassName); //$NON-NLS-1$
		extension.add(pageElement);
		if (!extension.isInTheModel())
			plugin.add(extension);

		IPluginExtension extension2 = createExtension("org.eclipse.core.runtime.preferences", true); //$NON-NLS-1$
		IPluginElement prefElement = factory.createElement(extension);
		prefElement.setName("initializer"); //$NON-NLS-1$
		prefElement.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + ".PreferenceInitializer"); //$NON-NLS-1$ //$NON-NLS-2$
		extension2.add(prefElement);
		if (!extension2.isInTheModel())
			plugin.add(extension2);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#formatPackageName(java.lang.String)
	 */
	protected String getFormattedPackageName(String id) {
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".preferences"; //$NON-NLS-1$
		return "preferences"; //$NON-NLS-1$
	}
}
