
package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.IBasePluginWizard;
import org.eclipse.pde.model.plugin.IPluginModelBase;
import org.eclipse.pde.model.plugin.IPluginReference;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.jdt.core.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.PDEPlugin;
import java.net.*;
import java.util.*;
import org.eclipse.jface.wizard.*;

public class HelloWorldTemplate extends PDETemplateSection {
	public static final String KEY_CLASS_NAME = "className";
	public static final String KEY_MESSAGE = "message";
	public static final String CLASS_NAME = "SampleAction";
	private WizardPage page;
	private Hashtable options = new Hashtable();
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
		ArrayList list = new ArrayList();
		TemplateOption option;
		
		option = new StringOption(this, "packageName", "Java Package Name:");
		list.add(option);
		registerOption(option);

		option = new StringOption(this, KEY_CLASS_NAME, "Action Class Name:");
		option.setValue(CLASS_NAME);
		list.add(option);
		registerOption(option);
		
		option = new StringOption(this, KEY_MESSAGE, "Message Box Text:");
		option.setValue("Hello, Eclipse world");
		list.add(option);
		registerOption(option);
		
		page = new GenericTemplateWizardPage(this, list);
		page.setTitle("Sample Action Set");
		page.setDescription("This template will generate a sample action set extension with a menu, a menu item and a tool bar button. When selected, they will show a simple message dialog.");
		wizard.addPage(page);
	}
	
	public void validateOptions(TemplateOption source) {
		String message = null;
		String name = source.getName();
		if (source.equals(KEY_CLASS_NAME) || source.equals(KEY_MESSAGE)) {
			page.setPageComplete(!source.isEmpty());
		}
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
		
		IPluginElement actionElement = factory.createElement(setElement);
		actionElement.setName("action");
		actionElement.setAttribute("id", plugin.getId()+".sampleAction");
		actionElement.setAttribute("label", "&Sample Action");
		actionElement.setAttribute("menubarPath", "sampleMenu/sampleGroup");
		actionElement.setAttribute("toolbarPath", "sampleGroup");
		actionElement.setAttribute("icon", "icons/sample.gif");
		actionElement.setAttribute("tooltip", "Hello Eclipse World");
		actionElement.setAttribute("class", plugin.getId()+"."+CLASS_NAME);
		setElement.add(actionElement);
		extension.add(setElement);
		plugin.add(extension);
	}
}
