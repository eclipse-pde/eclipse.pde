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
import org.eclipse.pde.ui.templates.*;

public class PropertyPageTemplate extends PDETemplateSection {

	public static final String KEY_CLASSNAME = "className";
	public static final String KEY_PAGE_NAME = "pageName";
	public static final String KEY_TARGET_CLASS = "targetClass";
	public static final String KEY_NAME_FILTER = "nameFilter";
	
	/**
	 * Constructor for PropertyPageTemplate.
	 */
	public PropertyPageTemplate() {
		super();
	}

	public void addPages(Wizard wizard) {
		lists = new ArrayList[1];
		lists[0] = new ArrayList();
		
		createOptions();

		pages = new WizardPage[1];
		pages[0] = new OptionTemplateWizardPage(this, lists[0]);
		pages[0].setTitle("Sample Property Page");
		pages[0].setDescription("This template adds a property page to a resource and will appear in the Properties Dialog for that resource.");
		wizard.addPage(pages[0]);
	}
	
	private void createOptions() {
		addOption(KEY_PACKAGE_NAME, "&Java Package Name:", (String)null, lists[0]);
		addOption(KEY_CLASSNAME, "&Property Page Class:", "SamplePropertyPage", lists[0]);
		addOption(KEY_PAGE_NAME, "P&roperty Page Name:", "Sample Page", lists[0]);
		addOption(KEY_TARGET_CLASS, "&Target Class:","org.eclipse.core.resources.IFile", lists[0]);
		addOption(KEY_NAME_FILTER, "&Name Filter:", "*.*",lists[0]);
	}
	/**
	 * @see PDETemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "propertyPages";
	}

	public boolean isDependentOnFirstPage() {
		return true;
	}
	
	protected void initializeFields(IPluginStructureData sdata, FieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String pluginId = sdata.getPluginId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".properties");
	}
	
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".properties");
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

		IPluginElement pageElement = factory.createElement(extension);
		pageElement.setName("page");
		pageElement.setAttribute("id", getStringOption(KEY_PACKAGE_NAME) + ".samplePropertyPage");
		pageElement.setAttribute("name", getStringOption(KEY_PAGE_NAME));
		pageElement.setAttribute("objectClass", getStringOption(KEY_TARGET_CLASS));
		pageElement.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_CLASSNAME));
		pageElement.setAttribute("nameFilter", getStringOption(KEY_NAME_FILTER));
		
		extension.add(pageElement);
		if (!extension.isInTheModel())
			plugin.add(extension);		
	}

	/**
	 * @see ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.propertyPages";
	}

}
