package org.eclipse.pde.internal.ui.wizards.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.IPluginStructureData;
import org.eclipse.pde.ui.templates.*;

public class PropertyPageTemplate extends PDETemplateSection {
	public static final String KEY_CLASSNAME = "className";
	public static final String KEY_PAGE_NAME = "pageName";
	public static final String KEY_TARGET_CLASS = "targetClass";
	public static final String KEY_NAME_FILTER = "nameFilter";

	private static final String NL_TITLE = "PropertyPageTemplate.title";
	private static final String NL_DESC = "PropertyPageTemplate.desc";
	private static final String NL_PACKAGE_NAME =
		"PropertyPageTemplate.packageName";
	private static final String NL_PAGE_CLASS = "PropertyPageTemplate.pageClass";
	private static final String NL_PAGE_NAME = "PropertyPageTemplate.pageName";
	private static final String NL_DEFAULT_PAGE_NAME =
		"PropertyPageTemplate.defaultPageName";
	private static final String NL_TARGET_CLASS =
		"PropertyPageTemplate.targetClass";
	private static final String NL_NAME_FILTER = "PropertyPageTemplate.nameFilter";

	/**
	 * Constructor for PropertyPageTemplate.
	 */
	public PropertyPageTemplate() {
		setPageCount(1);
		createOptions();
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0);
		page.setTitle(PDEPlugin.getResourceString(NL_TITLE));
		page.setDescription(PDEPlugin.getResourceString(NL_DESC));
		wizard.addPage(page);
		markPagesAdded();
	}

	private void createOptions() {
		addOption(
			KEY_PACKAGE_NAME,
			PDEPlugin.getResourceString(NL_PACKAGE_NAME),
			(String) null,
			0);
		addOption(
			KEY_CLASSNAME,
			PDEPlugin.getResourceString(NL_PAGE_CLASS),
			"SamplePropertyPage",
			0);
		addOption(
			KEY_PAGE_NAME,
			PDEPlugin.getResourceString(NL_PAGE_NAME),
			PDEPlugin.getResourceString(NL_DEFAULT_PAGE_NAME),
			0);
		addOption(
			KEY_TARGET_CLASS,
			PDEPlugin.getResourceString(NL_TARGET_CLASS),
			"org.eclipse.core.resources.IFile",
			0);
		addOption(
			KEY_NAME_FILTER,
			PDEPlugin.getResourceString(NL_NAME_FILTER),
			"*.*",
			0);
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

	protected void initializeFields(IPluginStructureData sdata, IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String pluginId = sdata.getPluginId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".properties");
	}

	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".properties");
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

	/**
	 * @see AbstractTemplateSection#updateModel(IProgressMonitor)
	 */
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getFactory();

		IPluginElement pageElement = factory.createElement(extension);
		pageElement.setName("page");
		pageElement.setAttribute(
			"id",
			getStringOption(KEY_PACKAGE_NAME) + ".samplePropertyPage");
		pageElement.setAttribute("name", getStringOption(KEY_PAGE_NAME));
		pageElement.setAttribute("objectClass", getStringOption(KEY_TARGET_CLASS));
		pageElement.setAttribute(
			"class",
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_CLASSNAME));
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