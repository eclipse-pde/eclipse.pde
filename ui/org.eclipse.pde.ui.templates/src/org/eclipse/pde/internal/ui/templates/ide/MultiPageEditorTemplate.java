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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.templates.IHelpContextIds;
import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.PluginReference;
import org.eclipse.pde.ui.templates.TemplateOption;

public class MultiPageEditorTemplate extends BaseEditorTemplate {
	/**
	 * Constructor for MultiPageEditorTemplate.
	 */
	public MultiPageEditorTemplate() {
		setPageCount(1);
		createOptions();
	}

	public String getSectionId() {
		return "multiPageEditor"; //$NON-NLS-1$
	}

	public IPluginReference[] getDependencies(String schemaVersion) {
		if (schemaVersion != null) {
			IPluginReference[] dep = new IPluginReference[6];
			dep[0] = new PluginReference("org.eclipse.jface.text", null, 0); //$NON-NLS-1$
			dep[1] = new PluginReference("org.eclipse.core.resources", null, 0); //$NON-NLS-1$
			dep[2] = new PluginReference("org.eclipse.ui", null, 0); //$NON-NLS-1$
			dep[3] = new PluginReference("org.eclipse.ui.editors", null, 0); //$NON-NLS-1$
			dep[4] = new PluginReference("org.eclipse.ui.ide", null, 0); //$NON-NLS-1$
			dep[5] = new PluginReference("org.eclipse.core.runtime", null, 0); //$NON-NLS-1$
			return dep;
		}
		return super.getDependencies(schemaVersion);
	}

	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	private void createOptions() {
		// first page	
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.MultiPageEditorTemplate_packageName, (String) null, 0);
		addOption("editorClassName", //$NON-NLS-1$
				PDETemplateMessages.MultiPageEditorTemplate_className, "MultiPageEditor", //$NON-NLS-1$
				0);
		addOption("contributorClassName", //$NON-NLS-1$
				PDETemplateMessages.MultiPageEditorTemplate_contributor, "MultiPageEditorContributor", //$NON-NLS-1$
				0);
		addOption("editorName", //$NON-NLS-1$
				PDETemplateMessages.MultiPageEditorTemplate_editorName, PDETemplateMessages.MultiPageEditorTemplate_defaultEditorName, 0);
		addOption("extensions", //$NON-NLS-1$
				PDETemplateMessages.MultiPageEditorTemplate_extensions, "mpe", //$NON-NLS-1$
				0);
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

	public boolean isDependentOnParentWizard() {
		return true;
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_MULTIPAGE_EDITOR);
		page.setTitle(PDETemplateMessages.MultiPageEditorTemplate_title);
		page.setDescription(PDETemplateMessages.MultiPageEditorTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

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

	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.editors", true); //$NON-NLS-1$
		IPluginModelFactory factory = model.getPluginFactory();

		String editorClassName = getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption("editorClassName"); //$NON-NLS-1$ //$NON-NLS-2$
		String contributorClassName = getStringOption(KEY_PACKAGE_NAME) + "." //$NON-NLS-1$
				+ getStringOption("contributorClassName"); //$NON-NLS-1$

		IPluginElement editorElement = factory.createElement(extension);
		editorElement.setName("editor"); //$NON-NLS-1$
		editorElement.setAttribute("id", editorClassName); //$NON-NLS-1$
		editorElement.setAttribute("name", getStringOption("editorName")); //$NON-NLS-1$ //$NON-NLS-2$
		editorElement.setAttribute("icon", "icons/sample.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		editorElement.setAttribute("extensions", getStringOption("extensions")); //$NON-NLS-1$ //$NON-NLS-2$

		editorElement.setAttribute("class", editorClassName); //$NON-NLS-1$
		editorElement.setAttribute("contributorClass", contributorClassName); //$NON-NLS-1$
		extension.add(editorElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#formatPackageName(java.lang.String)
	 */
	protected String getFormattedPackageName(String id) {
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".editors"; //$NON-NLS-1$
		return "editors"; //$NON-NLS-1$
	}

}
