package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;


public class ViewRCPTemplate extends PDETemplateSection {
	
	public static final String KEY_APPLICATION_CLASS = "applicationClass"; //$NON-NLS-1$
	public static final String KEY_APPLICATION_ID = "applicationID"; //$NON-NLS-1$
	public static final String KEY_WINDOW_TITLE = "windowTitle"; //$NON-NLS-1$
	
	public ViewRCPTemplate() {
		setPageCount(1);
		createOptions();
	}
	
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_RCP_MAIL);
		page.setTitle(PDEPlugin.getResourceString("ViewRCPTemplate.title")); //$NON-NLS-1$
		page.setDescription(PDEPlugin.getResourceString("ViewRCPTemplate.desc"));  //$NON-NLS-1$
		wizard.addPage(page);
		markPagesAdded();
	}

	
	private void createOptions() {
		addOption(KEY_WINDOW_TITLE, PDEPlugin.getResourceString("ViewRCPTemplate.windowTitle"), "RCP Application", 0); //$NON-NLS-1$ //$NON-NLS-2$
		
		addOption(KEY_APPLICATION_ID, PDEPlugin.getResourceString("ViewRCPTemplate.appId"), "application", 0); //$NON-NLS-1$ //$NON-NLS-2$

		addOption(KEY_PACKAGE_NAME, PDEPlugin.getResourceString("ViewRCPTemplate.packageName"), (String) null, 0); //$NON-NLS-1$
		
		addOption(KEY_APPLICATION_CLASS, PDEPlugin.getResourceString("ViewRCPTemplate.appClass"), "Application", 0); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id) + ".rcp");  //$NON-NLS-1$
	}
	
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId) + ".rcp");  //$NON-NLS-1$
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.ui.templates.OptionTemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "viewRCP"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.BaseOptionTemplateSection#validateOptions(org.eclipse.pde.ui.templates.TemplateOption)
	 */
	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		} else {
			validateContainerPage(source);
		}
	}
	
	private void validateContainerPage(TemplateOption source) {
		TemplateOption[] allPageOptions = getOptions(0);
		for (int i = 0; i < allPageOptions.length; i++) {
			TemplateOption nextOption = allPageOptions[i];
			if (nextOption.isRequired() && nextOption.isEmpty()) {
				flagMissingRequiredOption(nextOption);
				return;
			}
		}
		resetPageState();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#updateModel(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		createApplicationExtension();
		createPerspectiveExtension();
		createViewExtension();
	}
	
	private void createApplicationExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		
		IPluginExtension extension = createExtension("org.eclipse.core.runtime.applications", true); //$NON-NLS-1$
		extension.setId(getStringOption(KEY_APPLICATION_ID));
		
		IPluginElement element = model.getPluginFactory().createElement(extension);
		element.setName("application"); //$NON-NLS-1$
		extension.add(element);
		
		IPluginElement run = model.getPluginFactory().createElement(element);
		run.setName("run"); //$NON-NLS-1$
		run.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_APPLICATION_CLASS)); //$NON-NLS-1$ //$NON-NLS-2$
		element.add(run);
		
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	private void createPerspectiveExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		
		IPluginExtension extension = createExtension("org.eclipse.ui.perspectives", true); //$NON-NLS-1$
		IPluginElement element = model.getPluginFactory().createElement(extension);
		element.setName("perspective"); //$NON-NLS-1$
		element.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + ".Perspective"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("name", "Perspective"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("id", plugin.getId() + ".perspective"); //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(element);
		
		if (!extension.isInTheModel())
			plugin.add(extension);
	}
	
	private void createViewExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		String id = plugin.getId();
		IPluginExtension extension = createExtension("org.eclipse.ui.views", true); //$NON-NLS-1$
		
		IPluginElement view = model.getPluginFactory().createElement(extension);
		view.setName("view"); //$NON-NLS-1$
		view.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + ".View" ); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("name", "View"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("id", id + ".view"); //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(view);
		
		if (!extension.isInTheModel())
			plugin.add(extension);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.BaseOptionTemplateSection#isDependentOnParentWizard()
	 */
	public boolean isDependentOnParentWizard() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		IPluginReference[] dep = new IPluginReference[2];
		dep[0] = new PluginReference("org.eclipse.core.runtime", null, 0); //$NON-NLS-1$
		dep[1] = new PluginReference("org.eclipse.ui", null, 0); //$NON-NLS-1$
		return dep;
	}
	
}
