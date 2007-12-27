/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.templates.ide;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.templates.IHelpContextIds;
import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.internal.ui.templates.PDETemplateSection;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.PluginReference;

public class NewWizardTemplate extends PDETemplateSection {
	public NewWizardTemplate() {
		setPageCount(1);
		createOptions();
	}

	public String getSectionId() {
		return "newWizard"; //$NON-NLS-1$
	}

	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
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

	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id));
		initializeOption("categoryId", id); //$NON-NLS-1$
	}

	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId));
		initializeOption("categoryId", pluginId); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList result = new ArrayList();
		result.add(new PluginReference("org.eclipse.core.resources", null, 0)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.ui", null, 0)); //$NON-NLS-1$
		if (schemaVersion != null) {
			result.add(new PluginReference("org.eclipse.ui.ide", null, 0)); //$NON-NLS-1$
			result.add(new PluginReference("org.eclipse.core.runtime", null, 0)); //$NON-NLS-1$
		}
		return (IPluginReference[]) result.toArray(new IPluginReference[result.size()]);
	}

	public boolean isDependentOnParentWizard() {
		return true;
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_NEW_WIZARD);
		page.setTitle(PDETemplateMessages.NewWizardTemplate_title);
		page.setDescription(PDETemplateMessages.NewWizardTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.newWizards"; //$NON-NLS-1$
	}

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
		viewElement.setAttribute("icon", "icons/sample.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		viewElement.setAttribute("class", fullClassName); //$NON-NLS-1$
		viewElement.setAttribute("category", cid); //$NON-NLS-1$
		extension.add(viewElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	private void createCategory(IPluginExtension extension, String id) throws CoreException {
		IPluginObject[] elements = extension.getChildren();
		for (int i = 0; i < elements.length; i++) {
			IPluginElement element = (IPluginElement) elements[i];
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#getFoldersToInclude()
	 */
	public String[] getNewFiles() {
		return new String[] {"icons/"}; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#formatPackageName(java.lang.String)
	 */
	protected String getFormattedPackageName(String id) {
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".wizards"; //$NON-NLS-1$
		return "wizards"; //$NON-NLS-1$
	}
}
