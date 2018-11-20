/*******************************************************************************
 * Copyright (c) 2015, 2016 OPCoach
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Olivier Prouvost <olivier.prouvost@opcoach.com> - initial API and implementation (bug #481340)
 *     Patrik Suzzi <psuzzi@gmail.com> - Bug 492292
 *******************************************************************************/

package org.eclipse.pde.internal.ui.templates.e4;

import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.PluginReference;

public class E4ToolbarContributionTemplate extends PDETemplateSection {

	static final String E4_FRAGMENT_FILE = "fragment.e4xmi"; //$NON-NLS-1$

	/**
	 * Constructor for HelloWorldTemplate.
	 */
	public E4ToolbarContributionTemplate() {
		setPageCount(1);
		createOptions();
	}

	@Override
	public String getSectionId() {
		// return the name of the directory containing the files in org.eclipse.pde.ui.templates/templates_3.5
		return "E4ToolbarContribution"; //$NON-NLS-1$
	}

	@Override
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	private void createOptions() {
		// first page
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.E4ToolbarContributionTemplate_packageName, (String) null, 0);
		addOption("className", PDETemplateMessages.E4ToolbarContributionTemplate_className, "HelloWorldHandler", 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption("message", PDETemplateMessages.E4ToolbarContributionMessage, //$NON-NLS-1$
				PDETemplateMessages.E4ToolbarContributionMessage_default, 0);

	}

	@Override
	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		initializeFields(data.getId());

	}

	@Override
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so
		// we can initialize directly from it
		initializeFields(model.getPluginBase().getId());
	}

	public void initializeFields(String id) {
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id));
	}

	@Override
	public boolean isDependentOnParentWizard() {
		return true;
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page0 = createPage(0, IHelpContextIds.TEMPLATE_E4_VIEW);
		page0.setTitle(PDETemplateMessages.E4ToolbarContributionTemplate_title);
		page0.setDescription(PDETemplateMessages.E4ToolbarContributionTemplate_desc);
		wizard.addPage(page0);

		markPagesAdded();
	}



	@Override
	public String getUsedExtensionPoint() {
		return "org.eclipse.e4.workbench.model"; //$NON-NLS-1$
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {

		createE4ModelExtension();

	}

	private void createE4ModelExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.e4.workbench.model", true); //$NON-NLS-1$

		IPluginElement element = model.getFactory().createElement(extension);
		extension.setId(getValue(KEY_PACKAGE_NAME) + ".fragment"); //$NON-NLS-1$

		element.setName("fragment"); //$NON-NLS-1$
		element.setAttribute("apply", "initial"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("uri", E4_FRAGMENT_FILE); //$NON-NLS-1$

		extension.add(element);

		if (!extension.isInTheModel())
			plugin.add(extension);
	}


	@Override
	public String[] getNewFiles() {
		return new String[] { "icons/", E4_FRAGMENT_FILE }; //$NON-NLS-1$
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList<PluginReference> result = new ArrayList<>();

		final int matchRule = IMatchRules.GREATER_OR_EQUAL;

		result.add(new PluginReference("javax.inject", null, matchRule)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.osgi", null, matchRule)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.jface", null, matchRule)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.e4.ui.services", null, matchRule)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.e4.core.di.annotations", null, matchRule)); //$NON-NLS-1$

		return result.toArray(new IPluginReference[result.size()]);

	}



}
