/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.ide;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.IHelpContextIds;
import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.PluginReference;

public class ExtensbileEditorTemplate extends BaseEditorTemplate {

	private static final String FILE_EXTENSION = "fileExtension"; //$NON-NLS-1$
	private String javaClassPrefix;
	private String contentTypeName;

	public ExtensbileEditorTemplate() {
		setPageCount(1);
		createOptions();
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		IPluginReference[] dep = new IPluginReference[7];
		dep[0] = new PluginReference("org.eclipse.core.runtime"); //$NON-NLS-1$
		dep[1] = new PluginReference("org.eclipse.ui"); //$NON-NLS-1$
		dep[2] = new PluginReference("org.eclipse.jface.text"); //$NON-NLS-1$
		dep[3] = new PluginReference("org.eclipse.ui.editors"); //$NON-NLS-1$
		dep[4] = new PluginReference("org.eclipse.ui.genericeditor"); //$NON-NLS-1$
		dep[5] = new PluginReference("org.eclipse.core.filebuffers"); //$NON-NLS-1$
		dep[6] = new PluginReference("org.eclipse.core.resources"); //$NON-NLS-1$

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
		addOption(FILE_EXTENSION, PDETemplateMessages.ExtensibleEditorTemplate_targetFileExtension,
				"project", //$NON-NLS-1$
				0);
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.EditorTemplate_packageName, (String) null, 0);
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
		String extensionId = toJavaIdentifier(getStringOption(FILE_EXTENSION));
		this.javaClassPrefix = Character.toUpperCase(extensionId.charAt(0)) + extensionId.substring(1);
		this.contentTypeName = getStringOption(KEY_PACKAGE_NAME) + '.' + extensionId;
		addOption("javaClassPrefix", "/!\\ Shouldn't be presented in UI /!\\", javaClassPrefix, -1); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public boolean isDependentOnParentWizard() {
		return true;
	}

