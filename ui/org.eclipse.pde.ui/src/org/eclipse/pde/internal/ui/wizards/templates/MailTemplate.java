package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;


public class MailTemplate extends PDETemplateSection {
	
	public static final String KEY_PRODUCT_NAME = "productName"; //$NON-NLS-1$
	public static final String KEY_PRODUCT_ID = "productID"; //$NON-NLS-1$
	public static final String KEY_CLOSEABLE = "closeable"; //$NON-NLS-1$
	public static final String KEY_NON_CLOSEABLE = "noncloseable"; //$NON-NLS-1$
	public static final String KEY_PERSPECTIVE_NAME = "perspectiveName"; //$NON-NLS-1$
	public static final String KEY_WORKBENCH_ADVISOR = "advisor"; //$NON-NLS-1$
	public static final String KEY_APPLICATION_CLASS = "applicationClass"; //$NON-NLS-1$
	
	public MailTemplate() {
		setPageCount(1);
		createOptions();
	}
	
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_RCP_MAIL);
		page.setTitle(PDEPlugin.getResourceString("MailTemplate.title")); //$NON-NLS-1$
		page.setDescription(PDEPlugin.getResourceString("MailTemplate.desc")); //$NON-NLS-1$
		wizard.addPage(page);
		markPagesAdded();
	}

	
	private void createOptions() {	
		addOption(KEY_PRODUCT_NAME, PDEPlugin.getResourceString("MailTemplate.productName"), "RCP Product", 0); //$NON-NLS-1$ //$NON-NLS-2$
		
		addOption(KEY_PRODUCT_ID, PDEPlugin.getResourceString("MailTemplate.productID"), "product", 0); //$NON-NLS-1$ //$NON-NLS-2$
		
		addOption(KEY_PERSPECTIVE_NAME, PDEPlugin.getResourceString("MailTemplate.perspectiveName"), "Sample Perspective", 0); //$NON-NLS-1$ //$NON-NLS-2$
		
		addOption(KEY_PACKAGE_NAME, PDEPlugin.getResourceString("MailTemplate.packageName"), (String) null, 0); //$NON-NLS-1$
		
		addOption(KEY_CLOSEABLE, PDEPlugin.getResourceString("MailTemplate.closeable") , "SampleView", 0); //$NON-NLS-1$ //$NON-NLS-2$
		
		addOption(KEY_NON_CLOSEABLE, PDEPlugin.getResourceString("MailTemplate.non-closeable"), "NoncloseableView", 0); //$NON-NLS-1$ //$NON-NLS-2$
		
		addOption(KEY_WORKBENCH_ADVISOR, PDEPlugin.getResourceString("MailTemplate.advisor"), "SampleWorkbenchAdvisor", 0); //$NON-NLS-1$ //$NON-NLS-2$
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.BaseOptionTemplateSection#getReplacementString(java.lang.String, java.lang.String)
	 */
	public String getReplacementString(String fileName, String key) {
		if (key.equals(KEY_APPLICATION_CLASS)) {
			IPluginElement element = getAppRunElement();
			String name = element.getAttribute("class").getValue(); //$NON-NLS-1$
			int dot = name.lastIndexOf('.');
			if (dot != -1)
				return name.substring(dot + 1);
		}
		return super.getReplacementString(fileName, key);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.ui.templates.OptionTemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "mail"; //$NON-NLS-1$
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
		updateApplicationClass();
		createPerspectiveExtension();
		createViewExtension();
		createCommandExtension();
		createProductExtension();
	}
	
	private void updateApplicationClass() throws CoreException {
		IPluginElement element = getAppRunElement();
		String name = element.getAttribute("class").getValue(); //$NON-NLS-1$
		int dot = name.lastIndexOf('.');
		if (dot != -1)
			name = getStringOption(KEY_PACKAGE_NAME) + name.substring(dot);
		else
			name = getStringOption(KEY_PACKAGE_NAME) + "." + name; //$NON-NLS-1$
		element.setAttribute("class", name); //$NON-NLS-1$
	}
	
	private IPluginElement getAppRunElement() {
		IPluginExtension ext = getAppExtension();
		IPluginElement app = (IPluginElement)ext.getChildren()[0];
		return (IPluginElement)app.getChildren()[0];
	}
	
	private IPluginExtension getAppExtension() {
		IPluginBase plugin = model.getPluginBase();	
		IPluginExtension[] extensions = plugin.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IPluginExtension ext = extensions[i];
			if ("org.eclipse.core.runtime.applications".equals(ext.getPoint())) { //$NON-NLS-1$
				return ext;
			}
		}
		return null;
	}

	private void createPerspectiveExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		
		IPluginExtension extension = createExtension("org.eclipse.ui.perspectives", true); //$NON-NLS-1$
		IPluginElement element = model.getPluginFactory().createElement(extension);
		element.setName("perspective"); //$NON-NLS-1$
		element.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + ".Perspective"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("name", getStringOption(KEY_PERSPECTIVE_NAME)); //$NON-NLS-1$
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
		view.setAttribute("allowMultiple", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("icon", "icons/sample2.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_CLOSEABLE)); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("name", "Message"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("id", id + "." + getStringOption(KEY_CLOSEABLE)); //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(view);
		
		view = model.getPluginFactory().createElement(extension);
		view.setName("view"); //$NON-NLS-1$
		view.setAttribute("allowMultiple", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("icon", "icons/sample3.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_NON_CLOSEABLE)); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("name", "Mailboxes"); //$NON-NLS-1$ //$NON-NLS-2$
		view.setAttribute("id", id + "." + getStringOption(KEY_NON_CLOSEABLE)); //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(view);
		
		if (!extension.isInTheModel())
			plugin.add(extension);
	}
	
	private void createCommandExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();	
		String id = plugin.getId();
		IPluginExtension extension = createExtension("org.eclipse.ui.commands", true); //$NON-NLS-1$

		IPluginElement element = model.getPluginFactory().createElement(extension);
		element.setName("command"); //$NON-NLS-1$
		element.setAttribute("description", "Opens a mailbox"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("name", "Open Mailbox"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("id", id + ".open"); //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(element);
		
		element = model.getPluginFactory().createElement(extension);
		element.setName("command"); //$NON-NLS-1$
		element.setAttribute("description", "Open a message dialog"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("name", "Open Message Dialog"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("id", id + ".openMessage");	 //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(element);
		
		element = model.getPluginFactory().createElement(extension);
		element.setName("keyConfiguration"); //$NON-NLS-1$
		element.setAttribute("description", "The key configuration for this sample"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("name", id + ".keyConfiguration"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("id", id + ".keyConfiguration");	 //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(element);
		
		element = model.getPluginFactory().createElement(extension);
		element.setName("keyBinding"); //$NON-NLS-1$
		element.setAttribute("commandId", id + ".open"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("keySequence", "CTRL+2"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("keyConfigurationId", "org.eclipse.ui.defaultAcceleratorConfiguration");	 //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(element);
		
		element = model.getPluginFactory().createElement(extension);
		element.setName("keyBinding"); //$NON-NLS-1$
		element.setAttribute("commandId", id + ".openMessage"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("keySequence", "CTRL+3"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("keyConfigurationId", "org.eclipse.ui.defaultAcceleratorConfiguration");	 //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(element);
		
		element = model.getPluginFactory().createElement(extension);
		element.setName("keyBinding"); //$NON-NLS-1$
		element.setAttribute("commandId", "org.eclipse.ui.file.exit"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("keySequence", "CTRL+X"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("keyConfigurationId", "org.eclipse.ui.defaultAcceleratorConfiguration");	 //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(element);
		
		if (!extension.isInTheModel())
			plugin.add(extension);
	}
	
	private void createProductExtension() throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.core.runtime.products", true); //$NON-NLS-1$
		extension.setId(getStringOption(KEY_PRODUCT_ID));
		
		IPluginElement element = model.getFactory().createElement(extension);
		element.setName("product"); //$NON-NLS-1$
		element.setAttribute("name", getStringOption(KEY_PRODUCT_NAME)); //$NON-NLS-1$
		element.setAttribute("application", plugin.getId() + "." + getAppExtension().getId()); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement property = model.getFactory().createElement(element);
		property.setName("property"); //$NON-NLS-1$
		property.setAttribute("name", "aboutText"); //$NON-NLS-1$ //$NON-NLS-2$
		property.setAttribute("value", "%aboutText"); //$NON-NLS-1$ //$NON-NLS-2$
		element.add(property);
		
		property = model.getFactory().createElement(element);
		property.setName("property"); //$NON-NLS-1$
		property.setAttribute("name", "windowImage"); //$NON-NLS-1$ //$NON-NLS-2$
		property.setAttribute("value", "icons/sample2.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		element.add(property);

		property = model.getFactory().createElement(element);
		property.setName("property"); //$NON-NLS-1$
		property.setAttribute("name", "aboutImage"); //$NON-NLS-1$ //$NON-NLS-2$
		property.setAttribute("value", "product_lg.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		element.add(property);
		
		extension.add(element);
		
		if (!extension.isInTheModel()) {
			plugin.add(extension);
		}

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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#getNewFiles()
	 */
	public String[] getNewFiles() {
		return new String[] {"icons/", "plugin.properties", "product_lg.gif", "splash.bmp"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

}
