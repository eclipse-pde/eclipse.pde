package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.ui.IBasePluginWizard;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.jdt.core.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.ui.PDEPlugin;
import java.net.*;
import java.util.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.ui.IPluginStructureData;

public class HelloWorldTemplate extends PDETemplateSection {
	public static final String KEY_CLASS_NAME = "className";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_ADD_TO_PERSPECTIVE = "addToPerspective";
	public static final String CLASS_NAME = "SampleAction";

	private static final String KEY_TITLE = "HelloWorldTemplate.title";
	private static final String KEY_DESC = "HelloWorldTemplate.desc";
	private static final String KEY_PACKAGE_LABEL =
		"HelloWorldTemplate.packageName";
	private static final String KEY_CLASS_LABEL = "HelloWorldTemplate.className";
	private static final String KEY_TEXT_LABEL = "HelloWorldTemplate.messageText";
	private static final String KEY_DEFAULT_MESSAGE =
		"HelloWorldTemplate.defaultMessage";
	private static final String KEY_SAMPLE_ACTION_SET = "HelloWorldTemplate.sampleActionSet";		
	private static final String KEY_SAMPLE_MENU = "HelloWorldTemplate.sampleMenu";
	private static final String KEY_SAMPLE_ACTION = "HelloWorldTemplate.sampleAction";
	private static final String NL_ADD_TO_PERSPECTIVE = "HelloWorldTemplate.addToPerspective";
	
	private BooleanOption addToPerspective;
	/**
	 * Constructor for HelloWorldTemplate.
	 */
	public HelloWorldTemplate() {

	}

	public String getSectionId() {
		return "helloWorld";
	}
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	public void addPages(Wizard wizard) {
		setPageCount(1);

		addOption(
			KEY_PACKAGE_NAME,
			PDEPlugin.getResourceString(KEY_PACKAGE_LABEL),
			(String) null,
			0);
		addOption(
			KEY_CLASS_NAME,
			PDEPlugin.getResourceString(KEY_CLASS_LABEL),
			CLASS_NAME,
			0);
		addOption(
			KEY_MESSAGE,
			PDEPlugin.getResourceString(KEY_TEXT_LABEL),
			PDEPlugin.getResourceString(KEY_DEFAULT_MESSAGE),
			0);
		addToPerspective = (BooleanOption) addOption(
			KEY_ADD_TO_PERSPECTIVE,
			PDEPlugin.getResourceString(NL_ADD_TO_PERSPECTIVE),
			true,
			0);
			
		WizardPage page = createPage(0);
		page.setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		page.setDescription(PDEPlugin.getResourceString(KEY_DESC));
		wizard.addPage(page);
	}

	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		} else
			resetPageState();
	}

	public boolean isDependentOnFirstPage() {
		return true;
	}

	protected void initializeFields(IPluginStructureData sdata, IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String pluginId = sdata.getPluginId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".actions");
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".actions");
	}

	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.actionSets";
	}

	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.actionSets", true);
		IPluginModelFactory factory = model.getFactory();

		IPluginElement setElement = factory.createElement(extension);
		setElement.setName("actionSet");
		setElement.setAttribute("id", plugin.getId() + ".actionSet");
		setElement.setAttribute("label", PDEPlugin.getResourceString(KEY_SAMPLE_ACTION_SET));
		setElement.setAttribute("visible", "true");

		IPluginElement menuElement = factory.createElement(setElement);
		menuElement.setName("menu");
		menuElement.setAttribute("label", PDEPlugin.getResourceString(KEY_SAMPLE_MENU));
		menuElement.setAttribute("id", "sampleMenu");

		IPluginElement groupElement = factory.createElement(menuElement);
		groupElement.setName("separator");
		groupElement.setAttribute("name", "sampleGroup");
		menuElement.add(groupElement);
		setElement.add(menuElement);

		String fullClassName =
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_CLASS_NAME);

		IPluginElement actionElement = factory.createElement(setElement);
		actionElement.setName("action");
		actionElement.setAttribute("id", fullClassName);
		actionElement.setAttribute("label", PDEPlugin.getResourceString(KEY_SAMPLE_ACTION));
		actionElement.setAttribute("menubarPath", "sampleMenu/sampleGroup");
		actionElement.setAttribute("toolbarPath", "sampleGroup");
		actionElement.setAttribute("icon", "icons/sample.gif");
		actionElement.setAttribute("tooltip", PDEPlugin.getResourceString(KEY_DEFAULT_MESSAGE));
		actionElement.setAttribute("class", fullClassName);
		setElement.add(actionElement);
		extension.add(setElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
		
		if (addToPerspective.isSelected()) {
			IPluginExtension perspectiveExtension = createExtension("org.eclipse.ui.perspectiveExtensions",true);
			IPluginElement perspectiveElement = factory.createElement(perspectiveExtension);
			perspectiveElement.setName("perspectiveExtension");
			perspectiveElement.setAttribute(
				"targetID",
				"org.eclipse.ui.resourcePerspective");

			IPluginElement actionSetElement = factory.createElement(perspectiveElement);
			actionSetElement.setName("actionSet");
			actionSetElement.setAttribute("id", plugin.getId() + ".actionSet");
			perspectiveElement.add(actionSetElement);
			
			perspectiveExtension.add(perspectiveElement);
			if (!perspectiveExtension.isInTheModel())
				plugin.add(perspectiveExtension);
		}		
			
	}
}