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
import org.eclipse.pde.ui.templates.*;

public class BuilderTemplate extends PDETemplateSection {

	private static final String KEY_BUILDER_CLASS_NAME = "builderClassName"; //$NON-NLS-1$

	private static final String KEY_BUILDER_ID = "builderId"; //$NON-NLS-1$

	private static final String KEY_BUILDER_NAME = "builderName"; //$NON-NLS-1$

	private static final String KEY_NATURE_CLASS_NAME = "natureClassName"; //$NON-NLS-1$

	private static final String KEY_NATURE_ID = "natureId"; //$NON-NLS-1$

	private static final String KEY_NATURE_NAME = "natureName"; //$NON-NLS-1$

	private static final String KEY_GEN_ACTION = "genAction"; //$NON-NLS-1$

	private BooleanOption actionOption;

	/**
	 * Constructor for BuilderTemplate.
	 */
	public BuilderTemplate() {
		setPageCount(1);
		createOptions();
	}

	@Override
	public String getSectionId() {
		return "builder"; //$NON-NLS-1$
	}

	@Override
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	private void createOptions() {
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.BuilderTemplate_packageLabel, (String) null, 0);

		addOption(KEY_BUILDER_CLASS_NAME, PDETemplateMessages.BuilderTemplate_builderClass, "SampleBuilder", 0); //$NON-NLS-1$
		addOption(KEY_BUILDER_ID, PDETemplateMessages.BuilderTemplate_builderId, "sampleBuilder", 0); //$NON-NLS-1$
		addOption(KEY_BUILDER_NAME, PDETemplateMessages.BuilderTemplate_builderName, PDETemplateMessages.BuilderTemplate_defaultBuilderName, 0);

		addOption(KEY_NATURE_CLASS_NAME, PDETemplateMessages.BuilderTemplate_natureClass, "SampleNature", 0); //$NON-NLS-1$
		addOption(KEY_NATURE_ID, PDETemplateMessages.BuilderTemplate_natureId, "sampleNature", 0); //$NON-NLS-1$
		addOption(KEY_NATURE_NAME, PDETemplateMessages.BuilderTemplate_natureName, PDETemplateMessages.BuilderTemplate_defaultNatureName, 0);

