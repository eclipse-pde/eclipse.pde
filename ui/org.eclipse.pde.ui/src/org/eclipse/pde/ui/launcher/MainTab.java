/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 171767
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * A launch configuration tab that displays and edits the main launching arguments
 * of an Eclipse application.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed by clients.
 * </p>
 * @since 3.2
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class MainTab extends AbstractLauncherTab implements IPDELauncherConstants {

	protected WorkspaceDataBlock fDataBlock;
	protected ProgramBlock fProgramBlock;
	protected JREBlock fJreBlock;

	private Image fImage;

	/**
	 * Contructor to create a new main tab
	 */
	public MainTab() {
		createWorkspaceDataBlock();
		createProgramBlock();
		createJREBlock();
		fImage = PDEPluginImages.DESC_MAIN_TAB.createImage();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		fImage.dispose();
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		final ScrolledComposite scrollContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		scrollContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite composite = new Composite(scrollContainer, SWT.NONE);
		scrollContainer.setContent(composite);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fDataBlock.createControl(composite);
		fProgramBlock.createControl(composite);
		fJreBlock.createControl(composite);

		// Add listener for each control to recalculate scroll bar when it is entered.
		// This results in scrollbar scrolling when user tabs to a control that is not in the field of view.
		Listener listener = new Listener() {
			public void handleEvent(Event e) {
				Control child = (Control) e.widget;
				Rectangle bounds = child.getBounds();
				Rectangle area = scrollContainer.getClientArea();
				Point origin = scrollContainer.getOrigin();
				if (origin.x > bounds.x)
					origin.x = Math.max(0, bounds.x);
				if (origin.y > bounds.y)
					origin.y = Math.max(0, bounds.y);
				if (origin.x + area.width < bounds.x + bounds.width)
					origin.x = Math.max(0, bounds.x + bounds.width - area.width);
				if (origin.y + area.height < bounds.y + bounds.height)
					origin.y = Math.max(0, bounds.y + bounds.height - area.height);
				scrollContainer.setOrigin(origin);
			}
		};
		Control[] controls = composite.getChildren();
		for (int i = 0; i < controls.length; i++)
			controls[i].addListener(SWT.Activate, listener);

		Dialog.applyDialogFont(composite);
		composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scrollContainer.setExpandHorizontal(true);
		setControl(scrollContainer);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LAUNCHER_BASIC);
	}

	/**
	 * Applies the given data to this page.
	 * 
	 * @param data the data to apply
	 * @since 3.7
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void applyData(Object data) {
		if (data == LOCATION)
			fDataBlock.selectWorkspaceLocation();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			fDataBlock.initializeFrom(config, false);
			fProgramBlock.initializeFrom(config);
			fJreBlock.initializeFrom(config);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} finally {
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		fDataBlock.setDefaults(config, false);
		fProgramBlock.setDefaults(config);
		fJreBlock.setDefaults(config);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		fDataBlock.performApply(config, false);
		fProgramBlock.performApply(config);
		fJreBlock.performApply(config);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return PDEUIMessages.MainTab_name;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/**
	 * Creates the Workspace Data group on the tab
	 */
	protected void createWorkspaceDataBlock() {
		fDataBlock = new WorkspaceDataBlock(this);
	}

	/**
	 * Creates the Program To Run group on the tab
	 *
	 */
	protected void createProgramBlock() {
		fProgramBlock = new ProgramBlock(this);
	}

	/**
	 * Creates the Java Runtime Environment group on the tab
	 *
	 * @since 3.4
	 */
	protected void createJREBlock() {
		fJreBlock = new JREBlock(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractLauncherTab#validateTab()
	 */
	public void validateTab() {
		String error = fDataBlock.validate();
		if (error == null)
			error = fJreBlock.validate();
		setErrorMessage(error);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 */
	public String getId() {
		return IPDELauncherConstants.TAB_MAIN_ID;
	}

}
