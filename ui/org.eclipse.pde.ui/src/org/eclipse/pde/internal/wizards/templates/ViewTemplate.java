
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
		addOption(KEY_PACKAGE_NAME, "&Java Package Name:", (String)null, lists[0]);
		addOption("className", "&View Class Name:", "SampleView", lists[0]);
		addOption("viewName", "View &Name:", "Sample View", lists[0]);
		addOption("viewCategoryId", "View &Category Id:", (String)null, lists[0]);
		addOption("viewCategoryName", "V&iew Category Name:", "Sample Category", lists[0]);
		addOption("viewType", "Select the viewer type that should be hosted in the view:", 
					new String [][] {
						{"tableViewer", "&Table viewer (can also be used for lists)"},
						{"treeViewer", "T&ree viewer" }},
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
		setOptionEnabled("drillDown", false);
		return lists;
	}

	protected void initializeFields(IPluginStructureData sdata, FieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String pluginId = sdata.getPluginId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".views");
		initializeOption("viewCategoryId", pluginId);
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
		pages = new WizardPage[2];
		createOptions();
		pages[0] = new GenericTemplateWizardPage(this, lists[0]);
		pages[0].setTitle("Main View Settings");
		pages[0].setDescription("Choose the way the new view will be added to the plug-in.");
		wizard.addPage(pages[0]);
		
		pages[1] = new GenericTemplateWizardPage(this, lists[1]);
		pages[1].setTitle("View Features");
		pages[1].setDescription("Choose the features that the new view should have.");
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
}