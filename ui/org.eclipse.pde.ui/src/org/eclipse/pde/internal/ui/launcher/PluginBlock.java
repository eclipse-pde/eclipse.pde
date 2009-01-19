/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Ian Bull <irbull@cs.uvic.ca> - bug 204404
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.ui.launcher.*;

public class PluginBlock extends AbstractPluginBlock {

	protected ILaunchConfiguration fLaunchConfig;

	public PluginBlock(AbstractLauncherTab tab) {
		super(tab);
	}

	public void initializeFrom(ILaunchConfiguration config, boolean customSelection) throws CoreException {
		super.initializeFrom(config);
		if (customSelection) {
			initWorkspacePluginsState(config);
			initExternalPluginsState(config);
		} else {
			handleRestoreDefaults();
		}
		enableViewer(customSelection);
		updateCounter();
		fTab.updateLaunchConfigurationDialog();
		fLaunchConfig = config;
		handleFilterButton(); // Once the page is initialized, apply any filtering
	}

	/*
	 * if the "automatic add" option is selected, then we save the ids of plugins
	 * that have been "deselected" by the user.
	 * When we initialize the tree, we first set the workspace plugins subtree to 'checked',
	 * then we check the plugins that had been deselected and saved in the config.
	 *
	 * If the "automatic add" option is not selected, then we save the ids of plugins
	 * that were "selected" by the user.
	 * When we initialize the tree, we first set the workspace plugins subtree to 'unchecked',
	 * then we check the plugins that had been selected and saved in the config.
	 */
	protected void initWorkspacePluginsState(ILaunchConfiguration configuration) throws CoreException {
		boolean automaticAdd = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
		fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, automaticAdd);
		fNumWorkspaceChecked = automaticAdd ? fWorkspaceModels.length : 0;

		String attribute = automaticAdd ? IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS : IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS;
		Map map = BundleLauncherHelper.getWorkspaceBundleMap(configuration, null, attribute);
		Iterator iter = map.keySet().iterator();
		fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, false);
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			if (fPluginTreeViewer.setChecked(model, true)) {
				setText(model, map.get(model).toString());
			}
		}
		fNumWorkspaceChecked = map.size();
		resetGroup(fWorkspacePlugins);

		fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
		fPluginTreeViewer.setGrayed(fWorkspacePlugins, fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < fWorkspaceModels.length);
	}

	protected void initExternalPluginsState(ILaunchConfiguration configuration) throws CoreException {
		Map map = BundleLauncherHelper.getTargetBundleMap(configuration, Collections.EMPTY_SET, IPDELauncherConstants.SELECTED_TARGET_PLUGINS);
		Iterator iter = map.keySet().iterator();
		fPluginTreeViewer.setSubtreeChecked(fExternalPlugins, false);
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			if (fPluginTreeViewer.setChecked(model, true)) {
				setText(model, map.get(model).toString());
			}
		}
		fNumExternalChecked = map.size();
		resetGroup(fExternalPlugins);
		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(fExternalPlugins, fNumExternalChecked > 0 && fNumExternalChecked < fExternalModels.length);
	}

	protected void savePluginState(ILaunchConfigurationWorkingCopy config) {
		if (isEnabled()) {
			Object[] selected = fPluginTreeViewer.getCheckedElements();
			StringBuffer wBuffer = new StringBuffer();
			StringBuffer tBuffer = new StringBuffer();
			for (int i = 0; i < selected.length; i++) {
				if (selected[i] instanceof IPluginModelBase) {
					IPluginModelBase model = (IPluginModelBase) selected[i];
					if (model.getUnderlyingResource() == null) {
						appendToBuffer(tBuffer, model);
					} else {
						appendToBuffer(wBuffer, model);
					}
				}
			}
			config.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS, wBuffer.length() == 0 ? (String) null : wBuffer.toString());
			config.setAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, tBuffer.length() == 0 ? (String) null : tBuffer.toString());

			StringBuffer buffer = new StringBuffer();
			if (fAddWorkspaceButton.getSelection()) {
				for (int i = 0; i < fWorkspaceModels.length; i++) {
					if (!fPluginTreeViewer.getChecked(fWorkspaceModels[i])) {
						appendToBuffer(buffer, fWorkspaceModels[i]);
					}
				}
			}
			config.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS, buffer.length() > 0 ? buffer.toString() : (String) null);
		}
	}

	protected void computeSubset() {
		validateExtensions();
		super.computeSubset();
	}

	private void validateExtensions() {
		try {
			if (fLaunchConfig.getAttribute(IPDELauncherConstants.USE_PRODUCT, true)) {
				String product = fLaunchConfig.getAttribute(IPDELauncherConstants.PRODUCT, (String) null);
				if (product != null) {
					validateLaunchId(product);
					String application = getApplication(product);
					if (application != null)
						validateLaunchId(application);
				}
			} else {
				String configType = fLaunchConfig.getType().getIdentifier();
				String attribute = configType.equals(EclipseLaunchShortcut.CONFIGURATION_TYPE) ? IPDELauncherConstants.APPLICATION : IPDELauncherConstants.APP_TO_TEST;
				String application = fLaunchConfig.getAttribute(attribute, TargetPlatform.getDefaultApplication());
				if (!IPDEUIConstants.CORE_TEST_APPLICATION.equals(application))
					validateLaunchId(application);
			}
		} catch (CoreException e) {
		}
	}

	private void validateLaunchId(String launchId) {
		if (launchId != null) {
			int index = launchId.lastIndexOf('.');
			if (index > 0) {
				String pluginId = launchId.substring(0, index);
				// see if launcher plugin is already included
				IPluginModelBase base = findPlugin(pluginId);
				if (base == null) {
					base = PluginRegistry.findModel(pluginId);
					if (base != null) {
						fPluginTreeViewer.setChecked(base, true);
					}
				}
			}
		}
	}

	private String getApplication(String product) {
		String bundleID = product.substring(0, product.lastIndexOf('.'));
		IPluginModelBase model = findPlugin(bundleID);

		if (model != null) {
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				IPluginExtension ext = extensions[i];
				String point = ext.getPoint();
				if ("org.eclipse.core.runtime.products".equals(point) //$NON-NLS-1$
						&& product.equals(IdUtil.getFullId(ext))) {
					if (ext.getChildCount() == 1) {
						IPluginElement prod = (IPluginElement) ext.getChildren()[0];
						if (prod.getName().equals("product")) { //$NON-NLS-1$
							IPluginAttribute attr = prod.getAttribute("application"); //$NON-NLS-1$
							return attr != null ? attr.getValue() : null;
						}
					}
				}
			}
		}
		return null;
	}

	protected LaunchValidationOperation createValidationOperation() {
		return new EclipsePluginValidationOperation(fLaunchConfig);
	}

}
