/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.wizards.cheatsheet;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.build.BuildObject;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.text.bundle.BundleSymbolicNameHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.Constants;

/**
 * RegisterCSOperation
 */
public class RegisterCSOperation extends WorkspaceModifyOperation {

	public final static String F_CS_EXTENSION_POINT_ID = "org.eclipse.ui.cheatsheets.cheatSheetContent"; //$NON-NLS-1$
	public static final String F_CS_EXTENSION_ID = "org.eclipse.ui.cheatsheets"; //$NON-NLS-1$
	public static final String F_CS_ATTRIBUTE_CONTENT_FILE = "contentFile"; //$NON-NLS-1$
	public static final String F_CS_ATTRIBUTE_COMPOSITE = "composite"; //$NON-NLS-1$

	private IRegisterCSData fRegisterCSData;

	private Shell fShell;

	/**
	 * 
	 */
	public RegisterCSOperation(IRegisterCSData registerCSData, Shell shell) {
		fRegisterCSData = registerCSData;
		fShell = shell;
	}

	/**
	 * @param rule
	 */
	public RegisterCSOperation(ISchedulingRule rule) {
		super(rule);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {

		try {
			boolean fragment = PluginRegistry.findModel(fRegisterCSData.getPluginProject()).isFragmentModel();
			IFile file = fRegisterCSData.getPluginProject().getFile(fragment ? ICoreConstants.FRAGMENT_PATH : ICoreConstants.PLUGIN_PATH);
			// If the plug-in exists modify it accordingly; otherwise, create
			// a new plug-in file
			if (file.exists()) {
				modifyExistingPluginFile(file, monitor);
			} else {
				createNewPluginFile(file, monitor);
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	/**
	 * FindCSExtensionResult
	 *
	 */
	private static class FindCSExtensionResult {

		public IPluginExtension fCSExtension;

		public IPluginElement fCSElement;

		/**
		 * 
		 */
		public FindCSExtensionResult() {
			fCSExtension = null;
			fCSElement = null;
		}

		/**
		 * @return
		 */
		public boolean foundCSExtension() {
			return (fCSExtension != null);
		}

		/**
		 * @return
		 */
		public boolean foundExactCSElement() {
			return (fCSElement != null);
		}
	}

	/**
	 * @param file
	 * @param monitor
	 * @throws CoreException
	 */
	private void modifyExistingPluginFile(IFile file, IProgressMonitor monitor) throws CoreException {

		// Validate the operation
		// Note: This is not accurate, we are validating the plugin.xml file 
		// but not the manifest.mf file
		IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {file}, fShell);
		if (status.getSeverity() != IStatus.OK) {
			throw new CoreException(new Status(IStatus.ERROR, PDEUserAssistanceUIPlugin.PLUGIN_ID, IStatus.ERROR, CSWizardMessages.RegisterCSOperation_errorMessage, null));
		}
		// Perform the modification of the plugin manifest file
		ModelModification mod = new ModelModification(fRegisterCSData.getPluginProject()) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				doModifyPluginModel(model, monitor);
				doModifyManifestModel(model);
			}
		};
		PDEModelUtility.modifyModel(mod, monitor);
	}

	/**
	 * @param model
	 * @param monitor
	 * @throws CoreException
	 */
	private void doModifyPluginModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
		if ((model instanceof IPluginModelBase) == false) {
			return;
		}
		IPluginModelBase modelBase = (IPluginModelBase) model;
		// Find an existing cheat sheet extension 
		FindCSExtensionResult result = findCSExtensionResult(modelBase);
		// Check search results and act accordingly
		if (result.foundCSExtension() && result.foundExactCSElement()) {
			// An exact match to an existing cheat sheet element was
			// found.  Update the elements description and category
			// fields
			modifyExistingElement(result.fCSElement, monitor);
			// Create the category if necessary
			// Category element
			IPluginElement categoryElement = createElementCategory(result.fCSExtension);
			if (categoryElement != null) {
				result.fCSExtension.add(categoryElement);
			}
		} else if (result.foundCSExtension()) {
			// No exact match to an existing cheat sheet element found within
			// the existing cheat sheet extension.  Update the 
			// existing extension by adding a new cheat sheet element
			// to it
			modifyExistingExtension(result.fCSExtension, monitor);
		} else {
			// No existing cheat sheet extension found, create a new
			// extension
			insertNewExtension(modelBase, monitor);
		}
	}

	/**
	 * @param modelBase
	 */
	private void insertNewExtension(IPluginModelBase modelBase, IProgressMonitor monitor) throws CoreException {
		// Update progress work units
		monitor.beginTask(CSWizardMessages.RegisterCSOperation_task, 1);
		// Create the new extension
		IPluginExtension extension = createExtensionCheatSheet(modelBase);
		modelBase.getPluginBase().add(extension);
		// Update progress work units
		monitor.done();
	}

