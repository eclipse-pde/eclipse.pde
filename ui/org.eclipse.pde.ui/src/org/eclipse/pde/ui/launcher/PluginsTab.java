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
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.PluginBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class PluginsTab extends AbstractLauncherTab {

	private Button fUseDefaultRadio;
	private Button fUseFeaturesRadio;
	private Button fUseListRadio;
	private Image fImage;
	private boolean fShowFeatures = true;
	private PluginBlock fPluginBlock;
	private Listener fListener = new Listener();
	
	class Listener extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			fPluginBlock.enableViewer(fUseListRadio.getSelection());
			updateLaunchConfigurationDialog();
		}
	}

	public PluginsTab() {
		this(true);
	}

	public PluginsTab(boolean showFeatures) {
		fShowFeatures = showFeatures;
		fImage = PDEPluginImages.DESC_REQ_PLUGINS_OBJ.createImage();
		fPluginBlock = new PluginBlock(this);
	}

	public void dispose() {
		fPluginBlock.dispose();
		fImage.dispose();
		super.dispose();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		fUseDefaultRadio = new Button(composite, SWT.RADIO);
		fUseDefaultRadio.setText(PDEUIMessages.AdvancedLauncherTab_useDefault);
		fUseDefaultRadio.addSelectionListener(fListener);

		if (fShowFeatures) {
			fUseFeaturesRadio = new Button(composite, SWT.RADIO);
			fUseFeaturesRadio.setText(PDEUIMessages.AdvancedLauncherTab_useFeatures); 
			fUseFeaturesRadio.addSelectionListener(fListener);
		}

		fUseListRadio = new Button(composite, SWT.RADIO);
		fUseListRadio.setText(PDEUIMessages.AdvancedLauncherTab_useList); 
		fUseListRadio.addSelectionListener(fListener);

		fPluginBlock.createControl(composite);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LAUNCHER_ADVANCED);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			fUseDefaultRadio.setSelection(config.getAttribute(IPDELauncherConstants.USE_DEFAULT, true));
			if (fShowFeatures) {
				fUseFeaturesRadio.setSelection(config.getAttribute(IPDELauncherConstants.USEFEATURES, false));
				fUseListRadio.setSelection(!fUseDefaultRadio.getSelection()
											&& !fUseFeaturesRadio.getSelection());
			} else {
				fUseListRadio.setSelection(!fUseDefaultRadio.getSelection());
			}
			fPluginBlock.initializeFrom(config, fUseDefaultRadio.getSelection());
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.USE_DEFAULT, true);
		if (fShowFeatures)
			config.setAttribute(IPDELauncherConstants.USEFEATURES, false);
		fPluginBlock.setDefaults(config);
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.USE_DEFAULT, fUseDefaultRadio.getSelection());
		if (fShowFeatures)
			config.setAttribute(IPDELauncherConstants.USEFEATURES, fUseFeaturesRadio.getSelection());
		fPluginBlock.performApply(config);
	}

	public String getName() {
		return PDEUIMessages.AdvancedLauncherTab_name; 
	}

	public Image getImage() {
		return fImage;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void activated(ILaunchConfigurationWorkingCopy config) {
		fPluginBlock.activated(config, !fShowFeatures);
	}

	public void validatePage() {
		String errorMessage = null;
		if (fShowFeatures && fUseFeaturesRadio.getSelection()) {
			IPath workspacePath = PDEPlugin.getWorkspace().getRoot().getLocation();
			IPath featurePath = workspacePath.removeLastSegments(1).append("features"); //$NON-NLS-1$
			if (!workspacePath.lastSegment().equalsIgnoreCase("plugins") //$NON-NLS-1$
				|| !featurePath.toFile().exists())
				errorMessage = PDEUIMessages.AdvancedLauncherTab_error_featureSetup; 
		} 
		setErrorMessage(errorMessage);
	}

}
