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

	private static final String NL_TITLE = "PopupMenuTemplate.title"; //$NON-NLS-1$
	private static final String NL_DESC = "PopupMenuTemplate.desc"; //$NON-NLS-1$
	private static final String NL_TARGET_CLASS = "PopupMenuTemplate.targetClass"; //$NON-NLS-1$
	private static final String NL_NAME_FILTER = "PopupMenuTemplate.nameFilter"; //$NON-NLS-1$
	private static final String NL_SUBMENU_NAME = "PopupMenuTemplate.submenuName"; //$NON-NLS-1$
	private static final String NL_DEFAULT_SUBMENU_NAME =
		"PopupMenuTemplate.defaultSubmenuName"; //$NON-NLS-1$
	private static final String NL_ACTION_LABEL = "PopupMenuTemplate.actionLabel"; //$NON-NLS-1$
	private static final String NL_DEFAULT_ACTION_NAME =
		"PopupMenuTemplate.defaultActionName"; //$NON-NLS-1$
	private static final String NL_PACKAGE_NAME = "PopupMenuTemplate.packageName"; //$NON-NLS-1$
	private static final String NL_ACTION_CLASS = "PopupMenuTemplate.actionClass"; //$NON-NLS-1$
	private static final String NL_ENABLED_FOR = "PopupMenuTemplate.enabledFor"; //$NON-NLS-1$
	private static final String NL_SINGLE_SELECTION =
		"PopupMenuTemplate.singleSelection"; //$NON-NLS-1$
	private static final String NL_MULTIPLE_SELECTION =
		"PopupMenuTemplate.multipleSelection"; //$NON-NLS-1$

	/**
	 * Constructor for PropertyPageTemplate.
	 */
	public PopupMenuTemplate() {
		setPageCount(1);
		createOptions();
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_POPUP_MENU);
		page.setTitle(PDEPlugin.getResourceString(NL_TITLE));
		page.setDescription(PDEPlugin.getResourceString(NL_DESC));
		wizard.addPage(page);
		markPagesAdded();
	}

	private void createOptions() {
		addOption(
			KEY_TARGET_OBJECT,
			PDEPlugin.getResourceString(NL_TARGET_CLASS),
			"org.eclipse.core.resources.IFile", //$NON-NLS-1$
			0);
		addOption(
			KEY_NAME_FILTER,
			PDEPlugin.getResourceString(NL_NAME_FILTER),
			"plugin.xml", //$NON-NLS-1$
			0);
		addOption(
			KEY_SUBMENU_LABEL,
			PDEPlugin.getResourceString(NL_SUBMENU_NAME),
			PDEPlugin.getResourceString(NL_DEFAULT_SUBMENU_NAME),
			0);
		addOption(
			KEY_ACTION_LABEL,
			PDEPlugin.getResourceString(NL_ACTION_LABEL),
			PDEPlugin.getResourceString(NL_DEFAULT_ACTION_NAME),
			0);
		addOption(
			KEY_PACKAGE_NAME,
			PDEPlugin.getResourceString(NL_PACKAGE_NAME),
			(String) null,
			0);
		addOption(
			KEY_ACTION_CLASS,
			PDEPlugin.getResourceString(NL_ACTION_CLASS),
			PDEPlugin.getResourceString("PopupMenuTemplate.newAction"), //$NON-NLS-1$
			0);
		addOption(
			KEY_SELECTION,
			PDEPlugin.getResourceString(NL_ENABLED_FOR),
			new String[][] {
				{ "singleSelection", PDEPlugin.getResourceString(NL_SINGLE_SELECTION)}, //$NON-NLS-1$
				{
				"multipleSelection", PDEPlugin.getResourceString(NL_MULTIPLE_SELECTION) //$NON-NLS-1$
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
		initializeOption(KEY_PACKAGE_NAME, id + ".popup.actions"); //$NON-NLS-1$
	}

	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".popup.actions"); //$NON-NLS-1$
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

}
