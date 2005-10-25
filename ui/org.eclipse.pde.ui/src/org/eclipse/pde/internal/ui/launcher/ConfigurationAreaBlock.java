/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class ConfigurationAreaBlock extends BaseBlock {

	private Button fUseDefaultLocationButton;
	private Button fClearConfig;
	private String fLastEnteredConfigArea;
	private String fConfigName;
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
					fLocationText.setText(DEFAULT_DIR + fConfigName);
				else
					fLocationText.setText(fLastEnteredConfigArea);
				enableBrowseSection(!useDefaultArea);
			}
		});

		createText(group, PDEUIMessages.ConfigurationTab_configLog, 20);
		
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
		
		createButtons(buttons);
	}
	
	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		fConfigName = configuration.getName();
		boolean useDefaultArea = configuration.getAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, true);
		fUseDefaultLocationButton.setSelection(useDefaultArea);
		enableBrowseSection(!useDefaultArea);
		
		fClearConfig.setSelection(configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false));
		fLastEnteredConfigArea = configuration.getAttribute(IPDELauncherConstants.CONFIG_LOCATION, ""); //$NON-NLS-1$

		if (useDefaultArea)
			fLocationText.setText(DEFAULT_DIR + fConfigName);
		else
			fLocationText.setText(fLastEnteredConfigArea);
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, fUseDefaultLocationButton.getSelection());
		if (!fUseDefaultLocationButton.getSelection()) {
			fLastEnteredConfigArea = getLocation();
			configuration.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, fLastEnteredConfigArea);
		} else {
			configuration.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, PDEUIMessages.ConfigurationAreaBlock_0);
		}
		configuration.setAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, fClearConfig.getSelection());
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration, boolean isJUnit) {		
		configuration.setAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, !isJUnit);
		configuration.setAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, isJUnit);
		if (isJUnit) {
			configuration.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, 
										DEFAULT_DIR + "pde-junit");  //$NON-NLS-1$
		}
	}
	
	protected String getName() {
		return PDEUIMessages.ConfigurationAreaBlock_name;
	}
	
}
