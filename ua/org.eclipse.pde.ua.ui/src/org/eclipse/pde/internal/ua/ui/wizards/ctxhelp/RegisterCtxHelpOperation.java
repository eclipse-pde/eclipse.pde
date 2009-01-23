/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.wizards.ctxhelp;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
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
import org.eclipse.pde.internal.ua.core.ctxhelp.ICtxHelpConstants;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.CtxHelpEditor;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.Constants;

/**
 * Register Context Help Operation, registers a context help xml file in the
 * plugin.xml. Must be run in the UI thread.
 * 
 * @since 3.4
 * @see RegisterCtxHelpOperation
 * @see CtxHelpEditor
 */
public class RegisterCtxHelpOperation extends WorkspaceModifyOperation {

	static final String CTX_HELP_EXTENSION_POINT_ID = "org.eclipse.help.contexts"; //$NON-NLS-1$
	static final String CTX_HELP_PLUGIN_ID = "org.eclipse.help"; //$NON-NLS-1$

	public static final String CTX_HELP_ATTR_FILE = "file"; //$NON-NLS-1$
	public final static String CTX_HELP_ATTR_PLUGIN = "plugin"; //$NON-NLS-1$

	private Shell fShell;
	private String fPluginText;
	private IProject fProject;
	private String fResourceString;

