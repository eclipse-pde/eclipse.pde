/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.wizards.toc;

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
import org.eclipse.pde.internal.ua.core.toc.ITocConstants;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.Constants;

public class RegisterTocOperation extends WorkspaceModifyOperation {

	public final static String F_TOC_EXTENSION_POINT_ID = "org.eclipse.help.toc"; //$NON-NLS-1$
	public static final String F_HELP_EXTENSION_ID = "org.eclipse.help"; //$NON-NLS-1$
	public static final String F_TOC_ATTRIBUTE_FILE = "file"; //$NON-NLS-1$
	public final static String F_TOC_ATTRIBUTE_PRIMARY = "primary"; //$NON-NLS-1$
	public final static String F_TOC_ATTRIBUTE_EXTRADIR = "extradir"; //$NON-NLS-1$
	public final static String F_TOC_ATTRIBUTE_CATEGORY = "category"; //$NON-NLS-1$

	private IRegisterTOCData fPage;
	private Shell fShell;

	/**
	 * 
	 */
	public RegisterTocOperation(IRegisterTOCData page, Shell shell) {
		fPage = page;
		fShell = shell;
	}

	/**
	 * @param rule
	 */
	public RegisterTocOperation(ISchedulingRule rule) {
		super(rule);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {

		try {
			boolean fragment = PluginRegistry.findModel(fPage.getPluginProject()).isFragmentModel();
			IFile file = fPage.getPluginProject().getFile(fragment ? ICoreConstants.FRAGMENT_PATH : ICoreConstants.PLUGIN_PATH);
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
	private static class FindTocExtensionResult {

		public IPluginExtension fTocExtension;

		public IPluginElement fTocElement;

		/**
		 * 
		 */
		public FindTocExtensionResult() {
			fTocExtension = null;
			fTocElement = null;
		}

		/**
		 * @return
		 */
		public boolean foundTocExtension() {
			return (fTocExtension != null);
		}

		/**
		 * @return
		 */
		public boolean foundExactTocElement() {
			return (fTocElement != null);
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
			throw new CoreException(new Status(IStatus.ERROR, PDEUserAssistanceUIPlugin.PLUGIN_ID, IStatus.ERROR, TocWizardMessages.RegisterTocOperation_errorMessage1, null));
		}
		// Perform the modification of the plugin manifest file
		ModelModification mod = new ModelModification(fPage.getPluginProject()) {
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
		FindTocExtensionResult result = findTocExtensionResult(modelBase);
		// Check search results and act accordingly
		if (result.foundTocExtension() && result.foundExactTocElement()) {
			// An exact match to an existing TOC element was
			// found.  Update the element fields
			modifyExistingElement(result.fTocElement, monitor);
		} else if (result.foundTocExtension()) {
			// No exact match to an existing TOC element found within
			// the existing TOC extension.  Update the 
			// existing extension by adding a new TOC element
			// to it
			modifyExistingExtension(result.fTocExtension, monitor);
		} else {
			// No existing TOC extension found, create a new
			// extension
			insertNewExtension(modelBase, monitor);
		}
	}

	/**
	 * @param modelBase
	 */
	private void insertNewExtension(IPluginModelBase modelBase, IProgressMonitor monitor) throws CoreException {
		// Update progress work units
		monitor.beginTask(TocWizardMessages.RegisterTocOperation_task, 1);
		// Create the new extension
		IPluginExtension extension = createExtensionToc(modelBase);
		modelBase.getPluginBase().add(extension);
		// Update progress work units
		monitor.done();
	}

	/**
	 * @param extension
	 */
	private void modifyExistingExtension(IPluginExtension extension, IProgressMonitor monitor) throws CoreException {
		// Update progress work units
		monitor.beginTask(TocWizardMessages.RegisterTocOperation_task2, 1);
		// Create new children for existing extension
		createExtensionChildren(extension);
		// Update progress work units
		monitor.done();
	}

	/**
	 * @param tocElement
	 * @param monitor
	 */
	private void modifyExistingElement(IPluginElement tocElement, IProgressMonitor monitor) throws CoreException {
		// Update progress work units
		monitor.beginTask(TocWizardMessages.RegisterTocOperation_task3, 1);

		// Update the file
		tocElement.setAttribute(F_TOC_ATTRIBUTE_FILE, fPage.getDataTocFile());

		// Update the primary attribute
		// But only if it already exists, or if this TOC will be primary
		boolean primary = fPage.getDataPrimary();
		if (primary || tocElement.getAttribute(F_TOC_ATTRIBUTE_PRIMARY) != null) {
			tocElement.setAttribute(F_TOC_ATTRIBUTE_PRIMARY, Boolean.toString(primary));
		}

		// Update progress work units
		monitor.done();
	}

	/**
	 * @param model
	 * @param extensionResult cheat sheet extension found or null
	 * @param elementResult cheat sheet element found or null
	 * @return
	 */
	private FindTocExtensionResult findTocExtensionResult(IPluginModelBase model) {
		// Container for result
		FindTocExtensionResult result = new FindTocExtensionResult();
		// Find all cheat sheet extensions within the host plug-in
		IPluginExtension[] extensions = findTOCExtensions(model);
		// Process all TOC extensions
		// Extension search results
		// (1) An existing extension containing a TOC element with the 
		//     exact TOC filename
		// (2) An existing extension (last one found) containing 0 or more 
		//     TOC elements
		// (3) No existing extension
		for (int i = 0; i < extensions.length; i++) {
			// TOC extension match found
			result.fTocExtension = extensions[i];
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
					// Find TOC elements
					if (element.getName().equals(ITocConstants.ELEMENT_TOC)) {
						// TOC element
						// Get the file attribute
						IPluginAttribute fileAttribute = element.getAttribute(F_TOC_ATTRIBUTE_FILE);
						// Check for the filename for this TOC element
						if ((fileAttribute != null) && PDETextHelper.isDefined(fileAttribute.getValue()) && fPage.getDataTocFile().equals(fileAttribute.getValue())) {
							// Matching TOC element found
							result.fTocElement = element;
							return result;
						}
					}
				}
			}
		}

		return result;
	}

	public static IPluginExtension[] findTOCExtensions(ISharedExtensionsModel model) {
		IPluginExtension[] extensions = model.getExtensions().getExtensions();

		ArrayList tocExtensions = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			String point = extensions[i].getPoint();
			if (F_TOC_EXTENSION_POINT_ID.equals(point)) {
				tocExtensions.add(extensions[i]);
			}
		}
		return (IPluginExtension[]) tocExtensions.toArray(new IPluginExtension[tocExtensions.size()]);
	}

	/**
	 * @param file
	 * @param monitor
	 */
	private void createNewPluginFile(IFile file, IProgressMonitor monitor) throws CoreException {

		// Update progress work units
		monitor.beginTask(TocWizardMessages.RegisterTocOperation_task4, 4);
		// Create the plug-in model
		WorkspacePluginModelBase model = (WorkspacePluginModelBase) createModel(file);
		// Update progress work units
		monitor.worked(1);

		IPluginBase base = model.getPluginBase();
		base.setSchemaVersion(TargetPlatformHelper.getSchemaVersion());
		// Create the cheat sheet extension
		base.add(createExtensionToc(model));
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
			throw new CoreException(new Status(IStatus.ERROR, PDEUserAssistanceUIPlugin.PLUGIN_ID, IStatus.ERROR, TocWizardMessages.RegisterTocOperation_errorMessage2, null));
		}
		// Perform the modification of the manifest file
		ModelModification mod = new ModelModification(fPage.getPluginProject()) {
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
			if (require.hasElement(F_HELP_EXTENSION_ID) == false) {
				require.addBundle(F_HELP_EXTENSION_ID);
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
	private IPluginExtension createExtensionToc(IPluginModelBase model) throws CoreException {
		IPluginExtension extension = model.getFactory().createExtension();
		// Point
		extension.setPoint(F_TOC_EXTENSION_POINT_ID);

		createExtensionChildren(extension);

		return extension;
	}

	/**
	 * @param extension
	 * @throws CoreException
	 */
	private void createExtensionChildren(IPluginExtension extension) throws CoreException {
		// TOC element
		IPluginElement tocElement = createElementToc(extension);
		if (tocElement != null) {
			extension.add(tocElement);
		}
	}

	/**
	 * @param extension
	 * @return
	 * @throws CoreException
	 */
	private IPluginElement createElementToc(IPluginExtension extension) throws CoreException {

		IPluginElement element = extension.getModel().getFactory().createElement(extension);

		// Element: toc
		element.setName(ITocConstants.ELEMENT_TOC);

		// Attribute: file
		element.setAttribute(F_TOC_ATTRIBUTE_FILE, fPage.getDataTocFile());

		// Attribute: primary
		boolean primary = fPage.getDataPrimary();

		if (primary) {
			element.setAttribute(F_TOC_ATTRIBUTE_PRIMARY, Boolean.TRUE.toString());
		} else {
			element.setAttribute(F_TOC_ATTRIBUTE_PRIMARY, Boolean.FALSE.toString());
		}

		return element;
	}
}
