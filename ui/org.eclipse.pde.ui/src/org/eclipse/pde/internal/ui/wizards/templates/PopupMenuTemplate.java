package org.eclipse.pde.internal.ui.wizards.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

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
import org.eclipse.pde.ui.IPluginStructureData;

public class PopupMenuTemplate extends PDETemplateSection {

	public static final String KEY_TARGET_OBJECT = "objectClass";
	public static final String KEY_NAME_FILTER = "nameFilter";
	public static final String KEY_SUBMENU_LABEL = "subMenuLabel";
	public static final String KEY_ACTION_LABEL = "actionLabel";
	public static final String KEY_ACTION_CLASS = "actionClass";
	public static final String KEY_SELECTION = "selection";
	
	/**
	 * Constructor for PropertyPageTemplate.
	 */
	public PopupMenuTemplate() {
		super();
	}

	public void addPages(Wizard wizard) {
		lists = new ArrayList[1];
		lists[0] = new ArrayList();
		
		createOptions();

		pages = new WizardPage[1];
		pages[0] = new GenericTemplateWizardPage(this, lists[0]);
		pages[0].setTitle("Sample Popup Menu");
		pages[0].setDescription("This template creates a submenu and adds a new action to a selected object's popup menu");
		wizard.addPage(pages[0]);
	}
	
	private void createOptions() {
		addOption(KEY_TARGET_OBJECT,"&Target Object's Class:","org.eclipse.core.resources.IFile", lists[0]);
		addOption(KEY_NAME_FILTER,"&Name Filter:","plugin.xml",lists[0]);
		addOption(KEY_SUBMENU_LABEL,"&Submenu Name:","New Submenu",lists[0]);
		addOption(KEY_ACTION_LABEL,"&Action Label:","New Action",lists[0]);
		addOption(KEY_PACKAGE_NAME,"&Java Package Name:",(String)null,lists[0]);
		addOption(KEY_ACTION_CLASS,"Action &Class:","NewAction",lists[0]);		
		addOption(KEY_SELECTION,"Action is enabled for:", new String[][] {
						{"singleSelection","single selection"},{"multipleSelection", "multiple selection"}},"singleSelection",lists[0]);
	}
	/**
	 * @see PDETemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "popupMenus";
	}

	public boolean isDependentOnFirstPage() {
		return true;
	}
	
	protected void initializeFields(IPluginStructureData sdata, FieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String pluginId = sdata.getPluginId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".actions");
	}
	
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".actions");
	}
	
	/**
	 * @see GenericTemplateSection#validateOptions(TemplateOption)
	 */
	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		} else {
			validateContainerPage(source);
		}
	}

	private void validateContainerPage(TemplateOption source) {
		ArrayList allPageOptions = lists[0];
		for (int i = 0; i < allPageOptions.size(); i++) {
			TemplateOption nextOption = (TemplateOption) allPageOptions.get(i);
			if (nextOption.isRequired() && nextOption.isEmpty()) {
				flagMissingRequiredOption(nextOption);
				return;
			}
		}
		resetPageState();
	}

	/**
	 * @see AbstractTemplateSection#updateModel(IProgressMonitor)
	 */
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getFactory();
		
		IPluginElement objectContributionElement = factory.createElement(extension);
		objectContributionElement.setName("objectContribution");
		objectContributionElement.setAttribute("objectClass",getStringOption(KEY_TARGET_OBJECT));
		objectContributionElement.setAttribute("nameFilter",getStringOption(KEY_NAME_FILTER));
		objectContributionElement.setAttribute("id", model.getPluginBase().getId() + ".contribution1");
		
		IPluginElement menuElement = factory.createElement(objectContributionElement);
		menuElement.setName("menu");
		menuElement.setAttribute("label",getStringOption(KEY_SUBMENU_LABEL));
		menuElement.setAttribute("path","additions");
		menuElement.setAttribute("id",model.getPluginBase().getId() + ".menu1");
		
		IPluginElement separatorElement = factory.createElement(menuElement);
		separatorElement.setName("separator");
		separatorElement.setAttribute("name","group1");
		menuElement.add(separatorElement);
		objectContributionElement.add(menuElement);
		
		IPluginElement actionElement = factory.createElement(objectContributionElement);
		actionElement.setName("action");
		actionElement.setAttribute("label",getStringOption(KEY_ACTION_LABEL));
		actionElement.setAttribute("class",getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_ACTION_CLASS));
		actionElement.setAttribute("menubarPath",model.getPluginBase().getId() +".menu1/group1");
		actionElement.setAttribute("enablesFor",getValue(KEY_SELECTION).toString().equals("singleSelection")?"1":"multiple");
		actionElement.setAttribute("id",model.getPluginBase().getId() +".newAction");
		objectContributionElement.add(actionElement);
		
		extension.add(objectContributionElement);
		if (!extension.isInTheModel())
			plugin.add(extension);		
	}

	/**
	 * @see ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.popupMenus";
	}

}
