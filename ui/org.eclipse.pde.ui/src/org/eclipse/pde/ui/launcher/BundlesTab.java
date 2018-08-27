/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.*;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * A launch configuration tab that customizes the list of bundles to launch with,
 * their start level and their auto-start attributes.
 * <p>
 * This class may be instantiated, but it is not intended to be subclassed by clients.
 * </p>
 * @since 3.3
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class BundlesTab extends AbstractLauncherTab {

	private Image fImage;
	private BlockAdapter fBlock;
	OSGiFrameworkBlock fFrameworkBlock;

	private static final int PLUGIN_SELECTION = 1;
	private static final int FEATURE_SELECTION = 2;

	/**
	 * Constructor to create a new bundle tab
	 */
	public BundlesTab() {
		fImage = PDEPluginImages.DESC_PLUGINS_FRAGMENTS.createImage();
		fBlock = new BlockAdapter(new OSGiBundleBlock(this), new FeatureBlock(this));
		fFrameworkBlock = new OSGiFrameworkBlock(this, fBlock);
	}

	/**
	 * Dispose images
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	@Override
	public void dispose() {
		fBlock.dispose();
		fImage.dispose();
		super.dispose();
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		fFrameworkBlock.createControl(composite);
		fBlock.createControl(composite, 2, 5);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LAUNCHER_ADVANCED);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			int index = PLUGIN_SELECTION;
			if (config.getAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, false)) {
				index = FEATURE_SELECTION;
			} else {
				index = PLUGIN_SELECTION;
			}
			fBlock.setActiveBlock(index);
			fFrameworkBlock.initializeFrom(config);
			fBlock.initializeFrom(config);
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		fBlock.setDefaults(config);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		fFrameworkBlock.performApply(config);
		fBlock.performApply(config);
	}

	@Override
	public String getName() {
		return PDEUIMessages.BundlesTab_title;
	}

	@Override
	public Image getImage() {
		return fImage;
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy config) {
	}

	@Override
	public void validateTab() {
	}

	@Override
	public String getId() {
		return IPDELauncherConstants.TAB_BUNDLES_ID;
	}
}
