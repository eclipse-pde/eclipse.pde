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

import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.ui.templates.TemplateOption;

public class IntroTemplate extends PDETemplateSection {

	private static final String KEY_PRODUCT_ID = "IntroTemplate.productID"; //$NON-NLS-1$

	private static final String KEY_PRODUCT_NAME = "productName"; //$NON-NLS-1$

	private static final String KEY_APPLICATION = "IntroTemplate.application"; //$NON-NLS-1$

	private static final String KEY_INTRO_ID = "IntroTemplate.introID"; //$NON-NLS-1$

	private static final String KEY_CONFIGURATION_ID = "IntroTemplate.configurationID"; //$NON-NLS-1$
    
    private static final String KEY_GENERATE_DYNAMIC_CONTENT = "IntroTemplate.generateDynamicContent"; //$NON-NLS-1$
    
    public static final String KEY_CLASS_NAME = "className"; //$NON-NLS-1$
    
    public static final String CLASS_NAME = "SampleXHTMLContentProvider"; //$NON-NLS-1$
    
    public static final String KEY_ANCHOR = "className";
    
    private BooleanOption generateDynamicContent = null;
    private StringOption classNameOption = null;
    private StringOption packageNameOption = null;

	// private static final String KEY_INTRO_TEXT = "IntroTemplate.text";

	public IntroTemplate() {
		super();
		setPageCount(1);
		createOptions();
        alterClassNameOptionStates();
	}

