/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.pde.launching.IPDELauncherConstants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.ConfigurationAreaBlock;
import org.eclipse.pde.internal.ui.launcher.JREBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * A launch configuration tab that displays and edits the VM install 
 * launch configuration attributes.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed.
 * </p>
 * @since 3.3
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OSGiSettingsTab extends AbstractLauncherTab {

	private JREBlock fJREBlock;
	private ConfigurationAreaBlock fConfigurationBlock;
	private Image fImage;
	private boolean fInitializing = false;

	/**
	 * Constructor
	 *
	 */
	public OSGiSettingsTab() {
		fImage = PDEPluginImages.DESC_SETTINGS_OBJ.createImage();
		fJREBlock = new JREBlock(this);
		fConfigurationBlock = new ConfigurationAreaBlock(this);
	}

	/*
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		fJREBlock.createControl(container);
		fConfigurationBlock.createControl(container);

		Dialog.applyDialogFont(container);
		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.LAUNCHER_CONFIGURATION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		fJREBlock.setDefaults(configuration);
		fConfigurationBlock.setDefaults(configuration, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			fInitializing = true;
			fJREBlock.initializeFrom(configuration);
			fConfigurationBlock.initializeFrom(configuration);
			fInitializing = false;
		} catch (CoreException e) {
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		fJREBlock.performApply(configuration);
		fConfigurationBlock.performApply(configuration);
	}

	/*
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return PDEUIMessages.EquinoxSettingsTab_name;
	}

	/*
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/*
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		if (fImage != null)
			fImage.dispose();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractLauncherTab#validateTab()
	 */
	public void validateTab() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractLauncherTab#updateLaunchConfigurationDialog()
	 */
	public void updateLaunchConfigurationDialog() {
		if (!fInitializing)
			super.updateLaunchConfigurationDialog();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 */
	public String getId() {
		return IPDELauncherConstants.TAB_OSGI_SETTINGS_ID;
	}
}
