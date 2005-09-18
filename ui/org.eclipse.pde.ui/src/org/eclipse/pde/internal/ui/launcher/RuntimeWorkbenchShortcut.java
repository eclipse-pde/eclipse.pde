/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
 
/**
 * A launch short cut for the run-time workspace.
 */
public class RuntimeWorkbenchShortcut implements ILaunchShortcut {
	
	public static final String CLASSPATH_PROVIDER = "org.eclipse.pde.ui.workbenchClasspathProvider"; //$NON-NLS-1$
	public static final String CONFIGURATION_TYPE = "org.eclipse.pde.ui.RuntimeWorkbench"; //$NON-NLS-1$

	private IPluginModelBase fModel = null;
	
	public void run(IPluginModelBase model) {
		launch(model, ILaunchManager.RUN_MODE);
	}
	
	public void debug(IPluginModelBase model) {
		launch(model, ILaunchManager.DEBUG_MODE);
	}
	
	/*
	 * @see ILaunchShortcut#launch(IEditorPart, String)
	 */
	public void launch(IEditorPart editor, String mode) {
		launch(mode, null);
	}

	/*
	 * @see ILaunchShortcut#launch(ISelection, String)
	 */
	public void launch(ISelection selection, String mode) {
		launch(getSelectedModel(selection), mode);
	}
	
	private void launch(IPluginModelBase model, String mode) {
		fModel = model;
		if (fModel != null) {
			String[] applicationNames = getAvailableApplications();
			if (applicationNames.length == 0) {
				launch(mode, null);
			} else if (applicationNames.length == 1) {
				launch(mode, applicationNames[0]);
			} else {		
				ApplicationSelectionDialog dialog = new ApplicationSelectionDialog(
						PDEPlugin.getActiveWorkbenchShell().getShell(), applicationNames,
						mode);
				if (dialog.open() == Dialog.OK) {
					launch(mode, dialog.getSelectedApplication());
				}
			}
		} else {
			launch(mode, null);
		}
	}
	
	private IPluginModelBase getSelectedModel(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			if (!ssel.isEmpty()) {
				Object object = ssel.getFirstElement();
				if (object instanceof IAdaptable) {
					IProject project = (IProject)((IAdaptable)object).getAdapter(IProject.class);
					if (project != null && project.isOpen())
						return PDECore.getDefault().getModelManager().findModel(project);
				}
			}
		}
		return null;
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
		
	/**
	 * Launches a configuration in the given mode
	 */
	protected void launch(String mode, String applicationName) {
		ILaunchConfiguration config = findLaunchConfiguration(mode, applicationName);
		if (config != null) {
			DebugUITools.launch(config, mode);
		}			
	}
	
	/**
	 * Locate a configuration to relaunch.  If one cannot be found, create one.
	 * 
	 * @return a re-useable config or <code>null</code> if none
	 */
	protected ILaunchConfiguration findLaunchConfiguration(String mode, String applicationName) {
		ILaunchConfiguration[] configs = getLaunchConfigurations(getWorkbenchLaunchConfigType(), applicationName);
			
		if (configs.length == 0)
			return createConfiguration(applicationName);

		if (configs.length == 1)
			return configs[0];

		// Prompt the user to choose a config. 
		return chooseConfiguration(configs, mode);
	}
	
