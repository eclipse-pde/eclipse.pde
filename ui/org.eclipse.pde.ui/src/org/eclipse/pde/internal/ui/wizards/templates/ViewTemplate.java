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

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.wizard.*;

public class ViewTemplate extends PDETemplateSection {
	private static final String NL_TITLE0 = "ViewTemplate.title0";
	private static final String NL_DESC0 = "ViewTemplate.desc0";
	private static final String NL_TITLE1 = "ViewTemplate.title1";
	private static final String NL_DESC1 = "ViewTemplate.desc1";
	private static final String NL_PACKAGE_NAME = "ViewTemplate.packageName";
	private static final String NL_CLASS_NAME = "ViewTemplate.className";
	private static final String NL_NAME = "ViewTemplate.name";
	private static final String NL_DEFAULT_NAME = "ViewTemplate.defaultName";
	private static final String NL_CATEGORY_ID = "ViewTemplate.categoryId";
	private static final String NL_CATEGORY_NAME = "ViewTemplate.categoryName";
	private static final String NL_DEFAULT_CATEGORY_NAME = "ViewTemplate.defaultCategoryName";
	private static final String NL_SELECT = "ViewTemplate.select";
	private static final String NL_TABLE = "ViewTemplate.table";
	private static final String NL_TREE = "ViewTemplate.tree";
	private static final String NL_DOUBLE_CLICK = "ViewTemplate.doubleClick";
	private static final String NL_POPUP = "ViewTemplate.popup";
	private static final String NL_TOOLBAR = "ViewTemplate.toolbar";
	private static final String NL_PULLDOWN = "ViewTemplate.pulldown";
	private static final String NL_SORTING = "ViewTemplate.sorting";
	private static final String NL_DRILLDOWN = "ViewTemplate.drilldown";
	private static final String NL_ADD_TO_PERSPECTIVE = "ViewTemplate.addToPerspective";
	
	private BooleanOption addToPerspective;
	/**
	 * Constructor for HelloWorldTemplate.
	 */
	public ViewTemplate() {
		setPageCount(2);
		createOptions();
	}
	
	public String getSectionId() {
		return "view";
	}
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits()+1;
	}
	
	private void createOptions() {
		// first page	
		addOption(KEY_PACKAGE_NAME, PDEPlugin.getResourceString(NL_PACKAGE_NAME), (String)null, 0);
		addOption("className", PDEPlugin.getResourceString(NL_CLASS_NAME), "SampleView", 0);
		addOption("viewName", PDEPlugin.getResourceString(NL_NAME), PDEPlugin.getResourceString(NL_DEFAULT_NAME), 0);
		addOption("viewCategoryId", PDEPlugin.getResourceString(NL_CATEGORY_ID), (String)null, 0);
		addOption("viewCategoryName", PDEPlugin.getResourceString(NL_CATEGORY_NAME), PDEPlugin.getResourceString(NL_DEFAULT_CATEGORY_NAME), 0);
		addOption("viewType", PDEPlugin.getResourceString(NL_SELECT), 
					new String [][] {
						{"tableViewer", PDEPlugin.getResourceString(NL_TABLE)},
						{"treeViewer", PDEPlugin.getResourceString(NL_TREE)}},
						"tableViewer", 0);
		addToPerspective = (BooleanOption)addOption("addToPerspective",PDEPlugin.getResourceString(NL_ADD_TO_PERSPECTIVE),true,0);
		// second page
		addOption("doubleClick", PDEPlugin.getResourceString(NL_DOUBLE_CLICK), true, 1);
		addOption("popup", PDEPlugin.getResourceString(NL_POPUP), true, 1);
		addOption("localToolbar", PDEPlugin.getResourceString(NL_TOOLBAR), true, 1);
		addOption("localPulldown", PDEPlugin.getResourceString(NL_PULLDOWN), true, 1);
		addOption("sorter", PDEPlugin.getResourceString(NL_SORTING), true, 1);
		//addOption("filter", PDEPlugin.getResourceString(NL_FILTER), true, lists[1]);
		addOption("drillDown", PDEPlugin.getResourceString(NL_DRILLDOWN), true, 1);
		setOptionEnabled("drillDown", false);
	}

	protected void initializeFields(String id) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		initializeOption(KEY_PACKAGE_NAME, id+".views");
		initializeOption("viewCategoryId", id);
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".views");
		initializeOption("viewCategoryId", pluginId);
	}
	
	public boolean isDependentOnFirstPage() {
		return true;
	}
	
	public void addPages(Wizard wizard) {
		WizardPage page0 = createPage(0, IHelpContextIds.TEMPLATE_VIEW);
		page0.setTitle(PDEPlugin.getResourceString(NL_TITLE0));
		page0.setDescription(PDEPlugin.getResourceString(NL_DESC0));
		wizard.addPage(page0);
		
		WizardPage page1 = createPage(1, IHelpContextIds.TEMPLATE_VIEW);
		page1.setTitle(PDEPlugin.getResourceString(NL_TITLE1));
		page1.setDescription(PDEPlugin.getResourceString(NL_DESC1));
		wizard.addPage(page1);
		markPagesAdded();
	}

	public void validateOptions(TemplateOption source) {
		String viewType = getValue("viewType").toString();
		setOptionEnabled("drillDown", viewType.equals("treeViewer"));
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
		return "org.eclipse.ui.views";
	}
	
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.views", true);
		IPluginModelFactory factory = model.getPluginFactory();
		
		String cid = getStringOption("viewCategoryId");

		createCategory(extension, cid);
		String fullClassName = getStringOption(KEY_PACKAGE_NAME)+"."+getStringOption("className");
		
		IPluginElement viewElement = factory.createElement(extension);
		viewElement.setName("view");
		viewElement.setAttribute("id", fullClassName);
		viewElement.setAttribute("name", getStringOption("viewName"));
		viewElement.setAttribute("icon", "icons/sample.gif");

		viewElement.setAttribute("class", fullClassName);
		viewElement.setAttribute("category", cid);
		extension.add(viewElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
			
		if (addToPerspective.isSelected()) {
			IPluginExtension perspectiveExtension =
				createExtension("org.eclipse.ui.perspectiveExtensions", true);

			IPluginElement perspectiveElement = factory.createElement(perspectiveExtension);
			perspectiveElement.setName("perspectiveExtension");
			perspectiveElement.setAttribute(
				"targetID",
				"org.eclipse.ui.resourcePerspective");

			IPluginElement view = factory.createElement(perspectiveElement);
			view.setName("view");
			view.setAttribute("id", fullClassName);
			view.setAttribute("relative", "org.eclipse.ui.views.TaskList");
			view.setAttribute("relationship","right");
			view.setAttribute("ratio", "0.5");
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
			if (element.getName().equalsIgnoreCase("category")) {
				IPluginAttribute att = element.getAttribute("id");
				if (att!=null) {
					String cid = att.getValue();
					if (cid!=null && cid.equals(id))
						return;
				}
			}
		}
		IPluginElement categoryElement = model.getFactory().createElement(extension);
		categoryElement.setName("category");
		categoryElement.setAttribute("name", getStringOption("viewCategoryName"));
		categoryElement.setAttribute("id", id);
		extension.add(categoryElement);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#getFoldersToInclude()
	 */
	public String[] getFoldersToInclude() {
		return new String[] {"icons/"};
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList result = new ArrayList();
		if (schemaVersion != null)
			result.add(new PluginReference("org.eclipse.core.runtime.compatibility", null, 0));
		result.add(new PluginReference("org.eclipse.ui", null, 0));	
		return (IPluginReference[]) result.toArray(new IPluginReference[result.size()]);
	}
}