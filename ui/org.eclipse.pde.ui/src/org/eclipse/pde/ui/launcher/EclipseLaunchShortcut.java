/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import java.util.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.launcher.ApplicationSelectionDialog;
import org.eclipse.pde.internal.ui.launcher.LaunchAction;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.launching.PDESourcePathProvider;
import org.eclipse.ui.IEditorPart;

/**
 * A launch shortcut capable of launching an Eclipse application.
 * Given the current selection, either a new Eclipse Application launch configuration is created with default settings, or the user is presented
 * with a list of suitable existing Eclipse Application launch configurations to choose from.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p>
 * @since 3.3
 */
public class EclipseLaunchShortcut extends AbstractLaunchShortcut {

	/**
	 * The launch configuration type name that this shortcut uses
	 */
	public static final String CONFIGURATION_TYPE = IPDELauncherConstants.ECLIPSE_APPLICATION_LAUNCH_CONFIGURATION_TYPE;

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
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (!ssel.isEmpty()) {
				Object object = ssel.getFirstElement();
				IProject project = null;
				if (object instanceof IFile) {
					// if instanceof Product model, we are launching from Product Editor.  Launch as Product
					if ("product".equals(((IFile) object).getFileExtension())) { //$NON-NLS-1$
						WorkspaceProductModel productModel = new WorkspaceProductModel((IFile) object, false);
						try {
							productModel.load();
							new LaunchAction(productModel.getProduct(), ((IFile) object).getFullPath().toOSString(), mode).run();
						} catch (CoreException e) {
							PDEPlugin.log(e);
						}
						return;
					}
					// if it isn't a .product file, then find the project of the file inorder to launch using that project's corresponding plug-in
					// bug 180043
					project = ((IFile) object).getProject();
				} else if (object instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) object).getAdapter(IProject.class);
				}
				if (project != null && project.isOpen())
					model = PluginRegistry.findModel(project);
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
			} else if (applicationNames.length > 1) {
				ApplicationSelectionDialog dialog = new ApplicationSelectionDialog(PDEPlugin.getActiveWorkbenchShell().getShell(), applicationNames, mode);
				if (dialog.open() == Window.OK) {
					fApplicationName = dialog.getSelectedApplication();
				}
			}
		}
		launch(mode);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractLaunchShortcut#findLaunchConfiguration(java.lang.String)
	 */
	protected ILaunchConfiguration findLaunchConfiguration(String mode) {
		ILaunchConfiguration config = super.findLaunchConfiguration(mode);
		if (config != null) {
			try {
				if (!(config.getAttribute(IPDELauncherConstants.USE_DEFAULT, false)) && (config.getAttribute(IPDEUIConstants.GENERATED_CONFIG, false))) {
					ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
					initializePluginsList(wc);
					return wc.doSave();
				}
			} catch (CoreException e) {
			}
		}
		return config;
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
				if (extensionID != null) {
					result.add(IdUtil.getFullId(extensions[i]));
				}
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
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
						IPluginElement prod = (IPluginElement) ext.getChildren()[0];
						if (prod.getName().equals("product")) { //$NON-NLS-1$
							IPluginAttribute attr = prod.getAttribute("application"); //$NON-NLS-1$
							if (attr != null && appName.equals(attr.getValue())) {
								return IdUtil.getFullId(ext);
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
				String configApp = configuration.getAttribute(IPDELauncherConstants.APPLICATION, (String) null);
				return (configApp == null && fApplicationName == null) || (fApplicationName != null && fApplicationName.equals(configApp));
			}
			String thisProduct = configuration.getAttribute(IPDELauncherConstants.PRODUCT, (String) null);
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
		if (TargetPlatformHelper.usesNewApplicationModel())
			wc.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "3.3"); //$NON-NLS-1$
		else if (TargetPlatformHelper.getTargetVersion() >= 3.2)
			wc.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "3.2a"); //$NON-NLS-1$
		wc.setAttribute(IPDELauncherConstants.LOCATION, LaunchArgumentsHelper.getDefaultWorkspaceLocation(wc.getName()));
		initializeProgramArguments(wc);
		initializeVMArguments(wc);
		wc.setAttribute(IPDELauncherConstants.USEFEATURES, false);
		wc.setAttribute(IPDELauncherConstants.DOCLEAR, false);
		wc.setAttribute(IPDELauncherConstants.ASKCLEAR, true);
		wc.setAttribute(IPDEConstants.APPEND_ARGS_EXPLICITLY, true);
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
		String programArgs = LaunchArgumentsHelper.getInitialProgramArguments();
		if (programArgs.length() > 0)
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, programArgs);
	}

	private void initializeVMArguments(ILaunchConfigurationWorkingCopy wc) {
		String vmArgs = LaunchArgumentsHelper.getInitialVMArguments();
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

	private void initializePluginsList(ILaunchConfigurationWorkingCopy wc) {
		StringBuffer wsplugins = new StringBuffer();
		StringBuffer explugins = new StringBuffer();
		// exclude "org.eclipse.ui.workbench.compatibility" - it is only needed for pre-3.0 bundles
		Set plugins = DependencyManager.getSelfAndDependencies(fModel, new String[] {"org.eclipse.ui.workbench.compatibility"}); //$NON-NLS-1$
		Iterator iter = plugins.iterator();
		while (iter.hasNext()) {
			String id = iter.next().toString();
			IPluginModelBase model = PluginRegistry.findModel(id);
			if (model == null || !model.isEnabled())
				continue;
			if (model.getUnderlyingResource() == null) {
				appendPlugin(explugins, model);
			} else {
				appendPlugin(wsplugins, model);
			}
		}
		wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS, wsplugins.toString());
		wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, explugins.toString());
	}

	private void appendPlugin(StringBuffer buffer, IPluginModelBase model) {
		if (buffer.length() > 0)
			buffer.append(',');
		buffer.append(model.getPluginBase().getId());
		buffer.append(BundleLauncherHelper.VERSION_SEPARATOR);
		buffer.append(model.getPluginBase().getVersion());
	}
}
