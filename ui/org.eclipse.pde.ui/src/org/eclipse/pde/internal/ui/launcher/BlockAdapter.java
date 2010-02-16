/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

public class BlockAdapter {

	private PluginBlock fPluginBlock;
	private FeatureBlock fFeatureBlock;
	private StackLayout fLayout;
	private Composite fPluginBlockComposite;
	private Composite fFeatureBlockComposite;
	private Composite fParent;
	private ILaunchConfiguration fLaunchConfig;
	private int fActiveIndex;

	// These constants MUST match the constants in PluginsTab
	private static final int CUSTOM_SELECTION = 1;
	private static final int CUSTOM_FEATURES_SELECTION = 2;

	public BlockAdapter(PluginBlock pluginBlock, FeatureBlock featureBlock) {
		Assert.isNotNull(pluginBlock);
		Assert.isNotNull(featureBlock);
		fPluginBlock = pluginBlock;
		fFeatureBlock = featureBlock;
	}

	public void createControl(Composite parent, int span, int indent) {
		fLayout = new StackLayout();
		parent.setLayout(fLayout);

		fLayout.topControl = fPluginBlockComposite;
		fParent = parent;
	}

	public void initializeFrom(ILaunchConfiguration config, boolean enableTable) throws CoreException {
		fLaunchConfig = config;
		if (fActiveIndex == CUSTOM_FEATURES_SELECTION) {
			fFeatureBlock.initializeFrom(config);
		} else {
			fPluginBlock.initializeFrom(config, enableTable);
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		if (fActiveIndex == CUSTOM_FEATURES_SELECTION) {
			fFeatureBlock.performApply(config);
		} else {
			fPluginBlock.performApply(config);
		}
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		if (fActiveIndex == CUSTOM_FEATURES_SELECTION) {
			fFeatureBlock.setDefaults(config);
		} else {
			fPluginBlock.setDefaults(config);
		}
	}

	public void enableViewer(boolean enable) {
	}

	public void dispose() {
		fPluginBlock.dispose();
		fFeatureBlock.dispose();
	}

	public void initialize(boolean enable) throws CoreException {
		if (fActiveIndex == CUSTOM_FEATURES_SELECTION) {
			fFeatureBlock.initialize();
		} else {
			fPluginBlock.initialize(enable);
		}
	}

	public void setActiveBlock(int index) throws CoreException {
		fActiveIndex = index;
		if (index == CUSTOM_FEATURES_SELECTION) {
			if (fFeatureBlockComposite == null) {
				fFeatureBlockComposite = SWTFactory.createComposite(fParent, 7, 1, GridData.FILL_BOTH, 0, 0);
				fFeatureBlock.createControl(fFeatureBlockComposite, 7, 10);
				if (fLaunchConfig != null) {
					fFeatureBlock.initializeFrom(fLaunchConfig);
				}
			}
			fLayout.topControl = fFeatureBlockComposite;
			return;
		} else if (fPluginBlockComposite == null) {
			fPluginBlockComposite = SWTFactory.createComposite(fParent, 7, 1, GridData.FILL_BOTH, 0, 0);
			fPluginBlock.createControl(fPluginBlockComposite, 7, 10);
			if (fLaunchConfig != null) {
				fPluginBlock.initializeFrom(fLaunchConfig, fActiveIndex == CUSTOM_SELECTION);
			}
		}
		fLayout.topControl = fPluginBlockComposite;

	}

}