	@Override
	public void execute(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		super.execute(project, model, monitor);
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginModelFactory factory = model.getPluginFactory();
		{
			IPluginExtension contentTypeExtension = createExtension("org.eclipse.core.contenttype.contentTypes", true); //$NON-NLS-1$
			IPluginElement contentTypeExtensionElement = factory.createElement(contentTypeExtension);
			contentTypeExtensionElement.setName("content-type"); //$NON-NLS-1$
			contentTypeExtensionElement.setAttribute("id", contentTypeName); //$NON-NLS-1$
			contentTypeExtensionElement.setAttribute("name", contentTypeName); //$NON-NLS-1$
			contentTypeExtensionElement.setAttribute("base-type", //$NON-NLS-1$
					IContentTypeManager.CT_TEXT);
			contentTypeExtensionElement.setAttribute("file-extensions", getStringOption(FILE_EXTENSION)); //$NON-NLS-1$
			contentTypeExtension.add(contentTypeExtensionElement);
			plugin.add(contentTypeExtension);
		}
		{
			IPluginExtension editorsExtension = createExtension("org.eclipse.ui.editors", true); //$NON-NLS-1$
			IPluginElement editorContentTypeBindingElement = factory.createElement(editorsExtension);
			editorContentTypeBindingElement.setName("editorContentTypeBinding"); //$NON-NLS-1$
			editorContentTypeBindingElement.setAttribute("contentTypeId", contentTypeName); //$NON-NLS-1$
			editorContentTypeBindingElement.setAttribute("editorId", "org.eclipse.ui.genericeditor.GenericEditor"); //$NON-NLS-1$ //$NON-NLS-2$
			editorsExtension.add(editorContentTypeBindingElement);
			plugin.add(editorsExtension);
		}
		{
			IPluginExtension presentationExtension = createExtension("org.eclipse.ui.genericeditor.presentationReconcilers", true); //$NON-NLS-1$
			IPluginElement presentationExtensionElement = factory.createElement(presentationExtension);
			presentationExtensionElement.setName("presentationReconciler"); //$NON-NLS-1$
			presentationExtensionElement.setAttribute("class", //$NON-NLS-1$
					getStringOption(KEY_PACKAGE_NAME) + '.' + javaClassPrefix + "PresentationReconciler"); //$NON-NLS-1$
			presentationExtensionElement.setAttribute("contentType", contentTypeName); //$NON-NLS-1$
			presentationExtension.add(presentationExtensionElement);
			plugin.add(presentationExtension);
		}
		{
			IPluginExtension presentationExtension = createExtension("org.eclipse.ui.genericeditor.hoverProviders", //$NON-NLS-1$
					true);
			IPluginElement presentationExtensionElement = factory.createElement(presentationExtension);
			presentationExtensionElement.setName("hoverProvider"); //$NON-NLS-1$
			presentationExtensionElement.setAttribute("class", //$NON-NLS-1$
					getStringOption(KEY_PACKAGE_NAME) + '.' + javaClassPrefix + "HoverProvider"); //$NON-NLS-1$
			presentationExtensionElement.setAttribute("contentType", contentTypeName); //$NON-NLS-1$
			presentationExtension.add(presentationExtensionElement);
			plugin.add(presentationExtension);
		}
		{
			IPluginExtension presentationExtension = createExtension(
					"org.eclipse.ui.genericeditor.contentAssistProcessors", //$NON-NLS-1$
					true);
			IPluginElement presentationExtensionElement = factory.createElement(presentationExtension);
			presentationExtensionElement.setName("contentAssistProcessor"); //$NON-NLS-1$
			presentationExtensionElement.setAttribute("class", //$NON-NLS-1$
					getStringOption(KEY_PACKAGE_NAME) + '.' + javaClassPrefix + "ContentAssistProcessor"); //$NON-NLS-1$
			presentationExtensionElement.setAttribute("contentType", contentTypeName); //$NON-NLS-1$
			presentationExtension.add(presentationExtensionElement);
			plugin.add(presentationExtension);
		}
		{
			IPluginExtension presentationExtension = createExtension("org.eclipse.ui.genericeditor.autoEditStrategies", //$NON-NLS-1$
					true);
			IPluginElement presentationExtensionElement = factory.createElement(presentationExtension);
			presentationExtensionElement.setName("autoEditStrategy"); //$NON-NLS-1$
			presentationExtensionElement.setAttribute("class", //$NON-NLS-1$
					getStringOption(KEY_PACKAGE_NAME) + '.' + javaClassPrefix + "AutoEditStrategy"); //$NON-NLS-1$
			presentationExtensionElement.setAttribute("contentType", contentTypeName); //$NON-NLS-1$
			presentationExtension.add(presentationExtensionElement);
			plugin.add(presentationExtension);
		}
		{
			IPluginExtension reconcilerExtension = createExtension("org.eclipse.ui.genericeditor.reconcilers", true); //$NON-NLS-1$
			IPluginElement reconcilerExtensionElement = factory.createElement(reconcilerExtension);
			reconcilerExtensionElement.setName("reconciler"); //$NON-NLS-1$
			reconcilerExtensionElement.setAttribute("class", //$NON-NLS-1$
					getStringOption(KEY_PACKAGE_NAME) + '.' + javaClassPrefix + "Reconciler"); //$NON-NLS-1$
			reconcilerExtensionElement.setAttribute("contentType", contentTypeName); //$NON-NLS-1$
			reconcilerExtension.add(reconcilerExtensionElement);
			plugin.add(reconcilerExtension);
		}
		{
			IPluginExtension documentSetupExtension = createExtension("org.eclipse.core.filebuffers.documentSetup", //$NON-NLS-1$
					true);
			IPluginElement presentationExtensionElement = factory.createElement(documentSetupExtension);
			presentationExtensionElement.setName("participant"); //$NON-NLS-1$
			presentationExtensionElement.setAttribute("class", //$NON-NLS-1$
					getStringOption(KEY_PACKAGE_NAME) + ".ValidatorDocumentSetupParticipant"); //$NON-NLS-1$
			presentationExtensionElement.setAttribute("contentTypeId", contentTypeName); //$NON-NLS-1$
			presentationExtensionElement.setAttribute("extensions", getStringOption(FILE_EXTENSION)); //$NON-NLS-1$
			documentSetupExtension.add(presentationExtensionElement);
			plugin.add(documentSetupExtension);
		}
	}

	@Override
	public String[] getNewFiles() {
		// so "icons" entry is not generated in build.properties
		return new String[0];
	}

	private String toJavaIdentifier(String str) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			if (Character.isJavaIdentifierStart(str.charAt(0))
					|| (i > 0 && Character.isJavaIdentifierPart(str.charAt(i)))) {
				sb.append(str.charAt(i));
			} else {
				sb.append('_');
				sb.append((int) str.charAt(i));
			}
		}
		return sb.toString();
	}

}
