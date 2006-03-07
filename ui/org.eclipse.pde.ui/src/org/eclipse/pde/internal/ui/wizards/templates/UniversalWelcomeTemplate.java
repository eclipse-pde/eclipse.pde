/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.templates;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.TemplateOption;

/**
 * Creates a template for contributing to the Universal Welcome
 * intro. Requires Eclipse version 3.2.
 * 
 *  @since 3.2
 */

public class UniversalWelcomeTemplate extends PDETemplateSection {
	private static final String KEY_LINK_ID = "linkId"; //$NON-NLS-1$

	private static final String KEY_EXTENSION_ID = "extensionId"; //$NON-NLS-1$

	private static final String KEY_INTRO_DIR = "introDir"; //$NON-NLS-1$

	private static final String KEY_PATH = "path"; //$NON-NLS-1$

	private static final String KEY_LINK_URL = "linkUrl"; //$NON-NLS-1$

	private String pluginId;

	public UniversalWelcomeTemplate() {
		setPageCount(1);
		createOptions();
	}

	private void createOptions() {
		// options
		addOption(KEY_INTRO_DIR, PDEUIMessages.UniversalWelcomeTemplate_key_directoryName, "intro", 0); //$NON-NLS-1$
		addOption(KEY_PATH, PDEUIMessages.UniversalWelcomeTemplate_key_targetPage, new String[][] {
				{ "overview/@", PDEUIMessages.UniversalWelcomeTemplate_page_Overview }, { "tutorials/@", PDEUIMessages.UniversalWelcomeTemplate_page_Tutorials }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{ "firststeps/@", PDEUIMessages.UniversalWelcomeTemplate_page_FirstSteps }, { "samples/@", PDEUIMessages.UniversalWelcomeTemplate_page_Samples }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{ "whatsnew/@", PDEUIMessages.UniversalWelcomeTemplate_page_Whatsnew }, { "migrate/@", PDEUIMessages.UniversalWelcomeTemplate_page_Migrate }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{ "webresources/@", PDEUIMessages.UniversalWelcomeTemplate_page_WebResources } }, "overview/@", 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		addOption(KEY_LINK_URL, PDEUIMessages.UniversalWelcomeTemplate_linkUrl, "http://www.eclipse.org", 0); //$NON-NLS-1$
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_UNIVERSAL_WELCOME);
		page.setTitle(PDEUIMessages.IntroTemplate_title);
		page.setDescription(PDEUIMessages.IntroTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	public boolean isDependentOnParentWizard() {
		return true;
	}

	public String getSectionId() {
		return "universalWelcome"; //$NON-NLS-1$
	}

	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		pluginId = data.getId();
	}

	public void initializeFields(IPluginModelBase model) {
		pluginId = model.getPluginBase().getId();
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

		IPluginExtension extension = createExtension(
				"org.eclipse.ui.intro.configExtension", false); //$NON-NLS-1$

		IPluginElement element = model.getPluginFactory().createElement(
				extension);
		element.setName("configExtension"); //$NON-NLS-1$
		element
				.setAttribute("configId", //$NON-NLS-1$
						"org.eclipse.ui.intro.universalConfig"); //$NON-NLS-1$
		element.setAttribute("content", getStringOption(KEY_INTRO_DIR) //$NON-NLS-1$
				+ "/sample.xml"); //$NON-NLS-1$
		extension.add(element);

		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	protected boolean isOkToCreateFolder(File sourceFolder) {
		return true;
	}

	/**
	 * @see AbstractTemplateSection#isOkToCreateFile(File)
	 */
	protected boolean isOkToCreateFile(File sourceFile) {
		return true;
	}

	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.intro.configExtension"; //$NON-NLS-1$
	}

	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList result = new ArrayList();

		// We really need Eclipse 3.2 or higher but since Universal
		// appears in 3.2 for the first time, just depending on
		// its presence has the same effect.
		result.add(new PluginReference("org.eclipse.ui.intro", null, 0)); //$NON-NLS-1$
		result.add(new PluginReference(
				"org.eclipse.ui.intro.universal", null, 0)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.ui", null, 0)); //$NON-NLS-1$

		return (IPluginReference[]) result.toArray(new IPluginReference[result
				.size()]);
	}

	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	/*
	 * We are going to compute some values even though we are
	 * not exposing them as options.
	 */
	public String getStringOption(String name) {
		if (name.equals(KEY_EXTENSION_ID)) {
			return pluginId + ".introExtension"; //$NON-NLS-1$
		}
		if (name.equals(KEY_LINK_ID)) {
			return pluginId + "-introLink"; //$NON-NLS-1$
		}
		return super.getStringOption(name);
	}

	public String[] getNewFiles() {
		return new String[] { getStringOption(KEY_INTRO_DIR) + "/" }; //$NON-NLS-1$
	}
}