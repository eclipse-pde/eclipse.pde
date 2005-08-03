/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.templates;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.core.plugin.PluginBase;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.TemplateOption;

public class IntroTemplate extends PDETemplateSection {
    
    private static final String DYNAMIC_SELECTED = "dynamic"; //$NON-NLS-1$
    
    private static final String STATIC_SELECTED = "static"; //$NON-NLS-1$

	private static final String KEY_PRODUCT_ID = "IntroTemplate.productID"; //$NON-NLS-1$

	private static final String KEY_PRODUCT_NAME = "productName"; //$NON-NLS-1$

	private static final String KEY_APPLICATION_ID = "IntroTemplate.application"; //$NON-NLS-1$

    private static final String KEY_GENERATE_DYNAMIC_CONTENT = "IntroTemplate.generateDynamicContent"; //$NON-NLS-1$
     
    public static final String CLASS_NAME = "SampleXHTMLContentProvider"; //$NON-NLS-1$
    
    public static final String KEY_PERSPECTIVE_NAME = "perspectiveName"; //$NON-NLS-1$
    
    private String packageName;
    private String introID;
    private String perspectiveName;
    private static final String APPLICATION_CLASS = "Application"; //$NON-NLS-1$

	public IntroTemplate() {
		super();
		setPageCount(1);
		createOptions();
	}

