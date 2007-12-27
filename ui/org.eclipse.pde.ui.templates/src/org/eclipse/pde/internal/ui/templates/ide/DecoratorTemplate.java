/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
import java.util.StringTokenizer;

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
import org.eclipse.pde.ui.templates.BooleanOption;
import org.eclipse.pde.ui.templates.PluginReference;
import org.eclipse.pde.ui.templates.TemplateOption;

public class DecoratorTemplate extends PDETemplateSection {
	public static final String DECORATOR_CLASS_NAME = "decoratorClassName"; //$NON-NLS-1$
	public static final String DECORATOR_ICON_PLACEMENT = "decoratorPlacement"; //$NON-NLS-1$
	public static final String DECORATOR_BLN_PROJECT = "decorateProjects"; //$NON-NLS-1$
	public static final String DECORATOR_BLN_READONLY = "decorateReadOnly"; //$NON-NLS-1$

	private WizardPage page;
	private TemplateOption packageOption;
	private TemplateOption classOption;
	private BooleanOption projectOption;
	private BooleanOption readOnlyOption;

	/**
	 * Constructor for DecoratorTemplate.
	 */
	public DecoratorTemplate() {
		setPageCount(1);
		createOptions();
		alterOptionStates();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		// Additional dependency required to decorate resource objects
		if (schemaVersion != null) {
			IPluginReference[] dep = new IPluginReference[1];
			dep[0] = new PluginReference("org.eclipse.core.resources", null, 0); //$NON-NLS-1$
			return dep;
		}
		return super.getDependencies(schemaVersion);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.OptionTemplateSection#getSectionId()
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#getDirectoryCandidates()
	 */
	public String getSectionId() {
		// Identifier used for the folder name within the templates_3.X
		// hierarchy  and as part of the lookup key for the template label
		// variable.
		return "decorator"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	/**
	 * Creates the options to be displayed on the template wizard.
	 * A multiple choice option (radio buttons) and a boolean option
	 * are used.
	 */
	private void createOptions() {
		String[][] choices = fromCommaSeparated(PDETemplateMessages.DecoratorTemplate_placementChoices);

		addOption(DECORATOR_ICON_PLACEMENT, PDETemplateMessages.DecoratorTemplate_placement, choices, choices[0][0], 0);

		projectOption = (BooleanOption) addOption(DECORATOR_BLN_PROJECT, PDETemplateMessages.DecoratorTemplate_decorateProject, true, 0);

		readOnlyOption = (BooleanOption) addOption(DECORATOR_BLN_READONLY, PDETemplateMessages.DecoratorTemplate_decorateReadOnly, false, 0);

		packageOption = addOption(KEY_PACKAGE_NAME, PDETemplateMessages.DecoratorTemplate_packageName, (String) null, 0);
		classOption = addOption(DECORATOR_CLASS_NAME, PDETemplateMessages.DecoratorTemplate_decoratorClass, "ReadOnly", //$NON-NLS-1$
				0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#addPages(org.eclipse.jface.wizard.Wizard)
	 */
	public void addPages(Wizard wizard) {
		int pageIndex = 0;

		page = createPage(pageIndex, IHelpContextIds.TEMPLATE_EDITOR);
		page.setTitle(PDETemplateMessages.DecoratorTemplate_title);
		page.setDescription(PDETemplateMessages.DecoratorTemplate_desc);

		wizard.addPage(page);
		markPagesAdded();
	}

	private void alterOptionStates() {
		projectOption.setEnabled(!readOnlyOption.isSelected());
		packageOption.setEnabled(!projectOption.isEnabled());
		classOption.setEnabled(!projectOption.isEnabled());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#isOkToCreateFolder(java.io.File)
	 */
	protected boolean isOkToCreateFolder(File sourceFolder) {
		//Define rules for creating folders from the Templates_3.X folders
		boolean isOk = true;
		String folderName = sourceFolder.getName();
		if (folderName.equals("java")) { //$NON-NLS-1$
			isOk = readOnlyOption.isEnabled() && readOnlyOption.isSelected();
		}
		return isOk;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#isOkToCreateFile(java.io.File)
	 */
	protected boolean isOkToCreateFile(File sourceFile) {
		//Define rules for creating files from the Templates_3.X folders
		boolean isOk = true;
		String fileName = sourceFile.getName();
		if (fileName.equals("read_only.gif")) { //$NON-NLS-1$
			isOk = readOnlyOption.isEnabled() && readOnlyOption.isSelected();
		} else if (fileName.equals("sample_decorator.gif")) { //$NON-NLS-1$
			isOk = !readOnlyOption.isSelected();
		} else if (fileName.equals("$decoratorClassName$.java")) { //$NON-NLS-1$
			isOk = readOnlyOption.isEnabled() && readOnlyOption.isSelected();
		}
		return isOk;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.BaseOptionTemplateSection#validateOptions(org.eclipse.pde.ui.templates.TemplateOption)
	 */
	public void validateOptions(TemplateOption source) {
		if (source == readOnlyOption) {
			alterOptionStates();
		}
		super.validateOptions(source);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.BaseOptionTemplateSection#isDependentOnParentWizard()
	 */
	public boolean isDependentOnParentWizard() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.BaseOptionTemplateSection#initializeFields(org.eclipse.pde.ui.IFieldData)
	 */
	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.BaseOptionTemplateSection#initializeFields(org.eclipse.pde.core.plugin.IPluginModelBase)
	 */
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#updateModel(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		// This method creates the extension point structure through the use
		// of IPluginElement objects. The element attributes are set based on
		// user input from the wizard page as well as values required for the 
		// operation of the extension point.
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement decoratorElement = factory.createElement(extension);
		decoratorElement.setName("decorator"); //$NON-NLS-1$
		decoratorElement.setAttribute("adaptable", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		decoratorElement.setAttribute("state", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		decoratorElement.setAttribute("lightweight", "true"); //$NON-NLS-1$ //$NON-NLS-2$

		if (!readOnlyOption.isSelected()) {
			decoratorElement.setAttribute("id", plugin.getId() + "." + getSectionId()); //$NON-NLS-1$ //$NON-NLS-2$
			decoratorElement.setAttribute("label", PDETemplateMessages.DecoratorTemplate_resourceLabel); //$NON-NLS-1$		
			decoratorElement.setAttribute("icon", "icons/sample_decorator.gif"); //$NON-NLS-1$ //$NON-NLS-2$
			decoratorElement.setAttribute("location", getValue(DECORATOR_ICON_PLACEMENT).toString()); //$NON-NLS-1$
		} else {
			decoratorElement.setAttribute("id", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(DECORATOR_CLASS_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
			decoratorElement.setAttribute("label", PDETemplateMessages.DecoratorTemplate_readOnlyLabel); //$NON-NLS-1$		
			decoratorElement.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(DECORATOR_CLASS_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
		}

		IPluginElement enablementElement = factory.createElement(decoratorElement);
		enablementElement.setName("enablement"); //$NON-NLS-1$

		IPluginElement andElement = factory.createElement(enablementElement);
		andElement.setName("and"); //$NON-NLS-1$

		IPluginElement resourceObjectElement = factory.createElement(andElement);
		resourceObjectElement.setName("objectClass"); //$NON-NLS-1$
		resourceObjectElement.setAttribute("name", "org.eclipse.core.resources.IResource"); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement orElement = factory.createElement(andElement);
		orElement.setName("or"); //$NON-NLS-1$

		IPluginElement fileObjectElement = factory.createElement(orElement);
		fileObjectElement.setName("objectClass"); //$NON-NLS-1$
		fileObjectElement.setAttribute("name", "org.eclipse.core.resources.IFile"); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement folderObjectElement = factory.createElement(orElement);
		folderObjectElement.setName("objectClass"); //$NON-NLS-1$
		folderObjectElement.setAttribute("name", "org.eclipse.core.resources.IFolder"); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement projectObjectElement = factory.createElement(orElement);
		projectObjectElement.setName("objectClass"); //$NON-NLS-1$
		projectObjectElement.setAttribute("name", "org.eclipse.core.resources.IProject"); //$NON-NLS-1$ //$NON-NLS-2$

		if (readOnlyOption.isSelected())
			orElement.add(folderObjectElement);
		else if (projectOption.isSelected())
			orElement.add(projectObjectElement);
		orElement.add(fileObjectElement);
		andElement.add(resourceObjectElement);
		andElement.add(orElement);
		enablementElement.add(andElement);
		decoratorElement.add(enablementElement);

		extension.add(decoratorElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#getNewFiles()
	 */
	public String[] getNewFiles() {
		return new String[] {"icons/"}; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#getFormattedPackageName(java.lang.String)
	 */
	protected String getFormattedPackageName(String id) {
		// Package name addition to create a location for containing
		// any classes required by the decorator. 
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".decorators"; //$NON-NLS-1$
		return "decorators"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.decorators"; //$NON-NLS-1$
	}

	/**
	 * Returns a 2-D String array based on a comma seperated
	 * string of choices. 
	 * 
	 * @param iconLocations
	 * 				comma seperated string of icon placement options
	 * @return the 2-D array of choices
	 * 				
	 */
	protected String[][] fromCommaSeparated(String iconLocations) {
		StringTokenizer tokens = new StringTokenizer(iconLocations, ","); //$NON-NLS-1$
		String[][] choices = new String[tokens.countTokens() / 2][2];
		int x = 0, y = 0;
		while (tokens.hasMoreTokens()) {
			choices[x][y++] = tokens.nextToken();
			choices[x++][y--] = tokens.nextToken();
		}
		return choices;
	}
}
