
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

public class EditorTemplate extends PDETemplateSection {
	public static final String EDITOR_CLASS_NAME = "editorClass";
	public static final String CONTRIBUTOR_CLASS = "contributorClass";
	public static final String EDITOR_NAME = "editorName";
	public static final String EXTENSIONS = "extensions";
	
	/**
	 * Constructor for EditorTemplate.
	 */
	public EditorTemplate() {
	}
	
	public void addPages(Wizard wizard) {
		pages = new WizardPage[1];
		createOptions();
		pages[0] = new OptionTemplateWizardPage(this, lists[0]);
		pages[0].setTitle("Sample XML Editor");
		pages[0].setDescription("Choose the options that will be used to generate the XML editor.");
		wizard.addPage(pages[0]);
	}

	private ArrayList [] createOptions() {
		lists = new ArrayList[1];
		lists[0] = new ArrayList();

		// first page	
		addOption(KEY_PACKAGE_NAME, "&Java Package Name:", (String)null, lists[0]);
		addOption(EDITOR_CLASS_NAME, "&Editor Class Name:", "XMLEditor", lists[0]);
		addOption(CONTRIBUTOR_CLASS, "Editor &Contributor Class Name:", "XMLEditorContributor", lists[0]);
		addOption(EDITOR_NAME, "Editor &Name:", "Sample XML Editor", lists[0]);
		addOption(EXTENSIONS, "&File Extension:", "xml", lists[0]);
		return lists;
	}

	public String getSectionId() {
		return "editor";
	}
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits()+1;
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
	
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.editors";
	}
	
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.editors", true);
		IPluginModelFactory factory = model.getFactory();
		
		IPluginElement editorElement = factory.createElement(extension);
		editorElement.setName("editor");
		editorElement.setAttribute("id", getStringOption(KEY_PACKAGE_NAME)+"."+getStringOption(EDITOR_CLASS_NAME));
		editorElement.setAttribute("name", getStringOption(EDITOR_NAME));
		editorElement.setAttribute("icon", "icons/sample.gif");
		editorElement.setAttribute("extensions", getStringOption(EXTENSIONS));

		editorElement.setAttribute("class", getStringOption(KEY_PACKAGE_NAME)+"."+getStringOption(EDITOR_CLASS_NAME));
		editorElement.setAttribute("contributorClass", getStringOption(KEY_PACKAGE_NAME)+"."+getStringOption(CONTRIBUTOR_CLASS));
		extension.add(editorElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}
}