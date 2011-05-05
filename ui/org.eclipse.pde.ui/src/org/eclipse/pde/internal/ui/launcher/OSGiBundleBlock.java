/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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

import java.util.*;
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

	protected void savePluginState(ILaunchConfigurationWorkingCopy config) {
		Object[] selected = fPluginTreeViewer.getCheckedElements();

		PluginModelNameBuffer wBuffer = new PluginModelNameBuffer();
		PluginModelNameBuffer tBuffer = new PluginModelNameBuffer();

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
		Map map = BundleLauncherHelper.getTargetBundleMap(configuration, Collections.EMPTY_SET, IPDELauncherConstants.TARGET_BUNDLES);
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

	private void initWorkspacePluginsState(ILaunchConfiguration configuration) throws CoreException {
		Map map = BundleLauncherHelper.getWorkspaceBundleMap(configuration);
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

	protected LaunchValidationOperation createValidationOperation() {
		return new OSGiValidationOperation(fLaunchConfiguration);
	}

}
