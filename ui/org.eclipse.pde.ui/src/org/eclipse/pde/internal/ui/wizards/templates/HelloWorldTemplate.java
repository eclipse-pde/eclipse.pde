
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.ui.IBasePluginWizard;
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
	public static final String CLASS_NAME = "SampleAction";

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
		return super.getNumberOfWorkUnits()+1;
	}
	
	public void addPages(Wizard wizard) {
		lists = new ArrayList[1];
		lists[0] = new ArrayList();

		addOption(KEY_PACKAGE_NAME, "&Java Package Name:", (String)null, lists[0]);
		addOption(KEY_CLASS_NAME, "&Action Class Name:", CLASS_NAME, lists[0]);
		addOption(KEY_MESSAGE, "&Message Box Text:", "Hello, Eclipse world", lists[0]);
		
		pages = new WizardPage[1];
		pages[0] = new GenericTemplateWizardPage(this, lists[0]);
		pages[0].setTitle("Sample Action Set");
		pages[0].setDescription("This template will generate a sample action set extension with a menu, a menu item and a tool bar button. When selected, they will show a simple message dialog.");
		wizard.addPage(pages[0]);
	}
	
	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		}
		else resetPageState();
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
	
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.actionSets";
	}
	
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.actionSets", true);
		IPluginModelFactory factory = model.getFactory();

		IPluginElement setElement = factory.createElement(extension);
		setElement.setName("actionSet");
		setElement.setAttribute("id", plugin.getId()+".actionSet");
		setElement.setAttribute("label", "Sample Action Set");
		setElement.setAttribute("visible", "true");
		
		IPluginElement menuElement = factory.createElement(setElement);
		menuElement.setName("menu");
		menuElement.setAttribute("label", "Sample &Menu");
		menuElement.setAttribute("id", "sampleMenu");

		IPluginElement groupElement = factory.createElement(menuElement);
		groupElement.setName("separator");
		groupElement.setAttribute("name", "sampleGroup");
		menuElement.add(groupElement);
		setElement.add(menuElement);
		
		String fullClassName = getStringOption(KEY_PACKAGE_NAME)+"."+getStringOption(KEY_CLASS_NAME);
		
		IPluginElement actionElement = factory.createElement(setElement);
		actionElement.setName("action");
		actionElement.setAttribute("id", fullClassName);
		actionElement.setAttribute("label", "&Sample Action");
		actionElement.setAttribute("menubarPath", "sampleMenu/sampleGroup");
		actionElement.setAttribute("toolbarPath", "sampleGroup");
		actionElement.setAttribute("icon", "icons/sample.gif");
		actionElement.setAttribute("tooltip", "Hello Eclipse World");
		actionElement.setAttribute("class", fullClassName);
		setElement.add(actionElement);
		extension.add(setElement);
		plugin.add(extension);
	}
}
