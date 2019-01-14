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
 *     Ian Bull <irbull@cs.uvic.ca> - bug 204404 and bug 207064
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.launching.launcher.*;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.BundlesTab;

public class OSGiBundleBlock extends AbstractPluginBlock {

	private ILaunchConfiguration fLaunchConfiguration;

	public OSGiBundleBlock(BundlesTab tab) {
		super(tab);
	}

	@Override
	protected void savePluginState(ILaunchConfigurationWorkingCopy config) {
		Object[] selected = fPluginTreeViewer.getCheckedLeafElements();

		PluginModelNameBuffer wBuffer = new PluginModelNameBuffer();
		PluginModelNameBuffer tBuffer = new PluginModelNameBuffer();

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

		config.setAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES, wBuffer.toString());
		config.setAttribute(IPDELauncherConstants.TARGET_BUNDLES, tBuffer.toString());

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

	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		super.initializeFrom(configuration, true);
		initializePluginsState(configuration);
		fLaunchConfiguration = configuration;
	}

	// TODO deal with the discrepency between save/init states of the two blocks

	private void initializePluginsState(ILaunchConfiguration configuration) throws CoreException {
		Map<IPluginModelBase, String> selected = new HashMap<>();
		selected.putAll(BundleLauncherHelper.getWorkspaceBundleMap(configuration));
		selected.putAll(
				BundleLauncherHelper.getTargetBundleMap(configuration, null, IPDELauncherConstants.TARGET_BUNDLES));

		initializePluginsState(selected);
	}

	@Override
	protected LaunchValidationOperation createValidationOperation() {
		return new OSGiValidationOperation(fLaunchConfiguration);
	}

}
