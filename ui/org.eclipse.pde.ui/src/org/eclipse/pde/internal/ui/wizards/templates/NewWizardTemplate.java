
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

public class NewWizardTemplate extends PDETemplateSection {

	public NewWizardTemplate() {
	}
	
	public String getSectionId() {
		return "newWizard";
	}
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits()+1;
	}
	
	private void createOptions() {
		// first page
		addOption(KEY_PACKAGE_NAME, "&Java Package Name:", (String)null, 0);
		addOption("categoryId", "&Wizard Category Id:", (String)null, 0);
		addOption("categoryName", "Wi&zard Category Name:", "Sample Wizards", 0);
		addOption("wizardClassName", "Wizard &Class Name:", "SampleNewWizard", 0);
		addOption("wizardPageClassName", "Wizard &Page Class Name:", "SampleNewWizardPage", 0);
		addOption("wizardName", "Wizard &Name:", "Multi-page Editor file", 0);
		addOption("extension", "&File Extension:", "mpe", 0);
		addOption("initialFileName", "&Initial File Name:", "new_file.mpe", 0);
	}

	protected void initializeFields(IPluginStructureData sdata, IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String pluginId = sdata.getPluginId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".wizards");
		initializeOption("categoryId", pluginId);
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".wizards");
		initializeOption("categoryId", pluginId);
	}
	
	public boolean isDependentOnFirstPage() {
		return true;
	}
	
	public void addPages(Wizard wizard) {
		setPageCount(1);
		createOptions();
		WizardPage page = createPage(0);
		page.setTitle("New Wizard Options");
		page.setDescription("The provided options allow you to control the new wizard will be created.");
		wizard.addPage(page);
	}

	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		}
		else resetPageState();
	}
	
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.newWizards";
	}
	
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.newWizards", true);
		IPluginModelFactory factory = model.getFactory();
		
		String cid = getStringOption("categoryId");

		createCategory(extension, cid);
		String fullClassName = getStringOption(KEY_PACKAGE_NAME)+"."+getStringOption("wizardClassName");
		
		IPluginElement viewElement = factory.createElement(extension);
		viewElement.setName("wizard");
		viewElement.setAttribute("id", fullClassName);
		viewElement.setAttribute("name", getStringOption("wizardName"));
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
		categoryElement.setAttribute("name", getStringOption("categoryName"));
		categoryElement.setAttribute("id", id);
		extension.add(categoryElement);
	}
}