/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * A launch configuration tab that displays and edits the configuration area
 * location and template for a PDE launch configuration.
 * <p>
 * This class may be instantiated, but it is not intended to be subclassed by clients.
 * </p>
 * @since 3.2
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ConfigurationTab extends AbstractLauncherTab implements IPDELauncherConstants {

	private ConfigurationAreaBlock fConfigurationArea;
	private ConfigurationTemplateBlock fTemplateArea;
	private SoftwareInstallBlock fSoftwareInstallArea;
	private Image fImage;
	private boolean fJUnitConfig;

	/**
	 * Constructor.  Equivalent to ConfigurationTab(false).
	 * 
	 * @see #ConfigurationTab(boolean)
	 */
	public ConfigurationTab() {
		this(false);
	}

	/**
	 * Constructor
	 * 
	 * @param isJUnitConfig  a flag to indicate if the tab is to be used with a Plug-in JUnit launch configuration.
	 */
	public ConfigurationTab(boolean isJUnitConfig) {
		fImage = PDEPluginImages.DESC_SETTINGS_OBJ.createImage();
		fConfigurationArea = new ConfigurationAreaBlock(this);
		fTemplateArea = new ConfigurationTemplateBlock(this);
		fSoftwareInstallArea = new SoftwareInstallBlock(this);
		fJUnitConfig = isJUnitConfig;
	}

	/*
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		fConfigurationArea.createControl(container);
		fTemplateArea.createControl(container);
		fSoftwareInstallArea.createControl(container);

		Dialog.applyDialogFont(container);
		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.LAUNCHER_CONFIGURATION);
	}

	/*
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		fConfigurationArea.setDefaults(configuration, fJUnitConfig);
		fTemplateArea.setDefaults(configuration);
		fSoftwareInstallArea.setDefaults(configuration);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			fConfigurationArea.initializeFrom(configuration);
			fTemplateArea.initializeFrom(configuration);
			fSoftwareInstallArea.initializeFrom(configuration);
		} catch (CoreException e) {
		}
	}

	/*
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		fConfigurationArea.performApply(configuration);
		fTemplateArea.performApply(configuration);
		fSoftwareInstallArea.performApply(configuration);
	}

	/*
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return PDEUIMessages.ConfigurationTab_name;
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

	/**
	 * Validates the page and flags an error if the configuration area
	 * location or the configuration template location does not exist.
	 * 
	 * @see org.eclipse.pde.ui.launcher.AbstractLauncherTab#validateTab()
	 */
	public void validateTab() {
		String error = fConfigurationArea.validate();
		if (error == null)
			error = fTemplateArea.validate();
		if (error == null) {
			error = fSoftwareInstallArea.validate();
		}
		setErrorMessage(error);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 */
	public String getId() {
		return IPDELauncherConstants.TAB_CONFIGURATION_ID;
	}
}
