/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 463272
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.ide;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.PluginReference;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.keys.IBindingService;

public class HelloWorldCmdTemplate extends PDETemplateSection {
	public static final String KEY_CLASS_NAME = "className"; //$NON-NLS-1$
	public static final String KEY_MESSAGE = "message"; //$NON-NLS-1$
	public static final String CLASS_NAME = "SampleHandler"; //$NON-NLS-1$

	/**
	 * Constructor for HelloWorldTemplate.
	 */
	public HelloWorldCmdTemplate() {
		setPageCount(1);
		createOptions();
	}

	@Override
	public String getSectionId() {
		return "helloWorldCmd"; //$NON-NLS-1$
	}

	@Override
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	private void createOptions() {
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.HelloWorldCmdTemplate_packageName, (String) null, 0);
		addOption(KEY_CLASS_NAME, PDETemplateMessages.HelloWorldCmdTemplate_className, CLASS_NAME, 0);
		addOption(KEY_MESSAGE, PDETemplateMessages.HelloWorldCmdTemplate_messageText, PDETemplateMessages.HelloWorldCmdTemplate_defaultMessage, 0);
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_HELLO_WORLD);
		page.setTitle(PDETemplateMessages.HelloWorldCmdTemplate_title);
		page.setDescription(PDETemplateMessages.HelloWorldCmdTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	@Override
	public boolean isDependentOnParentWizard() {
		return true;
	}

	@Override
	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id));
	}

	@Override
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId));
	}

	@Override
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.commands"; //$NON-NLS-1$
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		return new IPluginReference[] { new PluginReference("org.eclipse.ui") };//$NON-NLS-1$
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension commandsExtension = createExtension("org.eclipse.ui.commands", true); //$NON-NLS-1$
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement category = factory.createElement(commandsExtension);
		category.setName("category"); //$NON-NLS-1$
		String categoryId = plugin.getId() + ".commands.category"; //$NON-NLS-1$
		category.setAttribute("id", categoryId); //$NON-NLS-1$
		category.setAttribute("name", PDETemplateMessages.HelloWorldCmdTemplate_sampleCategory); //$NON-NLS-1$
		commandsExtension.add(category);

		IPluginElement command = factory.createElement(commandsExtension);
		command.setName("command"); //$NON-NLS-1$
		command.setAttribute("categoryId", categoryId); //$NON-NLS-1$
		command.setAttribute("name", //$NON-NLS-1$
				PDETemplateMessages.HelloWorldCmdTemplate_sampleAction_name);
		String commandId = plugin.getId() + ".commands.sampleCommand"; //$NON-NLS-1$
		command.setAttribute("id", commandId); //$NON-NLS-1$
		commandsExtension.add(command);

		String fullClassName = getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_CLASS_NAME); //$NON-NLS-1$
		IPluginExtension handlersExtension = createExtension("org.eclipse.ui.handlers", true); //$NON-NLS-1$
		IPluginElement handler = factory.createElement(handlersExtension);
		handler.setName("handler"); //$NON-NLS-1$
		handler.setAttribute("class", fullClassName); //$NON-NLS-1$
		handler.setAttribute("commandId", commandId); //$NON-NLS-1$
		handlersExtension.add(handler);

		IPluginExtension bindingsExtension = createExtension("org.eclipse.ui.bindings", true); //$NON-NLS-1$
		IPluginElement binding = factory.createElement(bindingsExtension);
		binding.setName("key"); //$NON-NLS-1$
		binding.setAttribute("commandId", commandId); //$NON-NLS-1$
		binding.setAttribute("schemeId", IBindingService.DEFAULT_DEFAULT_ACTIVE_SCHEME_ID); //$NON-NLS-1$
		binding.setAttribute("contextId", IContextService.CONTEXT_ID_WINDOW); //$NON-NLS-1$
		binding.setAttribute("sequence", "M1+6"); //$NON-NLS-1$ //$NON-NLS-2$
		bindingsExtension.add(binding);

		IPluginExtension menusExtension = createExtension("org.eclipse.ui.menus", true); //$NON-NLS-1$
		IPluginElement menuAddition = factory.createElement(menusExtension);
		menuAddition.setName("menuContribution"); //$NON-NLS-1$
		menuAddition.setAttribute("locationURI", //$NON-NLS-1$
				"menu:org.eclipse.ui.main.menu?after=additions"); //$NON-NLS-1$
		IPluginElement menu = factory.createElement(menuAddition);
		menu.setName("menu"); //$NON-NLS-1$
		String menuId = plugin.getId() + ".menus.sampleMenu"; //$NON-NLS-1$
		menu.setAttribute("id", menuId); //$NON-NLS-1$
		menu.setAttribute("label", //$NON-NLS-1$
				PDETemplateMessages.HelloWorldCmdTemplate_sampleMenu_name);
		menu.setAttribute("mnemonic", //$NON-NLS-1$
				PDETemplateMessages.HelloWorldCmdTemplate_sampleMenu_mnemonic);
		IPluginElement menuCommand = factory.createElement(menu);
		menuCommand.setName("command"); //$NON-NLS-1$
		menuCommand.setAttribute("commandId", commandId); //$NON-NLS-1$
		menuCommand.setAttribute("id", plugin.getId() + ".menus.sampleCommand"); //$NON-NLS-1$ //$NON-NLS-2$
		menuCommand.setAttribute("mnemonic", //$NON-NLS-1$
				PDETemplateMessages.HelloWorldCmdTemplate_sampleAction_mnemonic);
		menu.add(menuCommand);
		menuAddition.add(menu);
		menusExtension.add(menuAddition);

		IPluginElement toolbarAddition = factory.createElement(menusExtension);
		toolbarAddition.setName("menuContribution"); //$NON-NLS-1$
		toolbarAddition.setAttribute("locationURI", //$NON-NLS-1$
				"toolbar:org.eclipse.ui.main.toolbar?after=additions"); //$NON-NLS-1$
		IPluginElement toolbar = factory.createElement(toolbarAddition);
		toolbar.setName("toolbar"); //$NON-NLS-1$
		String toolbarId = plugin.getId() + ".toolbars.sampleToolbar"; //$NON-NLS-1$
		toolbar.setAttribute("id", toolbarId); //$NON-NLS-1$
		IPluginElement toolbarCommand = factory.createElement(toolbar);
		toolbarCommand.setName("command"); //$NON-NLS-1$
		toolbarCommand.setAttribute("id", plugin.getId() + ".toolbars.sampleCommand"); //$NON-NLS-1$ //$NON-NLS-2$
		toolbarCommand.setAttribute("commandId", commandId); //$NON-NLS-1$
		toolbarCommand.setAttribute("icon", "icons/sample.png"); //$NON-NLS-1$ //$NON-NLS-2$
		toolbarCommand.setAttribute("tooltip", //$NON-NLS-1$
				PDETemplateMessages.HelloWorldCmdTemplate_sampleAction_tooltip);
		toolbar.add(toolbarCommand);
		toolbarAddition.add(toolbar);
		menusExtension.add(toolbarAddition);

		if (!commandsExtension.isInTheModel()) {
			plugin.add(commandsExtension);
		}
		if (!handlersExtension.isInTheModel()) {
			plugin.add(handlersExtension);
		}
		if (!bindingsExtension.isInTheModel()) {
			plugin.add(bindingsExtension);
		}
		if (!menusExtension.isInTheModel()) {
			plugin.add(menusExtension);
		}
	}

	@Override
	public String[] getNewFiles() {
		return new String[] {"icons/"}; //$NON-NLS-1$
	}

	@Override
	protected String getFormattedPackageName(String id) {
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".handlers"; //$NON-NLS-1$
		return "handlers"; //$NON-NLS-1$
	}
}
