/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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
 *     Lars.Vogel <Lars.Vogel@vogella.com> - Bug 486247, 486261
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

public class PerspectiveTemplate extends PDETemplateSection {
	public static final String PERSPECTIVE_CLASS_NAME = "perspectiveClassName"; //$NON-NLS-1$
	public static final String PERSPECTIVE_NAME = "perspectiveCategoryName"; //$NON-NLS-1$

	public static final String BLN_PERSPECTIVE_SHORTS = "perspectiveShortcuts"; //$NON-NLS-1$
	public static final String BLN_NEW_WIZARD_SHORTS = "newWizardShortcuts"; //$NON-NLS-1$
	public static final String BLN_SHOW_VIEW_SHORTS = "showViewShortcuts"; //$NON-NLS-1$
	public static final String BLN_ACTION_SETS = "actionSets"; //$NON-NLS-1$

	private WizardPage page;

	/**
	 * Constructor for PerspectiveTemplate.
	 */
	public PerspectiveTemplate() {
		setPageCount(1);
		createOptions();
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		// add required dependencies
		IPluginReference[] dep = new IPluginReference[2];
		dep[0] = new PluginReference("org.eclipse.ui.console"); //$NON-NLS-1$
		dep[1] = new PluginReference("org.eclipse.jdt.ui"); //$NON-NLS-1$
		return dep;
	}

	@Override
	public String getSectionId() {
		return "perspective"; //$NON-NLS-1$
	}

	@Override
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	/**
	 * Creates the options to be displayed on the template wizard.
	 * Various string options, blank fields and a multiple choice
	 * option are used.
	 */
	private void createOptions() {
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.PerspectiveTemplate_packageName, (String) null, 0);
		addOption(PERSPECTIVE_CLASS_NAME, PDETemplateMessages.PerspectiveTemplate_perspectiveClass, PDETemplateMessages.PerspectiveTemplate_perspectiveClassName, 0);
		addOption(PERSPECTIVE_NAME, PDETemplateMessages.PerspectiveTemplate_perspective, PDETemplateMessages.PerspectiveTemplate_perspectiveName, 0);

		addBlankField(0);

		addOption(BLN_PERSPECTIVE_SHORTS, PDETemplateMessages.PerspectiveTemplate_perspectiveShortcuts, true, 0);
		addOption(BLN_SHOW_VIEW_SHORTS, PDETemplateMessages.PerspectiveTemplate_showViewShortcuts, true, 0);
		addOption(BLN_NEW_WIZARD_SHORTS, PDETemplateMessages.PerspectiveTemplate_newWizardShortcuts, true, 0);
		addOption(BLN_ACTION_SETS, PDETemplateMessages.PerspectiveTemplate_actionSets, true, 0);
	}

	@Override
	public void addPages(Wizard wizard) {
		int pageIndex = 0;

		page = createPage(pageIndex, IHelpContextIds.TEMPLATE_EDITOR);
		page.setTitle(PDETemplateMessages.PerspectiveTemplate_title);
		page.setDescription(PDETemplateMessages.PerspectiveTemplate_desc);

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
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		// This method creates the extension point structure through the use
		// of IPluginElement objects. The element attributes are set based on
		// user input from the wizard page as well as values required for the
		// operation of the extension point.
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement perspectiveElement = factory.createElement(extension);
		perspectiveElement.setName("perspective"); //$NON-NLS-1$
		perspectiveElement.setAttribute("id", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(PERSPECTIVE_CLASS_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
		perspectiveElement.setAttribute("name", getStringOption(PERSPECTIVE_NAME)); //$NON-NLS-1$
		perspectiveElement.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(PERSPECTIVE_CLASS_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
		perspectiveElement.setAttribute("icon", "icons/releng_gears.gif"); //$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-2$

		extension.add(perspectiveElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	@Override
	public String[] getNewFiles() {
		return new String[] {"icons/"}; //$NON-NLS-1$
	}

	@Override
	protected String getFormattedPackageName(String id) {
		// Package name addition to create a location for containing
		// any classes required by the decorator.
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".perspectives"; //$NON-NLS-1$
		return "perspectives"; //$NON-NLS-1$
	}

	@Override
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.perspectives"; //$NON-NLS-1$
	}
}
