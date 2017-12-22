/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ian Bull <irbull@cs.uvic.ca> - bug 204404 and bug 207064
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
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
		Object[] selected = fPluginTreeViewer.getCheckedElements();

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
				if (!fPluginTreeViewer.getChecked(workspaceModels[i])) {
					buffer.add(workspaceModels[i]);
				}
			}
		}
		config.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS, buffer.toString());
	}

	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		super.initializeFrom(configuration, true);
		initWorkspacePluginsState(configuration);
		initExternalPluginsState(configuration);
		updateCounter();
		fLaunchConfiguration = configuration;
		handleFilterButton(); // Once the page is initialized, apply any filtering.
	}

	// TODO deal with the discrepency between save/init states of the two blocks

	private void initExternalPluginsState(ILaunchConfiguration configuration) throws CoreException {
		Map<IPluginModelBase, String> map = BundleLauncherHelper.getTargetBundleMap(configuration,
				Collections.EMPTY_SET, IPDELauncherConstants.TARGET_BUNDLES);
		fPluginTreeViewer.setSubtreeChecked(fExternalPlugins, false);
		for (Entry<IPluginModelBase, String> entry : map.entrySet()) {
			IPluginModelBase model = entry.getKey();
			if (fPluginTreeViewer.setChecked(model, true)) {
				setText(model, entry.getValue().toString());
			}
		}
		fNumExternalChecked = map.size();
		resetGroup(fExternalPlugins);
		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(fExternalPlugins, fNumExternalChecked > 0 && fNumExternalChecked < getExternalModels().length);
	}

	private void initWorkspacePluginsState(ILaunchConfiguration configuration) throws CoreException {
		Map<IPluginModelBase, String> map = BundleLauncherHelper.getWorkspaceBundleMap(configuration);
		fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, false);
		for (Entry<IPluginModelBase, String> entry : map.entrySet()) {
			IPluginModelBase model = entry.getKey();
			if (fPluginTreeViewer.setChecked(model, true)) {
				setText(model, entry.getValue().toString());
			}
		}
		fNumWorkspaceChecked = map.size();
		resetGroup(fWorkspacePlugins);

		fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
		fPluginTreeViewer.setGrayed(fWorkspacePlugins, fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < getWorkspaceModels().length);
	}

	@Override
	protected LaunchValidationOperation createValidationOperation() {
		return new OSGiValidationOperation(fLaunchConfiguration);
	}

}
