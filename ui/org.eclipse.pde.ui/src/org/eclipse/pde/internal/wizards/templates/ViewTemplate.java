
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

public class ViewTemplate extends PDETemplateSection {
	public static final String KEY_CLASS_NAME = "className";
	public static final String KEY_MESSAGE = "message";
	public static final String CLASS_NAME = "SampleView";
	private WizardPage page;
	private Hashtable options = new Hashtable();
	/**
	 * Constructor for HelloWorldTemplate.
	 */
	public ViewTemplate() {
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
	
	private ArrayList createOptions() {
		ArrayList list = new ArrayList();
	
		addOption("packageName", "&Java Package Name:", "", list);
		addOption(KEY_CLASS_NAME, "&View Class Name:", CLASS_NAME, list);
		addOption("viewName", "View &Name:", "Sample View", list);
		addOption("viewCategory", "View &Category Name:", "Sample Category", list);
		addOption("react", "View &should react to selections in the workbench", true, list);
		addOption("doubleClick", "Add a double-click support", true, list);
		addOption("popup", "&Add actions to the pop-up menu", true, list);
		addOption("localToolbar", "Add actions to the view's tool bar", true, list);
		addOption("localPulldown", "Add actions to the view's pull-down menu", true, list);
		addOption("sorter", "Add support for sorting", true, list);
		addOption("filter", "Add support for filtering", true, list);
		return list;
	}
	
	public void addPages(Wizard wizard) {
		ArrayList list = createOptions();
		page = new GenericTemplateWizardPage(this, list);
		page.setTitle("Sample View");
		page.setDescription("This template will generate a sample view and add it into the platform by registering a new view category.");
		wizard.addPage(page);
	}
	
	public void validateOptions(TemplateOption source) {
		String message = null;
		String name = source.getName();
	}
	
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.views";
	}
	
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = model.getFactory().createExtension();
		extension.setPoint("org.eclipse.ui.views");
		IPluginModelFactory factory = model.getFactory();

		IPluginElement categoryElement = factory.createElement(extension);
		categoryElement.setName("category");
		categoryElement.setAttribute("name", "Sample View");
		String cid = plugin.getId();
		categoryElement.setAttribute("id", cid);
		extension.add(categoryElement);
		
		IPluginElement viewElement = factory.createElement(extension);
		viewElement.setName("view");
		viewElement.setAttribute("id", cid+".sampleView");
		viewElement.setAttribute("name", getStringOption("viewName"));
		viewElement.setAttribute("icon", "icons/sample.gif");
		viewElement.setAttribute("class", plugin.getId()+"."+getStringOption(KEY_CLASS_NAME));
		viewElement.setAttribute("category", cid);
		extension.add(viewElement);
		
		plugin.add(extension);
	}
}