	private ILaunchConfiguration[] getLaunchConfigurations(ILaunchConfigurationType configType, String applicationName) {
		ArrayList result = new ArrayList();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfiguration[] configs = manager.getLaunchConfigurations(configType);
			for (int i = 0; i < configs.length; i++) {
				if (!DebugUITools.isPrivate(configs[i])) {
					if (!configs[i].getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
						String configApp = configs[i].getAttribute(IPDELauncherConstants.APPLICATION, (String)null);
						if ((configApp == null && applicationName == null)
							|| (configApp != null && applicationName != null && configApp.equals(applicationName))) {
							result.add(configs[i]);
						}
					} else {
						String thisProduct = configs[i].getAttribute(IPDELauncherConstants.PRODUCT, (String)null);
						if (thisProduct != null && thisProduct.equals(getProduct(applicationName))) {
							result.add(configs[i]);
						}
					}
					
				}
			}
		} catch (CoreException e) {
		}
		return (ILaunchConfiguration[]) result.toArray(new ILaunchConfiguration[result.size()]);
	}
	
	/**
	 * Shows a selection dialog that allows the user to choose one of the specified
	 * launch configurations.  Return the chosen config, or <code>null</code> if the
	 * user cancelled the dialog.
	 */
	protected ILaunchConfiguration chooseConfiguration(ILaunchConfiguration[] configs, String mode) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setElements(configs);
		dialog.setTitle(PDEUIMessages.RuntimeWorkbenchShortcut_title);  
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(PDEUIMessages.RuntimeWorkbenchShortcut_select_debug); 
		} else {
			dialog.setMessage(PDEUIMessages.RuntimeWorkbenchShortcut_select_run);  
		}
		dialog.setMultipleSelection(false);
		int result= dialog.open();
		labelProvider.dispose();
		if (result == ElementListSelectionDialog.OK) {
			return (ILaunchConfiguration)dialog.getFirstResult();
		}
		return null;		
	}
	
	/**
	 * Creates a new configuration with default values.
	 */
	protected ILaunchConfiguration createConfiguration(String applicationName) {
		ILaunchConfiguration config = null;
		try {
			ILaunchConfigurationType configType = getWorkbenchLaunchConfigType();
			String computedName = getComputedName(configType.getName());
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, computedName);  
			wc.setAttribute(IPDELauncherConstants.LOCATION, getDefaultWorkspaceLocation(computedName)); //$NON-NLS-1$
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, ""); //$NON-NLS-1$
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, ""); //$NON-NLS-1$
			wc.setAttribute(IPDELauncherConstants.USEFEATURES, false);
			wc.setAttribute(IPDELauncherConstants.DOCLEAR, false);
			wc.setAttribute(IPDELauncherConstants.ASKCLEAR, true);
			wc.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);
			wc.setAttribute(IPDELauncherConstants.USE_DEFAULT, applicationName == null);
			if (applicationName != null) {
				String product = getProduct(applicationName);
				if (product == null) {
					wc.setAttribute(IPDELauncherConstants.APPLICATION, applicationName);
				} else {
					wc.setAttribute(IPDELauncherConstants.USE_PRODUCT, true);
					wc.setAttribute(IPDELauncherConstants.PRODUCT, product);
				}
				wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, false);
				TreeMap map = new TreeMap();
				addPluginAndDependencies(fModel, map);
				Object[] models = map.values().toArray();
				StringBuffer wsplugins = new StringBuffer();
				StringBuffer explugins = new StringBuffer();
				for (int i = 0; i < models.length; i++) {
					IPluginModelBase model = (IPluginModelBase)models[i];
					String id = model.getPluginBase().getId();
					if (model.getUnderlyingResource() == null) {
						explugins.append(id + File.pathSeparatorChar);
					} else {
						wsplugins.append(id + File.pathSeparatorChar);
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

			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, CLASSPATH_PROVIDER);
			config= wc.doSave();		
		} catch (CoreException ce) {
			PDEPlugin.logException(ce);
		} 
		return config;
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
	 * Returns the workbench config type
	 */
	protected ILaunchConfigurationType getWorkbenchLaunchConfigType() {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		return lm.getLaunchConfigurationType(CONFIGURATION_TYPE);	
	}	
	
	private String getComputedName(String prefix) {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		return lm.generateUniqueLaunchConfigurationNameFrom(prefix);
	}
	
	/**
	 * Convenience method to get the window that owns this action's Shell.
	 */
	protected Shell getShell() {
		return PDEPlugin.getActiveWorkbenchShell();
	}
	
	public static String getDefaultWorkspaceLocation(String uniqueName) {
		return LauncherUtils.getDefaultPath().append("runtime-" + uniqueName.replaceAll("\\s", "")).toPortableString();		//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static void addPluginAndDependencies(IPluginModelBase model, TreeMap map) {
		if (model == null)
			return;

		String id = model.getPluginBase().getId();
		if (map.containsKey(id))
			return;

		map.put(id, model);

		if (model instanceof IFragmentModel) {
			IPluginModelBase parent =
				findPlugin(((IFragmentModel) model).getFragment().getPluginId());
			addPluginAndDependencies(parent, map);
		} else {
			IFragmentModel[] fragments = findFragments(model.getPluginBase());
			for (int i = 0; i < fragments.length; i++) {
				String fragmentID = fragments[i].getPluginBase().getId();
				if (!"org.eclipse.ui.workbench.compatibility".equals(fragmentID)) //$NON-NLS-1$
					addPluginAndDependencies(fragments[i], map);
			}
		}

		IPluginImport[] imports = model.getPluginBase().getImports();
		for (int i = 0; i < imports.length; i++) {
			addPluginAndDependencies(findPlugin(imports[i].getId()), map);
		}	
	}
	
	private static IPluginModelBase findPlugin(String id) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		ModelEntry entry = manager.findEntry(id);
		return (entry != null) ? entry.getActiveModel() : null;
	}
	
	private static IFragmentModel[] findFragments(IPluginBase plugin) {
		ModelEntry[] entries = PDECore.getDefault().getModelManager().getEntries();
		ArrayList result = new ArrayList();
		for (int i = 0; i < entries.length; i++) {
			ModelEntry entry = entries[i];
			IPluginModelBase model = entry.getActiveModel();
			if (model instanceof IFragmentModel) {
				String id = ((IFragmentModel) model).getFragment().getPluginId();
				if (id.equals(plugin.getId())) {
					result.add(model);
				}
			}
		}
		return (IFragmentModel[]) result.toArray(new IFragmentModel[result.size()]);
	}	
}
