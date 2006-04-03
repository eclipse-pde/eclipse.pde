/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.TemplateOption;

public class HelloWorldTemplate extends PDETemplateSection {
	public static final String KEY_CLASS_NAME = "className"; //$NON-NLS-1$
	public static final String KEY_MESSAGE = "message"; //$NON-NLS-1$
	public static final String CLASS_NAME = "SampleAction"; //$NON-NLS-1$

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
			PDEUIMessages.HelloWorldTemplate_packageName,
			(String) null,
			0);
		addOption(
			KEY_CLASS_NAME,
			PDEUIMessages.HelloWorldTemplate_className,
			CLASS_NAME,
			0);
		addOption(
			KEY_MESSAGE,
			PDEUIMessages.HelloWorldTemplate_messageText,
			PDEUIMessages.HelloWorldTemplate_defaultMessage,
			0);
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_HELLO_WORLD);
		page.setTitle(PDEUIMessages.HelloWorldTemplate_title);
		page.setDescription(PDEUIMessages.HelloWorldTemplate_desc);
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
		setElement.setAttribute("label", PDEUIMessages.HelloWorldTemplate_sampleActionSet); //$NON-NLS-1$
		setElement.setAttribute("visible", "true"); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement menuElement = factory.createElement(setElement);
		menuElement.setName("menu"); //$NON-NLS-1$
		menuElement.setAttribute("label", PDEUIMessages.HelloWorldTemplate_sampleMenu); //$NON-NLS-1$
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
		actionElement.setAttribute("label", PDEUIMessages.HelloWorldTemplate_sampleAction); //$NON-NLS-1$
		actionElement.setAttribute("menubarPath", "sampleMenu/sampleGroup"); //$NON-NLS-1$ //$NON-NLS-2$
		actionElement.setAttribute("toolbarPath", "sampleGroup"); //$NON-NLS-1$ //$NON-NLS-2$
		actionElement.setAttribute("icon", "icons/sample.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		actionElement.setAttribute("tooltip", PDEUIMessages.HelloWorldTemplate_defaultMessage); //$NON-NLS-1$
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
