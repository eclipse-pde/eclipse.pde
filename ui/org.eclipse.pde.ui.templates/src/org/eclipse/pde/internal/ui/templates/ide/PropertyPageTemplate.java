/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 234376
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.ide;

import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.*;

public class PropertyPageTemplate extends PDETemplateSection {
	public static final String KEY_CLASSNAME = "className"; //$NON-NLS-1$
	public static final String KEY_PAGE_NAME = "pageName"; //$NON-NLS-1$
	public static final String KEY_TARGET_CLASS = "targetClass"; //$NON-NLS-1$
	public static final String KEY_NAME_FILTER = "nameFilter"; //$NON-NLS-1$

	/**
	 * Constructor for PropertyPageTemplate.
	 */
	public PropertyPageTemplate() {
		setPageCount(1);
		createOptions();
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_PROPERTY_PAGE);
		page.setTitle(PDETemplateMessages.PropertyPageTemplate_title);
		page.setDescription(PDETemplateMessages.PropertyPageTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	private void createOptions() {
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.PropertyPageTemplate_packageName, (String) null, 0);
		addOption(KEY_CLASSNAME, PDETemplateMessages.PropertyPageTemplate_pageClass, "SamplePropertyPage", //$NON-NLS-1$
				0);
		addOption(KEY_PAGE_NAME, PDETemplateMessages.PropertyPageTemplate_pageName, PDETemplateMessages.PropertyPageTemplate_defaultPageName, 0);
		addOption(KEY_TARGET_CLASS, PDETemplateMessages.PropertyPageTemplate_targetClass, "org.eclipse.core.resources.IFile", //$NON-NLS-1$
				0);
		addOption(KEY_NAME_FILTER, PDETemplateMessages.PropertyPageTemplate_nameFilter, "*.*", //$NON-NLS-1$
				0);
	}

	/**
	 * @see PDETemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "propertyPages"; //$NON-NLS-1$
	}

	public boolean isDependentOnParentWizard() {
		return true;
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

	/**
	 * @see AbstractTemplateSection#updateModel(IProgressMonitor)
	 */
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement pageElement = factory.createElement(extension);
		pageElement.setName("page"); //$NON-NLS-1$
		pageElement.setAttribute("id", //$NON-NLS-1$
				getStringOption(KEY_PACKAGE_NAME) + ".samplePropertyPage"); //$NON-NLS-1$
		pageElement.setAttribute("name", getStringOption(KEY_PAGE_NAME)); //$NON-NLS-1$
		if (getTargetVersion() < 3.3) {
			pageElement.setAttribute("objectClass", getStringOption(KEY_TARGET_CLASS)); //$NON-NLS-1$
		} else {
			IPluginElement enabledWhen = factory.createElement(pageElement);
			pageElement.add(enabledWhen);
			enabledWhen.setName("enabledWhen"); //$NON-NLS-1$
			IPluginElement instanceOf = factory.createElement(enabledWhen);
			enabledWhen.add(instanceOf);
			instanceOf.setName("instanceof"); //$NON-NLS-1$
			instanceOf.setAttribute("value", getStringOption(KEY_TARGET_CLASS)); //$NON-NLS-1$
		}
		pageElement.setAttribute("class", //$NON-NLS-1$
				getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_CLASSNAME)); //$NON-NLS-1$
		pageElement.setAttribute("nameFilter", getStringOption(KEY_NAME_FILTER)); //$NON-NLS-1$

		extension.add(pageElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	/**
	 * @see ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.propertyPages"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList result = new ArrayList();
		result.add(new PluginReference("org.eclipse.core.resources", null, 0)); //$NON-NLS-1$
		if (schemaVersion != null)
			result.add(new PluginReference("org.eclipse.core.runtime", null, 0)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.ui", null, 0)); //$NON-NLS-1$

		return (IPluginReference[]) result.toArray(new IPluginReference[result.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#formatPackageName(java.lang.String)
	 */
	protected String getFormattedPackageName(String id) {
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".properties"; //$NON-NLS-1$
		return "properties"; //$NON-NLS-1$
	}

}
