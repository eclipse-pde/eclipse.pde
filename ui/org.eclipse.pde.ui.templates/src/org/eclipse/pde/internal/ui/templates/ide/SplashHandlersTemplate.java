/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars.Vogel <Lars.Vogel@vogella.com> - Bug 486247, 486261
 *******************************************************************************/

package org.eclipse.pde.internal.ui.templates.ide;

import java.io.File;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.internal.ui.wizards.product.ISplashHandlerConstants;
import org.eclipse.pde.internal.ui.wizards.product.UpdateSplashHandlerAction;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.*;

/**
 * SplashHandlersTemplate
 *
 */
public class SplashHandlersTemplate extends PDETemplateSection {

	private final static int F_PAGE_INDEX = 0;

	private final static String F_DEFAULT_PRODUCT = "org.eclipse.sdk.ide"; //$NON-NLS-1$

	private final static String F_FIELD_TEMPLATE = "fieldTemplate"; //$NON-NLS-1$

	private final static String F_FIELD_PRODUCTS = "fieldProducts"; //$NON-NLS-1$

	private final static String F_FIELD_CLASS = "fieldClass"; //$NON-NLS-1$

	private final static String F_FIELD_SPLASH = "fieldSplash"; //$NON-NLS-1$

	private final static String F_SPLASH_SCREEN_FILE = "splash.bmp"; //$NON-NLS-1$

	private WizardPage fPage;

	private TemplateOption fFieldTemplate;

	private ComboChoiceOption fFieldProducts;

	private TemplateOption fFieldPackage;

	private StringOption fFieldClass;

	private TemplateOption fFieldSplash;

	/**
	 *
	 */
	public SplashHandlersTemplate() {
		initialize();
	}

	/**
	 *
	 */
	private void initialize() {
		// Default field values
		fFieldTemplate = null;
		fFieldProducts = null;
		fFieldPackage = null;
		fFieldClass = null;
		fFieldSplash = null;
		// One wizard page
		setPageCount(1);
		// GUI
		createUI();
	}

	@Override
	public void addPages(Wizard wizard) {
		// Create the page
		fPage = createPage(0, IHelpContextIds.TEMPLATE_SPLASH_HANDLERS);
		fPage.setTitle(PDETemplateMessages.SplashHandlersTemplate_titleSplashHandlerOptions);
		fPage.setDescription(PDETemplateMessages.SplashHandlersTemplate_descSplashHandlerOptions);
		// Add the page
		wizard.addPage(fPage);
		// Mark as added
		markPagesAdded();
	}

	@Override
	protected String getFormattedPackageName(String id) {
		// Package name addition to create a location for containing
		// any classes required by the splash handlers.
		String packageName = super.getFormattedPackageName(id);
		// Unqualifed
		if (packageName.length() == 0) {
			return ISplashHandlerConstants.F_UNQUALIFIED_EXTENSION_ID;
		}
		// Qualified
		return packageName + '.' + ISplashHandlerConstants.F_UNQUALIFIED_EXTENSION_ID;
	}

	@Override
	public String[] getNewFiles() {
		// Note:  This does not even get called for non-project templates
		// As a result, listed files are not added to the binary build
		// section
		if (isSplashFieldSelected()) {
			return new String[] {F_SPLASH_SCREEN_FILE};
		}
		// TODO: MP: SPLASH: Investigate if this is necessary, does not get called for non-project templates
		return super.getNewFiles();
	}

	private boolean isSplashFieldSelected() {
		if ((Boolean) fFieldSplash.getValue() == Boolean.TRUE) {
			return true;
		}
		return false;
	}