	/**
	 * @param extension
	 */
	private void modifyExistingExtension(IPluginExtension extension, IProgressMonitor monitor) throws CoreException {
		// Update progress work units
		monitor.beginTask(CSWizardMessages.RegisterCSOperation_task2, 1);
		// Create new children for existing extension
		createExtensionChildren(extension);
		// Update progress work units
		monitor.done();
	}

	/**
	 * @param csElement
	 * @param monitor
	 */
	private void modifyExistingElement(IPluginElement csElement, IProgressMonitor monitor) throws CoreException {
		// Update progress work units
		monitor.beginTask(CSWizardMessages.RegisterCSOperation_task3, 1);
		// Leave id attribute the same
		// Update the name
		// Attribute: name
		csElement.setAttribute(ICompCSConstants.ATTRIBUTE_NAME, fRegisterCSData.getDataCheatSheetName());
		// Attribute: category
		// Update the category.
		// if "<none>" was selected, clear the entry
		String categoryID = fRegisterCSData.getDataCategoryID();
		if (categoryID == null) {
			categoryID = ""; //$NON-NLS-1$
		}
		csElement.setAttribute(RegisterCSWizardPage.F_CS_ELEMENT_CATEGORY, categoryID);
		// Leave contentFile attribute the same
		// Leave composite attribute the same
		// Element: description
		// Update an existing description if one is found; otherwise, 
		// Create a new description
		IPluginElement descriptionElement = findExistingDescription(csElement);
		if (descriptionElement == null) {
			// Create a new description element
			descriptionElement = createElementDescription(csElement);
			if (descriptionElement != null) {
				csElement.add(descriptionElement);
			}
		} else {
			// Modify the existing description element
			boolean modified = modifyExistingDescription(descriptionElement);
			if (modified == false) {
				// New description is not defined, remove the existing 
				// description element
				csElement.remove(descriptionElement);
			}
		}
		// Update progress work units
		monitor.done();
	}

	/**
	 * @param descriptionElement
	 */
	private boolean modifyExistingDescription(IPluginElement element) throws CoreException {
		// If the new description is defined set it on the existing description
		// element; otherwise, delete the existing description element
		if (PDETextHelper.isDefinedAfterTrim(fRegisterCSData.getDataDescription())) {
			element.setText(fRegisterCSData.getDataDescription().trim());
			return true;
		}
		return false;
	}

	/**
	 * @param csElement
	 * @throws CoreException
	 */
	private IPluginElement findExistingDescription(IPluginElement csElement) throws CoreException {

		if (csElement.getChildCount() > 0) {
			IPluginObject pluginObject = csElement.getChildren()[0];
			if (pluginObject instanceof IPluginElement) {
				IPluginElement element = (IPluginElement) pluginObject;
				if (element.getName().equals(RegisterCSWizardPage.F_CS_ELEMENT_DESCRIPTION)) {
					return element;
				}
			}
		}
		return null;
	}

	/**
	 * @param model
	 * @param extensionResult cheat sheet extension found or null
	 * @param elementResult cheat sheet element found or null
	 * @return
	 */
	private FindCSExtensionResult findCSExtensionResult(IPluginModelBase model) {
		// Container for result
		FindCSExtensionResult result = new FindCSExtensionResult();
		// Find all cheat sheet extensions within the host plug-in
		IPluginExtension[] extensions = findCheatSheetExtensions(model);
		// Process all cheat sheet extensions
		// Extension search results
		// (1) An existing extension containing a cheatsheet element with the 
		//     exact cheat sheet ID
		// (2) An existing extension (last one found) containing 0 or more 
		//     cheatsheet or category elements
		// (3) No existing extension
		for (int i = 0; i < extensions.length; i++) {
			// Cheat sheet extension match found
			result.fCSExtension = extensions[i];
			// Check for children
			if (extensions[i].getChildCount() == 0) {
				// Extension has no children, skip to the next extension
				continue;
			}
			IPluginObject[] pluginObjects = extensions[i].getChildren();
			// Process all children
			for (int j = 0; j < pluginObjects.length; j++) {
				if (pluginObjects[j] instanceof IPluginElement) {
					IPluginElement element = (IPluginElement) pluginObjects[j];
					// Find cheat sheet elements
					if (element.getName().equals(RegisterCSWizardPage.F_CS_ELEMENT_CHEATSHEET)) {
						// Cheat sheet element
						// Get the id attribute
						IPluginAttribute idAttribute = element.getAttribute(ICompCSConstants.ATTRIBUTE_ID);
						// Check for the generated ID for this cheat sheet
						// element
						if ((idAttribute != null) && PDETextHelper.isDefined(idAttribute.getValue()) && fRegisterCSData.getDataCheatSheetID().equals(idAttribute.getValue())) {
							// Matching cheat sheet element found
							result.fCSElement = element;
							return result;
						}
					}

				}
			}
		}
		return result;
	}

