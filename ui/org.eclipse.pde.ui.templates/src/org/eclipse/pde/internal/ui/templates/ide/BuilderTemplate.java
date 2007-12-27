/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.ide;

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
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.templates.IHelpContextIds;
import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.internal.ui.templates.PDETemplateSection;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.AbstractTemplateSection;
import org.eclipse.pde.ui.templates.BooleanOption;
import org.eclipse.pde.ui.templates.PluginReference;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.ui.templates.OptionTemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "builder"; //$NON-NLS-1$
	}

	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
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

		actionOption = (BooleanOption) addOption(KEY_GEN_ACTION, PDETemplateMessages.BuilderTemplate_generateAction, true, 0);
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_BUILDER);
		page.setTitle(PDETemplateMessages.BuilderTemplate_title);
		page.setDescription(PDETemplateMessages.BuilderTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	public boolean isDependentOnParentWizard() {
		return true;
	}

	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id));
	}

	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.ui.templates.ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return "org.eclipse.core.resources.builders"; //$NON-NLS-1$
	}

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
			IPluginExtension extension3 = createExtension("org.eclipse.ui.popupMenus", true); //$NON-NLS-1$
			IPluginElement objectContribution = factory.createElement(extension3);
			objectContribution.setName("objectContribution"); //$NON-NLS-1$
			objectContribution.setAttribute("objectClass", //$NON-NLS-1$
					"org.eclipse.core.resources.IProject"); //$NON-NLS-1$
			objectContribution.setAttribute("adaptable", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			objectContribution.setAttribute("nameFilter", "*"); //$NON-NLS-1$ //$NON-NLS-2$
			objectContribution.setAttribute("id", model.getPluginBase().getId() //$NON-NLS-1$
					+ ".contribution1"); //$NON-NLS-1$
			extension3.add(objectContribution);

			IPluginElement action = factory.createElement(objectContribution);
			action.setName("action"); //$NON-NLS-1$
			action.setAttribute("label", PDETemplateMessages.BuilderTemplate_actionLabel); //$NON-NLS-1$
			action.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) //$NON-NLS-1$
					+ ".ToggleNatureAction"); //$NON-NLS-1$
			action.setAttribute("menubarPath", "additions"); //$NON-NLS-1$ //$NON-NLS-2$
			action.setAttribute("enablesFor", "+"); //$NON-NLS-1$ //$NON-NLS-2$
			action.setAttribute("id", model.getPluginBase().getId() //$NON-NLS-1$
					+ ".addRemoveNatureAction"); //$NON-NLS-1$
			objectContribution.add(action);

			if (!extension3.isInTheModel())
				plugin.add(extension3);
		}

		// Marker
		IPluginExtension extension4 = createExtension("org.eclipse.core.resources.markers", false); //$NON-NLS-1$
		extension4.setId("xmlProblem"); //$NON-NLS-1$
		extension4.setName(PDETemplateMessages.BuilderTemplate_markerName);

		IPluginElement superElement = factory.createElement(extension4);
		superElement.setName("super"); //$NON-NLS-1$
		superElement.setAttribute("type", //$NON-NLS-1$
				"org.eclipse.core.resources.problemmarker"); //$NON-NLS-1$
		extension4.add(superElement);

		IPluginElement persistent = factory.createElement(extension4);
		persistent.setName("persistent"); //$NON-NLS-1$
		persistent.setAttribute("value", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		extension4.add(persistent);

		if (!extension4.isInTheModel())
			plugin.add(extension4);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList result = new ArrayList();
		result.add(new PluginReference("org.eclipse.core.resources", null, 0)); //$NON-NLS-1$
		if (schemaVersion != null)
			result.add(new PluginReference("org.eclipse.core.runtime", null, //$NON-NLS-1$
					0));
		if (actionOption.isSelected())
			result.add(new PluginReference("org.eclipse.ui", null, 0)); //$NON-NLS-1$

		return (IPluginReference[]) result.toArray(new IPluginReference[result.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#formatPackageName(java.lang.String)
	 */
	protected String getFormattedPackageName(String id) {
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".builder"; //$NON-NLS-1$
		return "builder"; //$NON-NLS-1$
	}

	/**
	 * @see AbstractTemplateSection#isOkToCreateFile(File)
	 */
	protected boolean isOkToCreateFile(File sourceFile) {
		String fileName = sourceFile.getName();
		if (fileName.equals("ToggleNatureAction.java")) { //$NON-NLS-1$
			return actionOption.isSelected();
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.OptionTemplateSection#getLabel()
	 */
	public String getLabel() {
		return getPluginResourceString("newExtension.templates.builder.name"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.OptionTemplateSection#getDescription()
	 */
	public String getDescription() {
		return getPluginResourceString("newExtension.templates.builder.desc"); //$NON-NLS-1$
	}

}
