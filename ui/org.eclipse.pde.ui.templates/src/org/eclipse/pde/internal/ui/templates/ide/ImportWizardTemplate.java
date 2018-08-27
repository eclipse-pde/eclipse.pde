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

import java.util.StringTokenizer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.PluginReference;

public class ImportWizardTemplate extends PDETemplateSection {
	public static final String WIZARD_CLASS_NAME = "wizardClassName"; //$NON-NLS-1$
	public static final String WIZARD_CATEGORY_NAME = "wizardCategoryName"; //$NON-NLS-1$
	public static final String WIZARD_PAGE_CLASS_NAME = "wizardPageClassName"; //$NON-NLS-1$
	public static final String WIZARD_IMPORT_NAME = "wizardImportName"; //$NON-NLS-1$
	public static final String WIZARD_FILE_FILTERS = "wizardFileFilters"; //$NON-NLS-1$

	private WizardPage page;

	/**
	 * Constructor for ImportWizardTemplate.
	 */
	public ImportWizardTemplate() {
		setPageCount(1);
		createOptions();
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		// Additional dependency required to provide WizardNewFileCreationPage
		IPluginReference[] dep = new IPluginReference[2];
		dep[0] = new PluginReference("org.eclipse.ui.ide"); //$NON-NLS-1$
		dep[1] = new PluginReference("org.eclipse.core.resources"); //$NON-NLS-1$
		return dep;
	}

	@Override
	public String getSectionId() {
		// Identifier used for the folder name within the templates_3.X
		// hierarchy  and as part of the lookup key for the template label
		// variable.
		return "importWizard"; //$NON-NLS-1$
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
		String[][] choices = fromCommaSeparated(PDETemplateMessages.ImportWizardTemplate_filterChoices);

		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.ImportWizardTemplate_packageName, (String) null, 0);
		addOption(WIZARD_CLASS_NAME, PDETemplateMessages.ImportWizardTemplate_wizardClass, PDETemplateMessages.ImportWizardTemplate_wizardClassName, 0);
		addOption(WIZARD_PAGE_CLASS_NAME, PDETemplateMessages.ImportWizardTemplate_pageClass, PDETemplateMessages.ImportWizardTemplate_pageClassName, 0);

		addBlankField(0);

		addOption(WIZARD_CATEGORY_NAME, PDETemplateMessages.ImportWizardTemplate_importWizardCategory, PDETemplateMessages.ImportWizardTemplate_importWizardCategoryName, 0);
		addOption(WIZARD_IMPORT_NAME, PDETemplateMessages.ImportWizardTemplate_wizardName, PDETemplateMessages.ImportWizardTemplate_wizardDefaultName, 0);

		addBlankField(0);

		addOption(WIZARD_FILE_FILTERS, PDETemplateMessages.ImportWizardTemplate_filters, choices, choices[0][0], 0);
	}

	@Override
	public void addPages(Wizard wizard) {
		int pageIndex = 0;

		page = createPage(pageIndex, IHelpContextIds.TEMPLATE_EDITOR);
		page.setTitle(PDETemplateMessages.ImportWizardTemplate_title);
		page.setDescription(PDETemplateMessages.ImportWizardTemplate_desc);

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

		IPluginElement categoryElement = factory.createElement(extension);
		categoryElement.setName("category"); //$NON-NLS-1$
		categoryElement.setAttribute("id", getStringOption(KEY_PACKAGE_NAME) + ".sampleCategory"); //$NON-NLS-1$ //$NON-NLS-2$
		categoryElement.setAttribute("name", getStringOption(WIZARD_CATEGORY_NAME)); //$NON-NLS-1$

		IPluginElement wizardElement = factory.createElement(extension);
		wizardElement.setName("wizard"); //$NON-NLS-1$
		wizardElement.setAttribute("id", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(WIZARD_CLASS_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
		wizardElement.setAttribute("name", getStringOption(WIZARD_IMPORT_NAME)); //$NON-NLS-1$
		wizardElement.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(WIZARD_CLASS_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
		wizardElement.setAttribute("category", getStringOption(KEY_PACKAGE_NAME) + ".sampleCategory"); //$NON-NLS-1$ //$NON-NLS-2$
		wizardElement.setAttribute("icon", "icons/sample.png"); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement descriptionElement = factory.createElement(extension);
		descriptionElement.setName("description"); //$NON-NLS-1$
		descriptionElement.setText(PDETemplateMessages.ImportWizardTemplate_wizardDescription);

		wizardElement.add(descriptionElement);
		extension.add(categoryElement);
		extension.add(wizardElement);
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
			return packageName + ".importWizards"; //$NON-NLS-1$
		return "importWizards"; //$NON-NLS-1$
	}

	@Override
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.importWizards"; //$NON-NLS-1$
	}

	/**
	 * Returns a 2-D String array based on a comma seperated
	 * string of choices.
	 *
	 * @param iconLocations
	 * 				comma seperated string of icon placement options
	 * @return the 2-D array of choices
	 *
	 */
	protected String[][] fromCommaSeparated(String iconLocations) {
		StringTokenizer tokens = new StringTokenizer(iconLocations, ","); //$NON-NLS-1$
		String[][] choices = new String[tokens.countTokens() / 2][2];
		int x = 0, y = 0;
		while (tokens.hasMoreTokens()) {
			choices[x][y++] = tokens.nextToken();
			choices[x++][y--] = tokens.nextToken();
		}
		return choices;
	}
}
