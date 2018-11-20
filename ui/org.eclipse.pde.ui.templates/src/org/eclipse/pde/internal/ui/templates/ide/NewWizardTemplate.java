/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473694, 486261
 *******************************************************************************/

package org.eclipse.pde.internal.ui.templates.ide;

import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.PluginReference;

public class NewWizardTemplate extends PDETemplateSection {
	public NewWizardTemplate() {
		setPageCount(1);
		createOptions();
	}

	@Override
	public String getSectionId() {
		return "newWizard"; //$NON-NLS-1$
	}

	@Override
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	private void createOptions() {
		// first page
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.NewWizardTemplate_packageName, (String) null, 0);
		addOption("categoryId", PDETemplateMessages.NewWizardTemplate_categoryId, (String) null, 0); //$NON-NLS-1$
		addOption("categoryName", PDETemplateMessages.NewWizardTemplate_categoryName, "Sample Wizards", 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption("wizardClassName", PDETemplateMessages.NewWizardTemplate_className, "SampleNewWizard", 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption("wizardPageClassName", PDETemplateMessages.NewWizardTemplate_pageClassName, "SampleNewWizardPage", 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption("wizardName", PDETemplateMessages.NewWizardTemplate_wizardName, PDETemplateMessages.NewWizardTemplate_defaultName, 0); //$NON-NLS-1$
		addOption("extension", PDETemplateMessages.NewWizardTemplate_extension, "mpe", 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption("initialFileName", PDETemplateMessages.NewWizardTemplate_fileName, "new_file.mpe", 0); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id));
		initializeOption("categoryId", id); //$NON-NLS-1$
	}

	@Override
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId));
		initializeOption("categoryId", pluginId); //$NON-NLS-1$
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList<PluginReference> result = new ArrayList<>();
		result.add(new PluginReference("org.eclipse.core.resources")); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.ui")); //$NON-NLS-1$
		if (schemaVersion != null) {
			result.add(new PluginReference("org.eclipse.ui.ide")); //$NON-NLS-1$
			result.add(new PluginReference("org.eclipse.core.runtime")); //$NON-NLS-1$
		}
		return result.toArray(new IPluginReference[result.size()]);
	}

	@Override
	public boolean isDependentOnParentWizard() {
		return true;
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_NEW_WIZARD);
		page.setTitle(PDETemplateMessages.NewWizardTemplate_title);
		page.setDescription(PDETemplateMessages.NewWizardTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	@Override
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.newWizards"; //$NON-NLS-1$
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.newWizards", true); //$NON-NLS-1$
		IPluginModelFactory factory = model.getPluginFactory();

		String cid = getStringOption("categoryId"); //$NON-NLS-1$

		createCategory(extension, cid);
		String fullClassName = getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption("wizardClassName"); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement viewElement = factory.createElement(extension);
		viewElement.setName("wizard"); //$NON-NLS-1$
		viewElement.setAttribute("id", fullClassName); //$NON-NLS-1$
		viewElement.setAttribute("name", getStringOption("wizardName")); //$NON-NLS-1$ //$NON-NLS-2$
		viewElement.setAttribute("icon", "icons/sample.png"); //$NON-NLS-1$ //$NON-NLS-2$
		viewElement.setAttribute("class", fullClassName); //$NON-NLS-1$
		viewElement.setAttribute("category", cid); //$NON-NLS-1$
		extension.add(viewElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	private void createCategory(IPluginExtension extension, String id) throws CoreException {
		IPluginObject[] children = extension.getChildren();
		for (IPluginObject child : children) {
			IPluginElement element = (IPluginElement) child;
			if (element.getName().equalsIgnoreCase("category")) { //$NON-NLS-1$
				IPluginAttribute att = element.getAttribute("id"); //$NON-NLS-1$
				if (att != null) {
					String cid = att.getValue();
					if (cid != null && cid.equals(id))
						return;
				}
			}
		}
		IPluginElement categoryElement = model.getFactory().createElement(extension);
		categoryElement.setName("category"); //$NON-NLS-1$
		categoryElement.setAttribute("name", getStringOption("categoryName")); //$NON-NLS-1$ //$NON-NLS-2$
		categoryElement.setAttribute("id", id); //$NON-NLS-1$
		extension.add(categoryElement);
	}

	@Override
	public String[] getNewFiles() {
		return new String[] {"icons/"}; //$NON-NLS-1$
	}

	@Override
	protected String getFormattedPackageName(String id) {
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".wizards"; //$NON-NLS-1$
		return "wizards"; //$NON-NLS-1$
	}
}
