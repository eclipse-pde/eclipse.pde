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
import org.eclipse.pde.ui.templates.TemplateOption;

public class PerspectiveExtensionsTemplate extends PDETemplateSection {

	public static final String KEY_TARGET_PERSPECTIVE = "targetPerspective";
	public static final String KEY_PERSPECTIVE_SHORTCUT = "perspectiveShortcut";
	public static final String KEY_VIEW_SHORTCUT = "viewShortcut";
	public static final String KEY_WIZARD_SHORTCUT = "wizardShortcut";
	public static final String KEY_VIEW = "view";
	public static final String KEY_VIEW_RELATIVE = "viewRelative";
	public static final String KEY_VIEW_RELATIONSHIP = "viewRelationship";
	public static final String KEY_ACTION_SET = "actionSet";

	private static final String NL_TITLE0 = "PerspectiveExtensionsTemplate.title0";
	private static final String NL_DESC0 = "PerspectiveExtensionsTemplate.desc0";
	private static final String NL_TITLE1 = "PerspectiveExtensionsTemplate.title1";
	private static final String NL_DESC1 = "PerspectiveExtensionsTemplate.desc1";

	private static final String NL_PERSPECTIVE_ID =
		"PerspectiveExtensionsTemplate.perspectiveId";
	private static final String NL_ACTION_SET =
		"PerspectiveExtensionsTemplate.actionSet";
	private static final String NL_SHORTCUT_ID =
		"PerspectiveExtensionsTemplate.shortcutId";
	private static final String NL_VIEW_SHORTCUT_ID =
		"PerspectiveExtensionsTemplate.viewShortcutId";
	private static final String NL_WIZARD_SHORTCUT_ID =
		"PerspectiveExtensionsTemplate.wizardShortcutId";

	private static final String NL_VIEW_ID = "PerspectiveExtensionsTemplate.viewId";
	private static final String NL_RELATIVE_VIEW =
		"PerspectiveExtensionsTemplate.relativeView";
	private static final String NL_RELATIVE_LOCATION =
		"PerspectiveExtensionsTemplate.relativePosition";
	private static final String NL_STACK = "PerspectiveExtensionsTemplate.stack";
	private static final String NL_FAST = "PerspectiveExtensionsTemplate.fast";
	private static final String NL_LEFT = "PerspectiveExtensionsTemplate.left";
	private static final String NL_RIGHT = "PerspectiveExtensionsTemplate.right";
	private static final String NL_TOP = "PerspectiveExtensionsTemplate.top";
	private static final String NL_BOTTOM = "PerspectiveExtensionsTemplate.buttom";

	/**
	 * Constructor for PerspectiveExtensionsTemplate.
	 */
	public PerspectiveExtensionsTemplate() {
		setPageCount(2);
		createOptions();
	}

	public void addPages(Wizard wizard) {
		WizardPage page0 = createPage(0,IHelpContextIds.TEMPLATE_PERSPECTIVE_EXTENSIONS);
		page0.setTitle(PDEPlugin.getResourceString(NL_TITLE0));
		page0.setDescription(PDEPlugin.getResourceString(NL_DESC0));
		wizard.addPage(page0);

		WizardPage page1 = createPage(1, IHelpContextIds.TEMPLATE_PERSPECTIVE_EXTENSIONS);
		page1.setTitle(PDEPlugin.getResourceString(NL_TITLE1));
		page1.setDescription(PDEPlugin.getResourceString(NL_DESC1));
		wizard.addPage(page1);
		markPagesAdded();
	}

