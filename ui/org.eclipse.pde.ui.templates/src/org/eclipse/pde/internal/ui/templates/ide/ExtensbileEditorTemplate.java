/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
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

public class ExtensbileEditorTemplate extends BaseEditorTemplate {

	public static final String PRESENTATION_RECONCILER_CLASS_NAME = "presentationClass"; //$NON-NLS-1$

	public ExtensbileEditorTemplate() {
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
		dep[3] = new PluginReference("org.eclipse.ui.genericeditor"); //$NON-NLS-1$

		return dep;
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_EDITOR);
		page.setTitle(PDETemplateMessages.ExtensibleEditorTemplate_title);
		page.setDescription(PDETemplateMessages.ExtensibleEditorTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	private void createOptions() {
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.EditorTemplate_packageName, (String) null, 0);
		addOption(PRESENTATION_RECONCILER_CLASS_NAME, PDETemplateMessages.ExtensibleEditorTemplate_reconcilerClass,
				"TargetPlatformPresentationReconciler", //$NON-NLS-1$
				0);
	}

	@Override
	public String getSectionId() {
		return "extensibleEditor"; //$NON-NLS-1$
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
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.genericeditor.presentationReconcilers", true); //$NON-NLS-1$
		IPluginModelFactory factory = model.getPluginFactory();
		IPluginElement extensionElement = factory.createElement(extension);
		extensionElement.setName("presentationReconciler"); //$NON-NLS-1$
		extensionElement.setAttribute("class", //$NON-NLS-1$
				getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(PRESENTATION_RECONCILER_CLASS_NAME)); //$NON-NLS-1$
		extensionElement.setAttribute("contentType", "org.eclipse.pde.targetFile"); //$NON-NLS-1$//$NON-NLS-2$
		extension.add(extensionElement);
		plugin.add(extension);
	}

	@Override
	protected String getFormattedPackageName(String id) {
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".reconciler.presentation"; //$NON-NLS-1$
		return ".reconciler.presentation"; //$NON-NLS-1$
	}

}
