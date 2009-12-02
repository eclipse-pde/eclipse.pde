/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel - bug 265231
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.rcp;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.PluginReference;

public class ViewRCPTemplate extends PDETemplateSection {

	public static final String KEY_APPLICATION_CLASS = "applicationClass"; //$NON-NLS-1$
	public static final String KEY_WINDOW_TITLE = "windowTitle"; //$NON-NLS-1$

	public ViewRCPTemplate() {
		setPageCount(1);
		createOptions();
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_RCP_MAIL);
		page.setTitle(PDETemplateMessages.ViewRCPTemplate_title);
		page.setDescription(PDETemplateMessages.ViewRCPTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	private void createOptions() {
		addOption(KEY_WINDOW_TITLE, PDETemplateMessages.ViewRCPTemplate_windowTitle, "RCP Application", 0); //$NON-NLS-1$ 

		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.ViewRCPTemplate_packageName, (String) null, 0);

		addOption(KEY_APPLICATION_CLASS, PDETemplateMessages.ViewRCPTemplate_appClass, "Application", 0); //$NON-NLS-1$

		createBrandingOptions();
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.ui.templates.OptionTemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "viewRCP"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#updateModel(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		createApplicationExtension();
		createPerspectiveExtension();
		createViewExtension();
		createPerspectiveViewExtension();
		createMenuExtension();

		if (getBooleanOption(KEY_PRODUCT_BRANDING))
			createProductExtension();
	}

	private void createApplicationExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();

		IPluginExtension extension = createExtension("org.eclipse.core.runtime.applications", true); //$NON-NLS-1$
		extension.setId(VALUE_APPLICATION_ID);

		IPluginElement element = model.getPluginFactory().createElement(extension);
		element.setName("application"); //$NON-NLS-1$
		extension.add(element);

		IPluginElement run = model.getPluginFactory().createElement(element);
		run.setName("run"); //$NON-NLS-1$
		run.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_APPLICATION_CLASS)); //$NON-NLS-1$ //$NON-NLS-2$
		element.add(run);

		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	private void createPerspectiveExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();

		IPluginExtension extension = createExtension("org.eclipse.ui.perspectives", true); //$NON-NLS-1$
		IPluginElement element = model.getPluginFactory().createElement(extension);
		element.setName("perspective"); //$NON-NLS-1$
		element.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + ".Perspective"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("name", "Perspective"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("id", plugin.getId() + ".perspective"); //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(element);

		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	private void createViewExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		String id = plugin.getId();
		IPluginExtension extension = createExtension("org.eclipse.ui.views", true); //$NON-NLS-1$

		IPluginElement view = model.getPluginFactory().createElement(extension);
		view.setName("view"); //$NON-NLS-1$
		view.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + ".View"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("name", "View"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("id", id + ".view"); //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(view);

		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	private void createPerspectiveViewExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		String id = plugin.getId();

		IPluginExtension extension = createExtension("org.eclipse.ui.perspectiveExtensions", true); //$NON-NLS-1$
		IPluginElement perspectiveExtension = model.getPluginFactory().createElement(extension);
		perspectiveExtension.setName("perspectiveExtension"); //$NON-NLS-1$
		perspectiveExtension.setAttribute("targetID", "*"); //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(perspectiveExtension);

		IPluginElement view = model.getPluginFactory().createElement(perspectiveExtension);
		view.setName("view"); //$NON-NLS-1$
		view.setAttribute("id", id + ".view"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("minimized", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("relationship", "left"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("standalone", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("relative", "org.eclipse.ui.editorss"); //$NON-NLS-1$ //$NON-NLS-2$
		perspectiveExtension.add(view);

		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	private void createMenuExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.menus", true); //$NON-NLS-1$
		IPluginElement menuContribution = model.getPluginFactory().createElement(extension);
		menuContribution.setName("menuContribution"); //$NON-NLS-1$
		menuContribution.setAttribute("locationURI", "menu:org.eclipse.ui.main.menu"); //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(menuContribution);

		IPluginElement menu = model.getPluginFactory().createElement(menuContribution);
		menu.setName("menu"); //$NON-NLS-1$
		menu.setAttribute("label", "File"); //$NON-NLS-1$ //$NON-NLS-2$
		menuContribution.add(menu);

		IPluginElement command = model.getPluginFactory().createElement(menu);
		command.setName("command"); //$NON-NLS-1$
		command.setAttribute("commandId", "org.eclipse.ui.file.exit"); //$NON-NLS-1$ //$NON-NLS-2$
		command.setAttribute("label", "Exit"); //$NON-NLS-1$ //$NON-NLS-2$
		menu.add(command);

		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	private void createProductExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.core.runtime.products", true); //$NON-NLS-1$
		extension.setId(VALUE_PRODUCT_ID);

		IPluginElement element = model.getFactory().createElement(extension);
		element.setName("product"); //$NON-NLS-1$
		element.setAttribute("name", getStringOption(KEY_WINDOW_TITLE)); //$NON-NLS-1$
		element.setAttribute("application", plugin.getId() + "." + VALUE_APPLICATION_ID); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement property = model.getFactory().createElement(element);

		property = model.getFactory().createElement(element);
		property.setName("property"); //$NON-NLS-1$
		property.setAttribute("name", "windowImages"); //$NON-NLS-1$ //$NON-NLS-2$
		property.setAttribute("value", "icons/alt_window_16.gif,icons/alt_window_32.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		element.add(property);

		extension.add(element);

		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.ui.templates.ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.BaseOptionTemplateSection#isDependentOnParentWizard()
	 */
	public boolean isDependentOnParentWizard() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		IPluginReference[] dep = new IPluginReference[2];
		dep[0] = new PluginReference("org.eclipse.core.runtime", null, 0); //$NON-NLS-1$
		dep[1] = new PluginReference("org.eclipse.ui", null, 0); //$NON-NLS-1$
		return dep;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.templates.PDETemplateSection#getNewFiles()
	 */
	public String[] getNewFiles() {
		if (copyBrandingDirectory())
			return new String[] {"icons/", "splash.bmp"}; //$NON-NLS-1$ //$NON-NLS-2$
		return super.getNewFiles();
	}
}
