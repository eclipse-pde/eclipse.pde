
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
	public static final String KEY_MESSAGE = "message";
	public static final String CLASS_NAME = "SampleView";
	private WizardPage page1, page2;
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
	
	private ArrayList [] createOptions() {
		ArrayList [] lists = new ArrayList[2];
		lists[0] = new ArrayList();
		lists[1] = new ArrayList();

		// first page	
		addOption("packageName", "&Java Package Name:", "", lists[0]);
		addOption("className", "&View Class Name:", CLASS_NAME, lists[0]);
		addOption("viewName", "View &Name:", "Sample View", lists[0]);
		addOption("viewCategory", "View &Category Name:", "Sample Category", lists[0]);
		addOption("viewType", "Select the control the view should host:", 
					new String [][] {
						{"tableViewer", "Table (can also used for lists)"},
						{"treeViewer", "Tree" }},
						"tableViewer", lists[0]);
		
		addOption("react", "View &should react to selections in the workbench", true, lists[1]);
		addOption("doubleClick", "Add a double-click support", true, lists[1]);
		addOption("popup", "&Add actions to the pop-up menu", true, lists[1]);
		addOption("localToolbar", "Add actions to the view's tool bar", true, lists[1]);
		addOption("localPulldown", "Add actions to the view's pull-down menu", true, lists[1]);
		addOption("sorter", "Add support for sorting", true, lists[1]);
		addOption("filter", "Add support for filtering", true, lists[1]);
		return lists;
	}
	
	public void addPages(Wizard wizard) {
		ArrayList [] lists = createOptions();
		page1 = new GenericTemplateWizardPage(this, lists[0]);
		page1.setTitle("Main View Settings");
		page1.setDescription("Choose the way the new view will be added to the plug-in");
		wizard.addPage(page1);
		
		page2 = new GenericTemplateWizardPage(this, lists[1]);
		page2.setTitle("View Features");
		page2.setDescription("Choose the features that the new view should have");
		wizard.addPage(page2);
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
		categoryElement.setAttribute("name", getStringOption("viewCategory"));
		String cid = plugin.getId();
		categoryElement.setAttribute("id", cid);
		extension.add(categoryElement);
		
		IPluginElement viewElement = factory.createElement(extension);
		viewElement.setName("view");
		viewElement.setAttribute("id", cid+".sampleView");
		viewElement.setAttribute("name", getStringOption("viewName"));
		viewElement.setAttribute("icon", "icons/sample.gif");
		viewElement.setAttribute("class", plugin.getId()+"."+getStringOption("className"));
		viewElement.setAttribute("category", cid);
		extension.add(viewElement);
		
		plugin.add(extension);
	}
}