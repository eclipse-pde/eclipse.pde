/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.*;

public class PropertyPageTemplate extends PDETemplateSection {
	public static final String KEY_CLASSNAME = "className"; //$NON-NLS-1$
	public static final String KEY_PAGE_NAME = "pageName"; //$NON-NLS-1$
	public static final String KEY_TARGET_CLASS = "targetClass"; //$NON-NLS-1$
	public static final String KEY_NAME_FILTER = "nameFilter"; //$NON-NLS-1$

	private static final String NL_TITLE = "PropertyPageTemplate.title"; //$NON-NLS-1$
	private static final String NL_DESC = "PropertyPageTemplate.desc"; //$NON-NLS-1$
	private static final String NL_PACKAGE_NAME =
		"PropertyPageTemplate.packageName"; //$NON-NLS-1$
	private static final String NL_PAGE_CLASS = "PropertyPageTemplate.pageClass"; //$NON-NLS-1$
	private static final String NL_PAGE_NAME = "PropertyPageTemplate.pageName"; //$NON-NLS-1$
	private static final String NL_DEFAULT_PAGE_NAME =
		"PropertyPageTemplate.defaultPageName"; //$NON-NLS-1$
	private static final String NL_TARGET_CLASS =
		"PropertyPageTemplate.targetClass"; //$NON-NLS-1$
	private static final String NL_NAME_FILTER = "PropertyPageTemplate.nameFilter"; //$NON-NLS-1$

	/**
	 * Constructor for PropertyPageTemplate.
	 */
	public PropertyPageTemplate() {
		setPageCount(1);
		createOptions();
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_PROPERTY_PAGE);
		page.setTitle(PDEPlugin.getResourceString(NL_TITLE));
		page.setDescription(PDEPlugin.getResourceString(NL_DESC));
		wizard.addPage(page);
		markPagesAdded();
	}

	private void createOptions() {
		addOption(
			KEY_PACKAGE_NAME,
			PDEPlugin.getResourceString(NL_PACKAGE_NAME),
			(String) null,
			0);
		addOption(
			KEY_CLASSNAME,
			PDEPlugin.getResourceString(NL_PAGE_CLASS),
			"SamplePropertyPage", //$NON-NLS-1$
			0);
		addOption(
			KEY_PAGE_NAME,
			PDEPlugin.getResourceString(NL_PAGE_NAME),
			PDEPlugin.getResourceString(NL_DEFAULT_PAGE_NAME),
			0);
		addOption(
			KEY_TARGET_CLASS,
			PDEPlugin.getResourceString(NL_TARGET_CLASS),
			"org.eclipse.core.resources.IFile", //$NON-NLS-1$
			0);
		addOption(
			KEY_NAME_FILTER,
			PDEPlugin.getResourceString(NL_NAME_FILTER),
			"*.*", //$NON-NLS-1$
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
		initializeOption(KEY_PACKAGE_NAME, id + ".properties"); //$NON-NLS-1$
	}

	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".properties"); //$NON-NLS-1$
	}

	/**
	 * @see GenericTemplateSection#validateOptions(TemplateOption)
	 */
	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		} else {
			validateContainerPage(source);
		}
	}

	private void validateContainerPage(TemplateOption source) {
		TemplateOption[] allPageOptions = getOptions(0);
		for (int i = 0; i < allPageOptions.length; i++) {
			TemplateOption nextOption = allPageOptions[i];
			if (nextOption.isRequired() && nextOption.isEmpty()) {
				flagMissingRequiredOption(nextOption);
				return;
			}
		}
		resetPageState();
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
		pageElement.setAttribute(
			"id", //$NON-NLS-1$
			getStringOption(KEY_PACKAGE_NAME) + ".samplePropertyPage"); //$NON-NLS-1$
		pageElement.setAttribute("name", getStringOption(KEY_PAGE_NAME)); //$NON-NLS-1$
		pageElement.setAttribute("objectClass", getStringOption(KEY_TARGET_CLASS)); //$NON-NLS-1$
		pageElement.setAttribute(
			"class", //$NON-NLS-1$
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
			result.add(new PluginReference("org.eclipse.core.runtime.compatibility", null, 0));		 //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.ui", null, 0));		 //$NON-NLS-1$
			
		return (IPluginReference[])result.toArray(new IPluginReference[result.size()]);
	}

}
