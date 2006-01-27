/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.TemplateOption;

public class PreferencePageTemplate extends PDETemplateSection {
	private static final String KEY_PAGE_NAME = "pageName"; //$NON-NLS-1$
	private static final String KEY_PAGE_CLASS_NAME = "pageClassName"; //$NON-NLS-1$
	private static final String KEY_PLUGIN_CLASS_NAME = "pluginClassName"; //$NON-NLS-1$
	private static final String KEY_FULL_PLUGIN_CLASS_NAME = "fullPluginClassName"; //$NON-NLS-1$

	private String mainClassName;
	
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
		addOption(
			KEY_PACKAGE_NAME,
			PDEUIMessages.PreferencePageTemplate_packageName,
			(String) null,
			0);
		addOption(
			KEY_PAGE_CLASS_NAME,
			PDEUIMessages.PreferencePageTemplate_className,
			"SamplePreferencePage", //$NON-NLS-1$
			0);
		addOption(
			KEY_PAGE_NAME,
			PDEUIMessages.PreferencePageTemplate_pageName,
			PDEUIMessages.PreferencePageTemplate_defaultPageName,
			0);
	}

	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id)); 
		mainClassName = id + ".PreferenceClass"; //$NON-NLS-1$
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId)); 
		if (model instanceof IPluginModel) {
			IPlugin plugin = (IPlugin) model.getPluginBase();
			mainClassName = plugin.getClassName();
		} else if (model instanceof IFragmentModel) {
			IFragment fragment = (IFragment) model.getPluginBase();
			String pluginPluginId = fragment.getPluginId();
			ModelEntry entry = PDECore.getDefault().getModelManager()
					.findEntry(pluginPluginId);
			if (entry != null) {
				IPluginModelBase pluginModelBase = entry.getActiveModel();
				if (pluginModelBase instanceof IPluginModel) {
					IPlugin plugin = (IPlugin) pluginModelBase.getPluginBase();
					mainClassName = plugin.getClassName();
				}
			}
		}
		if (mainClassName == null) {
			mainClassName = pluginId + ".PreferenceClass"; //$NON-NLS-1$
		}
	}

	protected String getTemplateDirectory() {
		String schemaVersion = model.getPluginBase().getSchemaVersion();
		if (PDECore.getDefault().getModelManager().isOSGiRuntime()
				&& schemaVersion != null)
			return "templates_" + schemaVersion; //$NON-NLS-1$
		return "templates"; //$NON-NLS-1$
	}
	public String getReplacementString(String fileName, String key) {
		if (key.equals(KEY_FULL_PLUGIN_CLASS_NAME))
			return mainClassName;
		if (key.equals(KEY_PLUGIN_CLASS_NAME))
			return getPluginClassName();
		
		return super.getReplacementString(fileName, key);
	}

	private String getPluginClassName() {
		int dot = mainClassName.lastIndexOf('.');
		if (dot != -1) {
			return mainClassName.substring(dot + 1);
		}
		return mainClassName;
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
		deps[1] = new PluginReference("org.eclipse.ui", null, 0);		 //$NON-NLS-1$
		return deps;
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_PREFERENCE_PAGE);
		page.setTitle(PDEUIMessages.PreferencePageTemplate_title);
		page.setDescription(PDEUIMessages.PreferencePageTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		} else
			resetPageState();
	}

	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.preferencePages"; //$NON-NLS-1$
	}

	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getPluginFactory();

		String fullClassName =
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_PAGE_CLASS_NAME); //$NON-NLS-1$

		IPluginElement pageElement = factory.createElement(extension);
		pageElement.setName("page"); //$NON-NLS-1$
		pageElement.setAttribute("id", fullClassName); //$NON-NLS-1$
		pageElement.setAttribute("name", getStringOption(KEY_PAGE_NAME)); //$NON-NLS-1$
		pageElement.setAttribute("class", fullClassName); //$NON-NLS-1$
		extension.add(pageElement);
		if (!extension.isInTheModel())
			plugin.add(extension);

		if (PDECore.getDefault().getModelManager().isOSGiRuntime()
				&& model.getPluginBase().getSchemaVersion() != null) {
			IPluginExtension extension2 = createExtension("org.eclipse.core.runtime.preferences", true); //$NON-NLS-1$
			IPluginElement prefElement = factory.createElement(extension);
			prefElement.setName("initializer"); //$NON-NLS-1$
			prefElement.setAttribute("class", getStringOption(KEY_PACKAGE_NAME)+".PreferenceInitializer"); //$NON-NLS-1$ //$NON-NLS-2$
			extension2.add(prefElement);
			if (!extension2.isInTheModel())
				plugin.add(extension2);
		}
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
