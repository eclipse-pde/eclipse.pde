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
import org.eclipse.pde.ui.templates.TemplateOption;

public class PerspectiveExtensionsTemplate extends PDETemplateSection {

	public static final String KEY_TARGET_PERSPECTIVE = "targetPerspective"; //$NON-NLS-1$
	public static final String KEY_PERSPECTIVE_SHORTCUT = "perspectiveShortcut"; //$NON-NLS-1$
	public static final String KEY_VIEW_SHORTCUT = "viewShortcut"; //$NON-NLS-1$
	public static final String KEY_WIZARD_SHORTCUT = "wizardShortcut"; //$NON-NLS-1$
	public static final String KEY_VIEW = "view"; //$NON-NLS-1$
	public static final String KEY_VIEW_RELATIVE = "viewRelative"; //$NON-NLS-1$
	public static final String KEY_VIEW_RELATIONSHIP = "viewRelationship"; //$NON-NLS-1$
	public static final String KEY_ACTION_SET = "actionSet"; //$NON-NLS-1$

	private static final String NL_TITLE0 = "PerspectiveExtensionsTemplate.title0"; //$NON-NLS-1$
	private static final String NL_DESC0 = "PerspectiveExtensionsTemplate.desc0"; //$NON-NLS-1$
	private static final String NL_TITLE1 = "PerspectiveExtensionsTemplate.title1"; //$NON-NLS-1$
	private static final String NL_DESC1 = "PerspectiveExtensionsTemplate.desc1"; //$NON-NLS-1$

	private static final String NL_PERSPECTIVE_ID =
		"PerspectiveExtensionsTemplate.perspectiveId"; //$NON-NLS-1$
	private static final String NL_ACTION_SET =
		"PerspectiveExtensionsTemplate.actionSet"; //$NON-NLS-1$
	private static final String NL_SHORTCUT_ID =
		"PerspectiveExtensionsTemplate.shortcutId"; //$NON-NLS-1$
	private static final String NL_VIEW_SHORTCUT_ID =
		"PerspectiveExtensionsTemplate.viewShortcutId"; //$NON-NLS-1$
	private static final String NL_WIZARD_SHORTCUT_ID =
		"PerspectiveExtensionsTemplate.wizardShortcutId"; //$NON-NLS-1$

