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

import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.*;

public class PopupMenuTemplate extends PDETemplateSection {

	public static final String KEY_TARGET_OBJECT = "objectClass";
	public static final String KEY_NAME_FILTER = "nameFilter";
	public static final String KEY_SUBMENU_LABEL = "subMenuLabel";
	public static final String KEY_ACTION_LABEL = "actionLabel";
	public static final String KEY_ACTION_CLASS = "actionClass";
	public static final String KEY_SELECTION = "selection";

	private static final String NL_TITLE = "PopupMenuTemplate.title";
	private static final String NL_DESC = "PopupMenuTemplate.desc";
	private static final String NL_TARGET_CLASS = "PopupMenuTemplate.targetClass";
	private static final String NL_NAME_FILTER = "PopupMenuTemplate.nameFilter";
	private static final String NL_SUBMENU_NAME = "PopupMenuTemplate.submenuName";
	private static final String NL_DEFAULT_SUBMENU_NAME =
		"PopupMenuTemplate.defaultSubmenuName";
	private static final String NL_ACTION_LABEL = "PopupMenuTemplate.actionLabel";
	private static final String NL_DEFAULT_ACTION_NAME =
		"PopupMenuTemplate.defaultActionName";
	private static final String NL_PACKAGE_NAME = "PopupMenuTemplate.packageName";
	private static final String NL_ACTION_CLASS = "PopupMenuTemplate.actionClass";
	private static final String NL_ENABLED_FOR = "PopupMenuTemplate.enabledFor";
	private static final String NL_SINGLE_SELECTION =
		"PopupMenuTemplate.singleSelection";
	private static final String NL_MULTIPLE_SELECTION =
		"PopupMenuTemplate.multipleSelection";

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
			"org.eclipse.core.resources.IFile",
			0);
		addOption(
			KEY_NAME_FILTER,
			PDEPlugin.getResourceString(NL_NAME_FILTER),
			"plugin.xml",
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
			"NewAction",
			0);
		addOption(
			KEY_SELECTION,
			PDEPlugin.getResourceString(NL_ENABLED_FOR),
			new String[][] {
				{ "singleSelection", PDEPlugin.getResourceString(NL_SINGLE_SELECTION)},
				{
				"multipleSelection", PDEPlugin.getResourceString(NL_MULTIPLE_SELECTION)
				}
		}, "singleSelection", 0);
	}
	/**
	 * @see PDETemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "popupMenus";
	}

	public boolean isDependentOnFirstPage() {
		return true;
	}

	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, id + ".popup.actions");
	}

	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".popup.actions");
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
		objectContributionElement.setName("objectContribution");
		objectContributionElement.setAttribute(
			"objectClass",
			getStringOption(KEY_TARGET_OBJECT));
		objectContributionElement.setAttribute(
			"nameFilter",
			getStringOption(KEY_NAME_FILTER));
		objectContributionElement.setAttribute(
			"id",
			model.getPluginBase().getId() + ".contribution1");

		IPluginElement menuElement = factory.createElement(objectContributionElement);
		menuElement.setName("menu");
		menuElement.setAttribute("label", getStringOption(KEY_SUBMENU_LABEL));
		menuElement.setAttribute("path", "additions");
		menuElement.setAttribute("id", model.getPluginBase().getId() + ".menu1");

		IPluginElement separatorElement = factory.createElement(menuElement);
		separatorElement.setName("separator");
		separatorElement.setAttribute("name", "group1");
		menuElement.add(separatorElement);
		objectContributionElement.add(menuElement);

		IPluginElement actionElement = factory.createElement(objectContributionElement);
		actionElement.setName("action");
		actionElement.setAttribute("label", getStringOption(KEY_ACTION_LABEL));
		actionElement.setAttribute(
			"class",
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_ACTION_CLASS));
		actionElement.setAttribute(
			"menubarPath",
			model.getPluginBase().getId() + ".menu1/group1");
		actionElement.setAttribute(
			"enablesFor",
			getValue(KEY_SELECTION).toString().equals("singleSelection")
				? "1"
				: "multiple");
		actionElement.setAttribute("id", model.getPluginBase().getId() + ".newAction");
		objectContributionElement.add(actionElement);

		extension.add(objectContributionElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	/**
	 * @see ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.popupMenus";
	}

}