	private void createOptions() {
		// add options to first page
		addOption(
			KEY_TARGET_PERSPECTIVE,
			PDEPlugin.getResourceString(NL_PERSPECTIVE_ID),
			"org.eclipse.ui.resourcePerspective",
			0);
		addOption(
			KEY_ACTION_SET,
			PDEPlugin.getResourceString(NL_ACTION_SET),
			"org.eclipse.jdt.ui.JavaActionSet",
			0);
		addOption(
			KEY_PERSPECTIVE_SHORTCUT,
			PDEPlugin.getResourceString(NL_SHORTCUT_ID),
			"org.eclipse.debug.ui.DebugPerspective",
			0);
		addOption(
			KEY_VIEW_SHORTCUT,
			PDEPlugin.getResourceString(NL_VIEW_SHORTCUT_ID),
			"org.eclipse.jdt.ui.TypeHierarchy",
			0);
		addOption(
			KEY_WIZARD_SHORTCUT,
			PDEPlugin.getResourceString(NL_WIZARD_SHORTCUT_ID),
			"org.eclipse.jdt.ui.wizards.NewProjectCreationWizard",
			0);

		// add options to second page 
		addOption(
			KEY_VIEW,
			PDEPlugin.getResourceString(NL_VIEW_ID),
			"org.eclipse.jdt.ui.PackageExplorer",
			1);
		addOption(
			KEY_VIEW_RELATIVE,
			PDEPlugin.getResourceString(NL_RELATIVE_VIEW),
			"org.eclipse.ui.views.ResourceNavigator",
			1);
		addOption(
			KEY_VIEW_RELATIONSHIP,
			PDEPlugin.getResourceString(NL_RELATIVE_LOCATION),
			new String[][] {
				{"stack", PDEPlugin.getResourceString(NL_STACK)},
				{"fast", PDEPlugin.getResourceString(NL_FAST)},
				{"left", PDEPlugin.getResourceString(NL_LEFT)},
				{"right", PDEPlugin.getResourceString(NL_RIGHT)}, 
				{"top", PDEPlugin.getResourceString(NL_TOP)}, 
				{"bottom", PDEPlugin.getResourceString(NL_BOTTOM)}}, 
			"stack", 
			1);
	}

	private TemplateOption[] getAllPageOptions(TemplateOption source) {
		int pageIndex = getPageIndex(source);
		if (pageIndex != -1)
			return getOptions(pageIndex);
		return new TemplateOption[0];
	}

	/**
	 * @see PDETemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "perspectiveExtensions";
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
		TemplateOption[] siblings = getAllPageOptions(source);
		for (int i = 0; i < siblings.length; i++) {
			TemplateOption nextOption = siblings[i];
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

		IPluginElement perspectiveElement = factory.createElement(extension);
		perspectiveElement.setName("perspectiveExtension");
		perspectiveElement.setAttribute(
			"targetID",
			getStringOption(KEY_TARGET_PERSPECTIVE));

		IPluginElement wizardShortcutElement =
			factory.createElement(perspectiveElement);
		wizardShortcutElement.setName("newWizardShortcut");
		wizardShortcutElement.setAttribute("id", getStringOption(KEY_WIZARD_SHORTCUT));
		perspectiveElement.add(wizardShortcutElement);

		IPluginElement viewShortcutElement = factory.createElement(perspectiveElement);
		viewShortcutElement.setName("viewShortcut");
		viewShortcutElement.setAttribute("id", getStringOption(KEY_VIEW_SHORTCUT));
		perspectiveElement.add(viewShortcutElement);

		IPluginElement perspectiveShortcutElement =
			factory.createElement(perspectiveElement);
		perspectiveShortcutElement.setName("perspectiveShortcut");
		perspectiveShortcutElement.setAttribute(
			"id",
			getStringOption(KEY_PERSPECTIVE_SHORTCUT));
		perspectiveElement.add(perspectiveShortcutElement);

		IPluginElement actionSetElement = factory.createElement(perspectiveElement);
		actionSetElement.setName("actionSet");
		actionSetElement.setAttribute("id", getStringOption(KEY_ACTION_SET));
		perspectiveElement.add(actionSetElement);

		IPluginElement viewElement = factory.createElement(perspectiveElement);
		viewElement.setName("view");
		viewElement.setAttribute("id", getStringOption(KEY_VIEW));
		viewElement.setAttribute("relative", getStringOption(KEY_VIEW_RELATIVE));
		String relationship = getValue(KEY_VIEW_RELATIONSHIP).toString();
		viewElement.setAttribute("relationship", relationship);
		if (!relationship.equals("stack") && !relationship.equals("fast")) {
			viewElement.setAttribute("ratio", "0.5");
		}
		perspectiveElement.add(viewElement);

		extension.add(perspectiveElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	/**
	 * @see ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.perspectiveExtensions";
	}

}
