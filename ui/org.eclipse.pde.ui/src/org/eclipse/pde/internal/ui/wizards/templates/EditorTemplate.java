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
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.ui.IPluginStructureData;

public class EditorTemplate extends PDETemplateSection {
	public static final String EDITOR_CLASS_NAME = "editorClass";
	public static final String EDITOR_NAME = "editorName";
	public static final String EXTENSIONS = "extensions";
	private static final String KEY_TITLE = "EditorTemplate.title";
	private static final String KEY_DESC = "EditorTemplate.desc";
	private static final String KEY_PACKAGE_LABEL = "EditorTemplate.packageName";
	private static final String KEY_CLASS_LABEL = "EditorTemplate.editorClass";
	private static final String KEY_EDITOR_LABEL = "EditorTemplate.editorName";
	private static final String KEY_EXTENSION_LABEL =
		"EditorTemplate.fileExtension";
	private static final String KEY_DEFAULT_EDITOR_NAME =
		"EditorTemplate.defaultEditorName";

	/**
	 * Constructor for EditorTemplate.
	 */
	public EditorTemplate() {
		setPageCount(1);
		createOptions();
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
			"XMLEditor",
			0);
		addOption(
			EDITOR_NAME,
			PDEPlugin.getResourceString(KEY_EDITOR_LABEL),
			PDEPlugin.getResourceString(KEY_DEFAULT_EDITOR_NAME),
			0);
		addOption(
			EXTENSIONS,
			PDEPlugin.getResourceString(KEY_EXTENSION_LABEL),
			"xml",
			0);
	}

	public String getSectionId() {
		return "editor";
	}
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	protected void initializeFields(IPluginStructureData sdata, IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String pluginId = sdata.getPluginId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".editors");
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".editors");
	}

	public boolean isDependentOnFirstPage() {
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

	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.editors";
	}

	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.editors", true);
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement editorElement = factory.createElement(extension);
		editorElement.setName("editor");
		editorElement.setAttribute(
			"id",
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(EDITOR_CLASS_NAME));
		editorElement.setAttribute("name", getStringOption(EDITOR_NAME));
		editorElement.setAttribute("icon", "icons/sample.gif");
		editorElement.setAttribute("extensions", getStringOption(EXTENSIONS));

		editorElement.setAttribute(
			"class",
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(EDITOR_CLASS_NAME));
		editorElement.setAttribute(
			"contributorClass",
			"org.eclipse.ui.texteditor.BasicTextEditorActionContributor");
		extension.add(editorElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}
}