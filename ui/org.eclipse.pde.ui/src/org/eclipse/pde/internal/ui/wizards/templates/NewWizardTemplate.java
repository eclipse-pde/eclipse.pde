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

import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.wizard.*;

public class NewWizardTemplate extends PDETemplateSection {
	private static final String KEY_TITLE = "NewWizardTemplate.title"; //$NON-NLS-1$
	private static final String KEY_DESC = "NewWizardTemplate.desc"; //$NON-NLS-1$
	private static final String KEY_PACKAGE_LABEL = "NewWizardTemplate.packageName";	 //$NON-NLS-1$
	private static final String KEY_CATEGORY_ID_LABEL = "NewWizardTemplate.categoryId"; //$NON-NLS-1$
	private static final String KEY_CATEGORY_NAME_LABEL = "NewWizardTemplate.categoryName"; //$NON-NLS-1$
	private static final String KEY_CLASS_LABEL = "NewWizardTemplate.className"; //$NON-NLS-1$
	private static final String KEY_PAGE_CLASS_LABEL = "NewWizardTemplate.pageClassName"; //$NON-NLS-1$
	private static final String KEY_WIZARD_LABEL = "NewWizardTemplate.wizardName"; //$NON-NLS-1$
	private static final String KEY_DEFAULT_NAME = "NewWizardTemplate.defaultName"; //$NON-NLS-1$
	private static final String KEY_EXTENSION_LABEL = "NewWizardTemplate.extension"; //$NON-NLS-1$
	private static final String KEY_FILE_LABEL = "NewWizardTemplate.fileName"; //$NON-NLS-1$
	private MultiPageEditorNewWizard wizard;
	
	public NewWizardTemplate() {
		this(null);
	}
	public NewWizardTemplate(MultiPageEditorNewWizard wizard){
		this.wizard = wizard;
		setPageCount(1);
		createOptions();
	}
	
	public String getSectionId() {
		return "newWizard"; //$NON-NLS-1$
	}
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits()+1;
	}
	
	private void createOptions() {
		// first page
		addOption(KEY_PACKAGE_NAME, PDEPlugin.getResourceString(KEY_PACKAGE_LABEL), (String)null, 0);
		addOption("categoryId", PDEPlugin.getResourceString(KEY_CATEGORY_ID_LABEL), (String)null, 0); //$NON-NLS-1$
		addOption("categoryName", PDEPlugin.getResourceString(KEY_CATEGORY_NAME_LABEL), "Sample Wizards", 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption("wizardClassName", PDEPlugin.getResourceString(KEY_CLASS_LABEL), "SampleNewWizard", 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption("wizardPageClassName", PDEPlugin.getResourceString(KEY_PAGE_CLASS_LABEL), "SampleNewWizardPage", 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption("wizardName", PDEPlugin.getResourceString(KEY_WIZARD_LABEL), PDEPlugin.getResourceString(KEY_DEFAULT_NAME), 0); //$NON-NLS-1$
		addOption("extension", PDEPlugin.getResourceString(KEY_EXTENSION_LABEL), (String)null, 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption("initialFileName", PDEPlugin.getResourceString(KEY_FILE_LABEL), (String)null, 0); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, id+".wizards"); //$NON-NLS-1$
		initializeOption("categoryId", id); //$NON-NLS-1$
		initializeOption("extension", wizard == null ? "mpe" : wizard.getFileExtension()); //$NON-NLS-1$ //$NON-NLS-2$
		initializeOption("initialFileName", wizard == null ? "new_file.mpe" : "new_file." + wizard.getFileExtension()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".wizards"); //$NON-NLS-1$
		initializeOption("categoryId", pluginId); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList result = new ArrayList();
		result.add(new PluginReference("org.eclipse.core.resources", null, 0)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.ui", null, 0)); //$NON-NLS-1$
		if (schemaVersion != null) {
			result.add(new PluginReference("org.eclipse.ui.ide", null, 0)); //$NON-NLS-1$
			result.add(new PluginReference("org.eclipse.core.runtime", null, 0)); //$NON-NLS-1$
		}
		return (IPluginReference[])result.toArray(new IPluginReference[result.size()]);
	}
	
	public boolean isDependentOnParentWizard() {
		return true;
	}
	
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_NEW_WIZARD);
		page.setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		page.setDescription(PDEPlugin.getResourceString(KEY_DESC));
		wizard.addPage(page);
		markPagesAdded();
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
	
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.newWizards"; //$NON-NLS-1$
	}
	
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.newWizards", true); //$NON-NLS-1$
		IPluginModelFactory factory = model.getPluginFactory();
		
		String cid = getStringOption("categoryId"); //$NON-NLS-1$

		createCategory(extension, cid);
		String fullClassName = getStringOption(KEY_PACKAGE_NAME)+"."+getStringOption("wizardClassName"); //$NON-NLS-1$ //$NON-NLS-2$
		
		IPluginElement viewElement = factory.createElement(extension);
		viewElement.setName("wizard"); //$NON-NLS-1$
		viewElement.setAttribute("id", fullClassName); //$NON-NLS-1$
		viewElement.setAttribute("name", getStringOption("wizardName")); //$NON-NLS-1$ //$NON-NLS-2$
		viewElement.setAttribute("icon", "icons/sample.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		viewElement.setAttribute("class", fullClassName); //$NON-NLS-1$
		viewElement.setAttribute("category", cid); //$NON-NLS-1$
		extension.add(viewElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	private void createCategory(IPluginExtension extension, String id) throws CoreException {
		IPluginObject [] elements = extension.getChildren();
		for (int i=0; i<elements.length; i++) {
			IPluginElement element = (IPluginElement)elements[i];
			if (element.getName().equalsIgnoreCase("category")) { //$NON-NLS-1$
				IPluginAttribute att = element.getAttribute("id"); //$NON-NLS-1$
				if (att!=null) {
					String cid = att.getValue();
					if (cid!=null && cid.equals(id))
						return;
				}
			}
		}
		IPluginElement categoryElement = model.getFactory().createElement(extension);
		categoryElement.setName("category"); //$NON-NLS-1$
		categoryElement.setAttribute("name", getStringOption("categoryName")); //$NON-NLS-1$ //$NON-NLS-2$
		categoryElement.setAttribute("id", id); //$NON-NLS-1$
		extension.add(categoryElement);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#getFoldersToInclude()
	 */
	public String[] getNewFiles() {
		return new String[] {"icons/"}; //$NON-NLS-1$
	}
}