	@Override
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	@Override
	protected void initializeFields(IFieldData data) {
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id));
	}

	@Override
	public void initializeFields(IPluginModelBase model) {
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId));
	}

	@Override
	public boolean isDependentOnParentWizard() {
		return true;
	}

	@Override
	public void validateOptions(TemplateOption source) {
		// Update class name
		if (source == fFieldTemplate) {
			updateUIFieldClass();
		}
		super.validateOptions(source);
	}

	/**
	 *
	 */
	private void updateUIFieldClass() {
		// Update the class name depending on the splash screen type
		for (int i = 0; i < ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES.length; i++) {
			String choice = ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES[i][0];
			if (fFieldTemplate.getValue().equals(choice)) {
				fFieldClass.setValue(ISplashHandlerConstants.F_SPLASH_SCREEN_CLASSES[i]);
				break;
			}
		}
	}

	/**
	 *
	 */
	private void createUI() {
		// Field:  template
		createUIFieldTemplate();
		// Field:  product ID
		createUIFieldProductID();
		// Field:  package
		createUIFieldPackage();
		// Field:  class
		createUIFieldClass();
		// Field:  splash
		createUIFieldSplash();
	}

	/**
	 *
	 */
	private void createUIFieldSplash() {
		fFieldSplash = addOption(F_FIELD_SPLASH, PDETemplateMessages.SplashHandlersTemplate_fieldAddSplash, false, F_PAGE_INDEX);
	}

	/**
	 *
	 */
	private void createUIFieldClass() {
		fFieldClass = (StringOption) addOption(F_FIELD_CLASS, PDETemplateMessages.SplashHandlersTemplate_fieldClassName, ISplashHandlerConstants.F_SPLASH_SCREEN_CLASSES[0], F_PAGE_INDEX);
		fFieldClass.setReadOnly(true);
	}

	/**
	 *
	 */
	private void createUIFieldPackage() {
		fFieldPackage = addOption(KEY_PACKAGE_NAME, PDETemplateMessages.SplashHandlersTemplate_fieldJavaPackage, null, F_PAGE_INDEX);
	}

	/**
	 *
	 */
	private void createUIFieldTemplate() {
		fFieldTemplate = addOption(F_FIELD_TEMPLATE, PDETemplateMessages.SplashHandlersTemplate_fieldSplashScreenType, ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES, ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES[0][0], F_PAGE_INDEX);
	}

	/**
	 *
	 */
	private void createUIFieldProductID() {

		String[] products = TargetPlatform.getProducts();
		String[][] choices = new String[products.length][2];
		String initialChoice = null;
		boolean foundInitialChoice = false;
		// Populate choices with products
		for (int i = 0; i < products.length; i++) {
			// ID
			choices[i][0] = products[i];
			// Name
			choices[i][1] = products[i];
			// Determine whether default product is present
			if ((foundInitialChoice == false) && (products[i].equals(F_DEFAULT_PRODUCT))) {
				foundInitialChoice = true;
			}
		}
		// Use default product as the initial product choice if found;
		// otherwise, use the first item found
		if (foundInitialChoice) {
			initialChoice = F_DEFAULT_PRODUCT;
		} else {
			initialChoice = choices[0][0];
		}
		// Create the field
		fFieldProducts = addComboChoiceOption(F_FIELD_PRODUCTS, PDETemplateMessages.SplashHandlersTemplate_fieldProductID, choices, initialChoice, F_PAGE_INDEX);
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		// add the dependencies for the splash handler extension template
		IPluginReference[] dependencies = new IPluginReference[4];
		dependencies[0] = new PluginReference("org.eclipse.core.runtime"); //$NON-NLS-1$
		dependencies[1] = new PluginReference("org.eclipse.swt"); //$NON-NLS-1$
		dependencies[2] = new PluginReference("org.eclipse.jface"); //$NON-NLS-1$
		dependencies[3] = new PluginReference("org.eclipse.ui.workbench"); //$NON-NLS-1$

		return dependencies;
	}

	@Override
	public String getSectionId() {
		return ISplashHandlerConstants.F_UNQUALIFIED_EXTENSION_ID;
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		// Create the action to update the model with the associated
		// splash handler extensions, extension points, elements and attributes
		UpdateSplashHandlerAction action = new UpdateSplashHandlerAction();
		// Configure the acation
		String id = createAttributeValueID();
		action.setFieldID(id);
		action.setFieldClass(createAttributeValueClass());
		action.setFieldSplashID(id);
		action.setFieldProductID((String) fFieldProducts.getValue());
		action.setFieldTemplate((String) fFieldTemplate.getValue());
		action.setFieldPluginID(model.getPluginBase().getId());
		action.setModel(model);
		action.setMonitor(monitor);
		// Execute the action
		action.run();
		// If an exception was caught, release it
		action.hasException();
	}

	private String createAttributeValueID() {
		// Create the ID based on the splash screen type
		return fFieldPackage.getValue() + "." + //$NON-NLS-1$
				fFieldTemplate.getValue();
	}

	private String createAttributeValueClass() {
		// Create the class based on the splash screen type
		return fFieldPackage.getValue() + "." + //$NON-NLS-1$
				fFieldClass.getValue();
	}

	@Override
	public String getUsedExtensionPoint() {
		return ISplashHandlerConstants.F_SPLASH_HANDLERS_EXTENSION;
	}

	@Override
	protected boolean isOkToCreateFile(File sourceFile) {
		// TODO: MP: SPLASH:  Sync this with org.eclipse.pde.internal.ui.util.TemplateFileGenerator
		String javaSuffix = ".java"; //$NON-NLS-1$
		String targetFile = fFieldClass.getValue() + javaSuffix;
		String copyFile = sourceFile.toString();

		if (copyFile.endsWith(javaSuffix) && (copyFile.endsWith(targetFile) == false)) {
			return false;
		}

		return true;
	}

	@Override
	protected boolean copyBrandingDirectory() {
		return isSplashFieldSelected();
	}

	@Override
	protected boolean isOkToCreateFolder(File sourceFolder) {
		// TODO: MP: SPLASH:  Sync this with org.eclipse.pde.internal.ui.util.TemplateFileGenerator
		boolean extensibleTemplateSelected = UpdateSplashHandlerAction.isExtensibleTemplateSelected((String) fFieldTemplate.getValue());
		String sourceFolderString = sourceFolder.toString();

		if ((extensibleTemplateSelected == false) && sourceFolderString.endsWith("icons")) { //$NON-NLS-1$
			return false;
		} else if ((extensibleTemplateSelected == false) && sourceFolderString.endsWith("schema")) { //$NON-NLS-1$
			return false;
		}

		return true;
	}

	@Override
	public String getLabel() {
		return getPluginResourceString("wizard.name.splash.handler"); //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return getPluginResourceString("wizard.description.splash.handler"); //$NON-NLS-1$
	}

}
