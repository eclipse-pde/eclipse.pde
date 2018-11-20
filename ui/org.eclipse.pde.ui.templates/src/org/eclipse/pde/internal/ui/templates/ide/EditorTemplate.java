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
 *     Lars.Vogel <Lars.Vogel@vogella.com> - Bug 486247, 486261
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.ide;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.IHelpContextIds;
import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.PluginReference;
import org.eclipse.pde.ui.templates.TemplateOption;

public class EditorTemplate extends BaseEditorTemplate {
	public static final String EDITOR_CLASS_NAME = "editorClass"; //$NON-NLS-1$
	public static final String EDITOR_NAME = "editorName"; //$NON-NLS-1$
	public static final String EXTENSIONS = "extensions"; //$NON-NLS-1$

	/**
	 * Constructor for EditorTemplate.
	 */
	public EditorTemplate() {
		setPageCount(1);
		createOptions();
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		IPluginReference[] dep = new IPluginReference[4];
		dep[0] = new PluginReference("org.eclipse.core.runtime"); //$NON-NLS-1$
		dep[1] = new PluginReference("org.eclipse.ui"); //$NON-NLS-1$
		dep[2] = new PluginReference("org.eclipse.jface.text"); //$NON-NLS-1$
		dep[3] = new PluginReference("org.eclipse.ui.editors"); //$NON-NLS-1$
		return dep;
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_EDITOR);
		page.setTitle(PDETemplateMessages.EditorTemplate_title);
		page.setDescription(PDETemplateMessages.EditorTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	private void createOptions() {
		// first page
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.EditorTemplate_packageName, (String) null, 0);
		addOption(EDITOR_CLASS_NAME, PDETemplateMessages.EditorTemplate_editorClass, "XMLEditor", //$NON-NLS-1$
				0);
		addOption(EDITOR_NAME, PDETemplateMessages.EditorTemplate_editorName, PDETemplateMessages.EditorTemplate_defaultEditorName, 0);
		addOption(EXTENSIONS, PDETemplateMessages.EditorTemplate_fileExtension, "xml", //$NON-NLS-1$
				0);
	}

	@Override
	public String getSectionId() {
		return "editor"; //$NON-NLS-1$
	}

	@Override
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
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
	public boolean isDependentOnParentWizard() {
		return true;
	}

	@Override
	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		} else {
			validateContainerPage(source);
		}
	}

	private void validateContainerPage(TemplateOption source) {
		TemplateOption[] options = getOptions(0);
		for (TemplateOption nextOption : options) {
			if (nextOption.isRequired() && nextOption.isEmpty()) {
				flagMissingRequiredOption(nextOption);
				return;
			}
		}
		resetPageState();
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement editorElement = factory.createElement(extension);
		editorElement.setName("editor"); //$NON-NLS-1$
		editorElement.setAttribute("id", //$NON-NLS-1$
				getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(EDITOR_CLASS_NAME)); //$NON-NLS-1$
		editorElement.setAttribute("name", getStringOption(EDITOR_NAME)); //$NON-NLS-1$
		editorElement.setAttribute("icon", "icons/sample.png"); //$NON-NLS-1$ //$NON-NLS-2$
		editorElement.setAttribute("extensions", getStringOption(EXTENSIONS)); //$NON-NLS-1$

		editorElement.setAttribute("class", //$NON-NLS-1$
				getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(EDITOR_CLASS_NAME)); //$NON-NLS-1$
		editorElement.setAttribute("contributorClass", //$NON-NLS-1$
				"org.eclipse.ui.texteditor.BasicTextEditorActionContributor"); //$NON-NLS-1$
		extension.add(editorElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	@Override
	protected String getFormattedPackageName(String id) {
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".editors"; //$NON-NLS-1$
		return "editors"; //$NON-NLS-1$
	}
}
