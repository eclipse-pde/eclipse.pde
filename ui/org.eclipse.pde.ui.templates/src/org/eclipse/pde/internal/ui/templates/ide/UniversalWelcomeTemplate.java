/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

import java.io.File;
import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.AbstractTemplateSection;
import org.eclipse.pde.ui.templates.PluginReference;

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
		addOption(KEY_INTRO_DIR, PDETemplateMessages.UniversalWelcomeTemplate_key_directoryName, "intro", 0); //$NON-NLS-1$
		addOption(KEY_PATH, PDETemplateMessages.UniversalWelcomeTemplate_key_targetPage, new String[][] { {"overview/@", PDETemplateMessages.UniversalWelcomeTemplate_page_Overview}, {"tutorials/@", PDETemplateMessages.UniversalWelcomeTemplate_page_Tutorials}, //$NON-NLS-1$ //$NON-NLS-2$
				{"firststeps/@", PDETemplateMessages.UniversalWelcomeTemplate_page_FirstSteps}, {"samples/@", PDETemplateMessages.UniversalWelcomeTemplate_page_Samples}, //$NON-NLS-1$ //$NON-NLS-2$
				{"whatsnew/@", PDETemplateMessages.UniversalWelcomeTemplate_page_Whatsnew}, {"migrate/@", PDETemplateMessages.UniversalWelcomeTemplate_page_Migrate}, //$NON-NLS-1$ //$NON-NLS-2$
				{"webresources/@", PDETemplateMessages.UniversalWelcomeTemplate_page_WebResources}}, "overview/@", 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption(KEY_LINK_URL, PDETemplateMessages.UniversalWelcomeTemplate_linkUrl, "http://www.eclipse.org", 0); //$NON-NLS-1$
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_UNIVERSAL_WELCOME);
		page.setTitle(PDETemplateMessages.IntroTemplate_title);
		page.setDescription(PDETemplateMessages.IntroTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	@Override
	public boolean isDependentOnParentWizard() {
		return true;
	}

	@Override
	public String getSectionId() {
		return "universalWelcome"; //$NON-NLS-1$
	}

	@Override
	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		pluginId = data.getId();
	}

	@Override
	public void initializeFields(IPluginModelBase model) {
		pluginId = model.getPluginBase().getId();
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();

		IPluginExtension extension = createExtension("org.eclipse.ui.intro.configExtension", false); //$NON-NLS-1$

		IPluginElement element = model.getPluginFactory().createElement(extension);
		element.setName("configExtension"); //$NON-NLS-1$
		element.setAttribute("configId", //$NON-NLS-1$
				"org.eclipse.ui.intro.universalConfig"); //$NON-NLS-1$
		element.setAttribute("content", getStringOption(KEY_INTRO_DIR) //$NON-NLS-1$
				+ "/sample.xml"); //$NON-NLS-1$
		extension.add(element);

		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	@Override
	protected boolean isOkToCreateFolder(File sourceFolder) {
		return true;
	}

	/**
	 * @see AbstractTemplateSection#isOkToCreateFile(File)
	 */
	@Override
	protected boolean isOkToCreateFile(File sourceFile) {
		return true;
	}

	@Override
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.intro.configExtension"; //$NON-NLS-1$
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList<PluginReference> result = new ArrayList<>();

		// We really need Eclipse 3.2 or higher but since Universal
		// appears in 3.2 for the first time, just depending on
		// its presence has the same effect.
		result.add(new PluginReference("org.eclipse.ui.intro")); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.ui.intro.universal")); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.ui")); //$NON-NLS-1$

		return result.toArray(new IPluginReference[result.size()]);
	}

	@Override
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	/*
	 * We are going to compute some values even though we are
	 * not exposing them as options.
	 */
	@Override
	public String getStringOption(String name) {
		if (name.equals(KEY_EXTENSION_ID)) {
			return stripNonAlphanumeric(pluginId) + "-introExtension"; //$NON-NLS-1$
		}
		if (name.equals(KEY_LINK_ID)) {
			return stripNonAlphanumeric(pluginId) + "-introLink"; //$NON-NLS-1$
		}
		return super.getStringOption(name);
	}

	/*
	 * Strips any non alphanumeric characters from the string so as not to break the css
	 */
	private String stripNonAlphanumeric(String id) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < id.length(); i++) {
			char next = id.charAt(i);
			if (Character.isLetterOrDigit(next)) {
				result.append(next);
			}
		}
		return result.toString();
	}

	@Override
	public String[] getNewFiles() {
		return new String[] {getStringOption(KEY_INTRO_DIR) + "/"}; //$NON-NLS-1$
	}
}