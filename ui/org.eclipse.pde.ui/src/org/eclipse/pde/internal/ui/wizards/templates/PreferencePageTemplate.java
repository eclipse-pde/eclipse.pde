
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

public class PreferencePageTemplate extends PDETemplateSection {
	private String mainClassName;

	public PreferencePageTemplate() {
	}
	
	public String getSectionId() {
		return "preferences";
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
		addOption("pageClassName", "&Page Class Name:", "SamplePreferencePage", lists[0]);
		addOption("pageName", "Page &Name:", "Sample Preferences", lists[0]);
		return lists;
	}

	protected void initializeFields(IPluginStructureData sdata, FieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String pluginId = sdata.getPluginId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".preferences");
		mainClassName = data.className;
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId+".preferences");
		if (model instanceof IPluginModel) {
			IPlugin plugin = (IPlugin)model.getPluginBase();
			mainClassName = plugin.getClassName();
		}
	}
	
	public String getReplacementString(String fileName, String key) {
		if (key.equals("fullPluginClassName")) return mainClassName;
		if (key.equals("pluginClassName"))
			return getPluginClassName();
		return super.getReplacementString(fileName, key);
	}
	
	private String getPluginClassName() {
		int dot = mainClassName.lastIndexOf('.');
		if (dot!= -1) {
			return mainClassName.substring(dot+1);
		}
		return mainClassName;
	}
	
	public boolean isDependentOnFirstPage() {
		return true;
	}
	
	public void addPages(Wizard wizard) {
		pages = new WizardPage[1];
		createOptions();
		pages[0] = new GenericTemplateWizardPage(this, lists[0]);
		pages[0].setTitle("Sample Preference Page");
		pages[0].setDescription("The provided options allow you to control the preference page that will be created.");
		wizard.addPage(pages[0]);
	}

	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		}
		else resetPageState();
	}
	
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.preferencePages";
	}
	
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getFactory();
		
		String fullClassName = getStringOption(KEY_PACKAGE_NAME)+"."+getStringOption("pageClassName");
		
		IPluginElement pageElement = factory.createElement(extension);
		pageElement.setName("page");
		pageElement.setAttribute("id", fullClassName);
		pageElement.setAttribute("name", getStringOption("pageName"));
		pageElement.setAttribute("class", fullClassName);
		extension.add(pageElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}
}