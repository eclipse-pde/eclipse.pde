/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.launcher.ApplicationSelectionDialog;
import org.eclipse.pde.internal.ui.launcher.LaunchArgumentsHelper;
import org.eclipse.ui.IEditorPart;

/**
 * A launch shortcut capable of launching an Eclipse application.
 * Given the current selection, either a new Eclipse Application launch configuration is created with default settings, or the user is presented
 * with a list of suitable existing Eclipse Application launch configurations to choose from.
 * <p>
 * This class may be substantiated or subclassed by clients.
 * </p>
 * @since 3.3
 */
public class EclipseLaunchShortcut extends AbstractLaunchShortcut {
	
	public static final String CONFIGURATION_TYPE = "org.eclipse.pde.ui.RuntimeWorkbench"; //$NON-NLS-1$

	private IPluginModelBase fModel = null;
	
	private String fApplicationName = null;
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart, java.lang.String)
	 */
	public void launch(IEditorPart editor, String mode) {
		fApplicationName = null;
		fModel = null;
		launch(mode);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection, java.lang.String)
	 */
	public void launch(ISelection selection, String mode) {
		IPluginModelBase model = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			if (!ssel.isEmpty()) {
				Object object = ssel.getFirstElement();
				if (object instanceof IAdaptable) {
					IProject project = (IProject)((IAdaptable)object).getAdapter(IProject.class);
					if (project != null && project.isOpen())
						model = PDECore.getDefault().getModelManager().findModel(project);
				}
			}
		}
		launch(model, mode);
	}
	
	private void launch(IPluginModelBase model, String mode) {
		fModel = model;
		fApplicationName = null;
		if (fModel != null) {
			String[] applicationNames = getAvailableApplications();
			if (applicationNames.length == 1) {
				fApplicationName = applicationNames[0];
			} else if (applicationNames.length > 1){		
				ApplicationSelectionDialog dialog = new ApplicationSelectionDialog(
						PDEPlugin.getActiveWorkbenchShell().getShell(), applicationNames,
						mode);
				if (dialog.open() == Window.OK) {
					fApplicationName = dialog.getSelectedApplication();
				}
			}
		}
		launch(mode);
	}
	
	private String[] getAvailableApplications() {
		IPluginBase plugin = fModel.getPluginBase();
		String id = plugin.getId();
		if (id == null || id.trim().length() == 0)
			return new String[0];
		
		IPluginExtension[] extensions = plugin.getExtensions();
		ArrayList result = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			IPluginExtension extension = extensions[i];
			if ("org.eclipse.core.runtime.applications".equals(extension.getPoint())) { //$NON-NLS-1$
				String extensionID = extension.getId();
				if (extensionID != null && extensionID.trim().length() > 0) {
					result.add(id.trim() + "." + extensionID.trim()); //$NON-NLS-1$
				}
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}

	private String getProduct(String appName) {
		if (appName == null)
			return TargetPlatform.getDefaultProduct();
		if (fModel != null && appName != null) {
			IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				IPluginExtension ext = extensions[i];
				String point = ext.getPoint();
				if ("org.eclipse.core.runtime.products".equals(point)) { //$NON-NLS-1$
					if (ext.getChildCount() == 1) {
						IPluginElement prod = (IPluginElement)ext.getChildren()[0];
						if (prod.getName().equals("product")) { //$NON-NLS-1$
							IPluginAttribute attr = prod.getAttribute("application"); //$NON-NLS-1$
							if (attr != null && appName.equals(attr.getValue())) {
								return fModel.getPluginBase().getId() + "." + ext.getId(); //$NON-NLS-1$
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns a boolean value indicating whether the launch configuration is a good match for
	 * the application or product to launch.
	 * 
	 * @param configuration 
	 * 			the launch configuration being evaluated
	 * 
	 * @return <code>true</coded> if the launch configuration is suitable for the application
	 * or product to launch with, <code>false</code> otherwise.
	 */
	protected boolean isGoodMatch(ILaunchConfiguration configuration) {
		try {
			if (!configuration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
				String configApp = configuration.getAttribute(IPDELauncherConstants.APPLICATION, (String)null);
				return (configApp == null && fApplicationName == null)
					   || (fApplicationName != null && fApplicationName.equals(configApp));
			}
			String thisProduct = configuration.getAttribute(IPDELauncherConstants.PRODUCT, (String)null);
			return thisProduct != null && thisProduct.equals(getProduct(fApplicationName));
			
		} catch (CoreException e) {
		}
		return false;
	}
	
	/**
	 * Initializes a new Eclipse Application launch configuration with defaults based
	 * on the current selection:
	 * <ul>
	 * <li>If there is no selection or the selected project is a plug-in that does not declare an application,
	 * the default product is launched.</li>
	 * <li>If the selected project is a plug-in that declares an application, then that application is launched.</li>
	 * <li>If the selected project is a plug-in that declares more than one application, then the user is presented
	 * with a list of choices to choose from.</li>
	 * </ul>
	 * Once an application is chosen, the plug-in is searched to see if there is a product
	 * bound to this application.  If a product is found, the product is launched instead, since
	 * a product provides a richer branded experience.
	 * 
	 * @since 3.3
	 */
	protected void initializeConfiguration(ILaunchConfigurationWorkingCopy wc) {
		if (TargetPlatform.getTargetVersion() >= 3.2)
			wc.setAttribute("pde.version", "3.2a"); //$NON-NLS-1$ //$NON-NLS-2$
		wc.setAttribute(IPDELauncherConstants.LOCATION, LaunchArgumentsHelper.getDefaultWorkspaceLocation(wc.getName())); //$NON-NLS-1$
		initializeProgramArguments(wc);
		initializeVMArguments(wc);
		wc.setAttribute(IPDELauncherConstants.USEFEATURES, false);
		wc.setAttribute(IPDELauncherConstants.DOCLEAR, false);
		wc.setAttribute(IPDELauncherConstants.ASKCLEAR, true);
		wc.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);
		wc.setAttribute(IPDELauncherConstants.USE_DEFAULT, fApplicationName == null);
		if (fApplicationName != null) {
			String product = getProduct(fApplicationName);
			if (product == null) {
				wc.setAttribute(IPDELauncherConstants.APPLICATION, fApplicationName);
			} else {
				wc.setAttribute(IPDELauncherConstants.USE_PRODUCT, true);
				wc.setAttribute(IPDELauncherConstants.PRODUCT, product);
			}
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, false);
			
			StringBuffer wsplugins = new StringBuffer();
			StringBuffer explugins = new StringBuffer();
			PluginModelManager manager = PDECore.getDefault().getModelManager();
			Set plugins = DependencyManager.getSelfAndDependencies(fModel);
			Iterator iter = plugins.iterator();
			while (iter.hasNext()) {
				String id = iter.next().toString();
				IPluginModelBase model = manager.findModel(id);
				if (!model.isEnabled())
					continue;
				if (model.getUnderlyingResource() == null) {
					if (explugins.length() > 0)
						explugins.append(","); //$NON-NLS-1$
					explugins.append(id);
				} else {
					if (wsplugins.length() > 0)
						wsplugins.append(","); //$NON-NLS-1$
					wsplugins.append(id);
				}
			}
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS, wsplugins.toString());
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, explugins.toString());
		} else {
			String defaultProduct = TargetPlatform.getDefaultProduct();
			if (defaultProduct != null) {
				wc.setAttribute(IPDELauncherConstants.USE_DEFAULT, true);
				wc.setAttribute(IPDELauncherConstants.USE_PRODUCT, true);
				wc.setAttribute(IPDELauncherConstants.PRODUCT, defaultProduct);
			}
		}
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, PDESourcePathProvider.ID);
	}
	
	private void initializeProgramArguments(ILaunchConfigurationWorkingCopy wc) {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String programArgs = preferences.getString(ICoreConstants.PROGRAM_ARGS);
		if (programArgs.length()  > 0)
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, programArgs);
	}
	
	private void initializeVMArguments(ILaunchConfigurationWorkingCopy wc) {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String vmArgs = preferences.getString(ICoreConstants.VM_ARGS);
		if (vmArgs.length() > 0)
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
	}	
	
	/**
	 * Returns the Eclipse application configuration type ID as declared in the plugin.xml
	 * 
	 * @return the Eclipse application configuration type ID
	 */
	protected String getLaunchConfigurationTypeName() {
		return CONFIGURATION_TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractLaunchShortcut#getName(org.eclipse.debug.core.ILaunchConfigurationType)
	 */
	protected String getName(ILaunchConfigurationType type) {
		// if launching default product, use default naming convention
		if (fApplicationName == null)
			return super.getName(type);
		String product = getProduct(fApplicationName);
		return (product == null) ? fApplicationName : product;
	}	
}
