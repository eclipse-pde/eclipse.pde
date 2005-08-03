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

import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.*;

public class PopupMenuTemplate extends PDETemplateSection {

	public static final String KEY_TARGET_OBJECT = "objectClass"; //$NON-NLS-1$
	public static final String KEY_NAME_FILTER = "nameFilter"; //$NON-NLS-1$
	public static final String KEY_SUBMENU_LABEL = "subMenuLabel"; //$NON-NLS-1$
	public static final String KEY_ACTION_LABEL = "actionLabel"; //$NON-NLS-1$
	public static final String KEY_ACTION_CLASS = "actionClass"; //$NON-NLS-1$
	public static final String KEY_SELECTION = "selection"; //$NON-NLS-1$

	/**
	 * Constructor for PropertyPageTemplate.
	 */
	public PopupMenuTemplate() {
		setPageCount(1);
		createOptions();
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_POPUP_MENU);
		page.setTitle(PDEUIMessages.PopupMenuTemplate_title);
		page.setDescription(PDEUIMessages.PopupMenuTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	private void createOptions() {
		addOption(
			KEY_TARGET_OBJECT,
			PDEUIMessages.PopupMenuTemplate_targetClass,
			"org.eclipse.core.resources.IFile", //$NON-NLS-1$
			0);
		addOption(
			KEY_NAME_FILTER,
			PDEUIMessages.PopupMenuTemplate_nameFilter,
			"plugin.xml", //$NON-NLS-1$
			0);
		addOption(
			KEY_SUBMENU_LABEL,
			PDEUIMessages.PopupMenuTemplate_submenuName,
			PDEUIMessages.PopupMenuTemplate_defaultSubmenuName,
			0);
		addOption(
			KEY_ACTION_LABEL,
			PDEUIMessages.PopupMenuTemplate_actionLabel,
			PDEUIMessages.PopupMenuTemplate_defaultActionName,
			0);
		addOption(
			KEY_PACKAGE_NAME,
			PDEUIMessages.PopupMenuTemplate_packageName,
			(String) null,
			0);
		addOption(
			KEY_ACTION_CLASS,
			PDEUIMessages.PopupMenuTemplate_actionClass,
			PDEUIMessages.PopupMenuTemplate_newAction, 
			0);
		addOption(
			KEY_SELECTION,
			PDEUIMessages.PopupMenuTemplate_enabledFor,
			new String[][] {
				{ "singleSelection", PDEUIMessages.PopupMenuTemplate_singleSelection}, //$NON-NLS-1$
				{
				"multipleSelection", PDEUIMessages.PopupMenuTemplate_multipleSelection //$NON-NLS-1$
				}
		}, "singleSelection", 0); //$NON-NLS-1$
	}
	/**
	 * @see PDETemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "popupMenus"; //$NON-NLS-1$
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

		IPluginElement objectContributionElement = factory.createElement(extension);
		objectContributionElement.setName("objectContribution"); //$NON-NLS-1$
		objectContributionElement.setAttribute(
			"objectClass", //$NON-NLS-1$
			getStringOption(KEY_TARGET_OBJECT));
		objectContributionElement.setAttribute(
			"nameFilter", //$NON-NLS-1$
			getStringOption(KEY_NAME_FILTER));
		objectContributionElement.setAttribute(
			"id", //$NON-NLS-1$
			model.getPluginBase().getId() + ".contribution1"); //$NON-NLS-1$

		IPluginElement menuElement = factory.createElement(objectContributionElement);
		menuElement.setName("menu"); //$NON-NLS-1$
		menuElement.setAttribute("label", getStringOption(KEY_SUBMENU_LABEL)); //$NON-NLS-1$
		menuElement.setAttribute("path", "additions"); //$NON-NLS-1$ //$NON-NLS-2$
		menuElement.setAttribute("id", model.getPluginBase().getId() + ".menu1"); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement separatorElement = factory.createElement(menuElement);
		separatorElement.setName("separator"); //$NON-NLS-1$
		separatorElement.setAttribute("name", "group1"); //$NON-NLS-1$ //$NON-NLS-2$
		menuElement.add(separatorElement);
		objectContributionElement.add(menuElement);

		IPluginElement actionElement = factory.createElement(objectContributionElement);
		actionElement.setName("action"); //$NON-NLS-1$
		actionElement.setAttribute("label", getStringOption(KEY_ACTION_LABEL)); //$NON-NLS-1$
		actionElement.setAttribute(
			"class", //$NON-NLS-1$
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_ACTION_CLASS)); //$NON-NLS-1$
		actionElement.setAttribute(
			"menubarPath", //$NON-NLS-1$
			model.getPluginBase().getId() + ".menu1/group1"); //$NON-NLS-1$
		actionElement.setAttribute(
			"enablesFor", //$NON-NLS-1$
			getValue(KEY_SELECTION).toString().equals("singleSelection") //$NON-NLS-1$
				? "1" //$NON-NLS-1$
				: "multiple"); //$NON-NLS-1$
		actionElement.setAttribute("id", model.getPluginBase().getId() + ".newAction"); //$NON-NLS-1$ //$NON-NLS-2$
		objectContributionElement.add(actionElement);

		extension.add(objectContributionElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	/**
	 * @see ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.popupMenus"; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#formatPackageName(java.lang.String)
     */
	protected String getFormattedPackageName(String id) {
	    String packageName = super.getFormattedPackageName(id);
	    if (packageName.length() !=0)
	        return packageName + ".popup.actions"; //$NON-NLS-1$
	    return "popup.actions"; //$NON-NLS-1$
	}
	
}
