/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class ConfigurationAreaBlock extends BaseBlock {

	private Button fUseDefaultLocationButton;
	private Button fClearConfig;
	private String fLastEnteredConfigArea;
	private String fLastKnownConfigName;
	private static String DEFAULT_DIR = "${workspace_loc}/.metadata/.plugins/org.eclipse.pde.core/"; //$NON-NLS-1$

	public ConfigurationAreaBlock(AbstractLauncherTab tab) {
		super(tab);
	}

	public void createControl(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.ConfigurationTab_configAreaGroup);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fUseDefaultLocationButton = new Button(group, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fUseDefaultLocationButton.setLayoutData(gd);
		fUseDefaultLocationButton.setText(PDEUIMessages.ConfigurationTab_useDefaultLoc);
		fUseDefaultLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean useDefaultArea = fUseDefaultLocationButton.getSelection();
				if (useDefaultArea)
					fLocationText.setText(DEFAULT_DIR + fLastKnownConfigName);
				else
					fLocationText.setText(fLastEnteredConfigArea);
				enableBrowseSection(!useDefaultArea);
				fLocationText.setEditable(!useDefaultArea);
				if (useDefaultArea)
					fLocationText.setEnabled(true);
			}
		});

		createText(group, PDEUIMessages.ConfigurationTab_configLog, 20);
		fLocationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fUseDefaultLocationButton.getSelection()) {
					// As the user types, save the text and restore it if default button is toggled
					fLastEnteredConfigArea = getLocation();
				}
			}
		});

		Composite buttons = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginHeight = layout.marginWidth = 0;
		buttons.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		buttons.setLayoutData(gd);

		fClearConfig = new Button(buttons, SWT.CHECK);
		fClearConfig.setText(PDEUIMessages.ConfigurationTab_clearArea);
		fClearConfig.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fClearConfig.addSelectionListener(fListener);

		createButtons(buttons, new String[] {PDEUIMessages.BaseBlock_workspace, PDEUIMessages.BaseBlock_filesystem, PDEUIMessages.BaseBlock_variables});
	}

	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		fLastKnownConfigName = configuration.getName();
		boolean useDefaultArea = configuration.getAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, true);
		fUseDefaultLocationButton.setSelection(useDefaultArea);
		enableBrowseSection(!useDefaultArea);
		fLocationText.setEditable(!useDefaultArea);
		if (useDefaultArea)
			fLocationText.setEnabled(true);

		fClearConfig.setSelection(configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false));

		if (useDefaultArea) {
			fLastEnteredConfigArea = DEFAULT_DIR + fLastKnownConfigName;
		} else {
			// If no attribute is set, use the default area instead of an empty string to avoid deleting the home directory, bug 356853
			fLastEnteredConfigArea = configuration.getAttribute(IPDELauncherConstants.CONFIG_LOCATION, DEFAULT_DIR + fLastKnownConfigName);
		}
		fLocationText.setText(fLastEnteredConfigArea);
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, fUseDefaultLocationButton.getSelection());

		// Check if the default area has changed because the launch config name changed
		if (fUseDefaultLocationButton.getSelection() && !fLastKnownConfigName.equals(configuration.getName())) {
			fLocationText.setText(DEFAULT_DIR + configuration.getName());
		}
		configuration.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, getLocation());
		configuration.setAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, fClearConfig.getSelection());
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration, boolean isJUnit) {
		configuration.setAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, isJUnit);

		boolean useDefaultArea = !isJUnit || LaunchArgumentsHelper.getDefaultJUnitWorkspaceIsContainer();
		configuration.setAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, useDefaultArea);

		String location;
		if (isJUnit && !useDefaultArea) {
			location = LaunchArgumentsHelper.getDefaultJUnitConfigurationLocation();
		} else {
			location = DEFAULT_DIR + configuration.getName();
		}
		configuration.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, location);
	}

	protected String getName() {
		return PDEUIMessages.ConfigurationAreaBlock_name;
	}

	protected boolean isFile() {
		return false;
	}

	public String validate() {
		if (fUseDefaultLocationButton.getSelection())
			return null;
		return super.validate();
	}
}