	private static final String NL_VIEW_ID = "PerspectiveExtensionsTemplate.viewId"; //$NON-NLS-1$
	private static final String NL_RELATIVE_VIEW =
		"PerspectiveExtensionsTemplate.relativeView"; //$NON-NLS-1$
	private static final String NL_RELATIVE_LOCATION =
		"PerspectiveExtensionsTemplate.relativePosition"; //$NON-NLS-1$
	private static final String NL_STACK = "PerspectiveExtensionsTemplate.stack"; //$NON-NLS-1$
	private static final String NL_FAST = "PerspectiveExtensionsTemplate.fast"; //$NON-NLS-1$
	private static final String NL_LEFT = "PerspectiveExtensionsTemplate.left"; //$NON-NLS-1$
	private static final String NL_RIGHT = "PerspectiveExtensionsTemplate.right"; //$NON-NLS-1$
	private static final String NL_TOP = "PerspectiveExtensionsTemplate.top"; //$NON-NLS-1$
	private static final String NL_BOTTOM = "PerspectiveExtensionsTemplate.buttom"; //$NON-NLS-1$

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
			"org.eclipse.ui.resourcePerspective", //$NON-NLS-1$
			0);
		addOption(
			KEY_ACTION_SET,
			PDEPlugin.getResourceString(NL_ACTION_SET),
			"org.eclipse.jdt.ui.JavaActionSet", //$NON-NLS-1$
			0);
		addOption(
			KEY_PERSPECTIVE_SHORTCUT,
			PDEPlugin.getResourceString(NL_SHORTCUT_ID),
			"org.eclipse.debug.ui.DebugPerspective", //$NON-NLS-1$
			0);
		addOption(
			KEY_VIEW_SHORTCUT,
			PDEPlugin.getResourceString(NL_VIEW_SHORTCUT_ID),
			"org.eclipse.jdt.ui.TypeHierarchy", //$NON-NLS-1$
			0);
		addOption(
			KEY_WIZARD_SHORTCUT,
			PDEPlugin.getResourceString(NL_WIZARD_SHORTCUT_ID),
			"org.eclipse.jdt.ui.wizards.NewProjectCreationWizard", //$NON-NLS-1$
			0);

		// add options to second page 
		addOption(
			KEY_VIEW,
			PDEPlugin.getResourceString(NL_VIEW_ID),
			"org.eclipse.jdt.ui.PackageExplorer", //$NON-NLS-1$
			1);
		addOption(
			KEY_VIEW_RELATIVE,
			PDEPlugin.getResourceString(NL_RELATIVE_VIEW),
			"org.eclipse.ui.views.ResourceNavigator", //$NON-NLS-1$
			1);
		addOption(
			KEY_VIEW_RELATIONSHIP,
			PDEPlugin.getResourceString(NL_RELATIVE_LOCATION),
			new String[][] {
				{"stack", PDEPlugin.getResourceString(NL_STACK)}, //$NON-NLS-1$
				{"fast", PDEPlugin.getResourceString(NL_FAST)}, //$NON-NLS-1$
				{"left", PDEPlugin.getResourceString(NL_LEFT)}, //$NON-NLS-1$
				{"right", PDEPlugin.getResourceString(NL_RIGHT)},  //$NON-NLS-1$
				{"top", PDEPlugin.getResourceString(NL_TOP)},  //$NON-NLS-1$
				{"bottom", PDEPlugin.getResourceString(NL_BOTTOM)}},  //$NON-NLS-1$
			"stack",  //$NON-NLS-1$
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
		return "perspectiveExtensions"; //$NON-NLS-1$
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
		perspectiveElement.setName("perspectiveExtension"); //$NON-NLS-1$
		perspectiveElement.setAttribute(
			"targetID", //$NON-NLS-1$
			getStringOption(KEY_TARGET_PERSPECTIVE));

		IPluginElement wizardShortcutElement =
			factory.createElement(perspectiveElement);
		wizardShortcutElement.setName("newWizardShortcut"); //$NON-NLS-1$
		wizardShortcutElement.setAttribute("id", getStringOption(KEY_WIZARD_SHORTCUT)); //$NON-NLS-1$
		perspectiveElement.add(wizardShortcutElement);

		IPluginElement viewShortcutElement = factory.createElement(perspectiveElement);
		viewShortcutElement.setName("viewShortcut"); //$NON-NLS-1$
		viewShortcutElement.setAttribute("id", getStringOption(KEY_VIEW_SHORTCUT)); //$NON-NLS-1$
		perspectiveElement.add(viewShortcutElement);

		IPluginElement perspectiveShortcutElement =
			factory.createElement(perspectiveElement);
		perspectiveShortcutElement.setName("perspectiveShortcut"); //$NON-NLS-1$
		perspectiveShortcutElement.setAttribute(
			"id", //$NON-NLS-1$
			getStringOption(KEY_PERSPECTIVE_SHORTCUT));
		perspectiveElement.add(perspectiveShortcutElement);

		IPluginElement actionSetElement = factory.createElement(perspectiveElement);
		actionSetElement.setName("actionSet"); //$NON-NLS-1$
		actionSetElement.setAttribute("id", getStringOption(KEY_ACTION_SET)); //$NON-NLS-1$
		perspectiveElement.add(actionSetElement);

		IPluginElement viewElement = factory.createElement(perspectiveElement);
		viewElement.setName("view"); //$NON-NLS-1$
		viewElement.setAttribute("id", getStringOption(KEY_VIEW)); //$NON-NLS-1$
		viewElement.setAttribute("relative", getStringOption(KEY_VIEW_RELATIVE)); //$NON-NLS-1$
		String relationship = getValue(KEY_VIEW_RELATIONSHIP).toString();
		viewElement.setAttribute("relationship", relationship); //$NON-NLS-1$
		if (!relationship.equals("stack") && !relationship.equals("fast")) { //$NON-NLS-1$ //$NON-NLS-2$
			viewElement.setAttribute("ratio", "0.5"); //$NON-NLS-1$ //$NON-NLS-2$
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
		return "org.eclipse.ui.perspectiveExtensions"; //$NON-NLS-1$
	}

}
