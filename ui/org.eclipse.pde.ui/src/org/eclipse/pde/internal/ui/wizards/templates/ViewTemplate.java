/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.templates;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.wizard.*;

public class ViewTemplate extends PDETemplateSection {
	private BooleanOption addToPerspective;
	/**
	 * Constructor for HelloWorldTemplate.
	 */
	public ViewTemplate() {
		setPageCount(2);
		createOptions();
	}
	
	public String getSectionId() {
		return "view"; //$NON-NLS-1$
	}
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits()+1;
	}
	
	private void createOptions() {
		// first page	
		addOption(KEY_PACKAGE_NAME, PDEUIMessages.ViewTemplate_packageName, (String)null, 0);
		addOption("className", PDEUIMessages.ViewTemplate_className, "SampleView", 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption("viewName", PDEUIMessages.ViewTemplate_name, PDEUIMessages.ViewTemplate_defaultName, 0); //$NON-NLS-1$
		addOption("viewCategoryId", PDEUIMessages.ViewTemplate_categoryId, (String)null, 0); //$NON-NLS-1$
		addOption("viewCategoryName", PDEUIMessages.ViewTemplate_categoryName, PDEUIMessages.ViewTemplate_defaultCategoryName, 0); //$NON-NLS-1$
		addOption("viewType", PDEUIMessages.ViewTemplate_select,  //$NON-NLS-1$
					new String [][] {
						{"tableViewer", PDEUIMessages.ViewTemplate_table}, //$NON-NLS-1$
						{"treeViewer", PDEUIMessages.ViewTemplate_tree}}, //$NON-NLS-1$
						"tableViewer", 0); //$NON-NLS-1$
		addToPerspective = (BooleanOption)addOption("addToPerspective",PDEUIMessages.ViewTemplate_addToPerspective,true,0); //$NON-NLS-1$
		// second page
		addOption("doubleClick", PDEUIMessages.ViewTemplate_doubleClick, true, 1); //$NON-NLS-1$
		addOption("popup", PDEUIMessages.ViewTemplate_popup, true, 1); //$NON-NLS-1$
		addOption("localToolbar", PDEUIMessages.ViewTemplate_toolbar, true, 1); //$NON-NLS-1$
		addOption("localPulldown", PDEUIMessages.ViewTemplate_pulldown, true, 1); //$NON-NLS-1$
		addOption("sorter", PDEUIMessages.ViewTemplate_sorting, true, 1); //$NON-NLS-1$
		//addOption("filter", PDEPlugin.getResourceString(NL_FILTER), true, lists[1]);
		addOption("drillDown", PDEUIMessages.ViewTemplate_drilldown, true, 1); //$NON-NLS-1$
		setOptionEnabled("drillDown", false); //$NON-NLS-1$
	}

	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id)); 
		initializeOption("viewCategoryId", id); //$NON-NLS-1$
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId));
		initializeOption("viewCategoryId", pluginId); //$NON-NLS-1$
	}
	
	public boolean isDependentOnParentWizard() {
		return true;
	}
	
	public void addPages(Wizard wizard) {
		WizardPage page0 = createPage(0, IHelpContextIds.TEMPLATE_VIEW);
		page0.setTitle(PDEUIMessages.ViewTemplate_title0);
		page0.setDescription(PDEUIMessages.ViewTemplate_desc0);
		wizard.addPage(page0);
		
		WizardPage page1 = createPage(1, IHelpContextIds.TEMPLATE_VIEW);
		page1.setTitle(PDEUIMessages.ViewTemplate_title1);
		page1.setDescription(PDEUIMessages.ViewTemplate_desc1);
		wizard.addPage(page1);
		markPagesAdded();
	}

	public void validateOptions(TemplateOption source) {
		String viewType = getValue("viewType").toString(); //$NON-NLS-1$
		setOptionEnabled("drillDown", viewType.equals("treeViewer")); //$NON-NLS-1$ //$NON-NLS-2$
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
		return "org.eclipse.ui.views"; //$NON-NLS-1$
	}
	
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.views", true); //$NON-NLS-1$
		IPluginModelFactory factory = model.getPluginFactory();
		
		String cid = getStringOption("viewCategoryId"); //$NON-NLS-1$

		createCategory(extension, cid);
		String fullClassName = getStringOption(KEY_PACKAGE_NAME)+"."+getStringOption("className"); //$NON-NLS-1$ //$NON-NLS-2$
		
		IPluginElement viewElement = factory.createElement(extension);
		viewElement.setName("view"); //$NON-NLS-1$
		viewElement.setAttribute("id", fullClassName); //$NON-NLS-1$
		viewElement.setAttribute("name", getStringOption("viewName")); //$NON-NLS-1$ //$NON-NLS-2$
		viewElement.setAttribute("icon", "icons/sample.gif"); //$NON-NLS-1$ //$NON-NLS-2$

		viewElement.setAttribute("class", fullClassName); //$NON-NLS-1$
		viewElement.setAttribute("category", cid); //$NON-NLS-1$
		extension.add(viewElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
			
		if (addToPerspective.isSelected()) {
			IPluginExtension perspectiveExtension =
				createExtension("org.eclipse.ui.perspectiveExtensions", true); //$NON-NLS-1$

			IPluginElement perspectiveElement = factory.createElement(perspectiveExtension);
			perspectiveElement.setName("perspectiveExtension"); //$NON-NLS-1$
			perspectiveElement.setAttribute(
				"targetID", //$NON-NLS-1$
				"org.eclipse.ui.resourcePerspective"); //$NON-NLS-1$

			IPluginElement view = factory.createElement(perspectiveElement);
			view.setName("view"); //$NON-NLS-1$
			view.setAttribute("id", fullClassName); //$NON-NLS-1$
			view.setAttribute("relative", "org.eclipse.ui.views.TaskList"); //$NON-NLS-1$ //$NON-NLS-2$
			view.setAttribute("relationship","right"); //$NON-NLS-1$ //$NON-NLS-2$
			view.setAttribute("ratio", "0.5"); //$NON-NLS-1$ //$NON-NLS-2$
			perspectiveElement.add(view);

			perspectiveExtension.add(perspectiveElement);
			if (!perspectiveExtension.isInTheModel())
				plugin.add(perspectiveExtension);
		}	
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
		categoryElement.setAttribute("name", getStringOption("viewCategoryName")); //$NON-NLS-1$ //$NON-NLS-2$
		categoryElement.setAttribute("id", id); //$NON-NLS-1$
		extension.add(categoryElement);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#getFoldersToInclude()
	 */
	public String[] getNewFiles() {
		return new String[] {"icons/"}; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList result = new ArrayList();
		if (schemaVersion != null)
			result.add(new PluginReference("org.eclipse.core.runtime", null, 0)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.ui", null, 0));	 //$NON-NLS-1$
		return (IPluginReference[]) result.toArray(new IPluginReference[result.size()]);
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#formatPackageName(java.lang.String)
     */
    protected String getFormattedPackageName(String id) {
        String packageName = super.getFormattedPackageName(id);
        if (packageName.length() != 0)
            return packageName + ".views"; //$NON-NLS-1$
        return "views"; //$NON-NLS-1$
    }
}
