/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.PluginBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * A launch configuration tab that displays the different self-hosting modes,
 * and lets the user customize the list of plug-ins to launch with.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed by clients.
 * </p>
 * @since 3.2
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class PluginsTab extends AbstractLauncherTab {

	private Image fImage;

	private boolean fShowFeatures = true;
	private Combo fSelectionCombo;
	private PluginBlock fPluginBlock;
	private Combo fDefaultAutoStart;
	private Spinner fDefaultStartLevel;
	private Listener fListener;

	private static final int DEFAULT_SELECTION = 0;
	private static final int CUSTOM_SELECTION = 1;
	private static final int FEATURE_SELECTION = 2;

	class Listener extends SelectionAdapter implements ModifyListener {
		public void widgetSelected(SelectionEvent e) {
			int index = fSelectionCombo.getSelectionIndex();
			fPluginBlock.enableViewer(index == CUSTOM_SELECTION);
			updateLaunchConfigurationDialog();
		}

		public void modifyText(ModifyEvent e) {
			updateLaunchConfigurationDialog();
		}
	}

	/**
	 * Constructor. Equivalent to PluginsTab(true).
	 * 
	 * @see #PluginsTab(boolean)
	 *
	 */
	public PluginsTab() {
		this(true);
	}

	/**
	 * Constructor
	 * 
	 * @param showFeatures  a flag indicating if the tab should present the feature-based 
	 * self-hosting option.
	 */
	public PluginsTab(boolean showFeatures) {
		fShowFeatures = showFeatures;
		fImage = PDEPluginImages.DESC_PLUGINS_FRAGMENTS.createImage();
		fPluginBlock = new PluginBlock(this);
		fListener = new Listener();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		fPluginBlock.dispose();
		fImage.dispose();
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(7, false));

		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEUIMessages.PluginsTab_launchWith);

		fSelectionCombo = new Combo(composite, SWT.READ_ONLY | SWT.BORDER);
		fSelectionCombo.setItems(new String[] {PDEUIMessages.PluginsTab_allPlugins, PDEUIMessages.PluginsTab_selectedPlugins, PDEUIMessages.PluginsTab_featureMode});
		fSelectionCombo.setText(fSelectionCombo.getItem(0));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.minimumWidth = 200;
		fSelectionCombo.setLayoutData(gd);
		fSelectionCombo.addSelectionListener(fListener);

		label = new Label(composite, SWT.NONE);
		gd = new GridData();
		gd.horizontalIndent = 20;
		label.setLayoutData(gd);
		label.setText(PDEUIMessages.EquinoxPluginsTab_defaultStart);

		fDefaultStartLevel = new Spinner(composite, SWT.BORDER);
		fDefaultStartLevel.setMinimum(1);
		fDefaultStartLevel.addModifyListener(fListener);

		label = new Label(composite, SWT.NONE);
		gd = new GridData();
		gd.horizontalIndent = 20;
		label.setLayoutData(gd);
		label.setText(PDEUIMessages.EquinoxPluginsTab_defaultAuto);

		fDefaultAutoStart = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		fDefaultAutoStart.setItems(new String[] {Boolean.toString(true), Boolean.toString(false)});
		fDefaultAutoStart.select(0);
		fDefaultAutoStart.addSelectionListener(fListener);

		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 7;
		label.setLayoutData(gd);

		fPluginBlock.createControl(composite, 7, 10);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LAUNCHER_ADVANCED);
	}

	/*
	 * (non-Javadoc) 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			int index = DEFAULT_SELECTION;
			if (fShowFeatures && configuration.getAttribute(IPDELauncherConstants.USEFEATURES, false)) {
				index = FEATURE_SELECTION;
			} else if (!configuration.getAttribute(IPDELauncherConstants.USE_DEFAULT, true)) {
				index = CUSTOM_SELECTION;
			}
			fSelectionCombo.setText(fSelectionCombo.getItem(index));
			boolean custom = fSelectionCombo.getSelectionIndex() == CUSTOM_SELECTION;
			fPluginBlock.initializeFrom(configuration, custom);
			boolean auto = configuration.getAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, false);
			fDefaultAutoStart.setText(Boolean.toString(auto));
			int level = configuration.getAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
			fDefaultStartLevel.setSelection(level);
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.USE_DEFAULT, true);
		if (fShowFeatures)
			configuration.setAttribute(IPDELauncherConstants.USEFEATURES, false);
		fPluginBlock.setDefaults(configuration);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		int index = fSelectionCombo.getSelectionIndex();
		configuration.setAttribute(IPDELauncherConstants.USE_DEFAULT, index == DEFAULT_SELECTION);
		if (fShowFeatures)
			configuration.setAttribute(IPDELauncherConstants.USEFEATURES, index == FEATURE_SELECTION);
		fPluginBlock.performApply(configuration);
		configuration.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, Boolean.toString(true).equals(fDefaultAutoStart.getText()));
		configuration.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, fDefaultStartLevel.getSelection());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return PDEUIMessages.AdvancedLauncherTab_name;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/**
	 * Validates the tab.  If the feature option is chosen, and the workspace is not correctly set up,
	 * the error message is set.
	 * 
	 * @see org.eclipse.pde.ui.launcher.AbstractLauncherTab#validateTab()
	 */
	public void validateTab() {
		String errorMessage = null;
		if (fShowFeatures && fSelectionCombo.getSelectionIndex() == FEATURE_SELECTION) {
			IPath workspacePath = PDEPlugin.getWorkspace().getRoot().getLocation();
			IPath featurePath = workspacePath.removeLastSegments(1).append("features"); //$NON-NLS-1$
			if (!workspacePath.lastSegment().equalsIgnoreCase("plugins") //$NON-NLS-1$
					|| !featurePath.toFile().exists())
				errorMessage = PDEUIMessages.AdvancedLauncherTab_error_featureSetup;
		}
		setErrorMessage(errorMessage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 */
	public String getId() {
		return IPDELauncherConstants.TAB_PLUGINS_ID;
	}
}
