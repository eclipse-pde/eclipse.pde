/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.wizard.*;

public class HelloWorldTemplate extends PDETemplateSection {
	public static final String KEY_CLASS_NAME = "className"; //$NON-NLS-1$
	public static final String KEY_MESSAGE = "message"; //$NON-NLS-1$
	public static final String KEY_ADD_TO_PERSPECTIVE = "addToPerspective"; //$NON-NLS-1$
	public static final String CLASS_NAME = "SampleAction"; //$NON-NLS-1$

	private static final String KEY_TITLE = "HelloWorldTemplate.title"; //$NON-NLS-1$
	private static final String KEY_DESC = "HelloWorldTemplate.desc"; //$NON-NLS-1$
	private static final String KEY_PACKAGE_LABEL =
		"HelloWorldTemplate.packageName"; //$NON-NLS-1$
	private static final String KEY_CLASS_LABEL = "HelloWorldTemplate.className"; //$NON-NLS-1$
	private static final String KEY_TEXT_LABEL = "HelloWorldTemplate.messageText"; //$NON-NLS-1$
	private static final String KEY_DEFAULT_MESSAGE =
		"HelloWorldTemplate.defaultMessage"; //$NON-NLS-1$
	private static final String KEY_SAMPLE_ACTION_SET = "HelloWorldTemplate.sampleActionSet";		 //$NON-NLS-1$
	private static final String KEY_SAMPLE_MENU = "HelloWorldTemplate.sampleMenu"; //$NON-NLS-1$
	private static final String KEY_SAMPLE_ACTION = "HelloWorldTemplate.sampleAction"; //$NON-NLS-1$
	
	/**
	 * Constructor for HelloWorldTemplate.
	 */
	public HelloWorldTemplate() {
		setPageCount(1);
		createOptions();
	}

	public String getSectionId() {
		return "helloWorld"; //$NON-NLS-1$
	}
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}
	
	private void createOptions() {
		addOption(
			KEY_PACKAGE_NAME,
			PDEPlugin.getResourceString(KEY_PACKAGE_LABEL),
			(String) null,
			0);
		addOption(
			KEY_CLASS_NAME,
			PDEPlugin.getResourceString(KEY_CLASS_LABEL),
			CLASS_NAME,
			0);
		addOption(
			KEY_MESSAGE,
			PDEPlugin.getResourceString(KEY_TEXT_LABEL),
			PDEPlugin.getResourceString(KEY_DEFAULT_MESSAGE),
			0);
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_HELLO_WORLD);
		page.setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		page.setDescription(PDEPlugin.getResourceString(KEY_DESC));
		wizard.addPage(page);
		markPagesAdded();
	}

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

	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.actionSets"; //$NON-NLS-1$
	}

	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.actionSets", true); //$NON-NLS-1$
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement setElement = factory.createElement(extension);
		setElement.setName("actionSet"); //$NON-NLS-1$
		setElement.setAttribute("id", plugin.getId() + ".actionSet"); //$NON-NLS-1$ //$NON-NLS-2$
		setElement.setAttribute("label", PDEPlugin.getResourceString(KEY_SAMPLE_ACTION_SET)); //$NON-NLS-1$
		setElement.setAttribute("visible", "true"); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement menuElement = factory.createElement(setElement);
		menuElement.setName("menu"); //$NON-NLS-1$
		menuElement.setAttribute("label", PDEPlugin.getResourceString(KEY_SAMPLE_MENU)); //$NON-NLS-1$
		menuElement.setAttribute("id", "sampleMenu"); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement groupElement = factory.createElement(menuElement);
		groupElement.setName("separator"); //$NON-NLS-1$
		groupElement.setAttribute("name", "sampleGroup"); //$NON-NLS-1$ //$NON-NLS-2$
		menuElement.add(groupElement);
		setElement.add(menuElement);

		String fullClassName =
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_CLASS_NAME); //$NON-NLS-1$

		IPluginElement actionElement = factory.createElement(setElement);
		actionElement.setName("action"); //$NON-NLS-1$
		actionElement.setAttribute("id", fullClassName); //$NON-NLS-1$
		actionElement.setAttribute("label", PDEPlugin.getResourceString(KEY_SAMPLE_ACTION)); //$NON-NLS-1$
		actionElement.setAttribute("menubarPath", "sampleMenu/sampleGroup"); //$NON-NLS-1$ //$NON-NLS-2$
		actionElement.setAttribute("toolbarPath", "sampleGroup"); //$NON-NLS-1$ //$NON-NLS-2$
		actionElement.setAttribute("icon", "icons/sample.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		actionElement.setAttribute("tooltip", PDEPlugin.getResourceString(KEY_DEFAULT_MESSAGE)); //$NON-NLS-1$
		actionElement.setAttribute("class", fullClassName); //$NON-NLS-1$
		setElement.add(actionElement);
		extension.add(setElement);
		if (!extension.isInTheModel())
			plugin.add(extension);			
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.ITemplateSection#getFoldersToInclude()
	 */
	public String[] getNewFiles() {
		return new String[] {"icons/"}; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#formatPackageName(java.lang.String)
     */
    protected String getFormattedPackageName(String id) {
        String packageName = super.getFormattedPackageName(id);
        if (packageName.length() != 0)
            return packageName + ".actions"; //$NON-NLS-1$
        return "actions"; //$NON-NLS-1$
    }
}
