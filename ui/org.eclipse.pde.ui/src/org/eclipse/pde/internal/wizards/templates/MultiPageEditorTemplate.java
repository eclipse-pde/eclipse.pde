
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

public class MultiPageEditorTemplate extends PDETemplateSection {
	/**
	 * Constructor for MultiPageEditorTemplate.
	 */
	public MultiPageEditorTemplate() {
	}
	
	public String getSectionId() {
		return "multiPageEditor";
	}
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits()+1;
	}
	
	private ArrayList [] createOptions() {
		lists = new ArrayList[1];
		lists[0] = new ArrayList();

		// first page	
		addOption(KEY_PACKAGE_NAME, "&Java Package Name:", (String)null, lists[0]);
		addOption("editorClassName", "&Editor Class Name:", "MultiPageEditor", lists[0]);
		addOption("contributorClassName", "Editor &Contributor Class &Name:", "MultiPageEditorContributor", lists[0]);
		addOption("editorName", "Editor &Name:", "Sample Multi-page Editor", lists[0]);
		addOption("extensions", "&File Extensions:", "mpe", lists[0]);
		return lists;
	}

	protected void initializeFields(IPluginStructureData sdata, FieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String pluginId = sdata.getPluginId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".editors");
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".editors");
	}
	
	public boolean isDependentOnFirstPage() {
		return true;
	}
	
	public void addPages(Wizard wizard) {
		pages = new WizardPage[1];
		createOptions();
		pages[0] = new GenericTemplateWizardPage(this, lists[0]);
		pages[0].setTitle("Sample Multi-Page Editor");
		pages[0].setDescription("Choose the options that will be used to generate the multi-page editor.");
		wizard.addPage(pages[0]);
	}

	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		}
		else resetPageState();
	}
	
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.editors";
	}
	
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.editors", true);
		IPluginModelFactory factory = model.getFactory();
		
		String editorClassName = getStringOption(KEY_PACKAGE_NAME)+"."+getStringOption("editorClassName");
		String contributorClassName = getStringOption(KEY_PACKAGE_NAME)+"."+getStringOption("contributorClassName");
		
		IPluginElement editorElement = factory.createElement(extension);
		editorElement.setName("editor");
		editorElement.setAttribute("id", editorClassName);
		editorElement.setAttribute("name", getStringOption("editorName"));
		editorElement.setAttribute("icon", "icons/copy.gif");
		editorElement.setAttribute("extensions", getStringOption("extensions"));

		editorElement.setAttribute("class", editorClassName);
		editorElement.setAttribute("contributorClass", contributorClassName);
		extension.add(editorElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}
}