	private void createOptions() {

		// product options
		addOption(KEY_PRODUCT_ID, PDEUIMessages.IntroTemplate_productID,
				"product", 0); //$NON-NLS-1$ 
		addOption(KEY_PRODUCT_NAME, PDEUIMessages.IntroTemplate_productName,
				"My New Product", 0); //$NON-NLS-1$        
		addOption(KEY_APPLICATION_ID, PDEUIMessages.IntroTemplate_application,
				"application", 0); //$NON-NLS-1$ 

        addOption( KEY_GENERATE_DYNAMIC_CONTENT,
                PDEUIMessages.IntroTemplate_generate,
                new String[][] { {STATIC_SELECTED, PDEUIMessages.IntroTemplate_generateStaticContent}, 
                                 {DYNAMIC_SELECTED, PDEUIMessages.IntroTemplate_generateDynamicContent}}, 
                STATIC_SELECTED, 0); 
	}
    
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_INTRO);
		page.setTitle(PDEUIMessages.IntroTemplate_title);
		page.setDescription(PDEUIMessages.IntroTemplate_desc);
		wizard.addPage(page);
		markPagesAdded();
	}

	public boolean isDependentOnParentWizard() {
		return true;
	}

	public String getSectionId() {
		return "intro"; //$NON-NLS-1$
	}
    
    protected void initializeFields(IFieldData data) {
        // In a new project wizard, we don't know this yet - the
        // model has not been created
        String pluginId = data.getId();
        initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId) + ".intro");  //$NON-NLS-1$
        initializeOption(KEY_APPLICATION_ID, getFormattedPackageName(pluginId) + ".application");  //$NON-NLS-1$
        initializeOption(KEY_PERSPECTIVE_NAME, getFormattedPackageName(pluginId) + " Perspective");  //$NON-NLS-1$
        perspectiveName = getFormattedPackageName(pluginId) + ".perspective"; //$NON-NLS-1$
        packageName =  getFormattedPackageName(pluginId) + ".intro"; //$NON-NLS-1$
        introID = getFormattedPackageName(pluginId) + ".introID"; //$NON-NLS-1$
    }
    public void initializeFields(IPluginModelBase model) {
        // In the new extension wizard, the model exists so 
        // we can initialize directly from it
        String pluginId = model.getPluginBase().getId();
        initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId) + ".intro");  //$NON-NLS-1$
        initializeOption(KEY_APPLICATION_ID, getFormattedPackageName(pluginId) + ".application");  //$NON-NLS-1$
        initializeOption(KEY_PERSPECTIVE_NAME, getFormattedPackageName(pluginId) + " Perspective");  //$NON-NLS-1$
        perspectiveName = getFormattedPackageName(pluginId) + ".perspective"; //$NON-NLS-1$
        packageName =  getFormattedPackageName(pluginId) + ".intro"; //$NON-NLS-1$
        introID = getFormattedPackageName(pluginId) + ".introID"; //$NON-NLS-1$
    }

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

	protected void updateModel(IProgressMonitor monitor) throws CoreException {

		IPluginBase plugin = model.getPluginBase();
		IPluginModelFactory factory = model.getPluginFactory();
        		
		// org.eclipse.core.runtime.applications
		IPluginExtension extension = createExtension("org.eclipse.core.runtime.applications", true); //$NON-NLS-1$
		extension.setId(getStringOption(KEY_APPLICATION_ID));
		
		IPluginElement element = model.getPluginFactory().createElement(extension);
		element.setName("application"); //$NON-NLS-1$
		extension.add(element);
		
		IPluginElement run = model.getPluginFactory().createElement(element);
		run.setName("run"); //$NON-NLS-1$
		run.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + "." + APPLICATION_CLASS); //$NON-NLS-1$ //$NON-NLS-2$
		element.add(run);
		
		if (!extension.isInTheModel())
			plugin.add(extension);
		
		
		// org.eclipse.ui.perspectives
		IPluginExtension perspectivesExtension = createExtension("org.eclipse.ui.perspectives", true); //$NON-NLS-1$
		IPluginElement perspectiveElement = model.getPluginFactory().createElement(perspectivesExtension);
		perspectiveElement.setName("perspective"); //$NON-NLS-1$
		perspectiveElement.setAttribute("class", getStringOption(KEY_PACKAGE_NAME) + ".Perspective"); //$NON-NLS-1$ //$NON-NLS-2$
		perspectiveElement.setAttribute("name", getStringOption(KEY_PERSPECTIVE_NAME)); //$NON-NLS-1$
		perspectiveElement.setAttribute("id", plugin.getId() + ".perspective"); //$NON-NLS-1$ //$NON-NLS-2$
		perspectivesExtension.add(perspectiveElement);
		
		if (!perspectivesExtension.isInTheModel())
			plugin.add(perspectivesExtension);


		// org.eclipse.core.runtime.products
		IPluginExtension extension1 = createExtension(
				"org.eclipse.core.runtime.products", true); //$NON-NLS-1$
		extension1.setId(getStringOption(KEY_PRODUCT_ID));
		//extension1.setName("org.eclipse.core.runtime.products"); //$NON-NLS-1$

		IPluginElement productElement = factory.createElement(extension1);
		productElement.setName("product"); //$NON-NLS-1$
		productElement.setAttribute("name", getStringOption(KEY_PRODUCT_NAME)); //$NON-NLS-1$
		productElement.setAttribute(
				"application", plugin.getId() + "." + getStringOption(KEY_APPLICATION_ID)); //$NON-NLS-1$ //$NON-NLS-2$
		extension1.add(productElement);

		if (!extension1.isInTheModel())
			plugin.add(extension1);

		// org.eclipse.ui.intro
		IPluginExtension extension2 = createExtension(
				"org.eclipse.ui.intro", true); //$NON-NLS-1$

		IPluginElement introElement = factory.createElement(extension2);
		introElement.setName("intro"); //$NON-NLS-1$
		introElement.setAttribute("id", introID); //$NON-NLS-1$
		introElement.setAttribute("class", //$NON-NLS-1$
				"org.eclipse.ui.intro.config.CustomizableIntroPart"); //$NON-NLS-1$
		extension2.add(introElement);

		IPluginElement introProductBindingElement = factory
				.createElement(extension2);
		introProductBindingElement.setName("introProductBinding"); //$NON-NLS-1$
		introProductBindingElement.setAttribute("introId", introID);//$NON-NLS-1$
                
		introProductBindingElement.setAttribute("productId", plugin.getId() //$NON-NLS-1$
				+ '.' + getStringOption(KEY_PRODUCT_ID));
		extension2.add(introProductBindingElement);

		if (!extension2.isInTheModel())
			plugin.add(extension2);

		// org.eclipse.ui.intro.config
		IPluginExtension extension3 = createExtension(
				"org.eclipse.ui.intro.config", true); //$NON-NLS-1$

		IPluginElement configurationElement = factory.createElement(extension3);
		configurationElement.setName("config"); //$NON-NLS-1$
		configurationElement.setAttribute("id", plugin.getId() + '.' //$NON-NLS-1$
				+ "configId"); //$NON-NLS-1$
		configurationElement.setAttribute("introId", introID);//$NON-NLS-1$            
		configurationElement.setAttribute("content", "introContent.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		IPluginElement presentationElement = factory
				.createElement(configurationElement);
		presentationElement.setName("presentation"); //$NON-NLS-1$
		presentationElement.setAttribute("home-page-id", "root"); //$NON-NLS-1$ //$NON-NLS-2$
		IPluginElement implementationElement = factory
				.createElement(presentationElement);
		implementationElement.setName("implementation"); //$NON-NLS-1$
        implementationElement.setAttribute("os", "win32,linux,macosx"); //$NON-NLS-1$ //$NON-NLS-2$
        if ( ((PluginBase)plugin).getTargetVersion().equals("3.0")) {   //$NON-NLS-1$
            implementationElement.setAttribute("style", "content/shared.css");  //$NON-NLS-1$//$NON-NLS-2$
        }
		implementationElement.setAttribute("kind", "html"); //$NON-NLS-1$ //$NON-NLS-2$
		presentationElement.add(implementationElement);
		configurationElement.add(presentationElement);
		extension3.add(configurationElement);

		if (!extension3.isInTheModel())
			plugin.add(extension3);
        
        // org.eclipse.ui.intro.configExtension
        if (getValue(KEY_GENERATE_DYNAMIC_CONTENT).toString().equals(DYNAMIC_SELECTED)) {
            IPluginExtension extension4 = createExtension(
                "org.eclipse.ui.intro.configExtension", true); //$NON-NLS-1$

            IPluginElement configExtensionElement = factory.createElement(extension4);
            configExtensionElement.setName("configExtension"); //$NON-NLS-1$
            configExtensionElement.setAttribute("configId", plugin.getId() + '.' + "configId"); //$NON-NLS-1$ //$NON-NLS-2$
            configExtensionElement.setAttribute("content", "ext.xml"); //$NON-NLS-1$ //$NON-NLS-2$
            extension4.add(configExtensionElement);

            if (!extension4.isInTheModel())
                plugin.add(extension4);
        }


	}

	protected boolean isOkToCreateFolder(File sourceFolder) {
		return true;
	}

	/**
	 * @see AbstractTemplateSection#isOkToCreateFile(File)
	 */
	protected boolean isOkToCreateFile(File sourceFile) {

        if ( getValue(KEY_GENERATE_DYNAMIC_CONTENT).toString().equals(STATIC_SELECTED) && 
                (sourceFile.getName().equals("DynamicContentProvider.java") ||  //$NON-NLS-1$
                 sourceFile.getName().equals("concept3.xhtml") || //$NON-NLS-1$
                 sourceFile.getName().equals("extContent.xhtml") || //$NON-NLS-1$
                 sourceFile.getName().equals("ext.xml") ) ) { //$NON-NLS-1$
            return false;
        }
        
        return true;
	}

	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.intro"; // need more then one extension point //$NON-NLS-1$
	}

	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList result = new ArrayList();

		result.add(new PluginReference("org.eclipse.ui.intro", null, 0)); //$NON-NLS-1$
        result.add(new PluginReference("org.eclipse.core.runtime", null, 0)); //$NON-NLS-1$
        result.add(new PluginReference("org.eclipse.ui", null, 0)); //$NON-NLS-1$
        
        if ( getValue(KEY_GENERATE_DYNAMIC_CONTENT).toString().equals(DYNAMIC_SELECTED)) {
            result.add(new PluginReference("org.eclipse.ui.forms", null, 0)); //$NON-NLS-1$
            result.add(new PluginReference("org.eclipse.swt", null, 0)); //$NON-NLS-1$
        }

		return (IPluginReference[]) result.toArray(new IPluginReference[result
				.size()]);
	}

	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}
    
    public Object getValue(String valueName) {
        
        if (valueName.equals(KEY_PACKAGE_NAME)) {
            return packageName;
        }
        
        return super.getValue(valueName);
    }
    
    public String getStringOption(String name) {
        
        if (name.equals(KEY_PERSPECTIVE_NAME)) {
            return perspectiveName;
        } else if ( name.equals(KEY_PACKAGE_NAME)) {
            return packageName;
        }
        
        return super.getStringOption(name);
    }

    public String[] getNewFiles() {
        
       if ( getValue(KEY_GENERATE_DYNAMIC_CONTENT).toString().equals(STATIC_SELECTED)) {
           return new String[] {"content/", "splash.bmp", "introContent.xml"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
       } 
           
       return new String[] {"content/", "splash.bmp", "introContent.xml", "ext.xml"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
       
    }
}