		actionOption = (BooleanOption) addOption(KEY_GEN_ACTION, PDETemplateMessages.BuilderTemplate_generateCommand, true, 0);
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_BUILDER);
		page.setTitle(PDETemplateMessages.BuilderTemplate_title);
		page.setDescription(PDETemplateMessages.BuilderTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	@Override
	public boolean isDependentOnParentWizard() {
		return true;
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
	public String getUsedExtensionPoint() {
		return "org.eclipse.core.resources.builders"; //$NON-NLS-1$
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginModelFactory factory = model.getPluginFactory();

		// Builder
		IPluginExtension extension1 = createExtension("org.eclipse.core.resources.builders", true); //$NON-NLS-1$
		extension1.setId(getStringOption(KEY_BUILDER_ID));
		extension1.setName(getStringOption(KEY_BUILDER_NAME));

		IPluginElement builder = factory.createElement(extension1);
		builder.setName("builder"); //$NON-NLS-1$
		builder.setAttribute("hasNature", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		extension1.add(builder);

		IPluginElement run = factory.createElement(builder);
		run.setName("run"); //$NON-NLS-1$
		run.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) //$NON-NLS-1$
				+ "." + getStringOption(KEY_BUILDER_CLASS_NAME)); //$NON-NLS-1$
		builder.add(run);

		if (!extension1.isInTheModel())
			plugin.add(extension1);

		// Nature
		IPluginExtension extension2 = createExtension("org.eclipse.core.resources.natures", true); //$NON-NLS-1$
		extension2.setId(getStringOption(KEY_NATURE_ID));
		extension2.setName(getStringOption(KEY_NATURE_NAME));

		IPluginElement runtime = factory.createElement(extension2);
		runtime.setName("runtime"); //$NON-NLS-1$
		extension2.add(runtime);

		IPluginElement run2 = factory.createElement(runtime);
		run2.setName("run"); //$NON-NLS-1$
		run2.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) //$NON-NLS-1$
				+ "." + getStringOption(KEY_NATURE_CLASS_NAME)); //$NON-NLS-1$
		runtime.add(run2);

		IPluginElement builder2 = factory.createElement(extension2);
		builder2.setName("builder"); //$NON-NLS-1$
		builder2.setAttribute("id", model.getPluginBase().getId() //$NON-NLS-1$
				+ "." + getStringOption(KEY_BUILDER_ID)); //$NON-NLS-1$
		extension2.add(builder2);

		if (!extension2.isInTheModel())
			plugin.add(extension2);

		// Popup Action
		if (actionOption.isSelected()) {
			IPluginExtension extension3 = createExtension("org.eclipse.ui.commands", true); //$NON-NLS-1$

			IPluginElement category = factory.createElement(extension3);
			category.setName("category"); //$NON-NLS-1$
			category.setAttribute("id", model.getPluginBase().getId() //$NON-NLS-1$
					+ "." + getStringOption(KEY_NATURE_ID) + ".category"); //$NON-NLS-1$ //$NON-NLS-2$
			category.setAttribute("name", getStringOption(KEY_NATURE_NAME) + " commands"); //$NON-NLS-1$ //$NON-NLS-2$
			extension3.add(category);

			IPluginElement command = factory.createElement(extension3);
			command.setName("command"); //$NON-NLS-1$
			command.setAttribute("categoryId", model.getPluginBase().getId() //$NON-NLS-1$
					+ "." + getStringOption(KEY_NATURE_ID) + ".category"); //$NON-NLS-1$ //$NON-NLS-2$
			command.setAttribute("defaultHandler", getStringOption(KEY_PACKAGE_NAME) //$NON-NLS-1$
					+ ".AddRemove" + getStringOption(KEY_NATURE_CLASS_NAME) + "Handler"); //$NON-NLS-1$ //$NON-NLS-2$
			command.setAttribute("id", model.getPluginBase().getId() //$NON-NLS-1$
					+ ".addRemove" + getStringOption(KEY_NATURE_CLASS_NAME)); //$NON-NLS-1$
			command.setAttribute("name", PDETemplateMessages.BuilderTemplate_commandName + getStringOption(KEY_NATURE_NAME)); //$NON-NLS-1$
			extension3.add(command);

			if (!extension3.isInTheModel())
				plugin.add(extension3);

			IPluginExtension extension4 = createExtension("org.eclipse.ui.menus", true); //$NON-NLS-1$
			IPluginElement menuContribution = factory.createElement(extension4);
			menuContribution.setName("menuContribution"); //$NON-NLS-1$
			menuContribution.setAttribute("locationURI", //$NON-NLS-1$
					"popup:org.eclipse.ui.projectConfigure?after=additions"); //$NON-NLS-1$
			extension4.add(menuContribution);

			IPluginElement disableCommand = factory.createElement(menuContribution);
			disableCommand.setName("command"); //$NON-NLS-1$
			disableCommand.setAttribute("label", PDETemplateMessages.BuilderTemplate_disableLabel); //$NON-NLS-1$
			disableCommand.setAttribute("commandId", model.getPluginBase().getId() //$NON-NLS-1$
					+ ".addRemove" + getStringOption(KEY_NATURE_CLASS_NAME)); //$NON-NLS-1$
			disableCommand.setAttribute("style", "push"); //$NON-NLS-1$ //$NON-NLS-2$
			menuContribution.add(disableCommand);

			IPluginElement visibleWhen = factory.createElement(disableCommand);
			visibleWhen.setName("visibleWhen"); //$NON-NLS-1$
			visibleWhen.setAttribute("checkEnabled", "false"); //$NON-NLS-1$ //$NON-NLS-2$
			disableCommand.add(visibleWhen);

			IPluginElement with = factory.createElement(visibleWhen);
			with.setName("with"); //$NON-NLS-1$
			with.setAttribute("variable", "selection"); //$NON-NLS-1$ //$NON-NLS-2$
			visibleWhen.add(with);

			IPluginElement count = factory.createElement(with);
			count.setName("count"); //$NON-NLS-1$
			count.setAttribute("value", "1"); //$NON-NLS-1$ //$NON-NLS-2$
			with.add(count);

			IPluginElement iterate = factory.createElement(with);
			iterate.setName("iterate"); //$NON-NLS-1$
			with.add(iterate);

			IPluginElement adapt = factory.createElement(iterate);
			adapt.setName("adapt"); //$NON-NLS-1$
			adapt.setAttribute("type", "org.eclipse.core.resources.IProject"); //$NON-NLS-1$ //$NON-NLS-2$
			iterate.add(adapt);

			IPluginElement test = factory.createElement(adapt);
			test.setName("test"); //$NON-NLS-1$
			test.setAttribute("property", "org.eclipse.core.resources.projectNature"); //$NON-NLS-1$ //$NON-NLS-2$
			test.setAttribute("value", model.getPluginBase().getId() //$NON-NLS-1$
					+ "." + getStringOption(KEY_NATURE_ID)); //$NON-NLS-1$
			adapt.add(test);

			IPluginElement enableCommand = factory.createElement(menuContribution);
			enableCommand.setName("command"); //$NON-NLS-1$
			enableCommand.setAttribute("label", PDETemplateMessages.BuilderTemplate_enableLabel); //$NON-NLS-1$
			enableCommand.setAttribute("commandId", model.getPluginBase().getId() //$NON-NLS-1$
					+ ".addRemove" + getStringOption(KEY_NATURE_CLASS_NAME)); //$NON-NLS-1$
			enableCommand.setAttribute("style", "push"); //$NON-NLS-1$ //$NON-NLS-2$
			menuContribution.add(enableCommand);

			IPluginElement visibleWhen2 = factory.createElement(enableCommand);
			visibleWhen2.setName("visibleWhen"); //$NON-NLS-1$
			visibleWhen2.setAttribute("checkEnabled", "false"); //$NON-NLS-1$ //$NON-NLS-2$
			enableCommand.add(visibleWhen2);

			IPluginElement with2 = factory.createElement(visibleWhen2);
			with2.setName("with"); //$NON-NLS-1$
			with2.setAttribute("variable", "selection"); //$NON-NLS-1$ //$NON-NLS-2$
			visibleWhen2.add(with2);

			IPluginElement count2 = factory.createElement(with2);
			count2.setName("count"); //$NON-NLS-1$
			count2.setAttribute("value", "1"); //$NON-NLS-1$ //$NON-NLS-2$
			with2.add(count2);

			IPluginElement iterate2 = factory.createElement(with2);
			iterate2.setName("iterate"); //$NON-NLS-1$
			with2.add(iterate2);

			IPluginElement adapt2 = factory.createElement(iterate2);
			adapt2.setName("adapt"); //$NON-NLS-1$
			adapt2.setAttribute("type", "org.eclipse.core.resources.IProject"); //$NON-NLS-1$ //$NON-NLS-2$
			iterate2.add(adapt2);

			IPluginElement not = factory.createElement(adapt2);
			not.setName("not"); //$NON-NLS-1$
			adapt2.add(not);

			IPluginElement test2 = factory.createElement(not);
			test2.setName("test"); //$NON-NLS-1$
			test2.setAttribute("property", "org.eclipse.core.resources.projectNature"); //$NON-NLS-1$ //$NON-NLS-2$
			test2.setAttribute("value", model.getPluginBase().getId() //$NON-NLS-1$
					+ "." + getStringOption(KEY_NATURE_ID)); //$NON-NLS-1$
			not.add(test2);

			if (!extension4.isInTheModel())
				plugin.add(extension4);
		}

		// Marker
		IPluginExtension extension8 = createExtension("org.eclipse.core.resources.markers", false); //$NON-NLS-1$
		extension8.setId("xmlProblem"); //$NON-NLS-1$
		extension8.setName(PDETemplateMessages.BuilderTemplate_markerName);

		IPluginElement superElement = factory.createElement(extension8);
		superElement.setName("super"); //$NON-NLS-1$
		superElement.setAttribute("type", //$NON-NLS-1$
				"org.eclipse.core.resources.problemmarker"); //$NON-NLS-1$
		extension8.add(superElement);

		IPluginElement persistent = factory.createElement(extension8);
		persistent.setName("persistent"); //$NON-NLS-1$
		persistent.setAttribute("value", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		extension8.add(persistent);

		if (!extension8.isInTheModel())
			plugin.add(extension8);
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList<PluginReference> result = new ArrayList<>();
		result.add(new PluginReference("org.eclipse.core.resources")); //$NON-NLS-1$
		if (schemaVersion != null)
			result.add(new PluginReference("org.eclipse.core.runtime")); //$NON-NLS-1$
		if (actionOption.isSelected())
			result.add(new PluginReference("org.eclipse.ui")); //$NON-NLS-1$

		return result.toArray(new IPluginReference[result.size()]);
	}

	@Override
	protected String getFormattedPackageName(String id) {
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".builder"; //$NON-NLS-1$
		return "builder"; //$NON-NLS-1$
	}

	/**
	 * @see AbstractTemplateSection#isOkToCreateFile(File)
	 */
	@Override
	protected boolean isOkToCreateFile(File sourceFile) {
		String fileName = sourceFile.getName();
		if (fileName.equals("AddRemove$natureClassName$Handler.java")) { //$NON-NLS-1$
			return actionOption.isSelected();
		}
		if (fileName.equals("ToggleNatureAction.java")) { //$NON-NLS-1$
			return false;
		}
		return true;
	}

	@Override
	public String getLabel() {
		return getPluginResourceString("newExtension.templates.builder.name"); //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return getPluginResourceString("newExtension.templates.builder.desc"); //$NON-NLS-1$
	}

}
