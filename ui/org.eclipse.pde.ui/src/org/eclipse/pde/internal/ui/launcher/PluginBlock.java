/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.launching.launcher.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;

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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.launcher.AbstractPluginBlock#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration, boolean)
	 */
	public void initializeFrom(ILaunchConfiguration config, boolean enableTable) throws CoreException {

		if (fLaunchConfig != null && fLaunchConfig.equals(config) && fIsEnabled == enableTable) {
			// Do nothing
			return;
		}

		fLaunchConfig = config;
		fIsEnabled = enableTable;

		super.initializeFrom(config, enableTable);

		if (enableTable) {
			initWorkspacePluginsState(config);
			initExternalPluginsState(config);
			handleFilterButton(); // Once the page is initialized, apply any filtering

			// If the workspace plug-in state has changed (project closed, etc.) the launch config needs to be updated without making the tab dirty
			if (fLaunchConfig.isWorkingCopy()) {
				savePluginState((ILaunchConfigurationWorkingCopy) fLaunchConfig);
			}
		}

		enableViewer(enableTable);
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
			initializeFrom(fLaunchConfig, enable);
		}
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
			PluginModelNameBuffer wBuffer = new PluginModelNameBuffer();
			PluginModelNameBuffer tBuffer = new PluginModelNameBuffer();

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
						wBuffer.add(model);
					} else if (model.isEnabled()) {
						tBuffer.add(model);
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
							tBuffer.add(model);
						} else {
							wBuffer.add(model);
						}
					}
				}

			}
			config.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS, wBuffer.toString());
			config.setAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, tBuffer.toString());

			PluginModelNameBuffer buffer = new PluginModelNameBuffer();
			if (fAddWorkspaceButton.getSelection()) {
				IPluginModelBase[] workspaceModels = getWorkspaceModels();
				for (int i = 0; i < workspaceModels.length; i++) {
					if (!fPluginTreeViewer.getChecked(workspaceModels[i])) {
						buffer.add(workspaceModels[i]);
					}
				}
			}
			config.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS, buffer.toString());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.launcher.AbstractPluginBlock#addRequiredPlugins()
	 */
	protected void addRequiredPlugins() {
		// Check that the application or product we are launching has its requirements included
		try {
			String[] requiredIds = RequirementHelper.getApplicationRequirements(fLaunchConfig);
			for (int i = 0; i < requiredIds.length; i++) {
				// see if launcher plugin is already included
				IPluginModelBase base = findPlugin(requiredIds[i]);
				if (base == null) {
					base = PluginRegistry.findModel(requiredIds[i]);
					if (base != null) {
						fPluginTreeViewer.setChecked(base, true);
					}
				}
			}
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
		super.addRequiredPlugins();
	}

	protected LaunchValidationOperation createValidationOperation() {
		return new EclipsePluginValidationOperation(fLaunchConfig);
	}

}