	private void createOptions() {

		// product options
		addOption(KEY_PRODUCT_ID, PDEUIMessages.IntroTemplate_productID,
				"product", 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption(KEY_PRODUCT_NAME, PDEUIMessages.IntroTemplate_productName,
				"My New Product", 0); //$NON-NLS-1$        
		addOption(KEY_APPLICATION, PDEUIMessages.IntroTemplate_application,
				"org.eclipse.ui.ide.workbench", 0); //$NON-NLS-1$ //$NON-NLS-2$

		// intro options
		addOption(KEY_INTRO_ID, PDEUIMessages.IntroTemplate_introID,
				"org.eclipse.ui.intro.introId", 0); //$NON-NLS-1$ //$NON-NLS-2$                     

		// configuration options
		addOption(KEY_CONFIGURATION_ID,
				PDEUIMessages.IntroTemplate_configurationID,
				"configId", 0); //$NON-NLS-1$

        generateDynamicContent = (BooleanOption)addOption( KEY_GENERATE_DYNAMIC_CONTENT,
                PDEUIMessages.IntroTemplate_generateDynamicContent,
                false,
                0);
        
        packageNameOption = (StringOption)addOption(
                KEY_PACKAGE_NAME,
                PDEUIMessages.IntroTemplate_packageName,
                (String)null,
                0);
        
        classNameOption = (StringOption)addOption(
                KEY_CLASS_NAME,
                PDEUIMessages.IntroTemplate_className,
                CLASS_NAME,
                0);
	}
    
    private void alterClassNameOptionStates() {
        classNameOption.setEnabled(generateDynamicContent.isSelected());
        packageNameOption.setEnabled(generateDynamicContent.isSelected());
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
        String id = data.getId();
        initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id) + ".intro"); 
    }
    public void initializeFields(IPluginModelBase model) {
        // In the new extension wizard, the model exists so 
        // we can initialize directly from it
        String pluginId = model.getPluginBase().getId();
        initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId) + ".intro"); 
    }

	public void validateOptions(TemplateOption source) {
        
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		} else {
			validateContainerPage(source);
		}        
        if ( source == generateDynamicContent) {
            alterClassNameOptionStates();
        }
	}

	private void validateContainerPage(TemplateOption source) {
		TemplateOption[] allPageOptions = getOptions(0);
		for (int i = 0; i < allPageOptions.length; i++) {
			TemplateOption nextOption = allPageOptions[i];
			if (nextOption.isRequired() && nextOption.isEmpty()) {
                if ( (nextOption != classNameOption) || 
                   ((nextOption == classNameOption) && (generateDynamicContent.isSelected())) ) {
                    flagMissingRequiredOption(nextOption);
				    return;
                }
			}
		}
		resetPageState();
	}

	protected void updateModel(IProgressMonitor monitor) throws CoreException {

		IPluginBase plugin = model.getPluginBase();
		IPluginModelFactory factory = model.getPluginFactory();

		// org.eclipse.core.runtime.products
		IPluginExtension extension1 = createExtension(
				"org.eclipse.core.runtime.products", true); //$NON-NLS-1$
		extension1.setId(getStringOption(KEY_PRODUCT_ID));
		extension1.setName("org.eclipse.core.runtime.products"); //$NON-NLS-1$

		IPluginElement productElement = factory.createElement(extension1);
		productElement.setName("product"); //$NON-NLS-1$
		productElement.setAttribute("name", getStringOption(KEY_PRODUCT_NAME)); //$NON-NLS-1$
		productElement.setAttribute(
				"application", getStringOption(KEY_APPLICATION)); //$NON-NLS-1$ //$NON-NLS-2$
		extension1.add(productElement);

		if (!extension1.isInTheModel())
			plugin.add(extension1);

		// org.eclipse.ui.intro
		IPluginExtension extension2 = createExtension(
				"org.eclipse.ui.intro", true); //$NON-NLS-1$

		IPluginElement introElement = factory.createElement(extension2);
		introElement.setName("intro"); //$NON-NLS-1$
		introElement.setAttribute("id", getStringOption(KEY_INTRO_ID)); //$NON-NLS-1$
		introElement.setAttribute("class", //$NON-NLS-1$
				"org.eclipse.ui.intro.config.CustomizableIntroPart"); //$NON-NLS-1$
		extension2.add(introElement);

		IPluginElement introProductBindingElement = factory
				.createElement(extension2);
		introProductBindingElement.setName("introProductBinding"); //$NON-NLS-1$
		introProductBindingElement.setAttribute("introId", //$NON-NLS-1$
				getStringOption(KEY_INTRO_ID));
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
		configurationElement.setAttribute("introId", //$NON-NLS-1$
				getStringOption(KEY_INTRO_ID));
		configurationElement.setAttribute("content", "introContent.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		IPluginElement presentationElement = factory
				.createElement(configurationElement);
		presentationElement.setName("presentation"); //$NON-NLS-1$
		presentationElement.setAttribute("home-page-id", "root"); //$NON-NLS-1$ //$NON-NLS-2$
		IPluginElement implementationElement = factory
				.createElement(presentationElement);
		implementationElement.setName("implementation"); //$NON-NLS-1$
		implementationElement.setAttribute("style", "empty");  //$NON-NLS-1$//$NON-NLS-2$
		implementationElement.setAttribute("kind", "html"); //$NON-NLS-1$ //$NON-NLS-2$
		presentationElement.add(implementationElement);
		configurationElement.add(presentationElement);
		extension3.add(configurationElement);

		if (!extension3.isInTheModel())
			plugin.add(extension3);
        
        // org.eclipse.ui.intro.configExtension
        if (generateDynamicContent.isSelected()) {
            IPluginExtension extension4 = createExtension(
                "org.eclipse.ui.intro.configExtension", true); //$NON-NLS-1$

            IPluginElement configExtensionElement = factory.createElement(extension4);
            configExtensionElement.setName("configExtension"); //$NON-NLS-1$
            configExtensionElement.setAttribute("configId", plugin.getId() + '.' + "configId"); //$NON-NLS-1$
            configExtensionElement.setAttribute("content", "ext.xml"); //$NON-NLS-1$
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

        if ( !generateDynamicContent.isSelected() && 
                (sourceFile.getName().equals("$className$.java") || 
                 sourceFile.getName().equals("concept3.xhtml") ||
                 sourceFile.getName().equals("extContent.xhtml") ||
                 sourceFile.getName().equals("ext.xml") ) ) {
            return false;
        }
        
        return true;
	}

	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.intro"; // need more then one extension point
	}

	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList result = new ArrayList();

		result.add(new PluginReference("org.eclipse.ui.intro", null, 0)); //$NON-NLS-1$
        
        if ( generateDynamicContent.isSelected()) {
            result.add(new PluginReference("org.eclipse.ui.forms", null, 0)); //$NON-NLS-1$
            result.add(new PluginReference("org.eclipse.swt", null, 0)); //$NON-NLS-1$
        }

		return (IPluginReference[]) result.toArray(new IPluginReference[result
				.size()]);
	}

	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#getFoldersToInclude()
	 */
	// public String[] getNewFiles() {
	// return new String[] {"content/", "*.xml"}; //$NON-NLS-1$ //$NON-NLS-2$
	// }
}
