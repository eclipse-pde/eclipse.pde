
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
import org.eclipse.pde.IPluginStructureData;

public class ViewTemplate extends PDETemplateSection {
	public static final String KEY_MESSAGE = "message";
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
		lists = new ArrayList[2];
		lists[0] = new ArrayList();
		lists[1] = new ArrayList();

		// first page	
		TemplateOption option;
		addOption(KEY_PACKAGE_NAME, "&Java Package Name:", (String)null, lists[0]);
		addOption("className", "&View Class Name:", "SampleView", lists[0]);
		addOption("viewName", "View &Name:", "Sample View", lists[0]);
		addOption("viewCategory", "View &Category Name:", "Sample Category", lists[0]);
		addOption("viewType", "Select the control the view should host:", 
					new String [][] {
						{"tableViewer", "&Table (can also used for lists)"},
						{"treeViewer", "T&ree" }},
						"tableViewer", lists[0]);
		// second page
		addOption("react", "&View should react to selections in the workbench", true, lists[1]);
		addOption("doubleClick", "&Add a double-click support", true, lists[1]);
		addOption("popup", "A&dd actions to the pop-up menu", true, lists[1]);
		addOption("localToolbar", "Add a&ctions to the view's tool bar", true, lists[1]);
		addOption("localPulldown", "Add ac&tions to the view's pull-down menu", true, lists[1]);
		addOption("sorter", "Add &support for sorting", true, lists[1]);
		//addOption("filter", "Add support for filtering", true, lists[1]);
		addOption("drillDown", "Add d&rill-down capability", true, lists[1]);
		return lists;
	}

	protected void initializeFields(IPluginStructureData sdata, FieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		initializeOption(KEY_PACKAGE_NAME, sdata.getPluginId());
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		initializeOption(KEY_PACKAGE_NAME, model.getPluginBase().getId());
	}
	
	public boolean isDependentOnFirstPage() {
		return true;
	}
	
	public void addPages(Wizard wizard) {
		pages = new WizardPage[2];
		createOptions();
		pages[0] = new GenericTemplateWizardPage(this, lists[0]);
		pages[0].setTitle("Main View Settings");
		pages[0].setDescription("Choose the way the new view will be added to the plug-in");
		wizard.addPage(pages[0]);
		
		pages[1] = new GenericTemplateWizardPage(this, lists[1]);
		pages[1].setTitle("View Features");
		pages[1].setDescription("Choose the features that the new view should have");
		wizard.addPage(pages[1]);
	}

	public void validateOptions(TemplateOption source) {
		String viewType = getValue("viewType").toString();
		setOptionEnabled("drillDown", viewType.equals("treeViewer"));
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		}
		else resetPageState();
	}
	
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.views";
	}
	
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.views", true);
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
		String fullClassName = getStringOption(KEY_PACKAGE_NAME)+"."+getStringOption("className");
		viewElement.setAttribute("class", fullClassName);
		viewElement.setAttribute("category", cid);
		extension.add(viewElement);
		plugin.add(extension);
	}
}