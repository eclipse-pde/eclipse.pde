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

	/**
	 * The configuration this block is currently displaying or <code>null</code> if none set
	 */
	protected ILaunchConfiguration fLaunchConfig;
	/**
	 * Whether the controls have been initialized for fLaunchConfig
	 */
	protected boolean fIsEnabled = false;

	/**
	 * Flag for when the combo changed.  We improve the performance of the config by waiting until the combo changed
	 * to initialize the contents of the table.
	 */
	protected boolean fInitDefaultCheckState = false;

	public PluginBlock(AbstractLauncherTab tab) {
		super(tab);
	}

	public void initializeFrom(ILaunchConfiguration config, boolean enable) throws CoreException {

		if (fLaunchConfig != null && fLaunchConfig.equals(config) && fIsEnabled == enable) {
			// Do nothing
			return;
		}

		fLaunchConfig = config;
		fIsEnabled = enable;

		if (enable) {
			super.initializeFrom(config);
			initWorkspacePluginsState(config);
			initExternalPluginsState(config);
			handleFilterButton(); // Once the page is initialized, apply any filtering
		} else {
			super.initializeFrom(null);
		}
		enableViewer(enable);
		updateCounter();
		fTab.updateLaunchConfigurationDialog();
	}

	/**
	 * Refresh the enable state of this block using the current launch config
	 * @param enable
	 * @throws CoreException
	 */
	public void initialize(boolean enable) throws CoreException {
		// To support lazy loading of the table we need to set some launch configuration attributes when the combo changes
		if (fLaunchConfig != null) {
			fInitDefaultCheckState = enable && !fLaunchConfig.hasAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS) && !fLaunchConfig.hasAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS);
			fTab.updateLaunchConfigurationDialog();
		}
		initializeFrom(fLaunchConfig, enable);
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
		Map map = BundleLauncherHelper.getWorkspaceBundleMap(configuration, null, IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS);
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
		fPluginTreeViewer.setGrayed(fWorkspacePlugins, fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < getWorkspaceModels().length);
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
		fPluginTreeViewer.setGrayed(fExternalPlugins, fNumExternalChecked > 0 && fNumExternalChecked < getExternalModels().length);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.launcher.AbstractPluginBlock#isEnabled()
	 */
	protected boolean isEnabled() {
		return fIsEnabled;
	}

	protected void savePluginState(ILaunchConfigurationWorkingCopy config) {
		// If the table is populated, store what is checked.  If we are lazy loading and need to init, store the default checkstate
		if (isEnabled() || fInitDefaultCheckState) {
			StringBuffer wBuffer = new StringBuffer();
			StringBuffer tBuffer = new StringBuffer();

			// If this is the first time the table is enabled, default the checkstate to all workspace plug-ins
			if (fInitDefaultCheckState) {
				TreeSet checkedWorkspace = new TreeSet();
				IPluginModelBase[] workspaceModels = getWorkspaceModels();
				for (int i = 0; i < workspaceModels.length; i++) {
					String id = workspaceModels[i].getPluginBase().getId();
					if (id != null) {
						checkedWorkspace.add(id);
					}
				}

				IPluginModelBase[] externalModels = getExternalModels();
				for (int i = 0; i < externalModels.length; i++) {
					IPluginModelBase model = externalModels[i];
					boolean masked = checkedWorkspace.contains(model.getPluginBase().getId());
					if (masked) {
						appendToBuffer(wBuffer, model);
					} else if (model.isEnabled()) {
						appendToBuffer(tBuffer, model);
					}
				}
				fInitDefaultCheckState = false;
				// If we have checked elements, save them to the config
			} else {
				Object[] selected = fPluginTreeViewer.getCheckedElements();
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

			}
			config.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS, wBuffer.length() == 0 ? (String) null : wBuffer.toString());
			config.setAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, tBuffer.length() == 0 ? (String) null : tBuffer.toString());

			StringBuffer buffer = new StringBuffer();
			if (fAddWorkspaceButton.getSelection()) {
				IPluginModelBase[] workspaceModels = getWorkspaceModels();
				for (int i = 0; i < workspaceModels.length; i++) {
					if (!fPluginTreeViewer.getChecked(workspaceModels[i])) {
						appendToBuffer(buffer, workspaceModels[i]);
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
