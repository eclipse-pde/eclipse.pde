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
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

public class BlockAdapter {

	private AbstractPluginBlock fPluginBlock;
	private FeatureBlock fFeatureBlock;
	private StackLayout fLayout;
	private Composite fPluginBlockComposite;
	private Composite fFeatureBlockComposite;
	private Composite fParent;
	private ILaunchConfiguration fLaunchConfig;
	private int fActiveIndex;
	private int fSpan = 1;
	private int fIndent = 0;

	// These constants MUST match the constants in PluginsTab
	private static final int PLUGINS_BLOCK = 1; //CUSTOM_SELECTION
	private static final int FEATURES_BLOCK = 2; //CUSTOM_FEATURES_SELECTION

	public BlockAdapter(AbstractPluginBlock pluginBlock, FeatureBlock featureBlock) {
		Assert.isNotNull(pluginBlock);
		Assert.isNotNull(featureBlock);
		fPluginBlock = pluginBlock;
		fFeatureBlock = featureBlock;
	}

	public void createControl(Composite parent, int span, int indent) {
		Composite blockComposite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		fSpan = span;
		fIndent = indent;
		fLayout = new StackLayout();
		blockComposite.setLayout(fLayout);

		fLayout.topControl = fPluginBlockComposite;
		fParent = blockComposite;
	}

	public void initializeFrom(ILaunchConfiguration config, boolean enableTable) throws CoreException {
		fLaunchConfig = config;
		if (fActiveIndex == FEATURES_BLOCK) {
			fFeatureBlock.initializeFrom(config);
		} else {
			fPluginBlock.initializeFrom(config, enableTable);
		}
	}

	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		fLaunchConfig = config;
		if (fActiveIndex == FEATURES_BLOCK) {
			fFeatureBlock.initializeFrom(config);
		} else {
			if (fPluginBlock instanceof OSGiBundleBlock)
				((OSGiBundleBlock) fPluginBlock).initializeFrom(config);
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		if (fActiveIndex == FEATURES_BLOCK) {
			fFeatureBlock.performApply(config);
		} else {
			fPluginBlock.performApply(config);
		}
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		if (fActiveIndex == FEATURES_BLOCK) {
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
		if (fActiveIndex == FEATURES_BLOCK) {
			fFeatureBlock.initialize();
		} else {
			if (fPluginBlock instanceof PluginBlock) {
				((PluginBlock) fPluginBlock).initialize(enable);
			}
		}
	}

	public void setActiveBlock(int index) {
		try {
			if (index == FEATURES_BLOCK) {
				fPluginBlock.setVisible(false);
				if (fFeatureBlockComposite == null) {
					fFeatureBlockComposite = SWTFactory.createComposite(fParent, 7, 1, GridData.FILL_BOTH, 0, 0);
					fFeatureBlock.createControl(fFeatureBlockComposite, 6, 10);
					if (fLaunchConfig != null) {
						fFeatureBlock.initializeFrom(fLaunchConfig);
					}
				}
				fLayout.topControl = fFeatureBlockComposite;
			} else {
				fFeatureBlock.setVisible(false);
				if (fActiveIndex != index) {
					fPluginBlock.setVisible(false);
				}
				if (fPluginBlockComposite == null) {
					fPluginBlockComposite = SWTFactory.createComposite(fParent, fSpan, 1, GridData.FILL_BOTH, 0, 0);
					fPluginBlock.createControl(fPluginBlockComposite, fSpan, fIndent);
					if (fLaunchConfig != null) {
						if (fPluginBlock instanceof PluginBlock) {
							fPluginBlock.initializeFrom(fLaunchConfig, index == PLUGINS_BLOCK);
						} else if (fPluginBlock instanceof OSGiBundleBlock) {
							((OSGiBundleBlock) fPluginBlock).initializeFrom(fLaunchConfig);
						}
					}
				}
				fLayout.topControl = fPluginBlockComposite;
			}
			fActiveIndex = index;
		} catch (CoreException ex) {
			PDEPlugin.log(ex);
		}

	}
}