	public RegisterCtxHelpOperation(Shell shell, IModel model, String pluginText) {
		fPluginText = pluginText;
		fProject = model.getUnderlyingResource().getProject();
		fResourceString = model.getUnderlyingResource()
				.getProjectRelativePath().toPortableString();
		fShell = shell;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core
	 * .runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		try {
			boolean fragment = PluginRegistry.findModel(fProject)
					.isFragmentModel();
			IFile file = fProject
					.getFile(fragment ? ICoreConstants.FRAGMENT_PATH
							: ICoreConstants.PLUGIN_PATH);
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

	private void modifyExistingPluginFile(IFile file, IProgressMonitor monitor)
			throws CoreException {
		IStatus status = ResourcesPlugin.getWorkspace().validateEdit(
				new IFile[] { file }, fShell);
		if (status.getSeverity() != IStatus.OK) {
			throw new CoreException(new Status(IStatus.ERROR,
					PDEUserAssistanceUIPlugin.PLUGIN_ID, IStatus.ERROR,
					CtxWizardMessages.RegisterCtxHelpOperation_errorMessage1,
					null));
		}
		// Perform the modification of the plugin manifest file
		ModelModification mod = new ModelModification(fProject) {
			protected void modifyModel(IBaseModel model,
					IProgressMonitor monitor) throws CoreException {
				doModifyPluginModel(model, monitor);
				doModifyManifestModel(model);
			}
		};
		PDEModelUtility.modifyModel(mod, monitor);
	}

	private void doModifyPluginModel(IBaseModel model, IProgressMonitor monitor)
			throws CoreException {
		if ((model instanceof IPluginModelBase) == false) {
			return;
		}

		IPluginModelBase modelBase = (IPluginModelBase) model;
		IPluginExtension[] extensions = modelBase.getExtensions()
				.getExtensions();
		IPluginExtension existingExtension = null;
		for (int i = 0; i < extensions.length; i++) {
			String point = extensions[i].getPoint();
			if (CTX_HELP_EXTENSION_POINT_ID.equals(point)) {
				if (checkExistingExtensionElement(extensions[i])) {
					// Exact match, no need to register anything
					return;
				}
				existingExtension = extensions[i];
			}
		}

		if (existingExtension != null) {
			// No exact match, add a new entry to the existing extension
			addElementToExtension(existingExtension);
		} else {
			// No existing extension found, create one
			addExtensionToModel(modelBase);
		}
	}

	private void doModifyManifestModel(IBaseModel model) {
		// Make sure we have a base model
		if ((model instanceof IBundlePluginModelBase) == false) {
			return;
		}
		IBundlePluginModelBase modelBase = (IBundlePluginModelBase) model;
		IBundle bundle = modelBase.getBundleModel().getBundle();
		// Get the heading specifying the singleton declaration
		IManifestHeader header = bundle
				.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
		if (header instanceof BundleSymbolicNameHeader) {
			BundleSymbolicNameHeader symbolic = (BundleSymbolicNameHeader) header;
			// If the singleton declaration is false, change it to true
			// This is required because plug-ins that specify extensions
			// must be singletons.
			if (symbolic.isSingleton() == false) {
				symbolic.setSingleton(true);
			}
		}
		// Add the context help plug-in to the list of required bundles
		header = bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header instanceof RequireBundleHeader) {
			RequireBundleHeader require = (RequireBundleHeader) header;
			if (require.hasElement(CTX_HELP_PLUGIN_ID) == false) {
				require.addBundle(CTX_HELP_PLUGIN_ID);
			}
		}
	}

	private void createNewPluginFile(IFile file, IProgressMonitor monitor)
			throws CoreException {
		monitor.beginTask(CtxWizardMessages.RegisterCtxHelpOperation_task, 4);

		WorkspacePluginModelBase model;
		if (file.getProjectRelativePath().equals(ICoreConstants.FRAGMENT_PATH)) {
			model = new WorkspaceFragmentModel(file, false);
		} else {
			model = new WorkspacePluginModel(file, false);
		}
		monitor.worked(1);

		IPluginBase base = model.getPluginBase();
		base.setSchemaVersion(TargetPlatformHelper.getSchemaVersion());

		addExtensionToModel(model);
		monitor.worked(1);

		model.save();
		monitor.worked(1);
		// Update the MANIFEST.MF file to ensure the singleton directive is set
		// to true
		modifyExistingManifestFile(file);
		monitor.done();
	}

	/**
	 * @param model
	 */
	private void modifyExistingManifestFile(IFile file) throws CoreException {
		// Validate the operation
		// Note: This is not accurate, we are validating the plugin.xml file
		// rather
		// than the manifest file
		IStatus status = ResourcesPlugin.getWorkspace().validateEdit(
				new IFile[] { file }, fShell);
		if (status.getSeverity() != IStatus.OK) {
			throw new CoreException(new Status(IStatus.ERROR,
					PDEUserAssistanceUIPlugin.PLUGIN_ID, IStatus.ERROR,
					CtxWizardMessages.RegisterCtxHelpOperation_errorMessage2,
					null));
		}
		// Perform the modification of the manifest file
		ModelModification mod = new ModelModification(fProject) {
			protected void modifyModel(IBaseModel model,
					IProgressMonitor monitor) throws CoreException {
				doModifyManifestModel(model);
				doModifyBuildModel(model);
			}
		};
		PDEModelUtility.modifyModel(mod, null);
	}

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

	private boolean checkExistingExtensionElement(IPluginExtension extension) {
		IPluginObject[] pluginObjects = extension.getChildren();
		for (int j = 0; j < pluginObjects.length; j++) {
			if (pluginObjects[j] instanceof IPluginElement) {
				IPluginElement element = (IPluginElement) pluginObjects[j];
				if (element.getName().equals(ICtxHelpConstants.ELEMENT_ROOT)) {
					IPluginAttribute fileAttribute = element
							.getAttribute(CTX_HELP_ATTR_FILE);
					if ((fileAttribute != null)
							&& PDETextHelper
									.isDefined(fileAttribute.getValue())
							&& fResourceString.equals(fileAttribute.getValue())) {
						IPluginAttribute pluginAttribute = element
								.getAttribute(CTX_HELP_ATTR_PLUGIN);
						if (pluginAttribute == null
								|| !PDETextHelper.isDefined(pluginAttribute
										.getValue())) {
							if (fPluginText.length() == 0) {
								return true;
							}
						} else if (fPluginText.equals(pluginAttribute
								.getValue())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void addExtensionToModel(IPluginModelBase model)
			throws CoreException {
		IPluginExtension extension = model.getFactory().createExtension();
		extension.setPoint(CTX_HELP_EXTENSION_POINT_ID);
		addElementToExtension(extension);
		model.getPluginBase().add(extension);
	}

	private void addElementToExtension(IPluginExtension extension)
			throws CoreException {
		IPluginElement element = extension.getModel().getFactory()
				.createElement(extension);
		element.setName(ICtxHelpConstants.ELEMENT_ROOT);
		element.setAttribute(CTX_HELP_ATTR_FILE, fResourceString);
		if (fPluginText.length() > 0) {
			element.setAttribute(CTX_HELP_ATTR_PLUGIN, fPluginText);
		}
		extension.add(element);
	}

}
