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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.wizard.*;

public class MultiPageEditorTemplate extends BaseEditorTemplate {
	private static final String KEY_TITLE = "MultiPageEditorTemplate.title"; //$NON-NLS-1$
	private static final String KEY_DESC = "MultiPageEditorTemplate.desc"; //$NON-NLS-1$
	private static final String KEY_PACKAGE_LABEL =
		"MultiPageEditorTemplate.packageName"; //$NON-NLS-1$
	private static final String KEY_CLASS_LABEL =
		"MultiPageEditorTemplate.className"; //$NON-NLS-1$
	private static final String KEY_CONTRIBUTOR_LABEL =
		"MultiPageEditorTemplate.contributor"; //$NON-NLS-1$
	private static final String KEY_EDITOR_LABEL =
		"MultiPageEditorTemplate.editorName"; //$NON-NLS-1$
	private static final String KEY_DEFAULT_EDITOR_NAME =
		"MultiPageEditorTemplate.defaultEditorName"; //$NON-NLS-1$
	private static final String KEY_EXTENSIONS_LABEL =
		"MultiPageEditorTemplate.extensions"; //$NON-NLS-1$

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
			IPluginReference[] dep = new IPluginReference[7];
			dep[0] = new PluginReference("org.eclipse.jface.text", null, 0); //$NON-NLS-1$
			dep[1] = new PluginReference("org.eclipse.core.resources", null, 0); //$NON-NLS-1$
			dep[2] = new PluginReference("org.eclipse.ui", null, 0); //$NON-NLS-1$
			dep[3] = new PluginReference("org.eclipse.ui.editors", null, 0); //$NON-NLS-1$
			dep[4] = new PluginReference("org.eclipse.ui.ide", null, 0); //$NON-NLS-1$
			dep[5] = new PluginReference("org.eclipse.ui.workbench.texteditor", null, 0); //$NON-NLS-1$
			dep[6] = new PluginReference("org.eclipse.core.runtime", null, 0); //$NON-NLS-1$
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
		addOption(
			KEY_PACKAGE_NAME,
			PDEPlugin.getResourceString(KEY_PACKAGE_LABEL),
			(String) null,
			0);
		addOption(
			"editorClassName", //$NON-NLS-1$
			PDEPlugin.getResourceString(KEY_CLASS_LABEL),
			"MultiPageEditor", //$NON-NLS-1$
			0);
		addOption(
			"contributorClassName", //$NON-NLS-1$
			PDEPlugin.getResourceString(KEY_CONTRIBUTOR_LABEL),
			"MultiPageEditorContributor", //$NON-NLS-1$
			0);
		addOption(
			"editorName", //$NON-NLS-1$
			PDEPlugin.getResourceString(KEY_EDITOR_LABEL),
			PDEPlugin.getResourceString(KEY_DEFAULT_EDITOR_NAME),
			0);
		addOption(
			"extensions", //$NON-NLS-1$
			PDEPlugin.getResourceString(KEY_EXTENSIONS_LABEL),
			"mpe",  //$NON-NLS-1$
			0);
	}

	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, id + ".editors"); //$NON-NLS-1$
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".editors"); //$NON-NLS-1$
	}

	public boolean isDependentOnParentWizard() {
		return true;
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_MULTIPAGE_EDITOR);
		page.setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		page.setDescription(PDEPlugin.getResourceString(KEY_DESC));
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

		String editorClassName =
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption("editorClassName"); //$NON-NLS-1$ //$NON-NLS-2$
		String contributorClassName =
			getStringOption(KEY_PACKAGE_NAME)
				+ "." //$NON-NLS-1$
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
	
}