	public static IPluginExtension[] findCheatSheetExtensions(ISharedExtensionsModel model) {
		IPluginExtension[] extensions = model.getExtensions().getExtensions();

		ArrayList csExtensions = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			String point = extensions[i].getPoint();
			if (F_CS_EXTENSION_POINT_ID.equals(point)) {
				csExtensions.add(extensions[i]);
			}
		}
		return (IPluginExtension[]) csExtensions.toArray(new IPluginExtension[csExtensions.size()]);
	}

	/**
	 * @param file
	 * @param monitor
	 */
	private void createNewPluginFile(IFile file, IProgressMonitor monitor) throws CoreException {

		// Update progress work units
		monitor.beginTask(CSWizardMessages.RegisterCSOperation_task4, 4);
		// Create the plug-in model
		WorkspacePluginModelBase model = (WorkspacePluginModelBase) createModel(file);
		// Update progress work units
		monitor.worked(1);

		IPluginBase base = model.getPluginBase();
		base.setSchemaVersion(TargetPlatformHelper.getSchemaVersion());
		// Create the cheat sheet extension
		base.add(createExtensionCheatSheet(model));
		// Update progress work units
		monitor.worked(1);
		// Save the model to file
		model.save();
		// Update progress work units
		monitor.worked(1);
		// Update the MANIFEST.MF file to ensure the singleton directive is set
		// to true
		modifyExistingManifestFile(file);
		// Update progress work units
		monitor.done();
	}

	/**
	 * @param model
	 */
	private void modifyExistingManifestFile(IFile file) throws CoreException {
		// Validate the operation
		// Note: This is not accurate, we are validating the plugin.xml file rather
		// than the manifest file
		IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {file}, fShell);
		if (status.getSeverity() != IStatus.OK) {
			throw new CoreException(new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, IStatus.ERROR, CSWizardMessages.RegisterCSOperation_errorMessage2, null));
		}
		// Perform the modification of the manifest file
		ModelModification mod = new ModelModification(fRegisterCSData.getPluginProject()) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				doModifyManifestModel(model);
				doModifyBuildModel(model);
			}
		};
		PDEModelUtility.modifyModel(mod, null);
	}

	/**
	 * @param model
	 */
	private void doModifyManifestModel(IBaseModel model) {
		// Make sure we have a base model
		if ((model instanceof IBundlePluginModelBase) == false) {
			return;
		}
		IBundlePluginModelBase modelBase = (IBundlePluginModelBase) model;
		IBundle bundle = modelBase.getBundleModel().getBundle();
		// Get the heading specifying the singleton declaration 
		IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
		if (header instanceof BundleSymbolicNameHeader) {
			BundleSymbolicNameHeader symbolic = (BundleSymbolicNameHeader) header;
			// If the singleton declaration is false, change it to true
			// This is required because plug-ins that specify extensions
			// must be singletons.
			if (symbolic.isSingleton() == false) {
				symbolic.setSingleton(true);
			}
		}
		// Add the cheat sheets plug-in to the list of required bundles
		header = bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header instanceof RequireBundleHeader) {
			RequireBundleHeader require = (RequireBundleHeader) header;
			if (require.hasElement(F_CS_EXTENSION_ID) == false) {
				require.addBundle(F_CS_EXTENSION_ID);
			}
		}
	}

	/**
	 * @param model
	 */
	private void doModifyBuildModel(IBaseModel model) throws CoreException {
		// Make sure we have a base model
		if ((model instanceof IPluginModelBase) == false) {
			return;
		}
		IPluginModelBase modelBase = (IPluginModelBase) model;
		IBuild build = ClasspathUtilCore.getBuild(modelBase);
		// Make sure we have a plugin.properties file
		if (build == null) {
			return;
		}
		// Get the entry for bin.includes
		IBuildEntry entry = build.getEntry(IBuildEntry.BIN_INCLUDES);
		if (entry == null) {
			// This should never happen since the manifest.mf file exists and
			// it has to be in the bin.includes
			return;
		}
		// Add the plugin.xml file to the bin.includes build entry if it does
		// not exist
		if (entry.contains(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) == false) {
			entry.addToken(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
		}
		// There does not seem to be any support in PDEModelUtility or the 
		// ModelModification framework to save build.properties modifications
		// As a result, explicitly do that here
		if (build instanceof BuildObject) {
			IBuildModel buildModel = ((BuildObject) build).getModel();
			if (buildModel instanceof WorkspaceBuildModel) {
				((WorkspaceBuildModel) buildModel).save();
			}
		}
	}

	/**
	 * @param file
	 * @return
	 */
	private IPluginModelBase createModel(IFile file) {
		if (file.getProjectRelativePath().equals(ICoreConstants.FRAGMENT_PATH)) {
			return new WorkspaceFragmentModel(file, false);
		}
		return new WorkspacePluginModel(file, false);
	}

	/**
	 * @param model
	 * @return
	 * @throws CoreException
	 */
	private IPluginExtension createExtensionCheatSheet(IPluginModelBase model) throws CoreException {
		IPluginExtension extension = model.getFactory().createExtension();
		// Point
		extension.setPoint(F_CS_EXTENSION_POINT_ID);
		// NO id
		// NO name 

		createExtensionChildren(extension);

		return extension;
	}

	/**
	 * @param extension
	 * @throws CoreException
	 */
	private void createExtensionChildren(IPluginExtension extension) throws CoreException {
		// Category element
		IPluginElement categoryElement = createElementCategory(extension);
		if (categoryElement != null) {
			extension.add(categoryElement);
		}
		// Cheatsheet element
		IPluginElement cheatSheetElement = createElementCheatSheet(extension);
		if (cheatSheetElement != null) {
			extension.add(cheatSheetElement);
		}
	}

	/**
	 * @param extension
	 * @return
	 * @throws CoreException
	 */
	private IPluginElement createElementCategory(IPluginExtension extension) throws CoreException {
		// Do not create the category if "<none>" was selected
		String categoryID = fRegisterCSData.getDataCategoryID();
		if (categoryID == null) {
			return null;
		}
		// Do not create the category if it is an old category type
		int type = fRegisterCSData.getDataCategoryType();
		if (type != CSCategoryTrackerUtil.F_TYPE_NEW_CATEGORY) {
			return null;
		}
		// Create the element
		IPluginElement element = extension.getModel().getFactory().createElement(extension);
		// Element: category
		element.setName(RegisterCSWizardPage.F_CS_ELEMENT_CATEGORY);
		// Attribute: id
		element.setAttribute(ICompCSConstants.ATTRIBUTE_ID, categoryID);
		// Attribute: name
		// Already trimmed
		element.setAttribute(ICompCSConstants.ATTRIBUTE_NAME, fRegisterCSData.getDataCategoryName());

		return element;
	}

	/**
	 * @param extension
	 * @return
	 * @throws CoreException
	 */
	private IPluginElement createElementCheatSheet(IPluginExtension extension) throws CoreException {

		IPluginElement element = extension.getModel().getFactory().createElement(extension);
		// Element: cheatsheet
		element.setName(RegisterCSWizardPage.F_CS_ELEMENT_CHEATSHEET);
		// Attribute: id
		element.setAttribute(ICompCSConstants.ATTRIBUTE_ID, fRegisterCSData.getDataCheatSheetID());
		// Attribute: name
		element.setAttribute(ICompCSConstants.ATTRIBUTE_NAME, fRegisterCSData.getDataCheatSheetName());
		// Attribute: category
		// Create the category only if "<none>" was not selected
		String categoryID = fRegisterCSData.getDataCategoryID();
		if (categoryID != null) {
			element.setAttribute(RegisterCSWizardPage.F_CS_ELEMENT_CATEGORY, categoryID);
		}
		// Attribute: contentFile
		element.setAttribute(F_CS_ATTRIBUTE_CONTENT_FILE, fRegisterCSData.getDataContentFile());
		// Attribute: composite
		element.setAttribute(F_CS_ATTRIBUTE_COMPOSITE, Boolean.toString(fRegisterCSData.isCompositeCheatSheet()));
		// Element: description
		IPluginElement descriptionElement = createElementDescription(element);
		if (descriptionElement != null) {
			element.add(descriptionElement);
		}

		return element;
	}

	/**
	 * @param parentElement
	 * @return
	 * @throws CoreException
	 */
	private IPluginElement createElementDescription(IPluginElement parentElement) throws CoreException {
		// Define the description element only if description text was 
		// specified 
		String descriptionText = fRegisterCSData.getDataDescription();
		if (PDETextHelper.isDefinedAfterTrim(descriptionText) == false) {
			return null;
		}
		// Create the element
		IPluginElement element = parentElement.getModel().getFactory().createElement(parentElement);
		// Element: description
		element.setName(ISimpleCSConstants.ELEMENT_DESCRIPTION);
		// Content
		element.setText(descriptionText.trim());

		return element;
	}

}
