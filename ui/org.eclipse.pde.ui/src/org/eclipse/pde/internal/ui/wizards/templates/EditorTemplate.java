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


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.wizard.*;

public class EditorTemplate extends BaseEditorTemplate {
	public static final String EDITOR_CLASS_NAME = "editorClass"; //$NON-NLS-1$
	public static final String EDITOR_NAME = "editorName"; //$NON-NLS-1$
	public static final String EXTENSIONS = "extensions"; //$NON-NLS-1$
	private static final String KEY_TITLE = "EditorTemplate.title"; //$NON-NLS-1$
	private static final String KEY_DESC = "EditorTemplate.desc"; //$NON-NLS-1$
	private static final String KEY_PACKAGE_LABEL = "EditorTemplate.packageName"; //$NON-NLS-1$
	private static final String KEY_CLASS_LABEL = "EditorTemplate.editorClass"; //$NON-NLS-1$
	private static final String KEY_EDITOR_LABEL = "EditorTemplate.editorName"; //$NON-NLS-1$
	private static final String KEY_EXTENSION_LABEL =
		"EditorTemplate.fileExtension"; //$NON-NLS-1$
	private static final String KEY_DEFAULT_EDITOR_NAME =
		"EditorTemplate.defaultEditorName"; //$NON-NLS-1$
	
	/**
	 * Constructor for EditorTemplate.
	 */
	public EditorTemplate() {
		setPageCount(1);
		createOptions();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		if (schemaVersion != null) {
			IPluginReference[] dep = new IPluginReference[5];
			dep[0] = new PluginReference("org.eclipse.core.runtime", null, 0); //$NON-NLS-1$
			dep[1] = new PluginReference("org.eclipse.ui", null, 0); //$NON-NLS-1$
			dep[2] = new PluginReference("org.eclipse.jface.text", null, 0); //$NON-NLS-1$
			dep[3] = new PluginReference("org.eclipse.ui.editors", null, 0); //$NON-NLS-1$
			dep[4] = new PluginReference("org.eclipse.ui.workbench.texteditor", null, 0); //$NON-NLS-1$
			return dep;
		}
		return super.getDependencies(schemaVersion);
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_EDITOR);
		page.setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		page.setDescription(PDEPlugin.getResourceString(KEY_DESC));
		wizard.addPage(page);
		markPagesAdded();
	}

	private void createOptions() {
		// first page	
		addOption(
			KEY_PACKAGE_NAME,
			PDEPlugin.getResourceString(KEY_PACKAGE_LABEL),
			(String) null,
			0);
		addOption(
			EDITOR_CLASS_NAME,
			PDEPlugin.getResourceString(KEY_CLASS_LABEL),
			"XMLEditor", //$NON-NLS-1$
			0);
		addOption(
			EDITOR_NAME,
			PDEPlugin.getResourceString(KEY_EDITOR_LABEL),
			PDEPlugin.getResourceString(KEY_DEFAULT_EDITOR_NAME),
			0);
		addOption(
			EXTENSIONS,
			PDEPlugin.getResourceString(KEY_EXTENSION_LABEL),
			"xml", //$NON-NLS-1$
			0);
	}

	public String getSectionId() {
		return "editor"; //$NON-NLS-1$
	}
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
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
		TemplateOption[] options = getOptions(0);
		for (int i = 0; i < options.length; i++) {
			TemplateOption nextOption = options[i];
			if (nextOption.isRequired() && nextOption.isEmpty()) {
				flagMissingRequiredOption(nextOption);
				return;
			}
		}
		resetPageState();
	}

	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement editorElement = factory.createElement(extension);
		editorElement.setName("editor"); //$NON-NLS-1$
		editorElement.setAttribute(
			"id", //$NON-NLS-1$
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(EDITOR_CLASS_NAME)); //$NON-NLS-1$
		editorElement.setAttribute("name", getStringOption(EDITOR_NAME)); //$NON-NLS-1$
		editorElement.setAttribute("icon", "icons/sample.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		editorElement.setAttribute("extensions", getStringOption(EXTENSIONS)); //$NON-NLS-1$

		editorElement.setAttribute(
			"class", //$NON-NLS-1$
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(EDITOR_CLASS_NAME)); //$NON-NLS-1$
		editorElement.setAttribute(
			"contributorClass", //$NON-NLS-1$
			"org.eclipse.ui.texteditor.BasicTextEditorActionContributor"); //$NON-NLS-1$
		extension.add(editorElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}
}