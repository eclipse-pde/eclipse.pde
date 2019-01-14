/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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

	@Override
	public void initializeFrom(ILaunchConfiguration config, boolean enableTable) throws CoreException {

		if (fLaunchConfig != null && fLaunchConfig.equals(config) && fIsEnabled == enableTable) {
			// Do nothing
			return;
		}

		fLaunchConfig = config;
		fIsEnabled = enableTable;

		super.initializeFrom(config, enableTable);

		if (enableTable) {
			initializePluginsState(config);

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

	private void initializePluginsState(ILaunchConfiguration config) throws CoreException {
		Map<IPluginModelBase, String> selected = new HashMap<>();
		selected.putAll(BundleLauncherHelper.getWorkspaceBundleMap(config, null,
				IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS));
		selected.putAll(
				BundleLauncherHelper.getTargetBundleMap(config, null, IPDELauncherConstants.SELECTED_TARGET_PLUGINS));

		initializePluginsState(selected);
	}

	@Override
	protected boolean isEnabled() {
		return fIsEnabled;
	}

	@Override
	protected void savePluginState(ILaunchConfigurationWorkingCopy config) {
		// If the table is populated, store what is checked.  If we are lazy loading and need to init, store the default checkstate
		if (isEnabled() || fInitDefaultCheckState) {
			PluginModelNameBuffer wBuffer = new PluginModelNameBuffer();
			PluginModelNameBuffer tBuffer = new PluginModelNameBuffer();

			if (fInitDefaultCheckState) {
				// If this is the first time the table is enabled, default the checkstate to all workspace plug-ins
				TreeSet<String> checkedWorkspace = new TreeSet<>();
				IPluginModelBase[] workspaceModels = getWorkspaceModels();
				for (IPluginModelBase workspaceModel : workspaceModels) {
					String id = workspaceModel.getPluginBase().getId();
					if (id != null) {
						wBuffer.add(workspaceModel);
						checkedWorkspace.add(id);
					}
				}

				IPluginModelBase[] externalModels = getExternalModels();
				for (IPluginModelBase model : externalModels) {
					// If there is a workspace bundle with the same id, don't check the external version
					if (!checkedWorkspace.contains(model.getPluginBase().getId()) && model.isEnabled()) {
						tBuffer.add(model);
					}
				}
				fInitDefaultCheckState = false;
			} else {
				// If we have checked elements, save them to the config
				Object[] selected = fPluginTreeViewer.getCheckedLeafElements();
				for (Object selectedElement : selected) {
					if (selectedElement instanceof IPluginModelBase) {
						IPluginModelBase model = (IPluginModelBase) selectedElement;
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
					if (!fPluginTreeViewer.isCheckedLeafElement(workspaceModels[i])) {
						buffer.add(workspaceModels[i]);
					}
				}
			}
			config.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS, buffer.toString());
		}
	}

	@Override
	protected void addRequiredPlugins() {
		// Check that the application or product we are launching has its requirements included
		try {
			String[] requiredIds = RequirementHelper.getApplicationRequirements(fLaunchConfig);
			for (String requiredId : requiredIds) {
				// see if launcher plugin is already included
				IPluginModelBase base = findPlugin(requiredId);
				if (base == null) {
					base = PluginRegistry.findModel(requiredId);
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

	@Override
	protected LaunchValidationOperation createValidationOperation() {
		return new EclipsePluginValidationOperation(fLaunchConfig);
	}

}
